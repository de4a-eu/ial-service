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
package eu.de4a.ial.webapp.config;

import javax.annotation.CheckForSigned;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.debug.GlobalDebug;
import com.helger.config.Config;
import com.helger.config.ConfigFactory;
import com.helger.config.IConfig;
import com.helger.config.source.MultiConfigurationValueProvider;

/**
 * This class contains global configuration elements for the DE4A IAL Service.
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class IALConfig
{
  private static final SimpleReadWriteLock RW_LOCK = new SimpleReadWriteLock ();

  @GuardedBy ("RW_LOCK")
  private static IConfig s_aConfig;

  static
  {
    setDefaultConfig ();
  }

  private IALConfig ()
  {}

  /**
   * @return The configuration file. Never <code>null</code>.
   */
  @Nonnull
  public static IConfig getConfig ()
  {
    return RW_LOCK.readLockedGet ( () -> s_aConfig);
  }

  /**
   * Set a different configuration. E.g. for testing.
   *
   * @param aConfig
   *        The config to be set. May not be <code>null</code>.
   */
  public static void setConfig (@Nonnull final IConfig aConfig)
  {
    ValueEnforcer.notNull (aConfig, "Config");
    RW_LOCK.writeLocked ( () -> s_aConfig = aConfig);
  }

  /**
   * Set the default configuration.
   */
  public static void setDefaultConfig ()
  {
    final MultiConfigurationValueProvider aMCSVP = ConfigFactory.createDefaultValueProvider ();
    final IConfig aConfig = Config.create (aMCSVP);
    setConfig (aConfig);
  }

  /**
   * Global settings.
   *
   * @author Philip Helger
   */
  public static final class Global
  {
    private Global ()
    {}

    public static boolean isGlobalDebug ()
    {
      return getConfig ().getAsBoolean ("global.debug", GlobalDebug.isDebugMode ());
    }

    public static boolean isGlobalProduction ()
    {
      return getConfig ().getAsBoolean ("global.production", GlobalDebug.isProductionMode ());
    }

    @Nullable
    public static String getDE4AInstanceName ()
    {
      return getConfig ().getAsString ("global.instancename");
    }
  }

  /**
   * Global HTTP settings
   *
   * @author Philip Helger
   */
  public static final class HTTP
  {
    private HTTP ()
    {}

    public static boolean isProxyServerEnabled ()
    {
      return getConfig ().getAsBoolean ("http.proxy.enabled", false);
    }

    @Nullable
    public static String getProxyServerAddress ()
    {
      // Scheme plus hostname or IP address
      return getConfig ().getAsString ("http.proxy.address");
    }

    @CheckForSigned
    public static int getProxyServerPort ()
    {
      return getConfig ().getAsInt ("http.proxy.port", -1);
    }

    @Nullable
    public static String getProxyServerNonProxyHosts ()
    {
      // Separated by pipe
      return getConfig ().getAsString ("http.proxy.non-proxy");
    }

    public static boolean isTLSTrustAll ()
    {
      return getConfig ().getAsBoolean ("http.tls.trustall", false);
    }

    public static int getConnectionTimeoutMS ()
    {
      // -1 = system default
      return getConfig ().getAsInt ("http.connection-timeout", -1);
    }

    public static int getReadTimeoutMS ()
    {
      // -1 = system default
      return getConfig ().getAsInt ("http.read-timeout", -1);
    }
  }

  /**
   * Directory related settings
   *
   * @author Philip Helger
   */
  public static final class Directory
  {
    private Directory ()
    {}

    /**
     * @return The DE4A Directory URL to base queries on. Should not be
     *         <code>null</code>.
     */
    @Nullable
    public static String getBaseURL ()
    {
      return getConfig ().getAsString ("de4a.directory.url");
    }
  }

  public static final class REST
  {
    private REST ()
    {}

    public static boolean isPayloadOnError ()
    {
      return getConfig ().getAsBoolean ("ial.rest.payload-on-error", true);
    }

    public static boolean isLogExceptions ()
    {
      return getConfig ().getAsBoolean ("ial.rest.log-exceptions", true);
    }
  }

  /**
   * Settings for the web application
   *
   * @author Philip Helger
   */
  public static final class WebApp
  {
    private WebApp ()
    {}

    /**
     * @return <code>true</code> if the <code>/status</code> API is enabled and
     *         may return details, <code>false</code> if not. Defaults to
     *         <code>true</code>.
     */
    public static boolean isStatusEnabled ()
    {
      return getConfig ().getAsBoolean ("ial.webapp.status.enabled", true);
    }

    /**
     * @return The storage path for files etc. inside the IAL.
     */
    @Nullable
    public static String getDataPath ()
    {
      return getConfig ().getAsString ("ial.webapp.data.path");
    }
  }
}
