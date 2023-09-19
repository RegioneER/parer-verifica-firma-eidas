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

package it.eng.parer.eidas.core.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.xml.sax.SAXException;

import eu.europa.esig.dss.enumerations.MimeType;
import eu.europa.esig.dss.enumerations.TokenExtractionStrategy;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.policy.ValidationPolicy;
import eu.europa.esig.dss.policy.ValidationPolicyFacade;
import eu.europa.esig.dss.policy.jaxb.Level;
import eu.europa.esig.dss.policy.jaxb.LevelConstraint;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.AdvancedSignature;
import eu.europa.esig.dss.validation.CertificateVerifier;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.reports.Reports;
import eu.europa.esig.dss.ws.validation.dto.WSReportsDTO;
import it.eng.parer.eidas.core.helper.EidasHelper;
import it.eng.parer.eidas.core.helper.ExtensionHelper;
import it.eng.parer.eidas.model.EidasDataToValidateMetadata;
import it.eng.parer.eidas.model.EidasRemoteDocument;
import it.eng.parer.eidas.model.EidasWSReportsDTOTree;
import it.eng.parer.eidas.model.exception.EidasParerException;
import it.eng.parer.eidas.model.exception.ParerError;
import jakarta.servlet.http.HttpServletRequest;

public class CustomRemoteDocumentValidationImpl implements ICustomRemoteDocumentValidation {

    private static final Logger LOG = LoggerFactory.getLogger(CustomRemoteDocumentValidationImpl.class);

    private CertificateVerifier verifier;

    private Resource defaultValidationPolicy;

    @Autowired
    Environment env;

    @Autowired
    EidasHelper helper;

    @Autowired
    ExtensionHelper extension;

    /**
     * Definisce il CertificateVerifier di riferimento
     * 
     * @param verifier
     *            oggetto standard DSS CertificateVerifier
     */
    @Override
    public void setVerifier(CertificateVerifier verifier) {
        this.verifier = verifier;
    }

    /**
     * Definisce il policy constraint da utilizzare in ambito di validazione della firma. Scenario 1 :
     * policy/constraint.xml di default rilasciato con le librerie DSS Scenario 2 : possibile effettuare diversa
     * configurazione attraverso un file xml implementato secondo specificia EIDAS, supportato il caricamento da
     * filesystem e via classloading (e.g. file su classpath o su resources)
     * 
     * @param defaultValidationPolicy
     *            risorsa con file di constraint da utilizzare
     */
    @Override
    public void setDefaultValidationPolicy(Resource defaultValidationPolicy) {
        this.defaultValidationPolicy = defaultValidationPolicy;
    }

