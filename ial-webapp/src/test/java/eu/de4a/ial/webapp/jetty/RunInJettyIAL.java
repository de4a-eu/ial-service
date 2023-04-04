package eu.de4a.ial.webapp.jetty;

import javax.annotation.concurrent.Immutable;

import com.helger.photon.jetty.JettyStarter;

/**
 * Run as a standalone web application in Jetty on port 8080.<br>
 * http://localhost:8080/
 *
 * @author Philip Helger
 */
@Immutable
public final class RunInJettyIAL
{
  public static void main (final String [] args) throws Exception
  {
    final JettyStarter js = new JettyStarter (RunInJettyIAL.class).setSessionCookieName ("IAL_SESSION")
                                                                  .setContainerIncludeJarPattern (JettyStarter.CONTAINER_INCLUDE_JAR_PATTERN_ALL);
    js.run ();
  }
}
