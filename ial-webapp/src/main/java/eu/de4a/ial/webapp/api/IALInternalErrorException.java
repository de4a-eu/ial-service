package eu.de4a.ial.webapp.api;

import javax.annotation.Nonnull;

/**
 * Exception that is thrown to indicate an HTTP 500 error.
 *
 * @author Philip Helger
 */
public class IALInternalErrorException extends Exception
{
  public IALInternalErrorException (@Nonnull final String sMsg)
  {
    super (sMsg);
  }

  public IALInternalErrorException (@Nonnull final String sMsg, @Nonnull final Throwable aCause)
  {
    super (sMsg, aCause);
  }
}
