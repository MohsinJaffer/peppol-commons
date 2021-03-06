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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Test;
import org.xbill.DNS.Name;
import org.xbill.DNS.TextParseException;

import com.helger.peppol.identifier.factory.IIdentifierFactory;
import com.helger.peppol.identifier.factory.PeppolIdentifierFactory;
import com.helger.peppol.identifier.factory.SimpleIdentifierFactory;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.identifier.generic.participant.SimpleParticipantIdentifier;
import com.helger.peppol.identifier.peppol.PeppolIdentifierHelper;
import com.helger.peppol.sml.ESML;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Test class for class {@link PeppolURLProvider}.
 *
 * @author Philip Helger
 */
public final class PeppolURLProviderTest
{
  private static final IPeppolURLProvider INSTANCE = PeppolURLProvider.INSTANCE;
  private static final IIdentifierFactory IF = PeppolIdentifierFactory.INSTANCE;

  @Test
  @SuppressFBWarnings ("NP_NONNULL_PARAM_VIOLATION")
  public void testGetDNSNameOfParticipant ()
  {
    assertEquals ("B-f5e78500450d37de5aabe6648ac3bb70.iso6523-actorid-upis.edelivery.tech.ec.europa.eu",
                  INSTANCE.getDNSNameOfParticipant (IF.createParticipantIdentifierWithDefaultScheme ("0088:123abc"),
                                                    ESML.DIGIT_PRODUCTION));
    // Same value but different casing
    assertEquals ("B-f5e78500450d37de5aabe6648ac3bb70.iso6523-actorid-upis.edelivery.tech.ec.europa.eu",
                  INSTANCE.getDNSNameOfParticipant (IF.createParticipantIdentifierWithDefaultScheme ("0088:123ABC"),
                                                    ESML.DIGIT_PRODUCTION));

    assertEquals ("B-85008b8279e07ab0392da75fa55856a2.iso6523-actorid-upis.acc.edelivery.tech.ec.europa.eu",
                  INSTANCE.getDNSNameOfParticipant (IF.createParticipantIdentifierWithDefaultScheme ("9915:test"),
                                                    ESML.DIGIT_TEST));

    // No identifier scheme
    assertEquals ("B-f5e78500450d37de5aabe6648ac3bb70.edelivery.tech.ec.europa.eu",
                  INSTANCE.getDNSNameOfParticipant (SimpleIdentifierFactory.INSTANCE.createParticipantIdentifier (null,
                                                                                                                  "0088:123abc"),
                                                    ESML.DIGIT_PRODUCTION));

    // No identifier value
    assertEquals ("B-d41d8cd98f00b204e9800998ecf8427e.iso6523-actorid-upis.edelivery.tech.ec.europa.eu",
                  INSTANCE.getDNSNameOfParticipant (SimpleIdentifierFactory.INSTANCE.createParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME,
                                                                                                                  null),
                                                    ESML.DIGIT_PRODUCTION));

    // No identifier scheme and value
    assertEquals ("B-d41d8cd98f00b204e9800998ecf8427e.edelivery.tech.ec.europa.eu",
                  INSTANCE.getDNSNameOfParticipant (SimpleIdentifierFactory.INSTANCE.createParticipantIdentifier (null,
                                                                                                                  null),
                                                    ESML.DIGIT_PRODUCTION));

    // No identifier scheme and value and no SML zone
    assertEquals ("B-d41d8cd98f00b204e9800998ecf8427e",
                  INSTANCE.getDNSNameOfParticipant (SimpleIdentifierFactory.INSTANCE.createParticipantIdentifier (null,
                                                                                                                  null),
                                                    (String) null));

    // Wildcard
    assertEquals ("*.iso6523-actorid-upis.edelivery.tech.ec.europa.eu",
                  INSTANCE.getDNSNameOfParticipant (IF.createParticipantIdentifierWithDefaultScheme ("*"),
                                                    ESML.DIGIT_PRODUCTION));

    // Empty DNS zone
    assertEquals ("B-f5e78500450d37de5aabe6648ac3bb70.iso6523-actorid-upis",
                  INSTANCE.getDNSNameOfParticipant (IF.createParticipantIdentifierWithDefaultScheme ("0088:123ABC"),
                                                    (String) null));
    assertEquals ("B-f5e78500450d37de5aabe6648ac3bb70.iso6523-actorid-upis",
                  INSTANCE.getDNSNameOfParticipant (IF.createParticipantIdentifierWithDefaultScheme ("0088:123ABC"),
                                                    ""));

    // Very simple zone
    assertEquals ("B-f5e78500450d37de5aabe6648ac3bb70.iso6523-actorid-upis.at",
                  INSTANCE.getDNSNameOfParticipant (IF.createParticipantIdentifierWithDefaultScheme ("0088:123ABC"),
                                                    "at."));

    if (false)
      System.out.println (INSTANCE.getDNSNameOfParticipant (IF.createParticipantIdentifierWithDefaultScheme ("9915:b"),
                                                            ESML.DIGIT_PRODUCTION));

    // Test invalid
    try
    {
      INSTANCE.getDNSNameOfParticipant (null, "anyzone.org.");
      fail ();
    }
    catch (final NullPointerException ex)
    {
      // expected
    }

    try
    {
      // Invalid DNS zone (missing dot)
      INSTANCE.getDNSNameOfParticipant (new SimpleParticipantIdentifier ("scheme", "value"), "anyzone");
      fail ();
    }
    catch (final IllegalArgumentException ex)
    {
      // expected
    }
  }

  @Test
  public void testGetDNSNameOfParticipantWithDNSName () throws TextParseException
  {
    // The first part must always end with a DOT
    Name aName = Name.fromString ("B-f5e78500450d37de5aabe6648ac3bb70.iso6523-actorid-upis.sml.peppolcentral.org.",
                                  Name.fromString ("sml.peppolcentral.org."));
    assertEquals ("B-f5e78500450d37de5aabe6648ac3bb70.iso6523-actorid-upis.sml.peppolcentral.org.", aName.toString ());

    aName = Name.fromString ("B-f5e78500450d37de5aabe6648ac3bb70.iso6523-actorid-upis.sml.peppolcentral.org.",
                             Name.fromString ("sml.peppolcentral.org"));
    assertEquals ("B-f5e78500450d37de5aabe6648ac3bb70.iso6523-actorid-upis.sml.peppolcentral.org.", aName.toString ());
  }

  @Test
  public void testGetSMPURIOfParticipant () throws URISyntaxException, MalformedURLException
  {
    final IParticipantIdentifier aPI = IF.createParticipantIdentifierWithDefaultScheme ("0088:123ABC");
    final URI aURI = INSTANCE.getSMPURIOfParticipant (aPI, ESML.DIGIT_PRODUCTION);
    assertEquals (new URI ("http://B-f5e78500450d37de5aabe6648ac3bb70.iso6523-actorid-upis.edelivery.tech.ec.europa.eu"),
                  aURI);

    final URL aURL = INSTANCE.getSMPURLOfParticipant (aPI, ESML.DIGIT_PRODUCTION);
    assertEquals (new URL ("http://B-f5e78500450d37de5aabe6648ac3bb70.iso6523-actorid-upis.edelivery.tech.ec.europa.eu"),
                  aURL);
  }
}
