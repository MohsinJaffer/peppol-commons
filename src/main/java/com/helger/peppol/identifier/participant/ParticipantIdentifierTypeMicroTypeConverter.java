/**
 * Copyright (C) 2014-2015 Philip Helger (www.helger.com)
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
package com.helger.peppol.identifier.participant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotations.Nonempty;
import com.helger.commons.microdom.IMicroElement;
import com.helger.commons.microdom.convert.IMicroTypeConverter;
import com.helger.commons.microdom.impl.MicroElement;
import com.helger.peppol.identifier.ParticipantIdentifierType;

public final class ParticipantIdentifierTypeMicroTypeConverter implements IMicroTypeConverter
{
  private static final String ATTR_SCHEME = "scheme";
  private static final String ATTR_VALUE = "value";

  @Nonnull
  public IMicroElement convertToMicroElement (@Nonnull final Object aObject,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull @Nonempty final String sTagName)
  {
    final ParticipantIdentifierType aValue = (ParticipantIdentifierType) aObject;
    final IMicroElement aElement = new MicroElement (sNamespaceURI, sTagName);
    aElement.setAttribute (ATTR_SCHEME, aValue.getScheme ());
    aElement.setAttribute (ATTR_VALUE, aValue.getValue ());
    return aElement;
  }

  @Nonnull
  public ParticipantIdentifierType convertToNative (@Nonnull final IMicroElement aElement)
  {
    final String sScheme = aElement.getAttributeValue (ATTR_SCHEME);
    final String sValue = aElement.getAttributeValue (ATTR_VALUE);
    final ParticipantIdentifierType aPI = new ParticipantIdentifierType ();
    aPI.setScheme (sScheme);
    aPI.setValue (sValue);
    return aPI;
  }
}
