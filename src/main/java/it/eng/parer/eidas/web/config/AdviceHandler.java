package it.eng.parer.eidas.web.config;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

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
        // log ONLY on debug (exception managed by application)
        LOG.debug(ExceptionUtils.getRootCauseMessage(ex));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(RestUtil.buildParerResponseEntity(ex, request), headers,
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public final ResponseEntity<RestExceptionResponse> handleMediaTypeException(HttpMediaTypeNotSupportedException ex,
            WebRequest request) {
        // log generic exception
        LOG.error(ExceptionUtils.getRootCauseMessage(ex));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        List<String> supportedMediaTypes = ex.getSupportedMediaTypes().stream()
                .map(mt -> "Supported media type: " + mt.toString()).collect(Collectors.toList());

        return new ResponseEntity<>(RestUtil.buildValidationException(ex.getMessage(), supportedMediaTypes), headers,
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<RestExceptionResponse> handleMaxSizeException(MaxUploadSizeExceededException ex,
            WebRequest request) {
        // log generic exception
        LOG.error(ExceptionUtils.getRootCauseMessage(ex));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(RestUtil.buildGenericResponseEntity(request), headers,
                HttpStatus.EXPECTATION_FAILED);
    }

    @ExceptionHandler(Throwable.class)
    public final ResponseEntity<RestExceptionResponse> handleGenericException(Exception ex, WebRequest request) {
        // log generic exception
        LOG.error(ExceptionUtils.getRootCauseMessage(ex));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(RestUtil.buildGenericResponseEntity(request), headers, HttpStatus.BAD_REQUEST);
    }

}
