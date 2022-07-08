package it.eng.parer.eidas.core.service;

import static it.eng.parer.eidas.core.util.Constants.TMP_FILE_SUFFIX;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import it.eng.parer.eidas.core.helper.EidasHelper;
import it.eng.parer.eidas.model.DataToValidateDTOExt;
import it.eng.parer.eidas.model.EidasMetadataToValidate;
import it.eng.parer.eidas.model.EidasWSReportsDTOTree;
import it.eng.parer.eidas.model.RemoteDocumentExt;
import it.eng.parer.eidas.model.exception.EidasParerException;
import it.eng.parer.eidas.model.exception.ParerError;

@Service
public class VerificaFirmaImpl implements IVerificaFirma {

    private static final Logger LOG = LoggerFactory.getLogger(VerificaFirmaImpl.class);

    private static final FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions
            .asFileAttribute(PosixFilePermissions.fromString("rw-------"));

    @Autowired
    ICustomRemoteDocumentValidation service;

    @Autowired
    EidasHelper helper;

    @Override
    public EidasWSReportsDTOTree validateSignatureOnJson(DataToValidateDTOExt dataToValidateDTO,
            HttpServletRequest request) {
        // verify armor ascii
        Path signedDocumentElab = helper.verifyAndExtractFileContent(dataToValidateDTO.getUuid().concat("-"),
                dataToValidateDTO.getSignedDocumentExt().getBytes());
        if (signedDocumentElab != null) {
            // delete bytes
            dataToValidateDTO.getSignedDocumentExt().setBytes(null);
            // set file Bae64 decoded
            dataToValidateDTO.getSignedDocumentExt().setAbsolutePath(signedDocumentElab.toAbsolutePath().toString());
            LOG.debug("Ascii Armor detected, riferimento file {}",
                    dataToValidateDTO.getSignedDocumentExt().getAbsolutePath());
        }
        // call verificaFirma
        return service.validateSignature(dataToValidateDTO, request);
    }

    @Override
    public EidasWSReportsDTOTree validateSignatureOnMultipart(EidasMetadataToValidate metadata,
            HttpServletRequest request, MultipartFile signedDocument, MultipartFile[] originalDocuments,
            MultipartFile validationPolicy) {
        // insert files on DataToValidateDTOExt
        DataToValidateDTOExt dataToValidateDTO = fillDtoByMetadataAndFiles(metadata, signedDocument, originalDocuments,
                validationPolicy);
        // call verificaFirma
        return service.validateSignature(dataToValidateDTO, request);
    }

