/*
 * Copyright (C) 2022-2023 DE4A, www.de4a.eu
 * Author: philip[at]helger[dot]com
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

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.mime.CMimeType;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.api.IAPIExecutor;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

public class ApiClearSmpClientCache implements IAPIExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ApiClearSmpClientCache.class);

  public void invokeAPI (final IAPIDescriptor aAPIDescriptor,
                         final String sPath,
                         final Map <String, String> aPathVariables,
                         final IRequestWebScopeWithoutResponse aRequestScope,
                         final UnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sLogPrefix = "[IAL-" + ApiGetGetAllDOs.COUNTER.incrementAndGet () + "] ";

    LOGGER.info (sLogPrefix + "Clearing IAL cache");

    IALCache.clearCache ();
    aUnifiedResponse.setContentAndCharset ("IAL caches was cleared", StandardCharsets.UTF_8)
                    .setMimeType (CMimeType.TEXT_PLAIN);
  }
}