    @Override
    public EidasWSReportsDTOTree validateSignature(EidasDataToValidateMetadata dataToValidateMetadata,
            HttpServletRequest request) {
        //
        final LocalDateTime startValidation = LocalDateTime.now(ZoneId.systemDefault());
        //
        if (LOG.isDebugEnabled()) {
            LOG.debug("Inizio validazione documento con identificativo [{}] - data/ora inizio {}",
                    dataToValidateMetadata.getDocumentId(), startValidation);
        }
        //
        String mimeTypeUnsigned = null;
        EidasWSReportsDTOTree root = new EidasWSReportsDTOTree();
        try {
            // elab signed file
            DSSDocument signedDocument = elabSignedFile(dataToValidateMetadata,
                    dataToValidateMetadata.getRemoteSignedDocument());
            // validator
            SignedDocumentValidator signedDocValidator = buildValidator(signedDocument, dataToValidateMetadata);
            // original files
            mimeTypeUnsigned = findOriginalFiles(dataToValidateMetadata, signedDocValidator);
            // create reports
            Reports reports = buildReports(dataToValidateMetadata, signedDocValidator);
            // tree root
            root = createRoot(signedDocument, reports, dataToValidateMetadata.getDocumentId());
            // add child
            Set<String> signProcessed = new HashSet<>();
            addChild(dataToValidateMetadata, root, signedDocValidator, signedDocument.getName(), signProcessed);
        } finally {
            LocalDateTime endValidation = LocalDateTime.now(ZoneId.systemDefault());

            // build version
            root.setVservice(helper.buildversion());
            // build Firma version
            root.setVlibrary(helper.dssversion());
            // selfLink
            if (request != null) {
                root.setSelfLink(request.getRequestURL().toString());
            }
            // mimetype unsigned
            if (StringUtils.isNotBlank(mimeTypeUnsigned)) {
                root.setMimeType(mimeTypeUnsigned);
            }
            // Inizio e fine validazione
            root.setStartValidation(Date.from(startValidation.atZone(ZoneId.systemDefault()).toInstant()));
            root.setEndValidation(Date.from(endValidation.atZone(ZoneId.systemDefault()).toInstant()));

            // delete temp files
            helper.deleteTmpDocExtFiles(dataToValidateMetadata.getRemoteSignedDocument(),
                    dataToValidateMetadata.getRemoteOriginalDocuments(), dataToValidateMetadata.getPolicyExt());

            long totalDateTime = Duration.between(startValidation, endValidation).toMillis();
            LOG.info("Fine validazione documento con identificativo [{}] - data/ora fine {} (totale : {} ms)",
                    dataToValidateMetadata.getDocumentId(), endValidation, totalDateTime);
        }

        return root;
    }

    private DSSDocument elabSignedFile(EidasDataToValidateMetadata dataToValidateMetadata,
            EidasRemoteDocument signedFile) {
        if (signedFile == null) {
            throw new EidasParerException(dataToValidateMetadata).withCode(ParerError.ErrorCode.METADATA_ERROR)
                    .withMessage("Errore nei metadati inviati, firma da verificare non presente");
        }
        DSSDocument signedDocument = null;
        if (Utils.isArrayNotEmpty(signedFile.getBytes())) {
            signedDocument = new InMemoryDocument(signedFile.getBytes(), signedFile.getName());
            LOG.debug("DSSDocument in memory name {}", signedFile.getName());
        } else {
            signedDocument = new FileDocument(signedFile.getAbsolutePath());
            // set original file name
            signedDocument.setName(signedFile.getName());
            LOG.debug("DSSDocument as file path {}", signedFile.getAbsolutePath());
        }

        // detect mimetype
        signedDocument.setMimeType(MimeType.fromMimeTypeString(helper.detectMimeType(signedDocument)));

        return signedDocument;
    }

    private Reports buildReports(EidasDataToValidateMetadata dataToValidateMetadata,
            SignedDocumentValidator signedDocValidator) {
        Reports reports = null;
        EidasRemoteDocument policy = dataToValidateMetadata.getPolicyExt();
        if (policy == null) {
            ValidationPolicy customValidationConstraints;
            try {
                customValidationConstraints = compileValidationPolicy(dataToValidateMetadata);
            } catch (JAXBException | XMLStreamException | SAXException ex) {
                throw new EidasParerException(dataToValidateMetadata, ex).withCode(ParerError.ErrorCode.GENERIC_ERROR)
                        .withMessage("Errore generico in fase di compilazione custom policy");
            } catch (IOException ex) {
                throw new EidasParerException(dataToValidateMetadata, ex).withCode(ParerError.ErrorCode.IO_ERROR)
                        .withMessage("Errore generico in fase di compilazione custom policy");
            }
            reports = signedDocValidator.validateDocument(customValidationConstraints);
        } else {
            if (Utils.isArrayNotEmpty(policy.getBytes())) {
                LOG.debug("ConstraintPolicy: in memory name {}", policy.getName());
                try (ByteArrayInputStream bais = new ByteArrayInputStream(policy.getBytes())) {
                    reports = signedDocValidator.validateDocument(bais);
                } catch (IOException ex) {
                    throw new EidasParerException(dataToValidateMetadata, ex).withCode(ParerError.ErrorCode.IO_ERROR)
                            .withMessage("Errore generico in fase di lettura policy");
                }
            } else if (StringUtils.isNotBlank(policy.getAbsolutePath())) {
                LOG.debug("ConstraintPolicy: as file path {}", policy.getAbsolutePath());
                reports = signedDocValidator.validateDocument(new File(policy.getAbsolutePath()));
            } else {
                LOG.error("ConstraintPolicy: check name of policy file on multipartfile and metadata sent,  "
                        + " policy file declared name {}", policy.getName());
                throw new EidasParerException(dataToValidateMetadata)
                        .withCode(ParerError.ErrorCode.BAD_FILENAME_MULTIPARTFILE_AND_METADATA)
                        .withMessage("Errore su recupero file policy, verificare nome file su multipart/form-data");
            }

        }

        // check if valid (no signature)
        if (reports.getSimpleReport().getSignaturesCount() == 0) {
            LOG.debug("Nessuna firma individuata");
        }

        return reports;
    }

