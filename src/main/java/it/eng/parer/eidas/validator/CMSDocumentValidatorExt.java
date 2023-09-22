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

import java.io.IOException;
import java.io.InputStream;

import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampToken;

import eu.europa.esig.dss.cades.validation.CMSDocumentValidator;
import eu.europa.esig.dss.model.DSSDocument;

/**
 * Questo Custom Validator è pensato per gestire gestire i formati definiti su CMSDocumentValidator con il caso
 * particolare dei file TSD non supportati su EIDAS
 * 
 */
public class CMSDocumentValidatorExt extends CMSDocumentValidator {

    public CMSDocumentValidatorExt(CMSSignedData cmsSignedData) {
        super(cmsSignedData);
    }

    /**
     * Default constructor
     * 
     * @param document
     *            documento DSS, sia esso un inMemory sia con path assoluto
     * 
     */
    public CMSDocumentValidatorExt(final DSSDocument document) {
        super(document);// default contructor
    }

    /*
     * (non-Javadoc)
     * 
     * @see eu.europa.esig.dss.cades.validation.CMSDocumentValidator#isSupported(eu. europa.esig.dss.DSSDocument)
     */
    @Override
    public boolean isSupported(DSSDocument dssDocument) {
        return super.isSupported(dssDocument) && !isTSR(dssDocument);
    }

    /**
     * Eidas non supporta file TSR TOFIX: verificare che la metodologia di riconoscimento del TSR sia corretta o meno
     * 
     * @param dssDocument
     *            documento DSS, sia esso un inMemory sia con path assoluto
     * 
     * @return boolean
     */
    private boolean isTSR(DSSDocument dssDocument) {
        try (InputStream inputStream = dssDocument.openStream()) {
            this.cmsSignedData = new CMSSignedData(inputStream);
            TimeStampToken resp = new TimeStampToken(this.cmsSignedData);
            return resp.getTimeStampInfo() != null;
        } catch (IOException | CMSException | TSPException e) {
            return false;
        }
    }

}
