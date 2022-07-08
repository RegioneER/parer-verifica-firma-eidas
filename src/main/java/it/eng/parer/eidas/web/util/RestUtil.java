package it.eng.parer.eidas.web.util;

import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import it.eng.parer.eidas.model.exception.EidasParerException;
import it.eng.parer.eidas.model.exception.ParerError;
import it.eng.parer.eidas.model.exception.ParerError.ErrorCode;
import it.eng.parer.eidas.web.bean.RestExceptionResponse;
import java.util.List;

public class RestUtil {

    private static final String ERRORS_ENDPOINT = "/v1/errors/";

    private RestUtil() {
        throw new IllegalStateException("RestUtil class");
    }

    public static RestExceptionResponse buildParerResponseEntity(EidasParerException epex, WebRequest request) {
        final String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        epex.withDetail(((ServletWebRequest) request).getRequest().getRequestURL().toString())
                .withMoreInfo(baseUrl + ERRORS_ENDPOINT + epex.getCode().urlFriendly());
        return new RestExceptionResponse(epex);
    }

    public static RestExceptionResponse buildGenericResponseEntity(WebRequest request) {
        final String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        RestExceptionResponse errorDetails = new RestExceptionResponse();
        errorDetails.withMessage("Errore applicativo").withCode(ErrorCode.GENERIC_ERROR)
                .withDetail(((ServletWebRequest) request).getRequest().getRequestURL().toString())
                .withMoreInfo(baseUrl + ERRORS_ENDPOINT + errorDetails.getCode().urlFriendly());
        return errorDetails;
    }

    public static RestExceptionResponse buildValidationException(String message, List<String> details) {
        final String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();

        EidasParerException exception = new EidasParerException().withCode(ParerError.ErrorCode.VALIDATION_ERROR)
                .withMessage(message).withDetails(details)
                .withMoreInfo(baseUrl + ERRORS_ENDPOINT + ParerError.ErrorCode.VALIDATION_ERROR.urlFriendly());
        return new RestExceptionResponse(exception);
    }

}
