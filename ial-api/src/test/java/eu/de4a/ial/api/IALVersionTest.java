package eu.de4a.ial.api;

import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

/**
 * Test class for class {@link IALVersion}
 *
 * @author Philip Helger
 */
public final class IALVersionTest
{
  @Test
  public void testBasic ()
  {
    assertNotEquals ("undefined", IALVersion.BUILD_VERSION);
    assertNotEquals ("undefined", IALVersion.BUILD_TIMESTAMP);
  }
}
