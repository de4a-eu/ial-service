/*
 * Copyright (C) 2023, Partners of the EU funded DE4A project consortium
 *   (https://www.de4a.eu/consortium), under Grant Agreement No.870635
 * Author: Austrian Federal Computing Center (BRZ)
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
package eu.de4a.ial.api;

import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.xml.bind.JAXBElement;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.jaxb.GenericJAXBMarshaller;

import eu.de4a.ial.api.jaxb.ResponseLookupRoutingInformationType;

/**
 * IAL Marshaller factory
 *
 * @author Philip Helger
 * @param <JAXBTYPE>
 *        The JAXB implementation type
 */
@Immutable
public class IALMarshaller <JAXBTYPE> extends GenericJAXBMarshaller <JAXBTYPE>
{
  public IALMarshaller (@Nonnull final Class <JAXBTYPE> aType,
                        @Nullable final List <? extends ClassPathResource> aXSDs,
                        @Nonnull final Function <? super JAXBTYPE, ? extends JAXBElement <JAXBTYPE>> aJAXBElementWrapper)
  {
    super (aType, aXSDs, aJAXBElementWrapper);
    setNamespaceContext (IALNamespaceContext.getInstance ());
  }

  /**
   * Enable formatted output. Syntactic sugar.
   *
   * @return this for chaining
   */
  @Nonnull
  public final IALMarshaller <JAXBTYPE> formatted ()
  {
    setFormattedOutput (true);
    return this;
  }

  /**
   * @return A new marshaller for the response. Never <code>null</code>.
   */
  @Nonnull
  public static IALMarshaller <ResponseLookupRoutingInformationType> responseLookupRoutingInformationMarshaller ()
  {
    return new IALMarshaller <> (ResponseLookupRoutingInformationType.class,
                                 new CommonsArrayList <> (CIALJAXB.XSD_IAL),
                                 new eu.de4a.ial.api.jaxb.ObjectFactory ()::createResponseLookupRoutingInformation);
  }
}
