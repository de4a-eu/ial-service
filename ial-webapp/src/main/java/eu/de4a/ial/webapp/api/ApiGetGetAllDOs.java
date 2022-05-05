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

import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.CommonsLinkedHashSet;
import com.helger.commons.collection.impl.CommonsTreeMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.collection.impl.ICommonsOrderedSet;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.string.StringHelper;
import com.helger.commons.timing.StopWatch;
import com.helger.commons.url.SimpleURL;
import com.helger.commons.url.URLHelper;
import com.helger.http.AcceptMimeTypeList;
import com.helger.httpclient.HttpClientManager;
import com.helger.httpclient.HttpClientSettings;
import com.helger.httpclient.response.ResponseHandlerXml;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.json.serialize.JsonReader;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.masterdata.nuts.ENutsLevel;
import com.helger.masterdata.nuts.ILauManager;
import com.helger.masterdata.nuts.INutsManager;
import com.helger.masterdata.nuts.LauItem;
import com.helger.masterdata.nuts.LauManager;
import com.helger.masterdata.nuts.NutsItem;
import com.helger.masterdata.nuts.NutsManager;
import com.helger.pd.searchapi.PDSearchAPIReader;
import com.helger.pd.searchapi.v1.EntityType;
import com.helger.pd.searchapi.v1.IDType;
import com.helger.pd.searchapi.v1.MatchType;
import com.helger.pd.searchapi.v1.ResultListType;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.api.IAPIExecutor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.servlet.request.RequestHelper;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.serialize.write.XMLWriterSettings;

