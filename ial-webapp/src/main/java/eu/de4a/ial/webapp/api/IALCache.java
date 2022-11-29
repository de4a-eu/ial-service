package eu.de4a.ial.webapp.api;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.CommonsHashSet;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.state.ETriState;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;

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

  @Nonnull
  public static ETriState getState (@Nonnull final IParticipantIdentifier aParticipantID,
                                    @Nonnull final IDocumentTypeIdentifier aDocumentTypeID)
  {
    final LocalDateTime aNow = PDTFactory.getCurrentLocalDateTime ();

    // Expire all entries older then the threshold, every 5 minutes
    if (aNow.isAfter (RW_LOCK.readLockedGet ( () -> s_aNextCacheCleansingDT)))
    {
      LOGGER.info ("Expiring IAL SMP cache entries if necessary");
      RW_LOCK.writeLocked ( () -> {
        // Iterate on copy
        int nExpired = 0;
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

    // Main cache lookup
    final String sKey = _getKey (aParticipantID, aDocumentTypeID);
    final CacheValue aValue = RW_LOCK.readLockedGet ( () -> MAP.get (sKey));
    return aValue == null ? ETriState.UNDEFINED : ETriState.valueOf (aValue.isFound ());
  }

  public static void cacheState (@Nonnull final IParticipantIdentifier aParticipantID,
                                 @Nonnull final IDocumentTypeIdentifier aDocumentTypeID,
                                 final boolean bState)
  {
    final String sKey = _getKey (aParticipantID, aDocumentTypeID);

    RW_LOCK.writeLocked ( () -> MAP.put (sKey, new CacheValue (bState)));
  }

  public static void clearCache ()
  {
    RW_LOCK.writeLocked ( () -> MAP.clear ());
  }
}
