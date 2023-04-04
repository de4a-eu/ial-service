package eu.de4a.ial.api;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Singleton;
import com.helger.xml.namespace.MapBasedNamespaceContext;

/**
 * XML Namespace context for IAL
 *
 * @author Philip Helger
 */
@Singleton
public class IALNamespaceContext extends MapBasedNamespaceContext
{
  private static final class SingletonHolder
  {
    static final IALNamespaceContext INSTANCE = new IALNamespaceContext ();
  }

  protected IALNamespaceContext ()
  {
    addMapping ("ial", CIALJAXB.NAMESPACE_URI_IAL);
  }

  @Nonnull
  public static IALNamespaceContext getInstance ()
  {
    return SingletonHolder.INSTANCE;
  }
}
