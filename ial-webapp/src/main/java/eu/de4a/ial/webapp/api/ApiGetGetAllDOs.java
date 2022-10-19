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

import java.security.KeyStore;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nonnull;

import org.apache.hc.client5.http.classic.methods.HttpGet;
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
import com.helger.commons.concurrent.ExecutorServiceHelper;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.http.CHttpHeader;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.state.ETriState;
import com.helger.commons.string.StringHelper;
import com.helger.commons.timing.StopWatch;
import com.helger.commons.url.SimpleURL;
import com.helger.commons.url.URLHelper;
import com.helger.http.AcceptMimeTypeList;
import com.helger.httpclient.HttpClientManager;
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
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.sml.SMLInfo;
import com.helger.peppolid.CIdentifier;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.factory.SimpleIdentifierFactory;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.api.IAPIExecutor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.security.keystore.EKeyStoreType;
import com.helger.security.keystore.KeyStoreHelper;
import com.helger.security.keystore.LoadedKeyStore;
import com.helger.servlet.request.RequestHelper;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.smpclient.bdxr1.BDXRClientReadOnly;
import com.helger.smpclient.url.BDXLURLProvider;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.serialize.write.XMLWriterSettings;
import com.helger.xsds.bdxr.smp1.ProcessType;
import com.helger.xsds.bdxr.smp1.SignedServiceMetadataType;

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
import eu.de4a.ial.webapp.config.IALHttpClientSettings;

/**
 * Provide the public query API
 *
 * @author Philip Helger
 */
