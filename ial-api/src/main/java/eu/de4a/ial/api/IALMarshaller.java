package eu.de4a.ial.api;

import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.xml.bind.JAXBElement;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.jaxb.GenericJAXBMarshaller;

import eu.de4a.ial.api.jaxb.ResponseLookupRoutingInformationType;

/**
 * IAL Marshaller factory
 *
 * @author Philip Helger
 * @param <JAXBTYPE>
 *        The JAXB implementation type
 */
@Immutable
public class IALMarshaller <JAXBTYPE> extends GenericJAXBMarshaller <JAXBTYPE>
{
  public IALMarshaller (@Nonnull final Class <JAXBTYPE> aType,
                        @Nullable final List <? extends ClassPathResource> aXSDs,
                        @Nonnull final Function <? super JAXBTYPE, ? extends JAXBElement <JAXBTYPE>> aJAXBElementWrapper)
  {
    super (aType, aXSDs, aJAXBElementWrapper);
    setNamespaceContext (IALNamespaceContext.getInstance ());
  }

  /**
   * Enable formatted output. Syntactic sugar.
   *
   * @return this for chaining
   */
  @Nonnull
  public final IALMarshaller <JAXBTYPE> formatted ()
  {
    setFormattedOutput (true);
    return this;
  }

  /**
   * @return A new marshaller for the response. Never <code>null</code>.
   */
  @Nonnull
  public static IALMarshaller <ResponseLookupRoutingInformationType> responseLookupRoutingInformationMarshaller ()
  {
    return new IALMarshaller <> (ResponseLookupRoutingInformationType.class,
                                 new CommonsArrayList <> (CIALJAXB.XSD_IAL),
                                 new eu.de4a.ial.api.jaxb.ObjectFactory ()::createResponseLookupRoutingInformation);
  }
}