    private String findOriginalFiles(EidasDataToValidateMetadata dataToValidateMetadata,
            SignedDocumentValidator signedDocValidator) {
        // mime type
        String mimeType = null;
        List<EidasRemoteDocument> originalFiles = dataToValidateMetadata.getRemoteOriginalDocuments();
        if (originalFiles != null && !originalFiles.isEmpty()) {
            List<DSSDocument> list = new ArrayList<>();
            for (EidasRemoteDocument originalFile : originalFiles) {
                DSSDocument originalDocument = null;
                if (Utils.isArrayNotEmpty(originalFile.getBytes())) {
                    originalDocument = new InMemoryDocument(originalFile.getBytes(), originalFile.getName());
                    LOG.debug("Original DSSDocument: in memory name {}", originalFile.getName());
                } else if (StringUtils.isNotBlank(originalFile.getAbsolutePath())) {
                    originalDocument = new FileDocument(originalFile.getAbsolutePath());
                    // set name
                    originalDocument.setName(originalFile.getName());
                    LOG.debug("Original DSSDocument: as file path {}", originalFile.getAbsolutePath());
                } else {
                    LOG.error("Original DSSDocument: check name of original file on multipartfile and metadata sent,  "
                            + " original file declared name {}", originalFile.getName());
                    throw new EidasParerException(dataToValidateMetadata)
                            .withCode(ParerError.ErrorCode.BAD_FILENAME_MULTIPARTFILE_AND_METADATA).withMessage(
                                    "Il nome dichiarato su 'name' del metadata 'originalDocumentsExt' non coicide con nessuno dei multipart file caricati su originalFiles");
                }
                // detect mimetype
                mimeType = helper.detectMimeType(originalDocument);
                list.add(originalDocument);
            }
            signedDocValidator.setDetachedContents(list);
        }
        return mimeType;
    }

