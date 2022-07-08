package it.eng.parer.eidas.client.util;

import java.io.IOException;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResponseErrorHandler;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.eng.parer.eidas.model.exception.EidasParerException;

@Component
public class RestTemplateErrorHandler implements ResponseErrorHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RestTemplateErrorHandler.class);

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return (response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR
                || response.getStatusCode() == HttpStatus.BAD_REQUEST);
    }

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        EidasParerException exe = mapper.readValue(response.getBody(), EidasParerException.class);
        LOG.error("Eccezione registrata {} con codice {}", exe, exe.getCode());
        throw exe;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.client.ResponseErrorHandler#handleError(java.net.URI,
     * org.springframework.http.HttpMethod, org.springframework.http.client.ClientHttpResponse)
     */
    @Override
    public void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
        // TODO Auto-generated method stub
        ResponseErrorHandler.super.handleError(url, method, response);
    }

}