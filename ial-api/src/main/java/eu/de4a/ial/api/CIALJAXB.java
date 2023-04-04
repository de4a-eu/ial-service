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

import javax.annotation.Nonnull;

import com.helger.commons.io.resource.ClassPathResource;

/**
 * Constants for handling IAL stuff
 *
 * @author Philip Helger
 */
public final class CIALJAXB
{
  @Nonnull
  private static ClassLoader _getCL ()
  {
    return CIALJAXB.class.getClassLoader ();
  }

  public static final String NAMESPACE_URI_IAL = "http://www.de4a.eu/2020/ial/v2";
  public static final ClassPathResource XSD_IAL = new ClassPathResource ("schemas/IAL.xsd", _getCL ());

  private CIALJAXB ()
  {}
}
