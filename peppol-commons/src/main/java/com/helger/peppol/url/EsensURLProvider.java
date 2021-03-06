/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Version: MPL 1.1/EUPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at:
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL
 * (the "Licence"); You may not use this work except in compliance
 * with the Licence.
 * You may obtain a copy of the Licence at:
 * http://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * If you wish to allow use of your version of this file only
 * under the terms of the EUPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the EUPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the EUPL License.
 */
package com.helger.peppol.url;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.xbill.DNS.TextParseException;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.charset.CharsetManager;
import com.helger.commons.codec.Base32Codec;
import com.helger.commons.collection.ext.CommonsHashMap;
import com.helger.commons.collection.ext.ICommonsMap;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.string.StringHelper;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.utils.NAPTRResolver;
import com.helger.security.messagedigest.EMessageDigestAlgorithm;
import com.helger.security.messagedigest.MessageDigestValue;

/**
 * The default implementation of {@link IPeppolURLProvider} suitable for the
 * E-SENS network. See e.g. http://wiki.ds.unipi.gr/display/ESENS/PR+-+BDXL<br>
 * Layout:
 * <code>strip-trailing(base32(sha256(lowercase(ID-VALUE))),"=")+"."+ID-SCHEME+"."+SML-ZONE-NAME</code>
 *
 * @author Philip Helger
 */
@ThreadSafe
public class EsensURLProvider implements IPeppolURLProvider
{
  public static final EsensURLProvider MUTABLE_INSTANCE = new EsensURLProvider ();
  public static final IPeppolURLProvider INSTANCE = MUTABLE_INSTANCE;
  public static final Charset URL_CHARSET = StandardCharsets.UTF_8;
  public static final Locale URL_LOCALE = Locale.US;

  private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
  private boolean m_bLowercaseValueBeforeHashing = true;
  private final ICommonsMap <String, String> m_aDNSCache = new CommonsHashMap<> ();
  private boolean m_bUseDNSCache = true;

  /**
   * Default constructor.
   */
  public EsensURLProvider ()
  {}

  public boolean isLowercaseValueBeforeHashing ()
  {
    return m_aRWLock.readLocked ( () -> m_bLowercaseValueBeforeHashing);
  }

  public void setLowercaseValueBeforeHashing (final boolean bLowercaseValueBeforeHashing)
  {
    m_aRWLock.writeLocked ( () -> m_bLowercaseValueBeforeHashing = bLowercaseValueBeforeHashing);
  }

  /**
   * @return <code>true</code> if internal DNS caching is enabled,
   *         <code>false</code> if not. By default it is enabled.
   */
  public boolean isUseDNSCache ()
  {
    return m_aRWLock.readLocked ( () -> m_bUseDNSCache);
  }

  /**
   * Enable or disable internal DNS caching. By default it is enabled.
   *
   * @param bUseDNSCache
   *        <code>true</code> to enable caching, <code>false</code> to disable
   *        it.
   */
  public void setUseDNSCache (final boolean bUseDNSCache)
  {
    m_aRWLock.writeLocked ( () -> m_bUseDNSCache = bUseDNSCache);
  }

  /**
   * Remove all internal DNS cache entries.
   */
  public void clearDNSCache ()
  {
    m_aRWLock.writeLocked ( () -> m_aDNSCache.clear ());
  }

  /**
   * @return A copy of all entries currently in the cache. Never
   *         <code>null</code> but maybe empty.
   */
  @Nonnull
  @ReturnsMutableCopy
  public ICommonsMap <String, String> getAllCacheEntries ()
  {
    return m_aRWLock.readLocked ( () -> m_aDNSCache.getClone ());
  }

  /**
   * Add entries to the cache. This might be helpful when there is a persistent
   * cache (outside this class) and the old cache entries should be re-added.
   *
   * @param aEntries
   *        The entries to be added. May be <code>null</code>.
   */
  public void addCacheEntries (@Nullable final Map <String, String> aEntries)
  {
    if (aEntries != null && !aEntries.isEmpty ())
      m_aRWLock.writeLocked ( () -> m_aDNSCache.putAll (aEntries));
  }

  /**
   * Get the Base32 encoded, SHA-256 hash-string-representation of the passed
   * value using the {@link #URL_CHARSET} encoding.
   *
   * @param sValueToHash
   *        The value to be hashed. May not be <code>null</code>.
   * @return The non-<code>null</code> String containing the hash value.
   */
  @Nonnull
  public static String getHashValueStringRepresentation (@Nonnull final String sValueToHash)
  {
    final byte [] aMessageDigest = MessageDigestValue.create (CharsetManager.getAsBytes (sValueToHash, URL_CHARSET),
                                                              EMessageDigestAlgorithm.SHA_256)
                                                     .getAllDigestBytes ();
    return new Base32Codec ().setAddPaddding (false).getEncodedAsString (aMessageDigest, StandardCharsets.ISO_8859_1);
  }