    /*
     * Modifica le policy di validazione predefinite.
     * 
     * Modificati i default https://github.com/esig/dss/blob/5.8.x/dss-policy-jaxb/src/main/resources/
     * policy/constraint.xml sulla base dei flag passati al servizio
     *
     */
    private ValidationPolicy compileValidationPolicy(EidasDataToValidateMetadata dataToValidateMetadata)
            throws JAXBException, XMLStreamException, IOException, SAXException {
        // Level IGNORE
        final LevelConstraint ignore = new LevelConstraint();
        ignore.setLevel(Level.IGNORE);

        // flag
        boolean controlloCrittograficoIgnorato = dataToValidateMetadata.isControlloCrittograficoIgnorato();
        boolean controlloCatenaTrustIgnorato = dataToValidateMetadata.isControlloCatenaTrustIgnorato();
        boolean controlloCertificatoIgnorato = dataToValidateMetadata.isControlloCertificatoIgnorato();
        boolean controlloRevocaIgnorato = dataToValidateMetadata.isControlloRevocaIgnorato();

        final ValidationPolicyFacade facade = ValidationPolicyFacade.newFacade();
        final ValidationPolicy validationPolicyJaxb = defaultValidationPolicy.exists()
                ? facade.getValidationPolicy(defaultValidationPolicy.getInputStream())
                : facade.getDefaultValidationPolicy();

        if (controlloCrittograficoIgnorato) {
            LOG.debug("Validation policy controlloCatenaTrustIgnorato set to level {}", Level.IGNORE);
            //
            validationPolicyJaxb.getCryptographic().setLevel(Level.IGNORE);// default FAIL
            LOG.debug("Validation policy: cryptographic constraint original level {}, to level {}",
                    validationPolicyJaxb.getCryptographic().getLevel(), Level.IGNORE);

            validationPolicyJaxb.getSignatureConstraints().getBasicSignatureConstraints().getCryptographic()
                    .setLevel(Level.IGNORE); // default FAIL
            LOG.debug(
                    "Validation policy: basicSignatureConstraints/cryptographic constraint original level {}, to level {}",
                    validationPolicyJaxb.getSignatureConstraints().getBasicSignatureConstraints().getCryptographic()
                            .getLevel(),
                    Level.IGNORE);

            validationPolicyJaxb.getSignatureConstraints().getBasicSignatureConstraints().getSignatureIntact()
                    .setLevel(Level.IGNORE); // default FAIL
            LOG.debug(
                    "Validation policy: basicSignatureConstraints/signatureIntact constraint original level {}, to level {}",
                    validationPolicyJaxb.getSignatureConstraints().getBasicSignatureConstraints().getSignatureIntact()
                            .getLevel(),
                    Level.IGNORE);

            validationPolicyJaxb.getSignatureConstraints().getBasicSignatureConstraints().getReferenceDataExistence()
                    .setLevel(Level.IGNORE); // default FAIL
            LOG.debug(
                    "Validation policy: basicSignatureConstraints/referenceDataExistence constraint original level {}, to level {}",
                    validationPolicyJaxb.getSignatureConstraints().getBasicSignatureConstraints()
                            .getReferenceDataExistence().getLevel(),
                    Level.IGNORE);

            validationPolicyJaxb.getSignatureConstraints().getBasicSignatureConstraints().getReferenceDataIntact()
                    .setLevel(Level.IGNORE); // default FAIL
            LOG.debug(
                    "Validation policy: basicSignatureConstraints/referenceDataIntact constraint original level {}, to level {}",
                    validationPolicyJaxb.getSignatureConstraints().getBasicSignatureConstraints()
                            .getReferenceDataIntact().getLevel(),
                    Level.IGNORE);

            /*
             * Impostati i medesemi Level.IGNORE sulla CA Potrebbe non essere necessario ma, dato che si deve ignorare
             * questo tipo di controllo, non porta a criticità.
             */
            // CA
            validationPolicyJaxb.getSignatureConstraints().getBasicSignatureConstraints().getCACertificate()
                    .getCryptographic().setLevel(Level.IGNORE); // default FAIL
            LOG.debug(
                    "Validation policy: basicSignatureConstraints/CACertificate/cryptographic constraint original level {}, to level {}",
                    validationPolicyJaxb.getSignatureConstraints().getBasicSignatureConstraints().getCACertificate()
                            .getCryptographic().getLevel(),
                    Level.IGNORE);

            // Timestamp
            validationPolicyJaxb.getTimestampConstraints().getBasicSignatureConstraints().getCryptographic()
                    .setLevel(Level.IGNORE);
            LOG.debug("Validation policy: timestampConstraints/cryptographic constraint original level {}, to level {}",
                    validationPolicyJaxb.getTimestampConstraints().getBasicSignatureConstraints().getCryptographic()
                            .getLevel(),
                    Level.IGNORE);

            validationPolicyJaxb.getTimestampConstraints().getBasicSignatureConstraints().getSignatureIntact()
                    .setLevel(Level.IGNORE);
            LOG.debug(
                    "Validation policy: timestampConstraints/cryptographic/getSignatureIntact constraint original level {}, to level {}",
                    validationPolicyJaxb.getTimestampConstraints().getBasicSignatureConstraints().getSignatureIntact()
                            .getLevel(),
                    Level.IGNORE);

            validationPolicyJaxb.getTimestampConstraints().getBasicSignatureConstraints().getReferenceDataExistence()
                    .setLevel(Level.IGNORE);
            LOG.debug(
                    "Validation policy: timestampConstraints/cryptographic/getReferenceDataExistence constraint original level {}, to level {}",
                    validationPolicyJaxb.getTimestampConstraints().getBasicSignatureConstraints()
                            .getReferenceDataExistence().getLevel(),
                    Level.IGNORE);

            validationPolicyJaxb.getTimestampConstraints().getBasicSignatureConstraints().getReferenceDataIntact()
                    .setLevel(Level.IGNORE);
            LOG.debug(
                    "Validation policy: timestampConstraints/cryptographic/getReferenceDataIntact constraint original level {}, to level {}",
                    validationPolicyJaxb.getTimestampConstraints().getBasicSignatureConstraints()
                            .getReferenceDataIntact().getLevel(),
                    Level.IGNORE);
        }

        if (controlloCatenaTrustIgnorato) {
            LOG.debug("Validation policy controlloCatenaTrustIgnorato set to level {}", Level.IGNORE);

            validationPolicyJaxb.getSignatureConstraints().getBasicSignatureConstraints()
                    .getProspectiveCertificateChain().setLevel(Level.IGNORE); // default FAIL
            LOG.debug(
                    "Validation policy: basicSignatureConstraints/prospectiveCertificateChain constraint original level {}, to level {}",
                    validationPolicyJaxb.getSignatureConstraints().getBasicSignatureConstraints()
                            .getProspectiveCertificateChain().getLevel(),
                    Level.IGNORE);
        }

        if (controlloCertificatoIgnorato) {
            LOG.debug("Validation policy controlloCertificatoIgnorato set to level {}", Level.IGNORE);

            validationPolicyJaxb.getSignatureConstraints().getBasicSignatureConstraints().getSignatureIntact()
                    .setLevel(Level.IGNORE);// default FAIL
            LOG.debug(
                    "Validation policy: basicSignatureConstraints/signatureIntact constraint original level {}, to level {}",
                    validationPolicyJaxb.getSignatureConstraints().getBasicSignatureConstraints().getSignatureIntact()
                            .getLevel(),
                    Level.IGNORE);
        }

        /*
         * Nota : dopo attenta analisi, non esiste ad oggi (versione 5.8 delle DSS) una policy check che permetta di
         * escludere del tutto l'acceptance checker sulla revoca (CRL/OCSP), ossia di impostare ad un livello globale di
         * IGNORE il controllo eseguito, in quanto, la logica delle librerie DSS prevede (vedere
         * eu.europa.esig.dss.validation.process.bbb.xcv.rac.checks. RevocationConsistentCheck) il controllo di
         * "consistenza" dei dati legati alla revoca, processo di cui, non si riesce ad impostare un livello specifico.
         * 
         */
        if (controlloRevocaIgnorato) {
            LOG.debug("Validation policy controlloCRLIgnorato set to level {}", Level.IGNORE);

            validationPolicyJaxb.getSignatureConstraints().getBasicSignatureConstraints().getSigningCertificate()
                    .setRevocationFreshnessNextUpdate(ignore);
            LOG.debug(
                    "Validation policy: basicSignatureConstraints/signingCertificate/revocationFreshnessNextUpdate constraint original level {}, to level {}",
                    validationPolicyJaxb.getSignatureConstraints().getBasicSignatureConstraints()
                            .getSigningCertificate().getRevocationFreshnessNextUpdate().getLevel(),
                    Level.IGNORE);

            validationPolicyJaxb.getSignatureConstraints().getBasicSignatureConstraints().getSigningCertificate()
                    .getRevocationDataAvailable().setLevel(Level.IGNORE); // default FAIL
            LOG.debug(
                    "Validation policy: basicSignatureConstraints/signingCertificate/revocationDataAvailable constraint original level {}, to level {}",
                    validationPolicyJaxb.getSignatureConstraints().getBasicSignatureConstraints()
                            .getSigningCertificate().getRevocationDataAvailable().getLevel(),
                    Level.IGNORE);

            validationPolicyJaxb.getSignatureConstraints().getBasicSignatureConstraints().getSigningCertificate()
                    .getRevocationIssuerNotExpired().setLevel(Level.IGNORE);
            LOG.debug(
                    "Validation policy: basicSignatureConstraints/signingCertificate/revocationIssuerNotExpired constraint original level {}, to level {}",
                    validationPolicyJaxb.getSignatureConstraints().getBasicSignatureConstraints()
                            .getSigningCertificate().getRevocationIssuerNotExpired().getLevel(),
                    Level.IGNORE);

            validationPolicyJaxb.getSignatureConstraints().getBasicSignatureConstraints().getSigningCertificate()
                    .getRevocationInfoAccessPresent().setLevel(Level.IGNORE); // default FAIL
            LOG.debug(
                    "Validation policy: basicSignatureConstraints/signingCertificate/revocationInfoAccessPresent constraint original level {}, to level {}",
                    validationPolicyJaxb.getSignatureConstraints().getBasicSignatureConstraints()
                            .getSigningCertificate().getRevocationInfoAccessPresent().getLevel(),
                    Level.IGNORE);
            /*
             * Impostati i medesemi Level.IGNORE sulla CA Potrebbe non essere necessario ma, dato che si deve ignorare
             * questo tipo di controllo, non porta a criticità.
             */
            // CA
            // no time constraint
            validationPolicyJaxb.getSignatureConstraints().getBasicSignatureConstraints().getCACertificate()
                    .setRevocationFreshnessNextUpdate(ignore);
            LOG.debug(
                    "Validation policy: basicSignatureConstraints/CACertificate/revocationFreshnessNextUpdate constraint original level {}, to level {}",
                    validationPolicyJaxb.getSignatureConstraints().getBasicSignatureConstraints().getCACertificate()
                            .getRevocationFreshnessNextUpdate().getLevel(),
                    Level.IGNORE);

            validationPolicyJaxb.getSignatureConstraints().getBasicSignatureConstraints().getCACertificate()
                    .getRevocationDataAvailable().setLevel(Level.IGNORE); // default FAIL
            LOG.debug(
                    "Validation policy: basicSignatureConstraints/CACertificate/revocationDataAvailable constraint original level {}, to level {}",
                    validationPolicyJaxb.getSignatureConstraints().getBasicSignatureConstraints().getCACertificate()
                            .getRevocationDataAvailable().getLevel(),
                    Level.IGNORE);

            //
            validationPolicyJaxb.getRevocationConstraints().setLevel(Level.IGNORE);
            LOG.debug("Validation policy: revocationConstraints constraint original level {}, to level {}",
                    validationPolicyJaxb.getRevocationConstraints().getLevel(), Level.IGNORE);

            // no time constraint
            validationPolicyJaxb.getRevocationConstraints().getBasicSignatureConstraints().getSigningCertificate()
                    .setRevocationFreshnessNextUpdate(ignore);
            LOG.debug(
                    "Validation policy: revocationConstraints/revocationFreshnessNextUpdate constraint original level {}, to level {}",
                    validationPolicyJaxb.getRevocationConstraints().getBasicSignatureConstraints()
                            .getSigningCertificate().getRevocationFreshnessNextUpdate(),
                    Level.IGNORE);

            // OCSP
            validationPolicyJaxb.getRevocationConstraints().getUnknownStatus().setLevel(Level.IGNORE);
            LOG.debug(
                    "Validation policy: revocationConstraints/UnknownStatus constraint original level {}, to level {}",
                    validationPolicyJaxb.getRevocationConstraints().getUnknownStatus().getLevel(), Level.IGNORE);

            validationPolicyJaxb.getRevocationConstraints().getSelfIssuedOCSP().setLevel(Level.IGNORE);
            LOG.debug(
                    "Validation policy: revocationConstraints/SelfIssuedOCSP constraint original level {}, to level {}",
                    validationPolicyJaxb.getRevocationConstraints().getSelfIssuedOCSP().getLevel(), Level.IGNORE);

        }

        return validationPolicyJaxb;
    }

