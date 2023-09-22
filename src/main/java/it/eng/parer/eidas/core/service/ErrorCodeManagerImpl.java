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

package it.eng.parer.eidas.core.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import it.eng.parer.eidas.model.exception.ParerError;
import it.eng.parer.eidas.model.exception.ParerError.ErrorCode;

/**
 *
 * @author Snidero_L
 */
@Service
public class ErrorCodeManagerImpl implements IErrorCodeManager {

    /**
     * Escluse il codice "particolare" NOT_FOUND
     * 
     * @return lista di errori
     * 
     */
    @Override
    public List<ErrorCode> get() {
        return Stream.of(ParerError.ErrorCode.values()).filter(e -> !e.equals(ErrorCode.NOT_FOUND))
                .collect(Collectors.toList());
    }

    @Override
    public String getSummary(String errorCode) {
        ParerError.ErrorCode code = ParerError.ErrorCode.fromUrlFriendly(errorCode);
        return "Errore classificato come: " + code.name() + " - " + decodeType(code.exceptionType());
    }

    /**
     * Anche in questo metodo per il momento non utilizzo il DB
     *
     * @param type
     *            tipologia di errore
     * 
     * @return decodifica della tipologia. Non viene mai restituito null.
     */
    private String decodeType(ParerError.ExceptionType type) {
        final String decode;
        switch (type) {
        case GENERIC:
            decode = "generico";
            break;
        case EIDAS_DSS:
            decode = "dss lib";
            break;
        case IO:
            decode = "I/O";
            break;
        case REQ:
            decode = "request";
            break;
        case VALIDATION:
            decode = "validation";
            break;
        default:
            decode = "non gestito";
        }
        return decode;
    }

    @Override
    public String getDescription(String errorCode) {
        final String message;
        ParerError.ErrorCode code = ParerError.ErrorCode.fromUrlFriendly(errorCode);
        switch (code) {
        case GENERIC_ERROR:
            message = "Errore generico di sistema";
            break;
        case EIDAS_ERROR:
            message = "Verifica del documento firmato non riuscita";
            break;
        case IO_ERROR:
            message = "Errore generico di sistema (I/O)";
            break;
        case BAD_FILENAME_MULTIPARTFILE_AND_METADATA:
            message = "Errore su chiamata multipart/form-data, nome file elemento non trovato su metadati";
            break;
        case METADATA_ERROR:
            message = "Errore sui metadati, assente uno o più campi obbligatori";
            break;
        case VALIDATION_ERROR:
            message = "Errore di validazione, il formato di uno o più campi risulta errato";
            break;
        default:
            message = "Errore non trovato " + errorCode;
        }
        return message;
    }

}
