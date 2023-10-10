package it.eng.parer.eidas.web.converter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.xml.AbstractXmlHttpMessageConverter;

public abstract class CustomAbstractJaxb2HttpMessageConverter<T> extends AbstractXmlHttpMessageConverter<T> {

    private final ConcurrentMap<Class<?>, JAXBContext> jaxbContexts = new ConcurrentHashMap<>(64);

    protected final Marshaller createMarshaller(Class<?> clazz) {
        try {
            JAXBContext jaxbContext = getJaxbContext(clazz);
            Marshaller marshaller = jaxbContext.createMarshaller();
            customizeMarshaller(marshaller);
            return marshaller;
        } catch (JAXBException ex) {
            throw new HttpMessageConversionException(
                    "Could not create Marshaller for class [" + clazz + "]: " + ex.getMessage(), ex);
        }
    }

    protected void customizeMarshaller(Marshaller marshaller) {
    }

    protected final Unmarshaller createUnmarshaller(Class<?> clazz) {
        try {
            JAXBContext jaxbContext = getJaxbContext(clazz);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            customizeUnmarshaller(unmarshaller);
            return unmarshaller;
        } catch (JAXBException ex) {
            throw new HttpMessageConversionException(
                    "Could not create Unmarshaller for class [" + clazz + "]: " + ex.getMessage(), ex);
        }
    }

    protected void customizeUnmarshaller(Unmarshaller unmarshaller) {
    }

    protected final JAXBContext getJaxbContext(Class<?> clazz) {
        return this.jaxbContexts.computeIfAbsent(clazz, key -> {
            try {
                return JAXBContext.newInstance(clazz);
            } catch (JAXBException ex) {
                throw new HttpMessageConversionException(
                        "Could not create JAXBContext for class [" + clazz + "]: " + ex.getMessage(), ex);
            }
        });
    }

}
