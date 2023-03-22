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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * HTTP 400 (Bad Request) exception wrapper
 *
 * @author Philip Helger
 */
public class IALBadRequestException extends Exception
{
  /**
   * Create a HTTP 400 (Bad request) exception.
   *
   * @param sMessage
   *        the String that is the entity of the HTTP response.
   * @param aRequest
   *        The request that caused the error.
   */
  public IALBadRequestException (@Nonnull final String sMessage,
                                 @Nullable final IRequestWebScopeWithoutResponse aRequest)
  {
    super ("Bad request: " +
           sMessage +
           (aRequest == null ? "" : " at '" + aRequest.getRequestURLEncoded ().toString () + "'"));
  }
}
