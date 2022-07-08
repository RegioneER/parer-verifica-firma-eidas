package it.eng.parer.eidas.core.service;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.multipart.MultipartFile;

import it.eng.parer.eidas.model.DataToValidateDTOExt;
import it.eng.parer.eidas.model.EidasMetadataToValidate;
import it.eng.parer.eidas.model.EidasWSReportsDTOTree;

public interface IVerificaFirma {

    default EidasWSReportsDTOTree validateSignature(DataToValidateDTOExt dataToValidate) {
        return validateSignatureOnJson(dataToValidate, null);
    }

    EidasWSReportsDTOTree validateSignatureOnJson(DataToValidateDTOExt dataToValidate, HttpServletRequest request);

    EidasWSReportsDTOTree validateSignatureOnMultipart(EidasMetadataToValidate metadata, HttpServletRequest request,
            MultipartFile signedDocument, MultipartFile[] originalDocuments, MultipartFile validationPolicy);

}
