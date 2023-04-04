package eu.de4a.ial.api;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.commons.io.resource.ClassPathResource;

/**
 * Test class for class {@link CIALJAXB}.
 *
 * @author Philip Helger
 */
public final class CIALJAXBTest
{
  @Test
  public void testBasic ()
  {
    for (final ClassPathResource aCP : new ClassPathResource [] { CIALJAXB.XSD_IAL })
      assertTrue (aCP.getPath (), aCP.exists ());
  }
}
