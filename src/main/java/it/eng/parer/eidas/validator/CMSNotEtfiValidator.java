package it.eng.parer.eidas.validator;

import org.bouncycastle.cms.CMSSignedData;

import eu.europa.esig.dss.cades.validation.CMSDocumentValidator;
import eu.europa.esig.dss.model.DSSDocument;

/**
 * Custom validator per la gestione degli armored ascii
 * 
 * @author Sinatti_S
 *
 */
public class CMSNotEtfiValidator extends CMSDocumentValidator {

    /**
     * New costructor passing both CMSSignedData and DSSDocument
     * 
     * @param cmsSignedData
     *            oggetto contenente la rappresentazione del documento firmato
     * @param document
     *            documento DSS, sia esso un inMemory sia con path assoluto
     */
    public CMSNotEtfiValidator(CMSSignedData cmsSignedData, final DSSDocument document) {
        super(cmsSignedData);
        super.document = document;
    }

}
