package it.eng.parer.eidas.core.bean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.esig.dss.cades.validation.CMSDocumentValidator;
import eu.europa.esig.dss.cades.validation.CMSDocumentValidatorFactory;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.validation.DocumentValidatorFactory;
import eu.europa.esig.dss.validation.SignedDocumentValidator;

/**
 * @deprecated
 * 
 *             Custom signed document validator che sostituisce lo standard {@link SignedDocumentValidator} in quanto a
 *             partire dalla versione 5.6 delle librerie DSS sono stati introdotti i meccanismi standard di Java dei
 *             "services" per la creazione delle factory di validazione. Introducendone una custom questa non veniva
 *             correttamente reperita secondo l'ordine necessario e, per tale modifico, è stata creata una sua
 *             sostituta. A partire dalla 5.8 non risulta più necessaria una logica custom in quanto sia i file TSD che
 *             TSR non sono supportati (come vuole la logica del chiamante).
 * 
 * @author sinatti_s
 *
 */
@Deprecated(since = "1.4.0", forRemoval = true)
public class SignedDocumentValidatorExt {

    private static final Logger LOG = LoggerFactory.getLogger(SignedDocumentValidatorExt.class);

    private SignedDocumentValidatorExt() {
        throw new IllegalStateException("SignedDocumentValidatorExt class");
    }

    /**
     * Extend method from {@link SignedDocumentValidator#fromDocument(DSSDocument)} Aggiunta logica di ordinamento dei
     * verificaFirma loader a partire da quelli interni si eclude inoltre il validator {@link CMSDocumentValidator} in
     * quanto deve essere esclusa la validazione dei .tsr
     * 
     * @param dssDocument
     *            documento DSS, sia esso un inMemory sia con path assoluto
     * 
     * @return ritorna l'oggetto {@link SignedDocumentValidator} con la lista corretta di validatory (factory) da
     *         utilizzare (standard e custom)
     * 
     **/
    public static SignedDocumentValidator fromDocument(final DSSDocument dssDocument) {
        Objects.requireNonNull(dssDocument, "DSSDocument is null");
        ServiceLoader<DocumentValidatorFactory> serviceLoaders = ServiceLoader.load(DocumentValidatorFactory.class);
        // as list
        List<DocumentValidatorFactory> serviceLoadersAsList = new ArrayList<>();
        // add each element of iterator to the List
        serviceLoaders.iterator().forEachRemaining(serviceLoadersAsList::add);
        // lambda filter / rule : skip if DocumentValidatorFactory is instanceof
        // CMSDocumentValidatorFactory
        /**
         * nota: CMSDocumentValidatorFactory non inclusa, viene utilizzata al suo posto
         * {@link CMSDocumentValidatorExtFactory}
         */
        serviceLoadersAsList = serviceLoadersAsList.stream().filter(c -> !(c instanceof CMSDocumentValidatorFactory))
                .collect(Collectors.toList());
        // sort list by class name (inverted)
        Collections.sort(serviceLoadersAsList,
                (c1, c2) -> c2.getClass().getPackage().getName().compareTo(c1.getClass().getPackage().getName()));

        for (DocumentValidatorFactory factory : serviceLoadersAsList) {
            try {
                if (factory.isSupported(dssDocument)) {
                    return factory.create(dssDocument);
                }
            } catch (Exception e) {
                /**
                 * Nel metodo orignale {@link SignedDocumentValidator#fromDocument(DSSDocument)} questa loggata viene
                 * classificata come ERROR ma in questo caso viene trasformato in WARNING per evitare dell'inutile
                 * "rumore" sui log, dovuto semplicemente al fatto che per quel formato documento non si è riusciti ad
                 * instanziare la factory evidentemente perché non possibile.
                 */
                LOG.debug(String.format("Unable to create a DocumentValidator with the factory '%s'",
                        factory.getClass().getSimpleName()), e);
            }
        }
        throw new DSSException("Document format not recognized/handled");
    }
}