    /**
     * Creazione elemento EidasWSReportsDTOTree
     *
     * @param signedDocument
     * @param reports
     * @param idComponente
     * 
     * @return
     */
    private EidasWSReportsDTOTree createRoot(DSSDocument signedDocument, Reports reports, String idComponente) {
        LOG.debug("Creating reports tree root element, ID {}", idComponente);

        /**
         * Vedi issue <a>https://gitlab.ente.regione.emr.it/parer/okd/verifica-firma-eidas/issues/7</a>
         * 
         * Per il momento il validation report non viene restituito al client
         */
        WSReportsDTO wsdto = new WSReportsDTO(reports.getDiagnosticDataJaxb(), reports.getSimpleReportJaxb(),
                reports.getDetailedReportJaxb());
        EidasWSReportsDTOTree dto = new EidasWSReportsDTOTree(wsdto);
        dto.setMimeType(helper.detectMimeType(signedDocument));
        //
        dto.setIdComponente(idComponente);
        // unsigned = NO signatures
        dto.setUnsigned(reports.getSimpleReport().getSignaturesCount() == 0);
        // TOFIX: alcuni dei parametri estratti tra le estensioni sono già presenti sui
        // reports EIDAS (vedi base64!)
        extension.createExtensions(wsdto, dto);

        return dto;
    }

