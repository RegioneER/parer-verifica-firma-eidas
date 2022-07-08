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
