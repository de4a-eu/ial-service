package eu.de4a.ial.webapp;

import org.junit.Test;

import com.helger.commons.mock.SPITestHelper;
import com.helger.photon.core.mock.PhotonCoreValidator;

/**
 * Test SPI definitions and web.xml
 *
 * @author Philip Helger
 */
public final class SPITest
{
  @Test
  public void testBasic () throws Exception
  {
    SPITestHelper.testIfAllSPIImplementationsAreValid ();
    PhotonCoreValidator.validateExternalResources ();
  }
}
