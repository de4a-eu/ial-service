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
package eu.de4a.ial.webapp.config;

import java.security.GeneralSecurityException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.httpclient.HttpClientSettings;

public final class IALHttpClientSettings extends HttpClientSettings
{
  private static final Logger LOGGER = LoggerFactory.getLogger (IALHttpClientSettings.class);

  public IALHttpClientSettings () throws GeneralSecurityException
  {
    if (IALConfig.Directory.isTLSTrustAll ())
    {
      LOGGER.warn ("The TLS connection trusts all certificates. That is not very secure.");
      // This block is not nice but needed, because the system truststore of the
      // machine running the IAL is empty.
      // For a real production scenario, a separate trust store should be
      // configured.
      setSSLContextTrustAll ();
    }
  }
}
