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
