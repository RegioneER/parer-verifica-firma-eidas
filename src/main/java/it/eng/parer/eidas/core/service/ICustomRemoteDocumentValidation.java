/**
 * 
 */
package it.eng.parer.eidas.core.service;

import org.springframework.core.io.Resource;

import eu.europa.esig.dss.validation.CertificateVerifier;
import it.eng.parer.eidas.model.EidasDataToValidateMetadata;
import it.eng.parer.eidas.model.EidasWSReportsDTOTree;
import jakarta.servlet.http.HttpServletRequest;

public interface ICustomRemoteDocumentValidation {

    /**
     * Definisce il CertificateVerifier di riferimento
     * 
     * @param verifier
     *            oggetto standard DSS CertificateVerifier
     */
    void setVerifier(CertificateVerifier verifier);

    /**
     * Definisce il policy constraint da utilizzare in ambito di validazione della firma. Scenario 1 :
     * policy/constraint.xml di default rilasciato con le librerie DSS Scenario 2 : possibile effettuare diversa
     * configurazione attraverso un file xml implementato secondo specificia EIDAS, supportato il caricamento da
     * filesystem e via classloading (e.g. file su classpath o su resources)
     * 
     * @param defaultValidationPolicy
     *            risorsa con file di constraint da utilizzare
     */
    void setDefaultValidationPolicy(Resource defaultValidationPolicy);

    EidasWSReportsDTOTree validateSignature(EidasDataToValidateMetadata dataToValidateMetadata,
            HttpServletRequest request);

}