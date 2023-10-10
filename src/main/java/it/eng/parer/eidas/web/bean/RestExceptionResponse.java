package it.eng.parer.eidas.web.bean;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.AccessMode;
import it.eng.parer.eidas.model.EidasDataToValidateMetadata;
import it.eng.parer.eidas.model.exception.EidasParerException;

/**
 *
 */
@JsonIgnoreProperties(value = { "eidasParerException", "localizedMessage", "suppressed", "cause", "stackTrace" })
public class RestExceptionResponse extends EidasParerException {

    private final EidasParerException eidasParerException;

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private final LocalDateTime datetime = LocalDateTime.now();

    /**
     * 
     */
    private static final long serialVersionUID = 225598790808095611L;

    public RestExceptionResponse() {
        super();
        this.eidasParerException = new EidasParerException();
    }

    public RestExceptionResponse(EidasParerException eidasParerException) {
        super();
        this.eidasParerException = eidasParerException;
    }

    /*
     * inherit properties
     */
    @Override
    public String getMessage() {
        return getEidasParerException().getMessage();
    }

    @Override
    public String getMoreInfo() {
        return getEidasParerException().getMoreInfo();
    }

    @Override
    public ErrorCode getCode() {
        return getEidasParerException().getCode();
    }

    @Override
    public List<String> getDetails() {
        return getEidasParerException().getDetails();
    }

    /*
     * local property
     */
    public LocalDateTime getDatetime() {
        return datetime;
    }

    @Schema(hidden = true, accessMode = AccessMode.READ_ONLY)
    public EidasParerException getEidasParerException() {
        return eidasParerException;
    }

    @Override
    public EidasParerException withMoreInfo(String moreInfo) {
        return getEidasParerException().withMoreInfo(moreInfo);
    }

    @Override
    public EidasParerException withCode(ErrorCode code) {
        return getEidasParerException().withCode(code);
    }

    @Override
    public EidasParerException withMessage(String message) {
        return getEidasParerException().withMessage(message);
    }

    @Override
    public EidasParerException withDetail(String message) {
        return getEidasParerException().withDetail(message);
    }

    @JsonInclude(Include.NON_NULL)
    @Override
    public EidasDataToValidateMetadata getMetadata() {
        return getEidasParerException().getMetadata();
    }

    @Schema(hidden = true, accessMode = AccessMode.READ_ONLY)
    @Override
    public String getLocalizedMessage() {
        return super.getLocalizedMessage();
    }

    @Override
    public String toString() {
        return super.toString() + " - datetime=" + datetime;
    }

}
