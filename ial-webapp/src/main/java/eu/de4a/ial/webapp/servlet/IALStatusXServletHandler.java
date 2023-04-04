package eu.de4a.ial.webapp.servlet;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.MimeType;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xservlet.handler.simple.IXServletSimpleHandler;

import eu.de4a.ial.webapp.config.IALConfig;
import eu.de4a.ial.webapp.status.IALStatusHelper;

/**
 * Main handler for the /status servlet
 *
 * @author Philip Helger
 */
final class IALStatusXServletHandler implements IXServletSimpleHandler
{
  private static final Logger LOGGER = LoggerFactory.getLogger (IALStatusXServletHandler.class);
  private static final Charset CHARSET = StandardCharsets.UTF_8;

  public void handleRequest (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                             @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Status information requested");

    // Build data to provide
    final IJsonObject aStatusData;
    if (IALConfig.WebApp.isStatusEnabled ())
    {
      aStatusData = IALStatusHelper.getDefaultStatusData ();
    }
    else
    {
      // Status is disabled in the configuration
      aStatusData = new JsonObject ();
      aStatusData.add ("status.enabled", false);
    }

    // Put JSON on response
    aUnifiedResponse.disableCaching ();
    aUnifiedResponse.setMimeType (new MimeType (CMimeType.APPLICATION_JSON).addParameter (CMimeType.PARAMETER_NAME_CHARSET,
                                                                                          CHARSET.name ()));
    aUnifiedResponse.setContentAndCharset (aStatusData.getAsJsonString (), CHARSET);

    if (LOGGER.isTraceEnabled ())
      LOGGER.trace ("Return status JSON: " + aStatusData.getAsJsonString ());
  }
}
