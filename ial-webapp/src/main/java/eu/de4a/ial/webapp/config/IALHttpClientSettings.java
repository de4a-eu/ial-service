package eu.de4a.ial.webapp.config;

import java.security.GeneralSecurityException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.httpclient.HttpClientSettings;

public final class IALHttpClientSettings extends HttpClientSettings
{
  private static final Logger LOGGER = LoggerFactory.getLogger (IALHttpClientSettings.class);

  public IALHttpClientSettings () throws GeneralSecurityException
  {
    if (IALConfig.Directory.isTLSTrustAll ())
    {
      LOGGER.warn ("The TLS connection trusts all certificates. That is not very secure.");
      // This block is not nice but needed, because the system truststore of the
      // machine running the IAL is empty.
      // For a real production scenario, a separate trust store should be
      // configured.
      setSSLContextTrustAll ();
    }
  }
}
