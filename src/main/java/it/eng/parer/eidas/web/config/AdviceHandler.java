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

package it.eng.parer.eidas.web.config;

import static it.eng.parer.eidas.core.util.Constants.STD_MSG_APP_ERROR;
import static it.eng.parer.eidas.core.util.Constants.STD_MSG_GENERIC_ERROR;
import static it.eng.parer.eidas.core.util.Constants.STD_MSG_VALIDATION_ERROR;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import it.eng.parer.eidas.model.exception.EidasParerException;
import it.eng.parer.eidas.web.bean.RestExceptionResponse;
import it.eng.parer.eidas.web.util.RestUtil;

/**
 * Gestione delle eccezioni.
 *
 * @author sinatti_s
 */
@ControllerAdvice(basePackages = { "it.eng.parer.eidas.web.rest" })
@RequestMapping(produces = MediaType.APPLICATION_PROBLEM_JSON_VALUE)
public class AdviceHandler {

    /*
     * Aggiunto log al fine di loggare i casi di generiche eccezioni ossia generate dopo l'esecuzione del servizio di
     * verifica firma (o prima quando si valuta la risposta del client), ma che comunque in generale non sono gestite
     * (EidasParerException) all'interno dell'implementazione dell'endpoint.
     */
    private static final Logger LOG = LoggerFactory.getLogger(AdviceHandler.class);

    @ExceptionHandler(EidasParerException.class)
    public final ResponseEntity<RestExceptionResponse> handleEidasParerException(EidasParerException ex,
            WebRequest request) {
        // log error
        LOG.error(STD_MSG_APP_ERROR, ex);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(RestUtil.buildParerResponseEntity(ex, request), headers,
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public final ResponseEntity<RestExceptionResponse> handleMediaTypeException(HttpMediaTypeNotSupportedException ex,
            WebRequest request) {
        // log generic exception
        LOG.error(STD_MSG_VALIDATION_ERROR, ex);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(
                RestUtil.buildValidationException(STD_MSG_VALIDATION_ERROR, Arrays.asList(ex.getBody().getDetail())),
                headers, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RestExceptionResponse> handleMethodArgumentsException(MethodArgumentNotValidException ex,
            WebRequest request) {
        // log generic exception
        LOG.error(STD_MSG_VALIDATION_ERROR, ex);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(
                RestUtil.buildValidationException(STD_MSG_VALIDATION_ERROR, Arrays.asList(ex.getBody().getDetail())),
                headers, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageConversionException.class)
    public ResponseEntity<RestExceptionResponse> handleHttpMessageConversionException(HttpMessageConversionException ex,
            WebRequest request) {
        // log generic exception
        LOG.error(STD_MSG_VALIDATION_ERROR, ex);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(RestUtil.buildValidationException(STD_MSG_VALIDATION_ERROR,
                Arrays.asList("Contenuto di metadata non corretto")), headers, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<RestExceptionResponse> handleMissingServletRequestPartException(
            MissingServletRequestPartException ex, WebRequest request) {
        // log generic exception
        LOG.error(STD_MSG_VALIDATION_ERROR, ex);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(
                RestUtil.buildValidationException(STD_MSG_VALIDATION_ERROR, Arrays.asList(ex.getBody().getDetail())),
                headers, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<RestExceptionResponse> handleMaxSizeException(MaxUploadSizeExceededException ex,
            WebRequest request) {
        // log generic exception
        LOG.error(STD_MSG_VALIDATION_ERROR, ex);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(
                RestUtil.buildValidationException(STD_MSG_VALIDATION_ERROR,
                        Arrays.asList("Dimensione massima consentita di upload " + ex.getMaxUploadSize() + " MB")),
                headers, HttpStatus.EXPECTATION_FAILED);
    }

    @ExceptionHandler(Throwable.class)
    public final ResponseEntity<RestExceptionResponse> handleGenericException(Exception ex, WebRequest request) {
        // log generic exception
        LOG.error(STD_MSG_GENERIC_ERROR, ex);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(RestUtil.buildGenericResponseEntity(request), headers,
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
