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

package it.eng.parer.eidas.web.view;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import eu.europa.esig.dss.enumerations.MimeTypeEnum;
import eu.europa.esig.dss.ws.validation.dto.WSReportsDTO;
import it.eng.parer.eidas.core.helper.ReportRenderingHelper;
import it.eng.parer.eidas.core.service.IVerificaFirma;
import it.eng.parer.eidas.core.util.Constants;
import it.eng.parer.eidas.model.EidasDataToValidateMetadata;
import it.eng.parer.eidas.model.EidasWSReportsDTOTree;
import it.eng.parer.eidas.model.EidasRemoteDocument;
import it.eng.parer.eidas.web.bean.VerificaFirmaBean;
import it.eng.parer.eidas.web.bean.VerificaFirmaResultBean;
import it.eng.parer.eidas.web.bean.VerificaFirmaResultPaginatorBean;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

/**
 * Controller deputato al form di verifica firma manuale.
 *
 * @author Snidero_L
 */
@Controller
@SessionAttributes({ "validationModel", "validationResultPaginator" })
@ConditionalOnProperty(name = "parer.eidas.validation-ui.enabled", havingValue = "true", matchIfMissing = true)
public class ValidationController {

    private static final Logger LOG = LoggerFactory.getLogger(ValidationController.class);

    private static final String MW_VERIFY = "validation";
    private static final String MW_VERIFYRESULT = "validationresult";

    private static final String RESULT_BEAN = "validationModel";
    private static final String RESULT_PAGINATOR = "validationResultPaginator";

    @Autowired
    Environment env;

    @Autowired
    BuildProperties buildProperties;

    @Autowired
    IVerificaFirma service;

    @Autowired
    private ReportRenderingHelper renderingService;

    @GetMapping("/validation")
    public ModelAndView verifica(Model model) {
        model.addAttribute("verificafirmaBean", new VerificaFirmaBean());
        return new ModelAndView(MW_VERIFY);
    }

    @PostMapping("/validation/clean")
    public ModelAndView reset(SessionStatus status) {
        status.setComplete();
        return new ModelAndView("redirect:/validation");
    }

