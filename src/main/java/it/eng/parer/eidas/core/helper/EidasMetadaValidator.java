/*
 * Engineering Ingegneria Informatica S.p.A.
 *
 * Copyright (C) 2023 Regione Emilia-Romagna <p/> This program is free software: you can
 * redistribute it and/or modify it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. <p/> This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Affero General Public License for more details. <p/> You should
 * have received a copy of the GNU Affero General Public License along with this program. If not,
 * see <https://www.gnu.org/licenses/>.
 */

/**
 *
 */
package it.eng.parer.eidas.core.helper;

import java.net.URI;

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
        EidasDataToValidateMetadata metadata = (EidasDataToValidateMetadata) target;
        URI uri = metadata.getRemoteSignedDocument().getUri();

        if (!isValidUri(uri)) {
            errors.reject("NOT-VALID-URI",
                    "Necessario indicare un URI valido del documento firmato da recuperare");
        }
    }

    private boolean isValidUri(URI uri) {
        if (uri == null) {
            return false;
        }

        try {
            // Use JDK's built-in URL validation
            java.net.URL url = uri.toURL();
            // Additional checks can be added here
            return url.getProtocol() != null
                    && (url.getProtocol().equals("http") || url.getProtocol().equals("https"));
        } catch (Exception e) {
            return false;
        }
    }
}
