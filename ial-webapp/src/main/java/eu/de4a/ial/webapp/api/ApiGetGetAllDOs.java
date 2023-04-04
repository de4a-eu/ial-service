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
import java.io.UncheckedIOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nonnegative;
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
import com.helger.commons.equals.EqualsHelper;
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
import com.helger.httpclient.response.ExtendedHttpResponseException;
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
  static final AtomicLong COUNTER = new AtomicLong ();
  private static final Logger LOGGER = LoggerFactory.getLogger (ApiGetGetAllDOs.class);
  private static final ISMLInfo SML_INFO = new SMLInfo ("sml-de4a",
                                                        "SML DE4A",
                                                        "de4a.edelivery.tech.ec.europa.eu.",
                                                        "https://edelivery.tech.ec.europa.eu/edelivery-sml",
                                                        true);
  private static final IIdentifierFactory IF = SimpleIdentifierFactory.INSTANCE;
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

  private static final class DirectoryMatch
  {
    private IParticipantIdentifier participantID;
    private IDocumentTypeIdentifier docTypeID;
    private List <EntityType> entity;
  }

  private static final class DirectoryResults
  {
    final ICommonsMap <String, List <DirectoryMatch>> m_aDirectoryResults = new CommonsHashMap <> ();

    private DirectoryResults ()
    {}

    public boolean isEmpty ()
    {
      return m_aDirectoryResults.isEmpty ();
    }

    @Nonnegative
    public int size ()
    {
      return m_aDirectoryResults.size ();
    }

    @Nonnegative
    public int getMatchCount ()
    {
      int ret = 0;
      for (final List <DirectoryMatch> aMatches : m_aDirectoryResults.values ())
        ret += aMatches.size ();
      return ret;
    }

    @Nonnull
    public Set <Map.Entry <String, List <DirectoryMatch>>> entrySet ()
    {
      return m_aDirectoryResults.entrySet ();
    }

    public void keepOnlyMatchesForCountryCode (@Nonnull final String sLogPrefix, @Nonnull final String sCountryCode)
    {
      ValueEnforcer.isTrue (sCountryCode.length () == 2, "Invalid country code");

      for (final Map.Entry <String, List <DirectoryMatch>> aEntry : new CommonsArrayList <> (m_aDirectoryResults.entrySet ()))
      {
        final List <DirectoryMatch> aMatches = aEntry.getValue ();

        // Work on a copy
        for (final DirectoryMatch aMatch : new CommonsArrayList <> (aMatches))
        {
          // Work on a copy
          for (final EntityType aEntity : new CommonsArrayList <> (aMatch.entity))
          {
            // Filter out invalid country codes
            if (!EqualsHelper.equals (aEntity.getCountryCode (), sCountryCode))
            {
              if (LOGGER.isDebugEnabled ())
                LOGGER.debug (sLogPrefix +
                              "Skipping Entity, because it doesn't match country code '" +
                              sCountryCode +
                              "' (has '" +
                              aEntity.getCountryCode () +
                              "')");
              aMatch.entity.remove (aEntity);
            }
          }

          if (aMatch.entity.isEmpty ())
          {
            if (LOGGER.isDebugEnabled ())
              LOGGER.debug (sLogPrefix + "Entity List of match is now empty - removing the match");
            aMatches.remove (aMatch);
          }
        }

        if (aMatches.isEmpty ())
        {
          LOGGER.info (sLogPrefix + "Match List is now empty - removing the result entry");
          m_aDirectoryResults.remove (aEntry.getKey ());
        }
      }
    }

    @Nonnull
    public static DirectoryResults createQueryingDirectory (@Nonnull final String sLogPrefix,
                                                            @Nonnull final ICommonsOrderedSet <String> aCOTIDs)
    {
      final DirectoryResults ret = new DirectoryResults ();
      try (final HttpClientManager aHCM = HttpClientManager.create (new IALHttpClientSettings ()))
      {
        for (final String sCOTID : aCOTIDs)
        {
          // Build base URL and fetch all records per HTTP request
          final SimpleURL aBaseURL = new SimpleURL (IALConfig.Directory.getBaseURL () + "/search/1.0/xml");
          // More than 1000 is not allowed
          aBaseURL.add ("rpc", 1000);
          aBaseURL.add ("doctype", sCOTID);

          // Don't add the country code to the URL, because it would use an "OR"
          // on DocType and CountryCode, but we need an "AND"

          LOGGER.info (sLogPrefix + "Querying Directory for DocTypeID '" + sCOTID + "'");

          // Main client call
          try
          {
            final HttpGet aGet = new HttpGet (aBaseURL.getAsStringWithEncodedParameters ());
            final Document aResponseXML = aHCM.execute (aGet, new ResponseHandlerXml (false));

            // Parse result
            final ResultListType aDirectoryResultList = PDSearchAPIReader.resultListV1 ().read (aResponseXML);
            if (aDirectoryResultList != null)
            {
              if (aDirectoryResultList.hasMatchEntries ())
              {
                // Remove all other DocumentTypes then the queried one
                final ICommonsList <DirectoryMatch> aLocalMatches = new CommonsArrayList <> (aDirectoryResultList.getMatchCount ());
                for (final MatchType m : aDirectoryResultList.getMatch ())
                {
                  // Make sure the queried Document Type is contained
                  for (final IDType id : new CommonsArrayList <> (m.getDocTypeID ()))
                  {
                    final String sCurID = CIdentifier.getURIEncoded (id.getScheme (), id.getValue ());
                    if (!sCurID.matches (sCOTID))
                      m.getDocTypeID ().remove (id);
                  }
                  if (m.hasNoDocTypeIDEntries ())
                    throw new IllegalStateException ("No document type left after filtering - weird");
                  if (m.getDocTypeIDCount () != 1)
                    throw new IllegalStateException ("Not exactly 1 document type left but " + m.getDocTypeIDCount ());

                  // Use simplified match data type
                  final DirectoryMatch dm = new DirectoryMatch ();
                  dm.participantID = IF.createParticipantIdentifier (m.getParticipantID ().getScheme (),
                                                                     m.getParticipantID ().getValue ());
                  dm.docTypeID = IF.createDocumentTypeIdentifier (m.getDocTypeIDAtIndex (0).getScheme (),
                                                                  m.getDocTypeIDAtIndex (0).getValue ());
                  dm.entity = m.getEntity ();
                  aLocalMatches.add (dm);
                }
                ret.m_aDirectoryResults.put (sCOTID, aLocalMatches);
              }
              else
                LOGGER.warn (sLogPrefix + "Search results have no matches");
            }
            else
            {
              LOGGER.error (sLogPrefix + "Failed to parse Directory result as XML");
            }
          }
          catch (final ExtendedHttpResponseException ex)
          {
            LOGGER.error (sLogPrefix + "Failed to query remote Directory", ex);
          }
        }
      }
      catch (final IOException ex)
      {
        throw new UncheckedIOException (ex);
      }
      catch (final GeneralSecurityException ex)
      {
        throw new IllegalStateException (ex);
      }
      return ret;
    }
  }

  private static class ResponseMatch
  {
    private IParticipantIdentifier participantID;
    private EntityType entity;
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

    // Perform Directory queries for each Canonical Object Type
    final DirectoryResults aDirectoryResults = DirectoryResults.createQueryingDirectory (sLogPrefix, aCOTIDs);
    int nMatchCount = aDirectoryResults.getMatchCount ();
    LOGGER.info (sLogPrefix + "The Directory query returned " + nMatchCount + " matches");
    if (m_bWithATUCode)
    {
      // Remove all entities with the wrong country code
      final String sCountryCode = sAtuCode.substring (0, 2);
      LOGGER.info (sLogPrefix + "Start filtering results by country code '" + sCountryCode + "'");
      aDirectoryResults.keepOnlyMatchesForCountryCode (sLogPrefix, sCountryCode);
      final int nNewMatchCount = aDirectoryResults.getMatchCount ();
      LOGGER.info (sLogPrefix +
                   "The match count was reduced from " +
                   nMatchCount +
                   " to " +
                   nNewMatchCount +
                   " after country matching");
      nMatchCount = nNewMatchCount;
    }

    final ResponseLookupRoutingInformationType aQueryResponse = new ResponseLookupRoutingInformationType ();
    if (aDirectoryResults.isEmpty ())
    {
      LOGGER.warn (sLogPrefix + "Found no matches in the Directory");
    }
    else
    {
      LOGGER.info (sLogPrefix + "Collected Directory results: " + aDirectoryResults);

      // Group by COT, then Country Code
      final ICommonsMap <String, ICommonsMap <String, ICommonsList <ResponseMatch>>> aGroupedMap = new CommonsTreeMap <> ();
      final StopWatch aSWGrouping = StopWatch.createdStarted ();
      int nCacheHitCount = 0;
      int nSMPCallCount = 0;
      for (final Map.Entry <String, List <DirectoryMatch>> aEntry : aDirectoryResults.entrySet ())
      {
        final String sDocTypeID = aEntry.getKey ();
        final String sLogPrefix2 = sLogPrefix + "[" + sDocTypeID + "] ";

        // Map from Country Code to list of ResponseMatch
        final ICommonsMap <String, ICommonsList <ResponseMatch>> aMapByCOTs = aGroupedMap.computeIfAbsent (sDocTypeID,
                                                                                                           k -> new CommonsTreeMap <> ());
        for (final DirectoryMatch aMatch : aEntry.getValue ())
        {
          // Check, if any of the document types
          ETriState eMatchState = ETriState.UNDEFINED;

          // Query in cache first
          final ETriState eCacheState = IALCache.getState (aMatch.participantID, aMatch.docTypeID);
          if (eCacheState != ETriState.UNDEFINED)
          {
            // Use from cache
            eMatchState = eCacheState;
            nCacheHitCount++;
          }
          else
          {
            // Query service metadata for this Participant
            final BDXRClientReadOnly aSMPClient = new BDXRClientReadOnly (BDXLURLProvider.INSTANCE,
                                                                          aMatch.participantID,
                                                                          SML_INFO);
            aSMPClient.httpClientSettings ().setAllFrom (new IALHttpClientSettings ());
            aSMPClient.setTrustStore (SMP_TRUSTSTORE);

            // Run the main action asynchronously
            try
            {
              nSMPCallCount++;

              LOGGER.info (sLogPrefix2 +
                           "Now performing SMP query '" +
                           aMatch.participantID.getURIEncoded () +
                           "' / '" +
                           aMatch.docTypeID.getURIEncoded () +
                           "' on '" +
                           aSMPClient.getSMPHostURI () +
                           "'");

              // SMP query
              final SignedServiceMetadataType aSM = aSMPClient.getServiceMetadataOrNull (aMatch.participantID,
                                                                                         aMatch.docTypeID);
              if (aSM != null &&
                  aSM.getServiceMetadata () != null &&
                  aSM.getServiceMetadata ().getServiceInformation () != null)
              {
                // Only allow SMP entries that have a certain process
                // identifier
                for (final ProcessType aProc : aSM.getServiceMetadata ()
                                                  .getServiceInformation ()
                                                  .getProcessList ()
                                                  .getProcess ())
                {
                  final String sProcIDScheme = aProc.getProcessIdentifier ().getScheme ();
                  final String sProcIDValue = aProc.getProcessIdentifier ().getValue ();

                  // As we only want to find Data Providers, they need to
                  // have registered the "request" process ID
                  if ("urn:de4a-eu:MessageType".equals (sProcIDScheme) && "request".equals (sProcIDValue))
                  {
                    LOGGER.info (sLogPrefix2 +
                                 "Found matching process ID '" +
                                 CIdentifier.getURIEncoded (sProcIDScheme, sProcIDValue) +
                                 "'");

                    // First match is enough for us, to continue with the
                    // participant
                    eMatchState = ETriState.TRUE;
                    break;
                  }

                  if (LOGGER.isDebugEnabled ())
                    LOGGER.debug (sLogPrefix2 +
                                  "Skipping process ID '" +
                                  CIdentifier.getURIEncoded (sProcIDScheme, sProcIDValue) +
                                  "' because it is not relevant");
                }

                if (eMatchState.isUndefined ())
                  eMatchState = ETriState.FALSE;
              }
            }
            catch (final Exception ex)
            {
              LOGGER.error (sLogPrefix2 +
                            "Failed to query SMP: " +
                            ex.getClass ().getName () +
                            " - " +
                            ex.getMessage ());

              // Don't cache in case of exception
              eMatchState = ETriState.UNDEFINED;
            }

            // Remember SMP query result in Cache
            if (eMatchState.isDefined ())
              IALCache.cacheState (aMatch.participantID, aMatch.docTypeID, eMatchState.isTrue ());
          }

          if (eMatchState.isUndefined ())
          {
            // Continue with next Match for the current COT
            LOGGER.info (sLogPrefix2 +
                         "Skipping result for '" +
                         aMatch.participantID.getURIEncoded () +
                         "' because no matching process ID was found.");
            continue;
          }

          // Build result entities
          for (final EntityType aEntity : aMatch.entity)
          {
            // New match with only one Entity
            final ResponseMatch aResponseMatch = new ResponseMatch ();
            aResponseMatch.participantID = aMatch.participantID;
            aResponseMatch.entity = aEntity;
            aMapByCOTs.computeIfAbsent (aEntity.getCountryCode (), k -> new CommonsArrayList <> ())
                      .add (aResponseMatch);
          }
        }
      }
      aSWGrouping.stop ();
      LOGGER.info (sLogPrefix +
                   "Grouping with " +
                   nCacheHitCount +
                   " cache hits and " +
                   nSMPCallCount +
                   " SMP queries - took " +
                   aSWGrouping.getMillis () +
                   " milliseconds in total");

      // fill IAL response data types
      for (final Map.Entry <String, ICommonsMap <String, ICommonsList <ResponseMatch>>> aEntry : aGroupedMap.entrySet ())
      {
        // One result item per COT
        final ResponseItemType aItem = new ResponseItemType ();
        aItem.setCanonicalObjectTypeId (aEntry.getKey ());

        // Iterate per Country
        for (final Map.Entry <String, ICommonsList <ResponseMatch>> aEntry2 : aEntry.getValue ().entrySet ())
        {
          // One result per Country
          final String sCountryCode = aEntry2.getKey ();
          final ResponsePerCountryType aPerCountry = new ResponsePerCountryType ();
          aPerCountry.setCountryCode (sCountryCode);

          for (final ResponseMatch aMatch : aEntry2.getValue ())
          {
            final EntityType aEntity = aMatch.entity;

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
              // Only take results that are on the same ATU level as the
              // requested on
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
                  throw new IllegalStateException (sLogPrefix + "Dunno level " + eNutsLevel);
              }
            }

            aProvision.setAtuLevel (eMatchAtuLevel);
            aProvision.setAtuCode (sMatchAtuCode);
            aProvision.setAtuLatinName (sMatchAtuName);
            aProvision.setDataOwnerId (aMatch.participantID.getScheme () + "::" + aMatch.participantID.getValue ());
            aProvision.setDataOwnerPrefLabel (aEntity.getNameAtIndex (0).getValue ());

            if (StringHelper.hasText (aEntity.getAdditionalInfo ()))
            {
              // Parse additional, optional, JSON - unlikely to ever be used
              LOGGER.info (sLogPrefix + "Trying to parse additional information as JSON");

              /**
               * [ { "title": "ES/BirthEvidence/BirthRegister", "parameterList":
               * [ { "name": "ES/Register/Volume", "optional": false } ] } ]
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
          aQueryResponse.addResponseItem (aItem);
      }
    }

    if (aQueryResponse.hasNoResponseItemEntries ())

    {
      // One error is required to fulfill the XSD requirements
      aQueryResponse.addError (_createError ("no-match",
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
      aPUR.json (getAsJson (aQueryResponse));
    }
    else
    {
      // As XML
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug (sLogPrefix + "Rendering response as XML");

      final byte [] aXML = IALMarshaller.responseLookupRoutingInformationMarshaller ()
                                        .formatted ()
                                        .getAsBytes (aQueryResponse);
      if (aXML == null)
        throw new IALInternalErrorException ("Failed to serialize IAL XML response");

      aPUR.xml (aXML, XMLWriterSettings.DEFAULT_XML_CHARSET_OBJ);
    }

    // Allow CORS safe calls
    aPUR.addCustomResponseHeader (CHttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN, "*");

    aSW.stop ();

    LOGGER.info (sLogPrefix + "Successfully finalized IAL querying Directory after " + aSW.getMillis () + "ms");
  }
}
