/*
 * Engineering Ingegneria Informatica S.p.A.
 *
 * Copyright (C) 2023 Regione Emilia-Romagna <p/> This program is free software: you can
 * redistribute it and/or modify it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. <p/> This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Affero General Public License for more details. <p/> You should
 * have received a copy of the GNU Affero General Public License along with this program. If not,
 * see <https://www.gnu.org/licenses/>.
 */

package it.eng.parer.eidas.core.service;

import static it.eng.parer.eidas.core.util.Constants.TMP_FILE_SUFFIX;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import it.eng.parer.eidas.core.helper.EidasHelper;
import it.eng.parer.eidas.model.EidasDataToValidateMetadata;
import it.eng.parer.eidas.model.EidasRemoteDocument;
import it.eng.parer.eidas.model.EidasWSReportsDTOTree;
import it.eng.parer.eidas.model.exception.EidasParerException;
import it.eng.parer.eidas.model.exception.ParerError;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class VerificaFirmaImpl implements IVerificaFirma {

    private static final Logger log = LoggerFactory.getLogger(VerificaFirmaImpl.class);

    private static final FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions
            .asFileAttribute(PosixFilePermissions.fromString("rw-------"));

    private final ICustomRemoteDocumentValidation service;
    private final EidasHelper helper;

    public VerificaFirmaImpl(ICustomRemoteDocumentValidation service, EidasHelper helper) {
        this.service = service;
        this.helper = helper;
    }

    @Override
    public EidasWSReportsDTOTree validateSignatureOnJson(
            EidasDataToValidateMetadata dataToValidateDTO, HttpServletRequest request) {
        // verify dto
        elabValidateDtoExt(dataToValidateDTO);
        return validateDocumentSignatureAndOrMimeType(dataToValidateDTO, request);
    }

    @Override
    public EidasWSReportsDTOTree validateSignatureOnMultipart(
            EidasDataToValidateMetadata dataToValidateDTO, HttpServletRequest request,
            MultipartFile signedDocument, MultipartFile[] originalDocuments,
            MultipartFile validationPolicy) {
        // verify dto
        elaborateValidateDtoExtMultiPart(dataToValidateDTO, signedDocument, originalDocuments,
                validationPolicy);
        return validateDocumentSignatureAndOrMimeType(dataToValidateDTO, request);
    }

    /*
     * se skipDocumentSignVerification è true, allora non eseguo la verifica della firma ma solo la
     * validazione del mimetype, altrimenti eseguo la verifica della firma. Questa logica è stata
     * introdotta per gestire il caso in cui si voglia eseguire solo la validazione del mimetype
     * senza eseguire la verifica della firma, ad esempio per i casi in cui si voglia eseguire la
     * validazione del mimetype su un documento che non è firmato, ma che è comunque un documento
     * valido per la validazione del mimetype, come ad esempio un documento che è stato firmato in
     * modo non standard o che è stato modificato dopo la firma. In questi casi, se
     * skipDocumentSignVerification è true, allora eseguo solo la validazione del mimetype e non
     * eseguo la verifica della firma, altrimenti eseguo la verifica della firma e la validazione
     * del mimetype.
     *
     */
    private EidasWSReportsDTOTree validateDocumentSignatureAndOrMimeType(
            EidasDataToValidateMetadata dataToValidateDTO, HttpServletRequest request) {
        if (dataToValidateDTO.isSkipDocumentSignVerification()) {
            return service.validateOnlyMimetype(dataToValidateDTO, request);
        }
        return service.validateSignatureWithMimetype(dataToValidateDTO, request);
    }

    private void elaborateValidateDtoExtMultiPart(EidasDataToValidateMetadata metadata,
            MultipartFile signedDocument, MultipartFile[] originalDocuments,
            MultipartFile validationPolicy) {

        try {
            // tmp files prefix
            final String tmpFilePrefix = metadata.getUuid().concat("-");
            // signed document
            EidasRemoteDocument signedRemoteDocumentExt = metadata.getRemoteSignedDocument();
            // transfer to (create tmp file on disk)
            final Path signedDocumentPath = transferViaMultiPartFile(signedDocument, tmpFilePrefix,
                    TMP_FILE_SUFFIX);
            // verify armor ascii
            final Path signedDocumentElab = helper.verifyAndExtractFileContent(metadata,
                    tmpFilePrefix, signedDocumentPath);

            // insert signed file
            if (signedDocumentElab != null) {
                signedRemoteDocumentExt
                        .setAbsolutePath(signedDocumentElab.toAbsolutePath().toString());
                //
                log.atDebug().log("Ascii Armor detected, riferimento file {}",
                        signedRemoteDocumentExt.getAbsolutePath());
                // delete previous file
                helper.deleteTmpFile(signedDocumentPath.toAbsolutePath().toString());
                log.atDebug().log("Ascii Armor detected, cancello file path {}",
                        signedDocumentPath.toAbsolutePath());
            } else {
                signedRemoteDocumentExt
                        .setAbsolutePath(signedDocumentPath.toAbsolutePath().toString());
            }

            // 2. if exists MultipartFile originalDocuments ....
            if (originalDocuments != null && originalDocuments.length != 0) {
                // for earch multipart
                for (MultipartFile originalDocument : originalDocuments) {
                    // search on metadata by multipart file name
                    EidasRemoteDocument originalRemoteDocumentExt = metadata
                            .getRemoteOriginalDocuments().stream()
                            .filter(d -> d.getName().equalsIgnoreCase(originalDocument.getName())
                                    || d.getName().equalsIgnoreCase(
                                            originalDocument.getOriginalFilename()))
                            .findFirst().orElseGet(() -> {
                                EidasRemoteDocument newDoc = new EidasRemoteDocument();
                                metadata.getRemoteOriginalDocuments().add(newDoc);
                                return newDoc;
                            });
                    // transfer to (create tmp file on disk)
                    final Path originalDocumentPath = transferViaMultiPartFile(originalDocument,
                            tmpFilePrefix, TMP_FILE_SUFFIX);
                    // set path
                    originalRemoteDocumentExt
                            .setAbsolutePath(originalDocumentPath.toAbsolutePath().toString());
                }
            }
            // 3. if exists validation policy ....
            if (validationPolicy != null && !validationPolicy.isEmpty()) {
                // transfer to (create tmp file on disk)
                final Path validationPolicyPath = transferViaMultiPartFile(validationPolicy,
                        tmpFilePrefix, TMP_FILE_SUFFIX);
                // check metadata
                EidasRemoteDocument policyRemoteDocumentExt = metadata.getPolicyExt();
                if (policyRemoteDocumentExt == null) {
                    policyRemoteDocumentExt = new EidasRemoteDocument();
                }
                // set path
                policyRemoteDocumentExt
                        .setAbsolutePath(validationPolicyPath.toAbsolutePath().toString());
            }
        } catch (IOException ex) {
            // clean from files
            helper.deleteTmpDocExtFiles(metadata.getRemoteSignedDocument(),
                    metadata.getRemoteOriginalDocuments(), metadata.getPolicyExt());

            throw new EidasParerException(metadata, ex).withCode(ParerError.ErrorCode.IO_ERROR)
                    .withMessage("Errore durante elaborazione richiesta");
        }
    }

    private Path transferViaMultiPartFile(MultipartFile multipartFile, final String tmpFilePrefix,
            final String tmpFileSuffix) throws IOException {
        final Path localDocumentPath = Files.createTempFile(tmpFilePrefix, tmpFileSuffix, attr);
        multipartFile.transferTo(localDocumentPath);
        return localDocumentPath;
    }

    private void elabValidateDtoExt(EidasDataToValidateMetadata dataToValidateDTO) {

        try {
            // tmp files prefix
            final String tmpFilePrefix = dataToValidateDTO.getUuid().concat("-");
            // 1. manage signed document
            // remote signed document
            EidasRemoteDocument signedRemoteDocumentExt = dataToValidateDTO
                    .getRemoteSignedDocument();
            // transfer to (create tmp file on disk)
            final Path signedDocumentPath = transferRemoteDocumentToLocalPath(
                    signedRemoteDocumentExt, tmpFilePrefix, TMP_FILE_SUFFIX);
            // set file path
            signedRemoteDocumentExt.setAbsolutePath(signedDocumentPath.toAbsolutePath().toString());
            // verify armor ascii
            final Path signedDocumentElab = helper.verifyAndExtractFileContent(dataToValidateDTO,
                    tmpFilePrefix, signedDocumentPath);
            if (signedDocumentElab != null) {
                // change file pointer
                signedRemoteDocumentExt
                        .setAbsolutePath(signedDocumentElab.toAbsolutePath().toString());
                //
                log.atDebug().log("Ascii Armor detected, riferimento file {}",
                        signedRemoteDocumentExt.getAbsolutePath());
                // delete previous file
                helper.deleteTmpFile(signedDocumentPath.toAbsolutePath().toString());
                log.atDebug().log("Ascii Armor detected, cancello file path {}",
                        signedDocumentPath.toAbsolutePath());
            }

            // 2. if exists originalDocuments ....
            // for each originalDocument
            for (EidasRemoteDocument originalDocument : dataToValidateDTO
                    .getRemoteOriginalDocuments()) {
                // transfer to (create tmp file on disk)
                final Path originalDocumentPath = transferRemoteDocumentToLocalPath(
                        originalDocument, tmpFilePrefix, TMP_FILE_SUFFIX);
                // set file path
                originalDocument.setAbsolutePath(originalDocumentPath.toAbsolutePath().toString());
            }

            // 3. if exists validation policy ....
            if (dataToValidateDTO.getPolicyExt() != null) {
                EidasRemoteDocument policyDocumentExt = dataToValidateDTO.getPolicyExt();
                // transfer to (create tmp file on disk)
                final Path validationPolicyPath = transferRemoteDocumentToLocalPath(
                        policyDocumentExt, tmpFilePrefix, TMP_FILE_SUFFIX);
                // set file path
                policyDocumentExt.setAbsolutePath(validationPolicyPath.toAbsolutePath().toString());
            }
        } catch (IOException ioex) {
            // clean from files
            helper.deleteTmpDocExtFiles(dataToValidateDTO.getRemoteSignedDocument(),
                    dataToValidateDTO.getRemoteOriginalDocuments(),
                    dataToValidateDTO.getPolicyExt());

            throw new EidasParerException(dataToValidateDTO, ioex)
                    .withCode(ParerError.ErrorCode.IO_ERROR)
                    .withMessage("Errore durante elaborazione richiesta");
        }
    }

    private Path transferRemoteDocumentToLocalPath(EidasRemoteDocument remoteDocumentExt,
            String tmpFilePrefix, String tmpFileSuffix) throws IOException {
        final Path localPath = Files.createTempFile(tmpFilePrefix, tmpFileSuffix, attr);
        helper.getResourceFromURI(remoteDocumentExt.getUri(), localPath);
        return localPath;
    }

}
