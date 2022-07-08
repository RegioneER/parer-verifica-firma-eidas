package it.eng.parer.eidas.validator.factory;

import eu.europa.esig.dss.cades.validation.CMSDocumentValidatorFactory;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.validation.DocumentValidatorFactory;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import it.eng.parer.eidas.validator.CMSDocumentValidatorExt;

/**
 * @deprecated
 * 
 *             Custom validator che sostituisce lo standard {@link CMSDocumentValidatorFactory} in quanto si applica la
 *             stessa logica di validazione ma non accettando file .tsr
 * 
 * @author sinatti_s
 *
 */
@Deprecated
public class CMSDocumentValidatorExtFactory implements DocumentValidatorFactory {

    @Override
    public boolean isSupported(DSSDocument document) {
        CMSDocumentValidatorExt validator = new CMSDocumentValidatorExt(document);
        return validator.isSupported(document);
    }

    @Override
    public SignedDocumentValidator create(DSSDocument document) {
        return new CMSDocumentValidatorExt(document);
    }

}
