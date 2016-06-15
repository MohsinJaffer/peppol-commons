/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.peppol.utils;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.UnrecoverableKeyException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.lang.ClassHelper;
import com.helger.commons.state.ISuccessIndicator;
import com.helger.commons.string.StringHelper;

/**
 * This class contains the result of loading the configured private key as
 * configured in the configuration file.
 *
 * @author Philip Helger
 */
public final class LoadedKey implements ISuccessIndicator
{
  private final PrivateKeyEntry m_aKeyEntry;
  private final String m_sErrorMessage;

  private LoadedKey (@Nullable final PrivateKeyEntry aKeyEntry, @Nullable final String sErrorMessage)
  {
    m_aKeyEntry = aKeyEntry;
    m_sErrorMessage = sErrorMessage;
  }

  public boolean isSuccess ()
  {
    return m_aKeyEntry != null;
  }

  /**
   * @return The loaded key entry. Never <code>null</code> in case of success.
   *         Always <code>null</code> in case of failure.
   */
  @Nullable
  public PrivateKeyEntry getKeyEntry ()
  {
    return m_aKeyEntry;
  }

  /**
   * @return The error message. Never <code>null</code> in case of failure.
   *         Always <code>null</code> in case of success.
   */
  @Nullable
  public String getErrorMessage ()
  {
    return m_sErrorMessage;
  }

  /**
   * Load the private key entry from the keystore.
   *
   * @param aKeyStore
   *        The keystore to load the key from. May not be <code>null</code>.
   * @param sKeyStorePath
   *        Keystore path. For nice error messages only. Should not be
   *        <code>null</code>.
   * @param sKeyStoreKeyAlias
   *        The alias to be resolved in the keystore. Must be non-
   *        <code>null</code> to succeed.
   * @param aKeyStoreKeyPassword
   *        The key password for the keystore. Must be non-<code>null</code> to
   *        succeed.
   * @return The key loading result. Never <code>null</code>.
   */
  @Nonnull
  public static LoadedKey loadKey (@Nonnull final KeyStore aKeyStore,
                                   @Nonnull final String sKeyStorePath,
                                   @Nullable final String sKeyStoreKeyAlias,
                                   @Nullable final char [] aKeyStoreKeyPassword)
  {
    if (StringHelper.hasNoText (sKeyStoreKeyAlias))
      return new LoadedKey (null, "No keystore key alias is defined in the configuration file.");

    if (aKeyStoreKeyPassword == null)
      return new LoadedKey (null, "No keystore key password is defined in the configuration file.");

    // Try to load the key.
    KeyStore.PrivateKeyEntry aKeyEntry = null;
    try
    {
      final KeyStore.Entry aEntry = aKeyStore.getEntry (sKeyStoreKeyAlias,
                                                        new KeyStore.PasswordProtection (aKeyStoreKeyPassword));
      if (aEntry == null)
      {
        // No such entry
        return new LoadedKey (null,
                              "The keystore key alias '" +
                                    sKeyStoreKeyAlias +
                                    "' was not found in keystore '" +
                                    sKeyStorePath +
                                    "'.");
      }
      if (!(aEntry instanceof KeyStore.PrivateKeyEntry))
      {
        // Not a private key
        return new LoadedKey (null,
                              "The keystore key alias '" +
                                    sKeyStoreKeyAlias +
                                    "' was found in keystore '" +
                                    sKeyStorePath +
                                    "' but it is not a private key! The internal type is " +
                                    ClassHelper.getClassName (aEntry));
      }

      aKeyEntry = (KeyStore.PrivateKeyEntry) aEntry;
    }
    catch (final UnrecoverableKeyException ex)
    {
      return new LoadedKey (null,
                            "Failed to load key with alias '" +
                                  sKeyStoreKeyAlias +
                                  "' from keystore at '" +
                                  sKeyStorePath +
                                  "'. Seems like the password for the key is invalid. Technical details: " +
                                  ex.getMessage ());
    }
    catch (final GeneralSecurityException ex)
    {
      return new LoadedKey (null,
                            "Failed to load key with alias '" +
                                  sKeyStoreKeyAlias +
                                  "' from keystore at '" +
                                  sKeyStorePath +
                                  "'. Technical details: " +
                                  ex.getMessage ());
    }

    // Finally success
    return new LoadedKey (aKeyEntry, null);
  }
}
