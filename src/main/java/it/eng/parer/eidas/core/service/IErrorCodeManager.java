/**
 * 
 */
package it.eng.parer.eidas.core.service;

import java.util.List;

import it.eng.parer.eidas.model.exception.ParerError.ErrorCode;

public interface IErrorCodeManager {

    /**
     * Escluse il codice "particolare" NOT_FOUND
     * 
     * @return lista di errori
     * 
     */
    List<ErrorCode> get();

    String getSummary(String errorCode);

    String getDescription(String errorCode);

}