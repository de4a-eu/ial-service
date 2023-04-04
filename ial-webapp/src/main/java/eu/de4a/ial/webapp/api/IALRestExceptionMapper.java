package eu.de4a.ial.webapp.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.http.CHttp;
import com.helger.commons.state.EHandled;
import com.helger.commons.string.StringHelper;
import com.helger.photon.api.AbstractAPIExceptionMapper;
import com.helger.photon.api.InvokableAPIDescriptor;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

import eu.de4a.ial.webapp.config.IALConfig;

/**
 * Special API exception mapper for the SMP REST API.
 *
 * @author Philip Helger
 */
public class IALRestExceptionMapper extends AbstractAPIExceptionMapper
{
  static final String LOG_PREFIX = "[REST API] ";

  private static final Logger LOGGER = LoggerFactory.getLogger (IALRestExceptionMapper.class);

  private static void _logRestException (@Nonnull final String sMsg, @Nonnull final Throwable t)
  {
    _logRestException (sMsg, t, false);
  }

  private static void _logRestException (@Nonnull final String sMsg,
                                         @Nonnull final Throwable t,
                                         final boolean bForceNoStackTrace)
  {
    if (IALConfig.REST.isLogExceptions ())
    {
      if (bForceNoStackTrace)
        LOGGER.error (LOG_PREFIX + sMsg + " - " + getResponseEntityWithoutStackTrace (t));
      else
        LOGGER.error (LOG_PREFIX + sMsg, t);
    }
    else
      LOGGER.error (LOG_PREFIX +
                    sMsg +
                    " - " +
                    getResponseEntityWithoutStackTrace (t) +
                    " (turn on REST exception logging to see all details)");
  }

  private static void _setSimpleTextResponse (@Nonnull final UnifiedResponse aUnifiedResponse,
                                              final int nStatusCode,
                                              @Nullable final String sContent)
  {
    if (IALConfig.REST.isPayloadOnError ())
    {
      // With payload
      setSimpleTextResponse (aUnifiedResponse, nStatusCode, sContent);
      if (StringHelper.hasText (sContent))
        aUnifiedResponse.disableCaching ();
    }
    else
    {
      // No payload
      aUnifiedResponse.setStatus (nStatusCode);
    }
  }

  @Nonnull
  public EHandled applyExceptionOnResponse (@Nonnull final InvokableAPIDescriptor aInvokableDescriptor,
                                            @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                                            @Nonnull final UnifiedResponse aUnifiedResponse,
                                            @Nonnull final Throwable aThrowable)
  {
    // From specific to general
    if (aThrowable instanceof IALInternalErrorException)
    {
      _logRestException ("Internal error", aThrowable);
      _setSimpleTextResponse (aUnifiedResponse,
                              CHttp.HTTP_INTERNAL_SERVER_ERROR,
                              GlobalDebug.isDebugMode () ? getResponseEntityWithStackTrace (aThrowable)
                                                         : getResponseEntityWithoutStackTrace (aThrowable));
      return EHandled.HANDLED;
    }
    if (aThrowable instanceof IALBadRequestException)
    {
      // Forcing no stack trace, because the context should be self-explanatory
      _logRestException ("Bad request", aThrowable, true);
      _setSimpleTextResponse (aUnifiedResponse,
                              CHttp.HTTP_BAD_REQUEST,
                              getResponseEntityWithoutStackTrace (aThrowable));
      return EHandled.HANDLED;
    }
    if (aThrowable instanceof RuntimeException)
    {
      _logRestException ("Runtime exception - " + aThrowable.getClass ().getName (), aThrowable);
      _setSimpleTextResponse (aUnifiedResponse,
                              CHttp.HTTP_INTERNAL_SERVER_ERROR,
                              GlobalDebug.isDebugMode () ? getResponseEntityWithStackTrace (aThrowable)
                                                         : getResponseEntityWithoutStackTrace (aThrowable));
      return EHandled.HANDLED;
    }

    // We don't know that exception
    return EHandled.UNHANDLED;
  }
}