import eu.de4a.ial.api.IALMarshaller;
import eu.de4a.ial.api.jaxb.AtuLevelType;
import eu.de4a.ial.api.jaxb.ErrorType;
import eu.de4a.ial.api.jaxb.ParameterSetType;
import eu.de4a.ial.api.jaxb.ParameterType;
import eu.de4a.ial.api.jaxb.ProvisionType;
import eu.de4a.ial.api.jaxb.ResponseItemType;
import eu.de4a.ial.api.jaxb.ResponseLookupRoutingInformationType;
import eu.de4a.ial.api.jaxb.ResponsePerCountryType;
import eu.de4a.ial.webapp.config.IALConfig;

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
  public static IJsonObject getAsJson (@Nonnull final ResponseLookupRoutingInformationType aResponse)
  {
    final IJsonObject ret = new JsonObject ();
    if (aResponse.hasErrorEntries ())
    {
      // Response errors
      final IJsonArray aArray = new JsonArray ();
      for (final ErrorType aError : aResponse.getError ())
        aArray.add (new JsonObject ().add ("code", aError.getCode ()).add ("text", aError.getText ()));
      ret.addJson ("errors", aArray);
    }
    else
    {
      // Response items
      final IJsonArray aJsonItems = new JsonArray ();
      for (final ResponseItemType aResponseItem : aResponse.getResponseItem ())
      {
        final IJsonObject aJsonItem = new JsonObject ().add ("canonicalObjectTypeId", aResponseItem.getCanonicalObjectTypeId ());
        final IJsonArray aJsonPerCountries = new JsonArray ();
        for (final ResponsePerCountryType aRPC : aResponseItem.getResponsePerCountry ())
        {
          final IJsonObject aJsonPerCountry = new JsonObject ().add ("countryCode", aRPC.getCountryCode ());
          final IJsonArray aJsonProvisions = new JsonArray ();
          for (final ProvisionType aProvision : aRPC.getProvision ())
          {
            final IJsonObject aJsonProvision = new JsonObject ();
            if (aProvision.getAtuLevel () != null)
              aJsonProvision.add ("atuLevel", aProvision.getAtuLevel ().value ());
            aJsonProvision.add ("atuCode", aProvision.getAtuCode ());
            aJsonProvision.add ("atuLatinName", aProvision.getAtuLatinName ());
            aJsonProvision.add ("dataOwnerID", aProvision.getDataOwnerId ());
            aJsonProvision.add ("dataOwnerPrefLabel", aProvision.getDataOwnerPrefLabel ());
            if (aProvision.hasParameterSetEntries ())
            {
              final IJsonArray aJsonParamSets = new JsonArray ();
              for (final ParameterSetType aParamSet : aProvision.getParameterSet ())
              {
                final IJsonObject aJsonParamSet = new JsonObject ();
                aJsonParamSet.add ("title", aParamSet.getTitle ());
                aJsonParamSet.addJson ("parameterList",
                                       new JsonArray ().addAllMapped (aParamSet.getParameter (),
                                                                      x -> new JsonObject ().add ("name", x.getName ())
                                                                                            .add ("optional", x.isOptional ())));
                aJsonParamSets.add (aJsonParamSet);
              }
              aJsonProvision.addJson ("parameterSets", aJsonParamSets);
            }
            aJsonProvisions.add (aJsonProvision);
          }
          aJsonPerCountry.addJson ("provisions", aJsonProvisions);
          aJsonPerCountries.add (aJsonPerCountry);
        }
        aJsonItem.addJson ("countries", aJsonPerCountries);
        aJsonItems.add (aJsonItem);
      }
      ret.addJson ("items", aJsonItems);
    }
    return ret;
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

    // Get and check parameters
    final String sCOTIDs = URLHelper.urlDecode (aPathVariables.get ("canonicalObjectTypeIDs"));

    final ICommonsOrderedSet <String> aCOTIDs = new CommonsLinkedHashSet <> ();
    StringHelper.explode (',', sCOTIDs, x -> aCOTIDs.add (x.trim ()));

    final String sAtuCode = m_bWithATUCode ? URLHelper.urlDecode (aPathVariables.get ("atuCode")) : null;

    if (LOGGER.isInfoEnabled ())
      LOGGER.info ("Querying for " + aCOTIDs + (m_bWithATUCode ? " in ATU code '" + sAtuCode + "'" : ""));

    if (aCOTIDs.isEmpty ())
      throw new IALBadRequestException ("No Canonical Object Type ID was passed", aRequestScope);

    final INutsManager aNutsMgr = NutsManager.INSTANCE_2021;
    final ILauManager aLauMgr = LauManager.INSTANCE_2021;
    if (m_bWithATUCode)
    {
      // Consistency check
      if (aNutsMgr.isIDValid (sAtuCode))
        LOGGER.info ("The provided ATU code '" + sAtuCode + "' is a valid NUTS code");
      else
        if (aLauMgr.isIDValid (sAtuCode))
          LOGGER.info ("The provided ATU code '" + sAtuCode + "' is a valid LAU code");
        else
          throw new IALBadRequestException ("The provided ATU code '" + sAtuCode + "' is neither a NUTS nor a LAU code", aRequestScope);
    }

    // Perform Directory queries
    final ICommonsMap <String, ResultListType> aDirectoryResults = new CommonsHashMap <> ();
    final HttpClientSettings aHCS = new HttpClientSettings ();
    if (IALConfig.Directory.isTLSTrustAll ())
    {
      LOGGER.warn ("The TLS connection trusts all certificates. That is not very secure.");
      // This block is not nice but needed, because the system truststore of the
      // machine running the IAL is empty.
      // For a real production scenario, a separate trust store should be
      // configured.
      aHCS.setSSLContextTrustAll ();
    }
    try (final HttpClientManager aHCM = HttpClientManager.create (aHCS))
    {
      for (final String sCOTID : aCOTIDs)
      {
        // Build base URL and fetch all records per HTTP request
        final SimpleURL aBaseURL = new SimpleURL (IALConfig.Directory.getBaseURL () + "/search/1.0/xml");
        // More than 1000 is not allowed
        aBaseURL.add ("rpc", 100);
        aBaseURL.add ("doctype", sCOTID);
        String sCountryCode = null;
        if (m_bWithATUCode)
        {
          // Both NUTS and LAU code always start with the country code
          sCountryCode = sAtuCode.substring (0, 2);
          aBaseURL.add ("country", sCountryCode);
        }

        LOGGER.info ("Querying Directory for DocTypeID '" +
                     sCOTID +
                     "'" +
                     (m_bWithATUCode ? " and country code '" + sCountryCode + "'" : ""));

        final HttpGet aGet = new HttpGet (aBaseURL.getAsStringWithEncodedParameters ());
        final Document aResponseXML = aHCM.execute (aGet, new ResponseHandlerXml (false));

        // Parse result
        final ResultListType aResultList = PDSearchAPIReader.resultListV1 ().read (aResponseXML);
        if (aResultList != null)
        {
          // Only remember results with matches
          if (aResultList.hasMatchEntries ())
            aDirectoryResults.put (sCOTID, aResultList);
        }
        else
          LOGGER.error ("Failed to parse Directory result as XML");
      }
    }

    if (aDirectoryResults.isEmpty ())
      LOGGER.warn ("Found no matches in the Directory");
    else
      LOGGER.info ("Collected Directory results: " + aDirectoryResults);

    // Group results by COT and Country
    final ICommonsMap <String, ICommonsMap <String, ICommonsList <MatchType>>> aGroupedMap = new CommonsTreeMap <> ();
    for (final Map.Entry <String, ResultListType> aEntry : aDirectoryResults.entrySet ())
    {
      final String sCOT = aEntry.getKey ();
      final ResultListType aRL = aEntry.getValue ();
      for (final MatchType aMatch : aRL.getMatch ())
      {
        final ICommonsMap <String, ICommonsList <MatchType>> aMapByCOT = aGroupedMap.computeIfAbsent (sCOT, k -> new CommonsTreeMap <> ());
        for (final EntityType aEntity : aMatch.getEntity ())
        {
          // Match with only one Entity
          final MatchType aSubMatch = aMatch.clone ();
          aSubMatch.getEntity ().clear ();
          aSubMatch.addEntity (aEntity);
          aMapByCOT.computeIfAbsent (aEntity.getCountryCode (), k -> new CommonsArrayList <> ()).add (aSubMatch);
        }
      }
    }

    // fill IAL response data types
    final ResponseLookupRoutingInformationType aResponse = new ResponseLookupRoutingInformationType ();
    for (final Map.Entry <String, ICommonsMap <String, ICommonsList <MatchType>>> aEntry : aGroupedMap.entrySet ())
    {
      final String sCOT = aEntry.getKey ();
      final ResponseItemType aItem = new ResponseItemType ();
      aItem.setCanonicalObjectTypeId (sCOT);

      for (final Map.Entry <String, ICommonsList <MatchType>> aEntry2 : aEntry.getValue ().entrySet ())
      {
        final String sCountryCode = aEntry2.getKey ();
        final ResponsePerCountryType aPerCountry = new ResponsePerCountryType ();
        aPerCountry.setCountryCode (sCountryCode);
        for (final MatchType aMatch : aEntry2.getValue ())
        {
          ValueEnforcer.isTrue ( () -> aMatch.getEntityCount () == 1, "Entity mismatch");
          final EntityType aEntity = aMatch.getEntityAtIndex (0);

          String sMatchAtuCode = CollectionHelper.findFirstMapped (aEntity.getIdentifier (),
                                                                   x -> "atuCode".equals (x.getScheme ()),
                                                                   IDType::getValue);
          if (StringHelper.hasNoText (sMatchAtuCode))
            sMatchAtuCode = sCountryCode;
          final ENutsLevel eNutsLevel = ENutsLevel.getFromLengthOrNull (sMatchAtuCode.length ());
          // Assume "LAUT" if nuts level is null

          final ProvisionType aProvision = new ProvisionType ();
          final AtuLevelType eMatchAtuLevel;
          final String sMatchAtuName;
          if (eNutsLevel == null)
          {
            // Check if it is a LAU
            final LauItem aLauItem = aLauMgr.getItemOfID (sMatchAtuCode);
            if (aLauItem != null)
            {
              eMatchAtuLevel = AtuLevelType.LAU;
              sMatchAtuName = aLauItem.getLatinDisplayName ();
            }
            else
            {
              // Fallback: assume EDU
              eMatchAtuLevel = AtuLevelType.EDU;
              // TODO
              sMatchAtuName = "EDU - dunno";
            }
          }
          else
          {
            final NutsItem aNutsItem = aNutsMgr.getItemOfID (sMatchAtuCode);
            if (aNutsItem != null)
              sMatchAtuName = aNutsItem.getLatinDisplayName ();
            else
              sMatchAtuName = "Unknown NUTS code '" + sMatchAtuCode + "'";

            switch (eNutsLevel)
            {
              case COUNTRY:
                eMatchAtuLevel = AtuLevelType.NUTS_0;
                break;
              case NUTS1:
                eMatchAtuLevel = AtuLevelType.NUTS_1;
                break;
              case NUTS2:
                eMatchAtuLevel = AtuLevelType.NUTS_2;
                break;
              case NUTS3:
                eMatchAtuLevel = AtuLevelType.NUTS_3;
                break;
              default:
                throw new IllegalStateException ("Dunno level " + eNutsLevel);
            }
          }

          aProvision.setAtuLevel (eMatchAtuLevel);
          aProvision.setAtuCode (sMatchAtuCode);
          aProvision.setAtuLatinName (sMatchAtuName);
          aProvision.setDataOwnerId (aMatch.getParticipantID ().getScheme () + "::" + aMatch.getParticipantIDValue ());
          aProvision.setDataOwnerPrefLabel (aEntity.getNameAtIndex (0).getValue ());

          if (StringHelper.hasText (aEntity.getAdditionalInfo ()))
          {
            LOGGER.info ("Trying to parse additional information as JSON");

            /**
             * [ { "title": "ES/BirthEvidence/BirthRegister", "parameterList": [
             * { "name": "ES/Register/Volume", "optional": false } ] } ]
             */
            final IJsonArray aJsonParamSets = JsonReader.builder ().source (aEntity.getAdditionalInfo ()).readAsArray ();
            if (aJsonParamSets != null && aJsonParamSets.isNotEmpty ())
            {
              for (final IJsonObject aJsonParamSet : aJsonParamSets.iteratorObjects ())
              {
                if (aJsonParamSet.containsKey ("title") && aJsonParamSet.containsKey ("parameterList"))
                {
                  final ParameterSetType aParamSet = new ParameterSetType ();
                  aParamSet.setTitle (aJsonParamSet.getAsString ("title"));
                  final IJsonArray aJsonParamList = aJsonParamSet.getAsArray ("parameterList");
                  if (aJsonParamList != null)
                    for (final IJsonObject aJsonParam : aJsonParamList.iteratorObjects ())
                    {
                      final ParameterType aParam = new ParameterType ();
                      aParam.setName (aJsonParam.getAsString ("name"));
                      aParam.setOptional (aJsonParam.getAsBoolean ("optional", false));
                      aParamSet.addParameter (aParam);
                    }

                  if (StringHelper.hasNoText (aParamSet.getTitle ()))
                    LOGGER.warn ("JSON parameter set object has an empty title");
                  else
                    if (aParamSet.hasNoParameterEntries ())
                      LOGGER.warn ("JSON parameter set object has no parameter set entry");
                    else
                      aProvision.addParameterSet (aParamSet);
                }
                else
                  LOGGER.warn ("JSON parameter set object is missing title and/or parameterList");
              }
            }
            else
              LOGGER.warn ("Failed to read additional information as JSON array");
          }
          aPerCountry.addProvision (aProvision);
        }
        aItem.addResponsePerCountry (aPerCountry);
      }
      aResponse.addResponseItem (aItem);
    }

    if (aResponse.hasNoResponseItemEntries ())
    {
      // One error is required to fulfill the XSD requirements
      aResponse.addError (_createError ("no-match",
                                        "Found matches searching for '" +
                                                    sCOTIDs +
                                                    "'" +
                                                    (m_bWithATUCode ? " and ATU code '" + sAtuCode + "'" : "")));
    }

    final AcceptMimeTypeList aAccept = RequestHelper.getAcceptMimeTypes (aRequestScope.getRequest ());
    if (aAccept.getQualityOfMimeType (CMimeType.APPLICATION_JSON) > aAccept.getQualityOfMimeType (CMimeType.APPLICATION_XML))
    {
      // As JSON
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Rendering response as JSON");

      // fill JSON
      aPUR.json (getAsJson (aResponse));
    }
    else
    {
      // As XML
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Rendering response as XML");

      final byte [] aXML = IALMarshaller.idkResponseLookupRoutingInformationMarshaller ().formatted ().getAsBytes (aResponse);
      if (aXML == null)
        throw new IALInternalErrorException ("Failed to serialize XML response");

      aPUR.setContent (aXML).setCharset (XMLWriterSettings.DEFAULT_XML_CHARSET_OBJ).setMimeType (CMimeType.APPLICATION_XML);
    }

    aSW.stop ();

    LOGGER.info ("Successfully finalized querying Directory after " + aSW.getMillis () + "ms");
  }
}