    @PostMapping(value = "/validation", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ModelAndView verifica(@ModelAttribute @Valid VerificaFirmaBean verificafirmaBean, BindingResult errors,
            Model model, HttpServletRequest request) {
        VerificaFirmaResultPaginatorBean paginator = new VerificaFirmaResultPaginatorBean();
        VerificaFirmaResultBean risultato = new VerificaFirmaResultBean();
        try {
            EidasDataToValidateMetadata metadata = convert(verificafirmaBean);
            EidasWSReportsDTOTree validateSignature = service.validateSignatureOnMultipart(metadata, request,
                    verificafirmaBean.getFileDaVerificare(),
                    verificafirmaBean.getFileOriginali().toArray(new MultipartFile[0]),
                    verificafirmaBean.getFileDssPolicy());

            risultato.setLivello(1);
            risultato.setBusta(1);

            compilaOutput(validateSignature, risultato, paginator);
        } catch (Exception e) {
            LOG.error("Errore durante la fase di verifica", e);
            risultato.setWithErrors(true);
        }
        model.addAttribute(RESULT_BEAN, risultato);
        model.addAttribute(RESULT_PAGINATOR, paginator);
        return new ModelAndView(MW_VERIFYRESULT);
    }

    @GetMapping(value = "/validation/nav/{dir}")
    public ModelAndView navdir(@PathVariable("dir") String dir,
            @ModelAttribute @Valid VerificaFirmaBean verificafirmaBean, BindingResult errors, Model model) {

        // get result
        VerificaFirmaResultBean risultatoVerifica = (VerificaFirmaResultBean) model.getAttribute(RESULT_BEAN);

        VerificaFirmaResultBean risultatoVerificaByLivelloBusta = renderingService.navNextBusta(dir, risultatoVerifica,
                risultatoVerifica.getLivello(), risultatoVerifica.getBusta());

        // update paginator
        VerificaFirmaResultPaginatorBean paginator = (VerificaFirmaResultPaginatorBean) model
                .getAttribute(RESULT_PAGINATOR);
        paginator.setCurPage(risultatoVerificaByLivelloBusta.getBusta());

        // update model
        model.addAttribute(RESULT_BEAN, risultatoVerificaByLivelloBusta);
        model.addAttribute(RESULT_PAGINATOR, paginator);
        return new ModelAndView(MW_VERIFYRESULT);
    }

    @GetMapping(value = "/validation/download-simple-report/{busta}/{livello}")
    public void downloadSimpleReport(@PathVariable("busta") int busta, @PathVariable("livello") int livello,
            HttpSession session, HttpServletResponse response) {
        try {
            VerificaFirmaResultBean risultatoVerifica = (VerificaFirmaResultBean) session.getAttribute(RESULT_BEAN);
            VerificaFirmaResultBean ricerca = risultatoVerifica.ricerca(livello, busta);
            String simpleReport = ricerca.getSimpleReportXml();

            response.setContentType(MimeTypeEnum.PDF.getMimeTypeString());
            response.setHeader("Content-Disposition", "attachment; filename=DSS-Simple-report.pdf");

            renderingService.generateSimpleReportPdf(simpleReport, response.getOutputStream());
        } catch (Exception e) {
            LOG.error("An error occured while generating pdf for simple report", e);
        }

    }

    @GetMapping(value = "/validation/download-detailed-report/{busta}/{livello}")
    public void downloadDetailedReport(@PathVariable("busta") int busta, @PathVariable("livello") int livello,
            HttpSession session, HttpServletResponse response) {
        try {
            VerificaFirmaResultBean risultatoVerifica = (VerificaFirmaResultBean) session.getAttribute(RESULT_BEAN);
            VerificaFirmaResultBean ricerca = risultatoVerifica.ricerca(livello, busta);
            String simpleReport = ricerca.getDetailedReportXml();

            response.setContentType(MimeTypeEnum.PDF.getMimeTypeString());
            response.setHeader("Content-Disposition", "attachment; filename=DSS-Detailed-report.pdf");

            renderingService.generateDetailedReportPdf(simpleReport, response.getOutputStream());
        } catch (Exception e) {
            LOG.error("An error occured while generating pdf for simple report", e);
        }
    }

    /**
     * Crea l'oggetto da utilizzare per la view contenente l'esito della verifica. <strong>Nota bene:</strong> il
     * popolamento ricorsivo
     *
     * @param validateSignature
     *            esito verifica firma
     * @param risultato
     *            oggetto view rappresentante l'esito.
     * @param paginator
     * @param nrBuste
     * @param nrrisultati
     */
    private void compilaOutput(EidasWSReportsDTOTree validateSignature, VerificaFirmaResultBean risultato,
            VerificaFirmaResultPaginatorBean paginator) {
        WSReportsDTO report = validateSignature.getReport();
        String simpleReport = renderingService.generateSimpleReport(report.getSimpleReport());
        String detailedReport = renderingService.generateDetailedReport(report.getDetailedReport());
        String diagnosticData = renderingService.generateDiagnosticData(report.getDiagnosticData());

        // memorizzarli eventualmente in sessione.
        String simpleReportXml = renderingService.marshallSimpleReport(report.getSimpleReport());
        String detailedReportXml = renderingService.marshallDetailedReport(report.getDetailedReport());

        risultato.setSimpleReportXml(simpleReportXml);
        risultato.setDetailedReportXml(detailedReportXml);

        risultato.setSimpleReport(simpleReport);
        risultato.setDetailedReport(detailedReport);
        risultato.setDiagnosticData(diagnosticData);

        if (validateSignature.getChildren() != null) {
            for (int indice = 0; indice < validateSignature.getChildren().size(); indice++) {
                EidasWSReportsDTOTree child = validateSignature.getChildren().get(indice);
                if (!child.isUnsigned()) {
                    VerificaFirmaResultBean figlio = new VerificaFirmaResultBean();
                    figlio.setBusta(risultato.getBusta() + 1);
                    figlio.setLivello(indice + 1);
                    // parent
                    figlio.setParent(risultato);
                    paginator.incNrResult();
                    compilaOutput(child, figlio, paginator);
                    risultato.add(figlio);
                }
            }
        }
    }

    private static EidasDataToValidateMetadata convert(VerificaFirmaBean verificaFirmaBean) {
        EidasDataToValidateMetadata dataToValidate = new EidasDataToValidateMetadata();
        try {
            dataToValidate.setControlloRevocaIgnorato(!verificaFirmaBean.isAbilitaControlloRevoca());
            dataToValidate.setControlloCatenaTrustIgnorato(!verificaFirmaBean.isAbilitaControlloCatenaTrusted());
            dataToValidate.setControlloCertificatoIgnorato(!verificaFirmaBean.isAbilitaControlloCa());
            dataToValidate.setControlloCrittograficoIgnorato(!verificaFirmaBean.isAbilitaControlloCrittografico());
            // include raw
            dataToValidate.setIncludeCertificateRevocationValues(verificaFirmaBean.isIncludiRaw());
            dataToValidate.setIncludeCertificateTokenValues(verificaFirmaBean.isIncludiRaw());
            dataToValidate.setIncludeTimestampTokenValues(verificaFirmaBean.isIncludiRaw());

            Date dataRiferimento = new Date();
            LocalDate dataRiferimentoForm = verificaFirmaBean.getDataRiferimento();
            LocalTime oraRiferimentoForm = verificaFirmaBean.getOraRiferimento();

            if (dataRiferimentoForm != null) {
                if (oraRiferimentoForm == null) {
                    oraRiferimentoForm = LocalTime.MIN;
                }
                LocalDateTime atDate = oraRiferimentoForm.atDate(dataRiferimentoForm);
                ZonedDateTime zatDate = atDate.atZone(ZoneId.systemDefault());
                dataRiferimento = Date.from(zatDate.toInstant());
            }

            dataToValidate.setDataDiRiferimento(dataRiferimento);

            MultipartFile fileDaVerificare = verificaFirmaBean.getFileDaVerificare();

            dataToValidate.setDocumentId(fileDaVerificare.getName());
            dataToValidate.setUuid(UUID.randomUUID().toString());

            EidasRemoteDocument signedDocument = buildDocument(fileDaVerificare);
            // Documento firmato
            dataToValidate.setRemoteSignedDocument(signedDocument);

            MultipartFile policy = verificaFirmaBean.getFileDssPolicy();
            if (policy != null && !policy.isEmpty()) {
                EidasRemoteDocument policyDocument = buildDocument(policy);
                // Custom policy DSS
                dataToValidate.setPolicyExt(policyDocument);
            }

            List<MultipartFile> documentiOriginali = verificaFirmaBean.getFileOriginali();
            if (documentiOriginali != null) {
                for (MultipartFile documentoOriginale : documentiOriginali) {
                    if (!documentoOriginale.isEmpty()) {
                        EidasRemoteDocument originalDocument = buildDocument(documentoOriginale);
                        if (dataToValidate.getRemoteOriginalDocuments() == null) {
                            dataToValidate.setRemoteOriginalDocuments(new ArrayList<EidasRemoteDocument>());
                        }
                        // Original Documents
                        dataToValidate.getRemoteOriginalDocuments().add(originalDocument);
                    }
                }
            }

        } catch (IOException e) {
            LOG.error("Errore durante la compilazione del dto di input", e);
        }
        return dataToValidate;
    }

    private static EidasRemoteDocument buildDocument(MultipartFile fileUploaded) throws IOException {
        EidasRemoteDocument policyDocument = new EidasRemoteDocument();
        policyDocument.setName(fileUploaded.getName());
        policyDocument.setAbsolutePath(fileUploaded.getOriginalFilename());
        policyDocument.setBytes(fileUploaded.getBytes());
        return policyDocument;

    }

    @ModelAttribute("version")
    public String getVersion() {
        return env.getProperty(Constants.BUILD_VERSION);
    }

    @ModelAttribute("builddate")
    public String getBuilddate() {
        return env.getProperty(Constants.BUILD_TIME);
    }

    @ModelAttribute("dss")
    public String getDss() {
        return buildProperties.get(Constants.DSS_VERSION);
    }

    @ModelAttribute("displayDownloadPdf")
    public boolean isDisplayDownloadPdf() {
        return true;
    }
}
