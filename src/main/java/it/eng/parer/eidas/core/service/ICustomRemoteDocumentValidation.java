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