    private DataToValidateDTOExt fillDtoByMetadataAndFiles(EidasMetadataToValidate metadata,
            MultipartFile signedDocument, MultipartFile[] originalDocuments, MultipartFile validationPolicy) {

        // create dss dto
        DataToValidateDTOExt dataToValidateDTOExt = new DataToValidateDTOExt(metadata);

        try {
            // tmp files prefix
            final String tmpFilePrefix = metadata.getUuid().concat("-");
            // manage document name
            String signedDocumentFileName = metadata.getSignedDocumentName(); // default
            if (StringUtils.isBlank(signedDocumentFileName)) {
                signedDocumentFileName = StringUtils.isNotBlank(signedDocument.getOriginalFilename())
                        ? signedDocument.getOriginalFilename() : signedDocument.getName();
            }
            // tranfer to (create tmp file on disk)
            final Path signedDocumentPath = Files.createTempFile(tmpFilePrefix, TMP_FILE_SUFFIX, attr);
            signedDocument.transferTo(signedDocumentPath);
            signedDocumentPath.toFile().deleteOnExit();

            // verify armor ascii
            Path signedDocumentElab = helper.verifyAndExtractFileContent(tmpFilePrefix, signedDocumentPath);

            RemoteDocumentExt signedRemoteDocumentExt = null;
            // insert signed file
            signedRemoteDocumentExt = new RemoteDocumentExt();
            signedRemoteDocumentExt.setName(signedDocumentFileName); // set original file name
            if (signedDocumentElab != null) {
                signedRemoteDocumentExt.setAbsolutePath(signedDocumentElab.toAbsolutePath().toString());
                //
                LOG.debug("Ascii Armor detected, riferimento file {}", signedRemoteDocumentExt.getAbsolutePath());
                // delete previous file
                helper.deleteTmpFile(signedDocumentPath.toAbsolutePath().toString());
                LOG.debug("Ascii Armor detected, cancello file path {}", signedDocumentPath.toAbsolutePath());
            } else {
                signedRemoteDocumentExt.setAbsolutePath(signedDocumentPath.toAbsolutePath().toString());
            }
            dataToValidateDTOExt.setSignedDocumentExt(signedRemoteDocumentExt);

            // 2. if exists MultipartFile originalDocuments ....
            if (originalDocuments != null && originalDocuments.length != 0) {
                // create list
                dataToValidateDTOExt.setOriginalDocumentsExt(new ArrayList<>());
                // for earch multipart
                int idx = 0;
                for (MultipartFile originalDocument : originalDocuments) {
                    String originalDocumentFileName = metadata.getOriginalDocumentNames() != null
                            && metadata.getOriginalDocumentNames().length > 0 ? metadata.getOriginalDocumentNames()[idx]
                                    : StringUtils.EMPTY; // default
                    if (StringUtils.isBlank(originalDocumentFileName)) {
                        originalDocumentFileName = StringUtils.isNotBlank(originalDocument.getOriginalFilename())
                                ? originalDocument.getOriginalFilename() : originalDocument.getName().concat("_" + idx);
                    }

                    // tranfer to
                    final Path originalDocumentPath = Files.createTempFile(tmpFilePrefix, TMP_FILE_SUFFIX, attr);
                    originalDocument.transferTo(originalDocumentPath);
                    originalDocumentPath.toFile().deleteOnExit();

                    RemoteDocumentExt originalRemoteDocumentExt = null;
                    // insert signed file
                    originalRemoteDocumentExt = new RemoteDocumentExt();
                    originalRemoteDocumentExt.setName(originalDocumentFileName); // set original file name
                    originalRemoteDocumentExt.setAbsolutePath(originalDocumentPath.toAbsolutePath().toString()); // set
                    // absolute
                    // path
                    // (file
                    // for
                    // real)
                    dataToValidateDTOExt.getOriginalDocumentsExt().add(originalRemoteDocumentExt);

                    // increment index
                    idx++;
                }
            }
            // 3. if exists validation policy ....
            if (validationPolicy != null && !validationPolicy.isEmpty()) {
                final String validationPolicyFileName = StringUtils.isNotBlank(validationPolicy.getOriginalFilename())
                        ? validationPolicy.getOriginalFilename() : validationPolicy.getName();

                // tranfer to
                final Path validationPolicyPath = Files.createTempFile(tmpFilePrefix, validationPolicyFileName, attr);
                validationPolicy.transferTo(validationPolicyPath);
                validationPolicyPath.toFile().deleteOnExit();

                RemoteDocumentExt policyExt = null;
                policyExt = new RemoteDocumentExt();
                policyExt.setName(validationPolicyFileName);
                policyExt.setAbsolutePath(validationPolicyPath.toAbsolutePath().toString()); // set absolute path (file
                // for real)
                //
                dataToValidateDTOExt.setPolicyExt(policyExt);
            }
            return dataToValidateDTOExt;
        } catch (IOException ex) {
            // clean from files
            helper.deleteTmpDocExtFiles(dataToValidateDTOExt.getSignedDocumentExt(),
                    dataToValidateDTOExt.getOriginalDocumentsExt(), dataToValidateDTOExt.getPolicyExt());

            throw new EidasParerException().withCode(ParerError.ErrorCode.IO_ERROR).withMessage(ex.getMessage());
        }
    }
}
