package it.eng.parer.eidas.core.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.xml.sax.SAXException;

import eu.europa.esig.dss.enumerations.TokenExtractionStrategy;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.MimeType;
import eu.europa.esig.dss.policy.ValidationPolicy;
import eu.europa.esig.dss.policy.ValidationPolicyFacade;
import eu.europa.esig.dss.policy.jaxb.Level;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.AdvancedSignature;
import eu.europa.esig.dss.validation.CertificateVerifier;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.reports.Reports;
import eu.europa.esig.dss.ws.validation.dto.WSReportsDTO;
import it.eng.parer.eidas.core.helper.EidasHelper;
import it.eng.parer.eidas.core.helper.ExtensionHelper;
import it.eng.parer.eidas.core.util.Constants;
import it.eng.parer.eidas.model.DataToValidateDTOExt;
import it.eng.parer.eidas.model.EidasWSReportsDTOTree;
import it.eng.parer.eidas.model.RemoteDocumentExt;
import it.eng.parer.eidas.model.exception.EidasParerException;
import it.eng.parer.eidas.model.exception.ParerError;

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
    public EidasWSReportsDTOTree validateSignature(DataToValidateDTOExt dataToValidateDTO, HttpServletRequest request) {
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_TIMESTAMP_TYPE);
        //
        final Date startValidation = new Date();
        //
        LOG.info("Inizio validazione : id componente {} - data/ora inizio {}", dataToValidateDTO.getIdComponente(),
                sdf.format(startValidation));

        //
        Reports reports = null;

        SignedDocumentValidator signedDocValidator = null;
        //
        String mimeTypeUnsigned = null;

        EidasWSReportsDTOTree root = new EidasWSReportsDTOTree();
        try {
            // elab signed file
            DSSDocument signedDocument = elabSignedFile(dataToValidateDTO.getSignedDocumentExt());
            // validator
            signedDocValidator = buildValidator(signedDocument, dataToValidateDTO);
            // original files
            mimeTypeUnsigned = findOriginalFiles(dataToValidateDTO, signedDocValidator);
            // create reports
            reports = buildReports(dataToValidateDTO, dataToValidateDTO.getPolicyExt(), signedDocValidator);
            // tree root
            root = createRoot(signedDocument, reports, dataToValidateDTO.getIdComponente());
            // add child
            addChild(dataToValidateDTO, root, dataToValidateDTO.getPolicyExt(),
                    dataToValidateDTO.getDataDiRiferimento(), signedDocValidator, dataToValidateDTO.getIdComponente(),
                    signedDocument.getName());
        } finally {
            Date endValidation = new Date();

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
            root.setStartValidation(startValidation);
            root.setEndValidation(endValidation);

            // delete temp files
            helper.deleteTmpDocExtFiles(dataToValidateDTO.getSignedDocumentExt(),
                    dataToValidateDTO.getOriginalDocumentsExt(), dataToValidateDTO.getPolicyExt());

            long totalDateTime = Duration
                    .between(startValidation.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(),
                            endValidation.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
                    .toMillis();
            LOG.info(
                    "Fine validazione: id componente {} - data/ora fine {} (totale : {} ms) (dim. richiesta : {} byte)",
                    dataToValidateDTO.getIdComponente(), sdf.format(endValidation), totalDateTime,
                    request != null ? request.getContentLength() : BigInteger.ZERO.intValue());
        }

        return root;
    }

    /**
     * @param signedFile
     * 
     * @return
     */
    private DSSDocument elabSignedFile(RemoteDocumentExt signedFile) {
        if (signedFile == null) {
            throw new EidasParerException().withCode(ParerError.ErrorCode.METADATA_ERROR)
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
        MimeType mime = new MimeType();
        mime.setMimeTypeString(helper.detectMimeType(signedDocument));
        signedDocument.setMimeType(mime);

        return signedDocument;
    }

    /**
     *
     * @param inputVerifica
     * @param policy
     * @param signedDocValidator
     * 
     * @return
     * 
     * @throws IOException
     */
    private Reports buildReports(DataToValidateDTOExt inputVerifica, RemoteDocumentExt policy,
            SignedDocumentValidator signedDocValidator) {
        Reports reports = null;
        if (policy == null) {
            ValidationPolicy customValidationConstraints;
            try {
                customValidationConstraints = compileValidationPolicy(inputVerifica);
            } catch (JAXBException | XMLStreamException | SAXException ex) {
                throw new EidasParerException().withCode(ParerError.ErrorCode.GENERIC_ERROR)
                        .withMessage(ex.getMessage());
            } catch (IOException ex) {
                throw new EidasParerException().withCode(ParerError.ErrorCode.IO_ERROR).withMessage(ex.getMessage());
            }
            reports = signedDocValidator.validateDocument(customValidationConstraints);
        } else {
            if (Utils.isArrayNotEmpty(policy.getBytes())) {
                LOG.debug("ConstraintPolicy: in memory name {}", policy.getName());
                try (ByteArrayInputStream bais = new ByteArrayInputStream(policy.getBytes())) {
                    reports = signedDocValidator.validateDocument(bais);
                } catch (IOException ex) {
                    throw new EidasParerException().withCode(ParerError.ErrorCode.IO_ERROR)
                            .withMessage(ex.getMessage());
                }
            } else if (StringUtils.isNotBlank(policy.getAbsolutePath())) {
                LOG.debug("ConstraintPolicy: as file path {}", policy.getAbsolutePath());
                reports = signedDocValidator.validateDocument(new File(policy.getAbsolutePath()));
            } else {
                LOG.error("ConstraintPolicy: check name of policy file on multipartfile and metadata sent,  "
                        + " policy file declared name {}", policy.getName());
                throw new EidasParerException().withCode(ParerError.ErrorCode.BAD_FILENAME_MULTIPARTFILE_AND_METADATA)
                        .withMessage("Errore su recupero file policy, verificare nome file su multipart/form-data");
            }

        }

        // check if valid (no signature)
        if (reports.getSimpleReport().getSignaturesCount() == 0) {
            LOG.debug("Nessuna firma individuata");
        }

        return reports;
    }

    /**
     *
     * @param inputVerifica
     * @param signedDocValidator
     * 
     * @return
     */
    private String findOriginalFiles(DataToValidateDTOExt inputVerifica, SignedDocumentValidator signedDocValidator) {
        // mime type
        String mimeType = null;
        List<RemoteDocumentExt> originalFiles = inputVerifica.getOriginalDocumentsExt();
        if (originalFiles != null && !originalFiles.isEmpty()) {
            List<DSSDocument> list = new ArrayList<>();
            for (RemoteDocumentExt originalFile : originalFiles) {
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
                    throw new EidasParerException()
                            .withCode(ParerError.ErrorCode.BAD_FILENAME_MULTIPARTFILE_AND_METADATA)
                            .withMessage("Errore su recupero file policy, verificare nome file su multipart/form-data");
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
    private ValidationPolicy compileValidationPolicy(DataToValidateDTOExt inputVerifica)
            throws JAXBException, XMLStreamException, IOException, SAXException {

        // flag
        boolean controlloCrittograficoIgnorato = inputVerifica.isControlloCrittograficoIgnorato();
        boolean controlloCatenaTrustIgnorato = inputVerifica.isControlloCatenaTrustIgnorato();
        boolean controlloCertificatoIgnorato = inputVerifica.isControlloCertificatoIgnorato();
        boolean controlloRevocaIgnorato = inputVerifica.isControlloRevocaIgnorato();
        boolean verificaAllaDataDiFirma = inputVerifica.isVerificaAllaDataDiFirma();

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
                    .getRevocationDataFreshness().setLevel(Level.IGNORE); // default FAIL
            LOG.debug(
                    "Validation policy: basicSignatureConstraints/signingCertificate/revocationDataFreshness constraint original level {}, to level {}",
                    validationPolicyJaxb.getSignatureConstraints().getBasicSignatureConstraints()
                            .getSigningCertificate().getRevocationDataFreshness().getLevel(),
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
            validationPolicyJaxb.getSignatureConstraints().getBasicSignatureConstraints().getCACertificate()
                    .getRevocationDataFreshness().setLevel(Level.IGNORE); // default FAIL
            LOG.debug(
                    "Validation policy: basicSignatureConstraints/CACertificate/revocationDataFreshness constraint original level {}, to level {}",
                    validationPolicyJaxb.getSignatureConstraints().getBasicSignatureConstraints().getCACertificate()
                            .getRevocationDataFreshness().getLevel(),
                    Level.IGNORE);

            // validationPolicyJaxb.getSignatureConstraints().getBasicSignatureConstraints().getCACertificate()
            // .getRevocationDataNextUpdatePresent().setLevel(Level.IGNORE); // default FAIL
            // LOG.debug(
            // "Validation policy:
            // basicSignatureConstraints/CACertificate/revocationDataNextUpdatePresent
            // constraint
            // original level {}, to level {}",
            // validationPolicyJaxb.getSignatureConstraints().getBasicSignatureConstraints().getCACertificate()
            // .getRevocationDataNextUpdatePresent().getLevel(),
            // Level.IGNORE);

            validationPolicyJaxb.getSignatureConstraints().getBasicSignatureConstraints().getCACertificate()
                    .getRevocationDataAvailable().setLevel(Level.IGNORE); // default FAIL
            LOG.debug(
                    "Validation policy: basicSignatureConstraints/CACertificate/revocationDataAvailable constraint original level {}, to level {}",
                    validationPolicyJaxb.getSignatureConstraints().getBasicSignatureConstraints().getCACertificate()
                            .getRevocationDataAvailable().getLevel(),
                    Level.IGNORE);

            validationPolicyJaxb.getRevocationFreshnessConstraint().setLevel(Level.IGNORE); // default (già IGNORE)
            LOG.debug("Validation policy: revocationFreshnessConstraint constraint original level {}, to level {}",
                    validationPolicyJaxb.getRevocationFreshnessConstraint().getLevel(), Level.IGNORE);

            //
            validationPolicyJaxb.getRevocationConstraints().setLevel(Level.IGNORE);
            LOG.debug("Validation policy: revocationConstraints constraint original level {}, to level {}",
                    validationPolicyJaxb.getRevocationConstraints().getLevel(), Level.IGNORE);

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

        /*
         * Nota : EIDAS non permette di effettuare una verifica secondo la logica
         * "verifica alla data di apposizione della firma", si sceglie il seguente workaround in cui viene ignorato il
         * controllo sulla data di apposizione della firma al fine di verifiarne la validità al netto di una data di
         * riferimento.
         */
        if (verificaAllaDataDiFirma) {
            LOG.debug("Validation policy verificaAllaDataDiFirma set to level {}", Level.IGNORE);

            validationPolicyJaxb.getSignatureConstraints().getSignedAttributes().getSigningTime()
                    .setLevel(Level.IGNORE); // defaul FAIL
            LOG.debug("Validation policy: signedAttributes/signingTime constraint original level {}, to level {}",
                    validationPolicyJaxb.getSignatureConstraints().getSignedAttributes().getSigningTime().getLevel(),
                    Level.IGNORE);

            validationPolicyJaxb.getSignatureConstraints().getBasicSignatureConstraints().getSigningCertificate()
                    .getNotExpired().setLevel(Level.IGNORE); // default FAIL
            LOG.debug("Validation policy: signingCertificate/notExpired constraint original level {}, to level {}",
                    validationPolicyJaxb.getSignatureConstraints().getBasicSignatureConstraints()
                            .getSigningCertificate().getNotExpired().getLevel(),
                    Level.IGNORE);
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
     * @param parent
     *            puntatore al padre
     * @param policy
     *            validation policy utilizzata
     * @param dataDiRiferimento
     *            eventuale data di riferimento
     * @param signedDocValidator
     *            validatore custom per il tipo di documento.
     * @param idComponente
     *            id del componente
     * @param signedDocumentName
     *            nome del documento
     */
    private void addChild(DataToValidateDTOExt dataToValidateDTO, EidasWSReportsDTOTree parent,
            RemoteDocumentExt policy, Date dataDiRiferimento, SignedDocumentValidator signedDocValidator,
            String idComponente, String signedDocumentName) {

        for (AdvancedSignature sign : signedDocValidator.getSignatures()) {
            LOG.debug("Creating reports tree child element, parent signature id {}", sign.getId());

            //
            extension.extractSignatureBytes(sign, parent);
            /**
             * Nota bene: EIDAS prevede l'estrazione di N documenti originali anche se di fatto, secondo la struttura
             * "classica" delle firme "imbustate" questa cardinalità è da vedere come "piatta" su un unico livello (più
             * che un albero una lista)
             */
            try {
                //
                for (DSSDocument doc : signedDocValidator.getOriginalDocuments(sign.getId())) {
                    // set document name (from parent)
                    doc.setName(signedDocumentName);
                    // crea child per ogni documento originale
                    signedDocValidator = createChild(dataToValidateDTO, parent, policy, dataDiRiferimento,
                            signedDocValidator, idComponente, doc);
                } // originalDocument
            } catch (Exception ignore) {
                /**
                 * Eccezioni (gestite) che avvengono sullo sbustato verranno ignorate. In questo caso particolare il
                 * validatore non è riuscito ad invidivuare un documento orginale (vedi caso dei P7S)
                 */
                LOG.debug("Reports DTO Tree child signature id {} no orginal documents", sign.getId());
            }
        } // sign
    }

    private SignedDocumentValidator createChild(DataToValidateDTOExt dataToValidateDTO, EidasWSReportsDTOTree parent,
            RemoteDocumentExt policy, Date dataDiRiferimento, SignedDocumentValidator signedDocValidator,
            String idComponente, DSSDocument doc) {

        boolean errorOnValidation = false;
        Path tmpDoc = null;

        // test file
        boolean writeTmpFile = Boolean
                .parseBoolean(env.getProperty(Constants.WRITE_FILE, Constants.WRITE_FILE_DEFAULT_VAL));
        if (writeTmpFile) {
            tmpDoc = helper.writeTmpFile(doc);
        }

        Reports reports = null;
        try {
            signedDocValidator = buildValidator(doc, dataToValidateDTO);
            // build reports
            reports = buildReports(dataToValidateDTO, policy, signedDocValidator);
            // tree root
            EidasWSReportsDTOTree child = createRoot(doc, reports, idComponente);
            // set child
            parent.addChild(child);
            // call recursive
            addChild(dataToValidateDTO, child, policy, dataDiRiferimento, signedDocValidator, idComponente,
                    doc.getName());
        } catch (Exception ignore) {
            /**
             * Eccezioni (gestite) che avvengono sullo sbustato verranno ignorate. Elemento (child) aggiunto sarà
             * considerato quindi come "unsigned" (dato che la validazione non è andata a buon fine)
             */
            LOG.debug("Reports tree child element doc {} is unsigned with mimetype {}", doc.getName(),
                    doc.getMimeType() != null ? doc.getMimeType().getMimeTypeString() : "none");
            errorOnValidation = true;
            // build tree (mimetype is needed for validation) unsigned doc
            EidasWSReportsDTOTree child = new EidasWSReportsDTOTree(helper.detectMimeType(doc));
            // id componente
            child.setIdComponente(dataToValidateDTO.getIdComponente());
            // add child
            parent.addChild(child);
        } finally {
            if (writeTmpFile && !errorOnValidation) {
                FileUtils.deleteQuietly(tmpDoc.toFile());
            }
        }
        return signedDocValidator;
    }

    /**
     * Costruisci il validatore custom per il documento.
     *
     * @param signedDocument
     *            documento da verificare
     * @param dataToValidateDTO
     *            data a cui deve essere effettuata la validazione (opzionale)
     * 
     * @return validatore per il documento
     */
    private SignedDocumentValidator buildValidator(DSSDocument signedDocument, DataToValidateDTOExt dataToValidateDTO) {
        /*
         * Nota: viene prima effettuata verifica con validatori "interni" e poi quello standard di EIDAS
         */
        try {
            SignedDocumentValidator signedDocValidator = SignedDocumentValidator.fromDocument(signedDocument);
            // dataDiRiferimento if null = NOW
            // validation date
            signedDocValidator.setValidationTime(dataToValidateDTO.getDataDiRiferimento());
            // token strategy
            signedDocValidator.setTokenExtractionStrategy(
                    TokenExtractionStrategy.fromParameters(dataToValidateDTO.isIncludeCertificateRevocationValues(),
                            dataToValidateDTO.isIncludeTimestampTokenValues(),
                            dataToValidateDTO.isIncludeCertificateTokenValues()));
            signedDocValidator.setIncludeSemantics(dataToValidateDTO.isIncludeSemanticTokenValues());
            //
            signedDocValidator.setCertificateVerifier(verifier);
            LOG.debug("Signed Document Validator created class name: {}", signedDocValidator.getClass().getName());
            return signedDocValidator;
        } catch (Exception ex) {
            throw new EidasParerException().withCode(ParerError.ErrorCode.EIDAS_ERROR).withMessage(ex.getMessage());
        }

    }

}
