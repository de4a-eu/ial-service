package eu.de4a.ial.api;

import javax.annotation.Nonnull;

import com.helger.commons.io.resource.ClassPathResource;

/**
 * Constants for handling IAL stuff
 *
 * @author Philip Helger
 */
public final class CIALJAXB
{
  @Nonnull
  private static ClassLoader _getCL ()
  {
    return CIALJAXB.class.getClassLoader ();
  }

  public static final String NAMESPACE_URI_IAL = "http://www.de4a.eu/2020/ial/v2";
  public static final ClassPathResource XSD_IAL = new ClassPathResource ("schemas/IAL.xsd", _getCL ());

  private CIALJAXB ()
  {}
}
