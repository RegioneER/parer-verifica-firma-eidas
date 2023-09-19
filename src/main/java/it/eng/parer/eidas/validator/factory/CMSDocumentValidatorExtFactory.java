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
@Deprecated(since = "1.4.0", forRemoval = true)
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
