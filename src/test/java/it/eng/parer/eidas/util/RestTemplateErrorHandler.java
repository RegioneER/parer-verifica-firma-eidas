/*
 * Engineering Ingegneria Informatica S.p.A.
 *
 * Copyright (C) 2023 Regione Emilia-Romagna
 * <p/>
 * This program is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package it.eng.parer.eidas.util;

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

/**
 * <p>
 * RestTemplateErrorHandler class.
 * </p>
 *
 * @author stefano
 * 
 * @version $Id: $Id
 * 
 * @since 1.10.1
 */
@Component
public class RestTemplateErrorHandler implements ResponseErrorHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RestTemplateErrorHandler.class);

    /** {@inheritDoc} */
    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return (response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR
                || response.getStatusCode() == HttpStatus.BAD_REQUEST);
    }

    /** {@inheritDoc} */
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
    /** {@inheritDoc} */
    @Override
    public void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
        // TODO Auto-generated method stub
        ResponseErrorHandler.super.handleError(url, method, response);
    }

}