  @Nonnull
  public String getDNSNameOfParticipant (@Nonnull final IParticipantIdentifier aParticipantIdentifier,
                                         @Nullable final String sSMLZoneName)
  {
    return getDNSNameOfParticipant (aParticipantIdentifier, sSMLZoneName, true);
  }

  @Nonnull
  public String getDNSNameOfParticipant (@Nonnull final IParticipantIdentifier aParticipantIdentifier,
                                         @Nullable final String sSMLZoneName,
                                         final boolean bDoNAPTRResolving)
  {
    // Use the custom SMK/SML DNS server first
    return getDNSNameOfParticipant (aParticipantIdentifier, sSMLZoneName, bDoNAPTRResolving, "ns1lux.europa.eu");
  }

  @Nonnull
  public String getDNSNameOfParticipant (@Nonnull final IParticipantIdentifier aParticipantIdentifier,
                                         @Nullable final String sSMLZoneName,
                                         final boolean bDoNAPTRResolving,
                                         @Nullable final String sPrimaryDNSServer)
  {
    ValueEnforcer.notNull (aParticipantIdentifier, "ParticipantIdentifier");

    // Ensure the DNS zone name ends with a dot!
    if (StringHelper.hasText (sSMLZoneName) && !StringHelper.endsWith (sSMLZoneName, '.'))
      throw new IllegalArgumentException ("if an SML zone name is specified, it must end with a dot (.). Value is: " +
                                          sSMLZoneName);

    final StringBuilder ret = new StringBuilder ();

    // Get the identifier value
    // Important: create hash from lowercase string!
    String sIdentifierValue = aParticipantIdentifier.getValue ();
    if (m_bLowercaseValueBeforeHashing)
      sIdentifierValue = sIdentifierValue.toLowerCase (URL_LOCALE);
    ret.append (getHashValueStringRepresentation (sIdentifierValue)).append ('.');

    // append the identifier scheme
    if (aParticipantIdentifier.hasScheme ())
    {
      // Check identifier scheme (must be lowercase for the URL later on!)
      String sIdentifierScheme = aParticipantIdentifier.getScheme ();
      if (m_bLowercaseValueBeforeHashing)
        sIdentifierScheme = sIdentifierScheme.toLowerCase (URL_LOCALE);
      ret.append (sIdentifierScheme).append ('.');
    }

    // append the SML DNS zone name (if available)
    if (StringHelper.hasText (sSMLZoneName))
    {
      // If it is present, it always ends with a dot
      ret.append (sSMLZoneName);
    }

    // in some cases it gives a problem later when trying to retrieve the
    // participant's metadata ex:
    // http://B-51538b9890f1999ca08302c65f544719.iso6523-actorid-upis.sml.peppolcentral.org./iso6523-actorid-upis%3A%3A9917%3A550403315099
    // That's why we cut of the dot here
    ret.deleteCharAt (ret.length () - 1);
    final String sBuildName = ret.toString ();

    if (!bDoNAPTRResolving)
      return sBuildName;

    final boolean bUseDNSCache = isUseDNSCache ();
    try
    {
      // Already in cache?
      String sResolvedNAPTR = bUseDNSCache ? m_aRWLock.readLocked ( () -> m_aDNSCache.get (sBuildName)) : null;
      if (sResolvedNAPTR == null)
      {
        // Now do the NAPTR resolving
        sResolvedNAPTR = NAPTRResolver.resolveFromNAPTR (sBuildName, sPrimaryDNSServer);
        if (sResolvedNAPTR == null)
          throw new IllegalArgumentException ("Failed to resolve '" + sBuildName + "'");

        // Strip any special protocol prefix
        sResolvedNAPTR = StringHelper.trimStart (sResolvedNAPTR, "http://");
        sResolvedNAPTR = StringHelper.trimStart (sResolvedNAPTR, "https://");

        if (bUseDNSCache)
        {
          // Put in cache
          final String sFinalResolved = sResolvedNAPTR;
          m_aRWLock.writeLocked ( () -> m_aDNSCache.put (sBuildName, sFinalResolved));
        }
      }
      return sResolvedNAPTR;
    }
    catch (final TextParseException ex)
    {
      throw new IllegalStateException ("Failed to parse '" + sBuildName + "'", ex);
    }
  }
}
