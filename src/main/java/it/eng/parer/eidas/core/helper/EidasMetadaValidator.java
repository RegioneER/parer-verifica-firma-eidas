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

/**
 * 
 */
package it.eng.parer.eidas.core.helper;

import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import it.eng.parer.eidas.model.EidasDataToValidateMetadata;

@Component
public class EidasMetadaValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return EidasDataToValidateMetadata.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        UrlValidator urlValidator = new UrlValidator();
        //
        EidasDataToValidateMetadata metadata = (EidasDataToValidateMetadata) target;
        if (metadata.getRemoteSignedDocument().getUri() == null
                || !urlValidator.isValid(metadata.getRemoteSignedDocument().getUri().toASCIIString())) {
            errors.reject("NOT-VALID-URI", "Necessario indicare un URI valido del documento firmato da recuperare");
        }
    }

}
