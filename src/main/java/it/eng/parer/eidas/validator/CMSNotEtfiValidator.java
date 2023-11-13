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
