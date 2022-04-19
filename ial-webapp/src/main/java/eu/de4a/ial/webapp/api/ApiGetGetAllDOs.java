/*
 * Copyright (C) 2022 DE4A, www.de4a.eu
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

import java.util.Map;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.CommonsLinkedHashSet;
import com.helger.commons.collection.impl.ICommonsOrderedSet;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.string.StringHelper;
import com.helger.commons.timing.StopWatch;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.api.IAPIExecutor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.serialize.write.XMLWriterSettings;

import eu.de4a.ial.api.IALMarshaller;
import eu.de4a.ial.api.jaxb.ErrorType;
import eu.de4a.ial.api.jaxb.ResponseLookupRoutingInformationType;

/**
 * Provide the public query API
 *
 * @author Philip Helger
 */
public class ApiGetGetAllDOs implements IAPIExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ApiGetGetAllDOs.class);

  private final boolean m_bWithATUCode;

  public ApiGetGetAllDOs (final boolean bWithATUCode)
  {
    m_bWithATUCode = bWithATUCode;
  }

  @Nonnull
  private static ErrorType _createError (@Nonnull final String sCode, @Nonnull final String sMsg)
  {
    final ErrorType ret = new ErrorType ();
    ret.setCode (sCode);
    ret.setText (sMsg);
    return ret;
  }

  public final void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                               @Nonnull @Nonempty final String sPath,
                               @Nonnull final Map <String, String> aPathVariables,
                               @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                               @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    final PhotonUnifiedResponse aPUR = (PhotonUnifiedResponse) aUnifiedResponse;
    aPUR.setJsonWriterSettings (new JsonWriterSettings ().setIndentEnabled (true));
    aPUR.disableCaching ();

    final StopWatch aSW = StopWatch.createdStarted ();

    final String sCOTIDs = aPathVariables.get ("canonicalObjectTypeIDs");
    final ICommonsOrderedSet <String> aCOTIDs = new CommonsLinkedHashSet <> ();
    StringHelper.explode (',', sCOTIDs, x -> aCOTIDs.add (x.trim ()));
    final String sAtuCode = m_bWithATUCode ? aPathVariables.get ("atuCode") : null;

    if (LOGGER.isInfoEnabled ())
      LOGGER.info ("Querying for " + aCOTIDs + (m_bWithATUCode ? " in ATU code '" + sAtuCode + "'" : ""));

    if (aCOTIDs.isEmpty ())
      throw new IALBadRequestException ("No Canonical Object Type ID was passed", aRequestScope);

    final ResponseLookupRoutingInformationType aResponse = new ResponseLookupRoutingInformationType ();
    // TODO fill
    aResponse.addError (_createError ("c1", "Test"));

    if (true)
    {
      // As XML
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Rendering response as XML");

      final byte [] aXML = IALMarshaller.idkResponseLookupRoutingInformationMarshaller ().getAsBytes (aResponse);
      if (aXML == null)
        throw new IALInternalErrorException ("Failed to serialize XML response");

      aPUR.setContent (aXML)
          .setCharset (XMLWriterSettings.DEFAULT_XML_CHARSET_OBJ)
          .setMimeType (CMimeType.APPLICATION_XML);
    }
    else
    {
      // As JSON
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Rendering response as XML");

      final IJsonObject aJson = new JsonObject ();
      // TODO fill JSON
      aPUR.json (aJson);
    }

    aSW.stop ();

    LOGGER.info ("Successfully finalized querying Directory after " + aSW.getMillis () + "ms");
  }
}
