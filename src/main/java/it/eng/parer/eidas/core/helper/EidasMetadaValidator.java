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
