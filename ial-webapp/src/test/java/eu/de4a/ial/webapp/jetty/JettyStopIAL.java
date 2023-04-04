package eu.de4a.ial.webapp.jetty;

import java.io.IOException;

import com.helger.photon.jetty.JettyStopper;

public final class JettyStopIAL
{
  public static void main (final String [] args) throws IOException
  {
    new JettyStopper ().run ();
  }
}
