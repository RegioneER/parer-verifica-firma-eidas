package it.eng.parer.eidas.core.service;

import org.springframework.web.multipart.MultipartFile;

import it.eng.parer.eidas.model.EidasDataToValidateMetadata;
import it.eng.parer.eidas.model.EidasWSReportsDTOTree;
import jakarta.servlet.http.HttpServletRequest;

public interface IVerificaFirma {

    default EidasWSReportsDTOTree validateSignature(EidasDataToValidateMetadata dataToValidate) {
        return validateSignatureOnJson(dataToValidate, null);
    }

    EidasWSReportsDTOTree validateSignatureOnJson(EidasDataToValidateMetadata dataToValidate,
            HttpServletRequest request);

    EidasWSReportsDTOTree validateSignatureOnMultipart(EidasDataToValidateMetadata metadata, HttpServletRequest request,
            MultipartFile signedDocument, MultipartFile[] originalDocuments, MultipartFile validationPolicy);

}