    /**
     * Crea, ricorsivamente, l'output del processo di verifica in cui in ogni "strato" è presente un livello di
     * annidamento.
     * 
     * @param dataToValidateMetadata
     *            metadati
     * @param parent
     *            puntatore al padre
     * @param signedDocValidator
     *            validatore custom per il tipo di documento.
     * @param signedDocumentName
     *            nome del documento
     * @param signatureAlreadyProcessed
     *            lista signature id processate
     */
    private void addChild(EidasDataToValidateMetadata dataToValidateMetadata, EidasWSReportsDTOTree parent,
            SignedDocumentValidator signedDocValidator, String signedDocumentName,
            Set<String> signatureAlreadyProcessed) {
        // for each signature
        for (AdvancedSignature signature : signedDocValidator.getSignatures()) {
            LOG.debug("Creating reports tree child element, parent signature id {}", signature.getId());

            // check if sign.getId() is already done -> go next
            if (signatureAlreadyProcessed.contains(signature.getId())) {
                continue;
            }
            //
            extension.extractSignatureBytes(signature, parent);
            /**
             * Nota bene: EIDAS prevede l'estrazione di N documenti originali anche se di fatto, secondo la struttura
             * "classica" delle firme "imbustate" questa cardinalità è da vedere come "piatta" su un unico livello (più
             * che un albero una lista)
             */
            try {
                //
                for (DSSDocument doc : signedDocValidator.getOriginalDocuments(signature.getId())) {
                    // set document name (from parent)
                    doc.setName(signedDocumentName);
                    // crea child per ogni documento originale
                    signedDocValidator = createChild(dataToValidateMetadata, parent, signedDocValidator, doc,
                            signature.getId(), signatureAlreadyProcessed);
                } // originalDocument
            } catch (Exception ignore) {
                /**
                 * Eccezioni (gestite) che avvengono sullo sbustato verranno ignorate. In questo caso particolare il
                 * validatore non è riuscito ad invidivuare un documento orginale (vedi caso dei P7S)
                 */
                LOG.debug("Reports DTO Tree child signature id {} no orginal documents", signature.getId());
            }
        } // sign
    }

