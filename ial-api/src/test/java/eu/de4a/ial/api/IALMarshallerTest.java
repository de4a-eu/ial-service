package eu.de4a.ial.api;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import javax.annotation.Nonnull;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.jaxb.GenericJAXBMarshaller;

/**
 * Test class for class {@link IALMarshaller}.
 *
 * @author Philip Helger
 */
public final class IALMarshallerTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (IALMarshallerTest.class);
  private static final String BASE_PATH = "src/test/resources/ial/";

  private static <T> void _testReadWrite (@Nonnull final GenericJAXBMarshaller <T> aMarshaller,
                                          @Nonnull final File aFile)
  {
    assertTrue ("Test file does not exists " + aFile.getAbsolutePath (), aFile.exists ());

    if (false)
    {
      aMarshaller.readExceptionCallbacks ().set (ex -> LOGGER.error ("Read error", ex));
      aMarshaller.writeExceptionCallbacks ().set (ex -> LOGGER.error ("Write error", ex));
    }

    final T aRead = aMarshaller.read (aFile);
    assertNotNull ("Failed to read " + aFile.getAbsolutePath (), aRead);

    final byte [] aBytes = aMarshaller.getAsBytes (aRead);
    assertNotNull ("Failed to re-write " + aFile.getAbsolutePath (), aBytes);

    if (false)
    {
      aMarshaller.setFormattedOutput (true);
      LOGGER.info (aMarshaller.getAsString (aRead));
    }
  }

  @Test
  public void testIDK_LookupRoutingInformation ()
  {
    // Response
    _testReadWrite (IALMarshaller.responseLookupRoutingInformationMarshaller (),
                    new File (BASE_PATH + "IDK-response-routing.xml"));
    _testReadWrite (IALMarshaller.responseLookupRoutingInformationMarshaller (),
                    new File (BASE_PATH + "IDK-response-routing-min.xml"));
  }
}
