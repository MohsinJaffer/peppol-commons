/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
import java.util.Locale;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.NAPTRRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.charset.CCharset;
import com.helger.commons.charset.CharsetManager;
import com.helger.commons.codec.Base32Codec;
import com.helger.commons.collection.ext.CommonsArrayList;
import com.helger.commons.collection.ext.ICommonsList;
import com.helger.commons.regex.RegExHelper;
import com.helger.commons.string.StringHelper;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
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
@Immutable
public class EsensURLProvider implements IPeppolURLProvider
{
  public static final IPeppolURLProvider INSTANCE = new EsensURLProvider ();
  public static final Charset URL_CHARSET = CCharset.CHARSET_UTF_8_OBJ;
  public static final Locale URL_LOCALE = Locale.US;

  private static final Logger s_aLogger = LoggerFactory.getLogger (EsensURLProvider.class);

  private boolean m_bLowercaseValueBeforeHashing = true;

  /**
   * Default constructor.
   */
  public EsensURLProvider ()
  {}

  public boolean isLowercaseValueBeforeHashing ()
  {
    return m_bLowercaseValueBeforeHashing;
  }

  public void setLowercaseValueBeforeHashing (final boolean bLowercaseValueBeforeHashing)
  {
    m_bLowercaseValueBeforeHashing = bLowercaseValueBeforeHashing;
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
    return new Base32Codec ().setAddPaddding (false).getEncodedAsString (aMessageDigest,
                                                                         CCharset.CHARSET_ISO_8859_1_OBJ);
  }

  @Nullable
  private static String _getAppliedNAPTRRegEx (@Nonnull final String sRegEx, @Nonnull final String sDomainName)
  {
    final char cSep = sRegEx.charAt (0);
    final int nSecond = sRegEx.indexOf (cSep, 1);
    if (nSecond < 0)
      return null;
    final String sEre = sRegEx.substring (1, nSecond);
    final int nThird = sRegEx.indexOf (cSep, nSecond + 1);
    if (nThird < 0)
      return null;
    final String sRepl = sRegEx.substring (nSecond + 1, nThird);
    final String sFlags = sRegEx.substring (nThird + 1);

    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("NAPTR regex: '" + sEre + "' - '" + sRepl + "' - '" + sFlags + "'");

    final int nOptions = "i".equalsIgnoreCase (sFlags) ? Pattern.CASE_INSENSITIVE : 0;
    final String ret = RegExHelper.stringReplacePattern (sEre, nOptions, sDomainName, sRepl);

    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("  NAPTR replacement: '" + sDomainName + "' -> '" + ret + "'");
    return ret;
  }

  @Nullable
  private static String _resolveFromNAPTR (@Nonnull final String sDNSName) throws TextParseException
  {
    if (StringHelper.hasNoText (sDNSName))
      return null;

    final Lookup aLookup = new Lookup (sDNSName, Type.NAPTR);
    Record [] aRecords;
    do
    {
      aRecords = aLookup.run ();
    } while (aLookup.getResult () == Lookup.TRY_AGAIN);

    if (aLookup.getResult () != Lookup.SUCCESSFUL)
    {
      // Wrong domain name
      s_aLogger.warn ("Error looking up '" + sDNSName + "': " + aLookup.getErrorString ());
      return null;
    }

    final ICommonsList <NAPTRRecord> aMatchingRecords = new CommonsArrayList <> ();
    for (final Record record : aRecords)
    {
      final NAPTRRecord naptrRecord = (NAPTRRecord) record;
      if ("U".equalsIgnoreCase (naptrRecord.getFlags ()) && "Meta:SMP".equals (naptrRecord.getService ()))
        aMatchingRecords.add (naptrRecord);
    }

    if (aMatchingRecords.isEmpty ())
    {
      // No matching NAPTR present
      s_aLogger.warn ("No matching DNS NAPTR records returned for '" + sDNSName + "'");
      return null;
    }

    // Sort by order than by preference according to RFC 2915
    aMatchingRecords.sort ( (x, y) -> {
      int ret = x.getOrder () - y.getOrder ();
      if (ret == 0)
        ret = x.getPreference () - y.getPreference ();
      return ret;
    });
    for (final NAPTRRecord aRecord : aMatchingRecords)
    {
      // The "U" record is terminal, so a RegExp must be present
      final String sRegEx = aRecord.getRegexp ();
      // At least 3 separator chars must be present :)
      if (StringHelper.getLength (sRegEx) > 3)
      {
        final String sFinalDNSName = _getAppliedNAPTRRegEx (sRegEx, sDNSName);
        if (sFinalDNSName != null)
        {
          if (s_aLogger.isDebugEnabled ())
            s_aLogger.debug ("Using '" + sFinalDNSName + "' for original DNS name '" + sDNSName + "'");
          return sFinalDNSName;
        }
      }
    }

    // Weird - no regexp present
    s_aLogger.warn ("None of the matching DNS NAPTR records for '" +
                    sDNSName +
                    "' has a valid regular expression. Details: " +
                    aMatchingRecords);
    return null;
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

    try
    {
      // Now do the NAPTR resolving
      final String sResolvedNAPTR = _resolveFromNAPTR (sBuildName);
      if (sResolvedNAPTR == null)
        throw new IllegalArgumentException ("Failed to resolve '" + sBuildName + "'");
      return sResolvedNAPTR;
    }
    catch (final TextParseException ex)
    {
      throw new IllegalStateException ("Failed to parse '" + sBuildName + "'", ex);
    }
  }
}