public class ApiGetGetAllDOs implements IAPIExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ApiGetGetAllDOs.class);
  private static final AtomicLong COUNTER = new AtomicLong ();
  private static final ISMLInfo SML_INFO = new SMLInfo ("sml-de4a",
                                                        "SML DE4A",
                                                        "de4a.edelivery.tech.ec.europa.eu.",
                                                        "https://edelivery.tech.ec.europa.eu/edelivery-sml",
                                                        true);
  private static final KeyStore SMP_TRUSTSTORE;
  static
  {
    final LoadedKeyStore aLTS = KeyStoreHelper.loadKeyStore (EKeyStoreType.JKS,
                                                             IALConfig.SMP.getTruststorePath (),
                                                             IALConfig.SMP.getTruststorePassword ());
    if (aLTS.isFailure ())
      throw new InitializationException ("Failed to load SMP truststore '" + IALConfig.SMP.getTruststorePath () + "'");
    SMP_TRUSTSTORE = aLTS.getKeyStore ();
  }

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
        final IJsonObject aJsonItem = new JsonObject ().add ("canonicalObjectTypeId",
                                                             aResponseItem.getCanonicalObjectTypeId ());
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
                                                                                            .add ("optional",
                                                                                                  x.isOptional ())));
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

  @Nonnull
  private static String _unifyATU (@Nonnull final String s)
  {
    return s.toUpperCase (Locale.ROOT);
  }

  @Nonnull
  public static String _createCacheKey (@Nonnull final String sParticipantID, @Nonnull final String sDocTypeID)
  {
    return sParticipantID + "-" + sDocTypeID;
  }

  public final void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                               @Nonnull @Nonempty final String sPath,
                               @Nonnull final Map <String, String> aPathVariables,
                               @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                               @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sLogPrefix = "[IAL-" + COUNTER.incrementAndGet () + "] ";

    final PhotonUnifiedResponse aPUR = (PhotonUnifiedResponse) aUnifiedResponse;
    aPUR.setJsonWriterSettings (new JsonWriterSettings ().setIndentEnabled (true));
    aPUR.disableCaching ();

    final StopWatch aSW = StopWatch.createdStarted ();

    // Get and check parameters
    final String sCOTIDs = URLHelper.urlDecode (aPathVariables.get ("canonicalObjectTypeIDs"));

    // Split into 1-n pieces
    final ICommonsOrderedSet <String> aCOTIDs = new CommonsLinkedHashSet <> ();
    StringHelper.explode (',', sCOTIDs, x -> aCOTIDs.add (x.trim ()));

    // Ensure the ATU code is upper case for consistent comparison
    final String sAtuCode = m_bWithATUCode ? _unifyATU (URLHelper.urlDecode (aPathVariables.get ("atuCode"))) : null;

    LOGGER.info (sLogPrefix + "Querying for " + aCOTIDs + (m_bWithATUCode ? " in ATU code '" + sAtuCode + "'" : ""));

    if (aCOTIDs.isEmpty ())
      throw new IALBadRequestException ("No Canonical Object Type ID was passed", aRequestScope);

    final INutsManager aNutsMgr = NutsManager.INSTANCE_2021;
    final ILauManager aLauMgr = LauManager.INSTANCE_2021;
    if (m_bWithATUCode)
    {
      // Consistency check
      if (aNutsMgr.isIDValid (sAtuCode))
        LOGGER.info (sLogPrefix + "The provided ATU code '" + sAtuCode + "' is a valid NUTS code");
      else
        if (aLauMgr.isIDValid (sAtuCode))
          LOGGER.info (sLogPrefix + "The provided ATU code '" + sAtuCode + "' is a valid LAU code");
        else
          throw new IALBadRequestException ("The provided ATU code '" + sAtuCode + "' is neither a NUTS nor a LAU code",
                                            aRequestScope);
    }

    // Perform Directory queries
    final ICommonsMap <String, ResultListType> aDirectoryResults = new CommonsHashMap <> ();
    try (final HttpClientManager aHCM = HttpClientManager.create (new IALHttpClientSettings ()))
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

        LOGGER.info (sLogPrefix +
                     "Querying Directory for DocTypeID '" +
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
        {
          LOGGER.error (sLogPrefix + "Failed to parse Directory result as XML");
        }
      }
    }

    if (aDirectoryResults.isEmpty ())
    {
      LOGGER.warn (sLogPrefix + "Found no matches in the Directory");
    }
    else
    {
      LOGGER.info (sLogPrefix + "Collected Directory results: " + aDirectoryResults);
    }

    // Group results by COT and Country Code
    final StopWatch aSWGrouping = StopWatch.createdStarted ();
    final AtomicInteger aSMPCallCount = new AtomicInteger (0);
    final ICommonsMap <String, ICommonsMap <String, ICommonsList <MatchType>>> aGroupedMap = new CommonsTreeMap <> ();
    for (final Map.Entry <String, ResultListType> aEntry : aDirectoryResults.entrySet ())
    {
      final String sCOT = aEntry.getKey ();
      final ICommonsMap <String, ICommonsList <MatchType>> aMapByCOT = aGroupedMap.computeIfAbsent (sCOT,
                                                                                                    k -> new CommonsTreeMap <> ());

      for (final MatchType aMatch : aEntry.getValue ().getMatch ())
      {
        if (aMatch.hasNoDocTypeIDEntries ())
        {
          LOGGER.info ("Skipping result for '" +
                       aMatch.getParticipantIDValue () +
                       "' because no document types are present.");
          continue;
        }

        // Check, if any of the document types in
        {
          final AtomicBoolean aAnyDocumentTypeSupportsRequest = new AtomicBoolean (false);
          final IIdentifierFactory aIIF = SimpleIdentifierFactory.INSTANCE;
          final IParticipantIdentifier aParticipantID = aIIF.createParticipantIdentifier (aMatch.getParticipantID ()
                                                                                                .getScheme (),
                                                                                          aMatch.getParticipantID ()
                                                                                                .getValue ());
          BDXRClientReadOnly aSMPClient = null;

          final ExecutorService aES = Executors.newFixedThreadPool (4);
          for (final IDType aDocTypeID : aMatch.getDocTypeID ())
          {
            // Query all service metadata for this Participant
            final IDocumentTypeIdentifier aDocumentTypeID = aIIF.createDocumentTypeIdentifier (aDocTypeID.getScheme (),
                                                                                               aDocTypeID.getValue ());

            final ETriState eState = IALCache.getState (aParticipantID, aDocumentTypeID);
            if (eState != ETriState.UNDEFINED)
            {
              aAnyDocumentTypeSupportsRequest.set (eState.getAsBooleanValue ());
            }
            else
            {
              // Avoid creating SMP client, if everything is in the cache to
              // avoid the DNS NAPTR lookup
              if (aSMPClient == null)
              {
                aSMPClient = new BDXRClientReadOnly (BDXLURLProvider.INSTANCE, aParticipantID, SML_INFO);
                aSMPClient.httpClientSettings ().setAllFrom (new IALHttpClientSettings ());
                aSMPClient.setTrustStore (SMP_TRUSTSTORE);
              }
              final BDXRClientReadOnly aFinalSMPClient = aSMPClient;

              aES.submit ( () -> {
                try
                {
                  aSMPCallCount.incrementAndGet ();
                  final SignedServiceMetadataType aSM = aFinalSMPClient.getServiceMetadataOrNull (aParticipantID,
                                                                                                  aDocumentTypeID);
                  if (aSM != null &&
                      aSM.getServiceMetadata () != null &&
                      aSM.getServiceMetadata ().getServiceInformation () != null)
                    for (final ProcessType aProc : aSM.getServiceMetadata ()
                                                      .getServiceInformation ()
                                                      .getProcessList ()
                                                      .getProcess ())
                    {
                      if ("urn:de4a-eu:MessageType".equals (aProc.getProcessIdentifier ().getScheme ()) &&
                          "request".equals (aProc.getProcessIdentifier ().getValue ()))
                      {
                        LOGGER.info ("Found matching process ID '" +
                                     CIdentifier.getURIEncoded (aProc.getProcessIdentifier ().getScheme (),
                                                                aProc.getProcessIdentifier ().getValue ()) +
                                     "'");

                        // First match is enough for us, to continue with the
                        // participant
                        aAnyDocumentTypeSupportsRequest.set (true);
                        break;
                      }
                      if (LOGGER.isDebugEnabled ())
                        LOGGER.debug ("Skip process ID '" +
                                      CIdentifier.getURIEncoded (aProc.getProcessIdentifier ().getScheme (),
                                                                 aProc.getProcessIdentifier ().getValue ()) +
                                      "'");
                    }
                }
                catch (final Exception ex)
                {
                  LOGGER.error ("Failed to query SMP: " + ex.getClass ().getName () + " - " + ex.getMessage ());
                }
                IALCache.cacheState (aParticipantID, aDocumentTypeID, aAnyDocumentTypeSupportsRequest.get ());
              });
            }
          }

          ExecutorServiceHelper.shutdownAndWaitUntilAllTasksAreFinished (aES, 100, TimeUnit.MILLISECONDS);

          if (!aAnyDocumentTypeSupportsRequest.get ())
          {
            LOGGER.info ("Skipping result for '" +
                         aMatch.getParticipantIDValue () +
                         "' because no matching process ID was found.");
            continue;
          }
        }

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
    aSWGrouping.stop ();
    LOGGER.info ("Grouping with " +
                 aSMPCallCount.intValue () +
                 " SMP queries took " +
                 aSWGrouping.getMillis () +
                 " milliseconds in total");

    // fill IAL response data types
    final ResponseLookupRoutingInformationType aResponse = new ResponseLookupRoutingInformationType ();
    for (final Map.Entry <String, ICommonsMap <String, ICommonsList <MatchType>>> aEntry : aGroupedMap.entrySet ())
    {
      // One result item per COT
      final ResponseItemType aItem = new ResponseItemType ();
      aItem.setCanonicalObjectTypeId (aEntry.getKey ());

      // Iterate per Country
      for (final Map.Entry <String, ICommonsList <MatchType>> aEntry2 : aEntry.getValue ().entrySet ())
      {
        // One result per Country
        final String sCountryCode = aEntry2.getKey ();
        final ResponsePerCountryType aPerCountry = new ResponsePerCountryType ();
        aPerCountry.setCountryCode (sCountryCode);

        for (final MatchType aMatch : aEntry2.getValue ())
        {
          ValueEnforcer.isTrue ( () -> aMatch.getEntityCount () == 1, "Entity mismatch");
          final EntityType aEntity = aMatch.getEntityAtIndex (0);

          // Check if this Entity has a specific "atuCode" defined
          String sMatchAtuCode = CollectionHelper.findFirstMapped (aEntity.getIdentifier (),
                                                                   x -> "atuCode".equals (x.getScheme ()),
                                                                   IDType::getValue);
          if (StringHelper.hasNoText (sMatchAtuCode))
          {
            // Fallback on country code
            sMatchAtuCode = sCountryCode;
          }
          sMatchAtuCode = _unifyATU (sMatchAtuCode);

          if (m_bWithATUCode)
          {
            // Only take results that are on the same ATU level as the requested
            // on
            if (!sMatchAtuCode.startsWith (sAtuCode))
            {
              LOGGER.info (sLogPrefix +
                           "Igoring result with ATU code '" +
                           sMatchAtuCode +
                           "' because it does not match the requested ATU code '" +
                           sAtuCode +
                           "'");
              continue;
            }
          }

          final ENutsLevel eNutsLevel = ENutsLevel.getFromLengthOrNull (sMatchAtuCode.length ());

          final ProvisionType aProvision = new ProvisionType ();
          final AtuLevelType eMatchAtuLevel;
          final String sMatchAtuName;
          if (eNutsLevel == null)
          {
            // Assume "LAU" code if nuts level is null
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
            // Parse additional, optional, JSON - unlikely to ever be used
            LOGGER.info (sLogPrefix + "Trying to parse additional information as JSON");

            /**
             * [ { "title": "ES/BirthEvidence/BirthRegister", "parameterList": [
             * { "name": "ES/Register/Volume", "optional": false } ] } ]
             */
            final IJsonArray aJsonParamSets = JsonReader.builder ()
                                                        .source (aEntity.getAdditionalInfo ())
                                                        .readAsArray ();
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
                    LOGGER.warn (sLogPrefix + "JSON parameter set object has an empty title");
                  else
                    if (aParamSet.hasNoParameterEntries ())
                      LOGGER.warn (sLogPrefix + "JSON parameter set object has no parameter set entry");
                    else
                      aProvision.addParameterSet (aParamSet);
                }
                else
                {
                  LOGGER.warn (sLogPrefix + "JSON parameter set object is missing title and/or parameterList");
                }
              }
            }
            else
            {
              LOGGER.warn (sLogPrefix + "Failed to read additional information as JSON array");
            }
          }
          aPerCountry.addProvision (aProvision);
        }
        if (aPerCountry.hasProvisionEntries ())
          aItem.addResponsePerCountry (aPerCountry);
      }
      if (aItem.hasResponsePerCountryEntries ())
        aResponse.addResponseItem (aItem);
    }

    if (aResponse.hasNoResponseItemEntries ())
    {
      // One error is required to fulfill the XSD requirements
      aResponse.addError (_createError ("no-match",
                                        "Found NO matches searching for '" +
                                                    sCOTIDs +
                                                    "'" +
                                                    (m_bWithATUCode ? " and ATU code '" + sAtuCode + "'" : "")));
    }

    final AcceptMimeTypeList aAccept = RequestHelper.getAcceptMimeTypes (aRequestScope.getRequest ());
    if (aAccept.getQualityOfMimeType (CMimeType.APPLICATION_JSON) > aAccept.getQualityOfMimeType (CMimeType.APPLICATION_XML))
    {
      // As JSON
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug (sLogPrefix + "Rendering response as JSON");

      // fill JSON
      aPUR.json (getAsJson (aResponse));
    }
    else
    {
      // As XML
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug (sLogPrefix + "Rendering response as XML");

      final byte [] aXML = IALMarshaller.responseLookupRoutingInformationMarshaller ()
                                        .formatted ()
                                        .getAsBytes (aResponse);
      if (aXML == null)
        throw new IALInternalErrorException ("Failed to serialize XML response");

      aPUR.setContent (aXML)
          .setCharset (XMLWriterSettings.DEFAULT_XML_CHARSET_OBJ)
          .setMimeType (CMimeType.APPLICATION_XML);
    }

    // Allow CORS safe calls
    aPUR.addCustomResponseHeader (CHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN, "*");

    aSW.stop ();

    LOGGER.info (sLogPrefix + "Successfully finalized querying Directory after " + aSW.getMillis () + "ms");
  }
}