    private SignedDocumentValidator createChild(EidasDataToValidateMetadata dataToValidateMetadata,
            EidasWSReportsDTOTree parent, SignedDocumentValidator signedDocValidator, DSSDocument doc,
            String signatureId, Set<String> signatureAlreadyProcessed) {
        try {
            signedDocValidator = buildValidator(doc, dataToValidateMetadata);
            // build reports
            Reports reports = buildReports(dataToValidateMetadata, signedDocValidator);
            // tree root
            EidasWSReportsDTOTree child = createRoot(doc, reports, dataToValidateMetadata.getDocumentId());
            // set child
            parent.addChild(child);
            // ad sign by id as processed
            signatureAlreadyProcessed.add(signatureId);
            // call recursive
            addChild(dataToValidateMetadata, child, signedDocValidator, doc.getName(), signatureAlreadyProcessed);
        } catch (Exception ignore) {
            /**
             * Eccezioni (gestite) che avvengono sullo sbustato verranno ignorate. Elemento (child) aggiunto sarà
             * considerato quindi come "unsigned" (dato che la validazione non è andata a buon fine)
             */
            LOG.debug("Reports tree child element doc {} is unsigned with mimetype {}", doc.getName(),
                    doc.getMimeType() != null ? doc.getMimeType().getMimeTypeString() : "none");
            // build tree (mimetype is needed for validation) unsigned doc
            EidasWSReportsDTOTree child = new EidasWSReportsDTOTree(helper.detectMimeType(doc));
            // id componente
            child.setIdComponente(dataToValidateMetadata.getDocumentId());
            // add child
            parent.addChild(child);
        }
        return signedDocValidator;
    }

