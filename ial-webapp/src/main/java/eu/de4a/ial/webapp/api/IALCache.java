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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.CommonsHashSet;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.state.ETriState;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;

/**
 * IAL cache. From "Participant ID" and "Document Type ID" to the status if it
 * was found or not.
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class IALCache
{
  private static final class CacheValue
  {
    private static final Duration CACHE_EXPIRATION_DURATION = Duration.ofHours (2);

    private final boolean m_bFound;
    private final LocalDateTime m_aExpirationDT;

    public CacheValue (final boolean bFound)
    {
      m_bFound = bFound;
      m_aExpirationDT = PDTFactory.getCurrentLocalDateTime ().plus (CACHE_EXPIRATION_DURATION);
    }

    public boolean isFound ()
    {
      return m_bFound;
    }

    public boolean isExpiredAt (@Nonnull final LocalDateTime aCheckDT)
    {
      return aCheckDT.isAfter (m_aExpirationDT);
    }
  }

  private static final Logger LOGGER = LoggerFactory.getLogger (IALCache.class);
  private static final SimpleReadWriteLock RW_LOCK = new SimpleReadWriteLock ();
  @GuardedBy ("RW_LOCK")
  private static final Map <String, CacheValue> MAP = new CommonsHashMap <> ();
  private static final Duration CLEANSING_INTERVAL = Duration.ofMinutes (5);
  @GuardedBy ("RW_LOCK")
  private static LocalDateTime s_aNextCacheCleansingDT = PDTFactory.getCurrentLocalDateTime ()
                                                                   .plus (CLEANSING_INTERVAL);

  private IALCache ()
  {}

  @Nonnull
  private static String _getKey (@Nonnull final IParticipantIdentifier aParticipantID,
                                 @Nonnull final IDocumentTypeIdentifier aDocumentTypeID)
  {
    return aParticipantID.getURIEncoded () + "@" + aDocumentTypeID.getURIEncoded ();
  }

  private static void _removeExpiredCacheEntries ()
  {
    final LocalDateTime aNow = PDTFactory.getCurrentLocalDateTime ();

    // Expire all entries older then the threshold, every 5 minutes
    if (aNow.isAfter (RW_LOCK.readLockedGet ( () -> s_aNextCacheCleansingDT)))
    {
      LOGGER.info ("Expiring IAL SMP cache entries if necessary");
      RW_LOCK.writeLocked ( () -> {
        int nExpired = 0;
        // Iterate on copy
        for (final Map.Entry <String, CacheValue> aEntry : new CommonsHashSet <> (MAP.entrySet ()))
          if (aEntry.getValue ().isExpiredAt (aNow))
          {
            if (LOGGER.isDebugEnabled ())
              LOGGER.debug ("Expiring IAL SMP entry " + aEntry.getKey () + " from cache");
            MAP.remove (aEntry.getKey ());
            nExpired++;
          }
        s_aNextCacheCleansingDT = aNow.plus (CLEANSING_INTERVAL);
        if (nExpired > 0)
          LOGGER.info ("Expired " + nExpired + " IAL SMP cache entries. " + MAP.size () + " entries left");
      });
    }
  }

  @Nonnull
  public static ETriState getState (@Nonnull final IParticipantIdentifier aParticipantID,
                                    @Nonnull final IDocumentTypeIdentifier aDocumentTypeID)
  {
    _removeExpiredCacheEntries ();

    // Main cache lookup
    final String sKey = _getKey (aParticipantID, aDocumentTypeID);
    final CacheValue aValue = RW_LOCK.readLockedGet ( () -> MAP.get (sKey));
    return aValue == null ? ETriState.UNDEFINED : ETriState.valueOf (aValue.isFound ());
  }

  public static void cacheState (@Nonnull final IParticipantIdentifier aParticipantID,
                                 @Nonnull final IDocumentTypeIdentifier aDocumentTypeID,
                                 final boolean bFound)
  {
    final String sKey = _getKey (aParticipantID, aDocumentTypeID);

    RW_LOCK.writeLocked ( () -> MAP.put (sKey, new CacheValue (bFound)));
  }

  public static void clearCache ()
  {
    LOGGER.info ("Clearing IAL cache");
    final int ret = RW_LOCK.writeLockedInt ( () -> {
      final int ret2 = MAP.size ();
      MAP.clear ();
      return ret2;
    });
    LOGGER.info ("Finished clearing IAL cache - " + ret + " entries evicted");
  }
}
