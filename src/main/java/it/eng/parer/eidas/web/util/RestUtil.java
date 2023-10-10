package it.eng.parer.eidas.web.util;

import static it.eng.parer.eidas.web.util.EndPointCostants.URL_ERRORS;
import static it.eng.parer.eidas.core.util.Constants.STD_MSG_GENERIC_ERROR;

import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import it.eng.parer.eidas.model.exception.EidasParerException;
import it.eng.parer.eidas.model.exception.ParerError;
import it.eng.parer.eidas.model.exception.ParerError.ErrorCode;
import it.eng.parer.eidas.web.bean.RestExceptionResponse;
import java.util.List;

public class RestUtil {

    private RestUtil() {
        throw new IllegalStateException("RestUtil class");
    }

    public static RestExceptionResponse buildParerResponseEntity(EidasParerException epex, WebRequest request) {
        final String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        epex.withDetail(((ServletWebRequest) request).getRequest().getRequestURL().toString())
                .withMoreInfo(baseUrl + URL_ERRORS + "/" + epex.getCode().urlFriendly());
        return new RestExceptionResponse(epex);
    }

    public static RestExceptionResponse buildGenericResponseEntity(WebRequest request) {
        final String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        RestExceptionResponse errorDetails = new RestExceptionResponse();
        errorDetails.withMessage(STD_MSG_GENERIC_ERROR).withCode(ErrorCode.GENERIC_ERROR)
                .withDetail(((ServletWebRequest) request).getRequest().getRequestURL().toString())
                .withMoreInfo(baseUrl + URL_ERRORS + "/" + ParerError.ErrorCode.GENERIC_ERROR.urlFriendly());
        return errorDetails;
    }

    public static RestExceptionResponse buildValidationException(String message, List<String> details) {
        final String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        EidasParerException exception = new EidasParerException().withCode(ParerError.ErrorCode.VALIDATION_ERROR)
                .withMessage(message).withDetails(details)
                .withMoreInfo(baseUrl + URL_ERRORS + "/" + ParerError.ErrorCode.VALIDATION_ERROR.urlFriendly());
        return new RestExceptionResponse(exception);
    }

}
