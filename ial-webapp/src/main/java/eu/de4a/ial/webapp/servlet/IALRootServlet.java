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
package eu.de4a.ial.webapp.servlet;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.http.CHttpHeader;
import com.helger.commons.mime.CMimeType;

import eu.de4a.ial.api.IALVersion;

/**
 * Servlet for handling the initial calls without any path. This servlet shows
 * some basic information.
 *
 * @author Philip Helger
 */
@WebServlet ("")
public class IALRootServlet extends HttpServlet
{
  private static final Logger LOGGER = LoggerFactory.getLogger (IALRootServlet.class);

  @Override
  protected void doGet (@Nonnull final HttpServletRequest req,
                        @Nonnull final HttpServletResponse resp) throws ServletException, IOException
  {
    final String sContextPath = req.getServletContext ().getContextPath ();
    final String sCSS = "* { font-family: sans-serif; }" +
                        " a:link, a:visited, a:hover, a:active { color: #2255ff; }" +
                        " code { font-family:monospace; color:#e83e8c; }";

    final StringBuilder aSB = new StringBuilder ();
    aSB.append ("<html><head><title>DE4A IAL Service</title><style>").append (sCSS).append ("</style></head><body>");
    aSB.append ("<h1>DE4A IAL Service</h1>");
    aSB.append ("<div>Version: ").append (IALVersion.BUILD_VERSION).append ("</div>");
    aSB.append ("<div>Build timestamp: ").append (IALVersion.BUILD_TIMESTAMP).append ("</div>");
    aSB.append ("<div>Current time: ").append (PDTFactory.getCurrentZonedDateTimeUTC ().toString ()).append ("</div>");
    aSB.append ("<div><a href='status'>Check /status</a></div>");
    aSB.append ("<div><a href='https://github.com/de4a-wp5/ial-service' target='_blank'>Source code on GitHub</a></div>");

    if (GlobalDebug.isDebugMode ())
    {
      aSB.append ("<h2>Servlet information</h2>");
      for (final Map.Entry <String, ? extends ServletRegistration> aEntry : CollectionHelper.getSortedByKey (req.getServletContext ()
                                                                                                                .getServletRegistrations ())
                                                                                            .entrySet ())
      {
        aSB.append ("<div>Servlet <code>")
           .append (aEntry.getKey ())
           .append ("</code> mapped to ")
           .append (aEntry.getValue ().getMappings ())
           .append ("</div>");
      }
    }

    // APIs
    {
      aSB.append ("<h2>API information</h2>");

      aSB.append ("<h3>SMP</h3>");
      aSB.append ("<div>GET /provision/{canonicalObjectTypeIDs} - <a href='" +
                  sContextPath +
                  "/api/provision/urn:de4a-eu:CanonicalEvidenceType::CompanyRegistration' target='_blank'>test me</a></div>");
      aSB.append ("<div>GET /provision/{canonicalObjectTypeIDs}/{atuCode} - <a href='" +
                  sContextPath +
                  "/api/provision/urn:de4a-eu:CanonicalEvidenceType::CompanyRegistration/AT' target='_blank'>test me</a></div>");
    }

    aSB.append ("</body></html>");

    resp.addHeader (CHttpHeader.CONTENT_TYPE, CMimeType.TEXT_HTML.getAsString ());
    try
    {
      resp.getWriter ().write (aSB.toString ());
      resp.getWriter ().flush ();
    }
    catch (final IOException ex)
    {
      LOGGER.error ("Failed to write result", ex);
    }
  }
}