    /**
     * Costruisci il validatore custom per il documento.
     *
     * @param signedDocument
     *            documento da verificare
     * @param dataToValidateMetadata
     *            data a cui deve essere effettuata la validazione (opzionale)
     * 
     * @return validatore per il documento
     */
    private SignedDocumentValidator buildValidator(DSSDocument signedDocument,
            EidasDataToValidateMetadata dataToValidateMetadata) {
        /*
         * Nota: viene prima effettuata verifica con validatori "interni" e poi quello standard di EIDAS
         */
        try {
            SignedDocumentValidator signedDocValidator = SignedDocumentValidator.fromDocument(signedDocument);
            // dataDiRiferimento if null = NOW
            // validation date
            signedDocValidator.setValidationTime(dataToValidateMetadata.getDataDiRiferimento());
            // token strategy
            signedDocValidator.setTokenExtractionStrategy(TokenExtractionStrategy.fromParameters(
                    dataToValidateMetadata.isIncludeCertificateRevocationValues(),
                    dataToValidateMetadata.isIncludeTimestampTokenValues(),
                    dataToValidateMetadata.isIncludeCertificateTokenValues()));
            signedDocValidator.setIncludeSemantics(dataToValidateMetadata.isIncludeSemanticTokenValues());
            //
            signedDocValidator.setCertificateVerifier(verifier);
            LOG.debug("Signed Document Validator created class name: {}", signedDocValidator.getClass().getName());
            return signedDocValidator;
        } catch (Exception ex) {
            throw new EidasParerException(dataToValidateMetadata, ex).withCode(ParerError.ErrorCode.EIDAS_ERROR)
                    .withMessage("Formato del documento non riconosciuto / gestito");
        }

    }

}
