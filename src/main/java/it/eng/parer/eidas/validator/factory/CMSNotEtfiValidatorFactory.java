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

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.validation.DocumentValidatorFactory;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import it.eng.parer.eidas.validator.CMSNotEtfiValidator;
import it.eng.parer.eidas.validator.CMSNotEtfiValidatorUtil;

/**
 * @deprecated
 * 
 *             Custom validator per la gestione degli armored ascii. Non utilizzato in quanto esiste una logica interna
 *             per cui i file riconosciuti come ascii armor vengono "sbustati" e passati al validatore previsto su
 *             librerie DSS. Implementazione realizzata con perfomance migliori (così come utilizzo di risorse, rispetto
 *             la modalità prevista da librerie di Bouncy)
 * 
 */
@Deprecated
public class CMSNotEtfiValidatorFactory implements DocumentValidatorFactory {

    /**
     * A differenza del comportamento standard delle factory dove, si utilizza l'implementazione del metodo isSupported
     * del validator stesso, in questo caso non è possibile in quanto, essendo figlio di CMSDocumentValidator non si ha
     * accesso al costruttore vuoto. Ai fine dell'ottimizzazione, per evitare l'accesso allo stream di byte del file al
     * fine di creare l'oggetto CMSSignedData (vedi {@link #create(DSSDocument)}, si utilizza una classe esterna.
     */
    @Override
    public boolean isSupported(DSSDocument document) {
        return CMSNotEtfiValidatorUtil.isAsciiArmor(document);
    }

    @Override
    public SignedDocumentValidator create(DSSDocument document) {
        return new CMSNotEtfiValidator(CMSNotEtfiValidatorUtil.toCMSSignedDataAsciiArmor(document), document);
    }

}
