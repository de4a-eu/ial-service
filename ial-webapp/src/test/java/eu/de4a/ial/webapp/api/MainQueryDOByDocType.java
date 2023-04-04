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
package eu.de4a.ial.webapp.api;

import java.io.IOException;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.helger.httpclient.HttpClientManager;
import com.helger.httpclient.HttpClientSettings;
import com.helger.httpclient.response.ResponseHandlerXml;
import com.helger.xml.serialize.write.XMLWriter;

public final class MainQueryDOByDocType
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainQueryDOByDocType.class);

  public static void main (final String [] args) throws IOException
  {
    final HttpClientSettings aHCS = new HttpClientSettings ().setResponseTimeout (Timeout.ofSeconds (30));
    try (final HttpClientManager aHCM = HttpClientManager.create (aHCS))
    {
      final HttpGet aGet = new HttpGet ("http://localhost:8080/api/provision/urn:de4a-eu:CanonicalEvidenceType::BirthCertificate:1.0");
      final Document aXML = aHCM.execute (aGet, new ResponseHandlerXml (false));
      LOGGER.info (XMLWriter.getNodeAsString (aXML));
    }
  }
}
