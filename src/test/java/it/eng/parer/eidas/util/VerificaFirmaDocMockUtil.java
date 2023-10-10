package it.eng.parer.eidas.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.IOUtils;

import it.eng.parer.eidas.model.EidasDataToValidateMetadata;
import it.eng.parer.eidas.model.EidasRemoteDocument;

/**
 * <p>
 * VerificaFirmaDocMockUtil class.
 * </p>
 *
 * @author stefano
 * 
 * @version $Id: $Id
 * 
 * @since 1.10.1
 */
public class VerificaFirmaDocMockUtil {

    /**
     * <p>
     * createMockDto.
     * </p>
     *
     * @param fileWithSignature
     *            a {@link java.io.InputStream} object
     * 
     * @return a {@link it.eng.parer.eidas.model.EidasDataToValidateMetadata} object
     * 
     * @throws java.io.IOException
     *             if any.
     * @throws java.net.URISyntaxException
     *             if any.
     */
    public static EidasDataToValidateMetadata createMockDto(InputStream fileWithSignature)
            throws IOException, URISyntaxException {
        return createMockDto(null, fileWithSignature);
    }

    public static EidasDataToValidateMetadata createMockDto(URI fileWithSignature)
            throws IOException, URISyntaxException {
        return createMockDto(null, fileWithSignature);
    }

    public static EidasDataToValidateMetadata createMockDto(InputStream fileOriginal, InputStream fileWithSignature)
            throws IOException {
        EidasDataToValidateMetadata dto = createBaseDto(fileOriginal != null);
        return remoteDocumentAsByteArr(dto, fileOriginal, fileWithSignature);
    }

    public static EidasDataToValidateMetadata createMockDto(URI fileOriginal, URI fileWithSignature)
            throws IOException {
        EidasDataToValidateMetadata dto = createBaseDto(fileOriginal != null);
        return remoteDocumentAsURI(dto, fileOriginal, fileWithSignature);
    }

    private static EidasDataToValidateMetadata createBaseDto(boolean hasFileOriginal) {
        EidasDataToValidateMetadata dto = new EidasDataToValidateMetadata();
        List<EidasRemoteDocument> asList = new ArrayList<>();

        if (hasFileOriginal) {
            EidasRemoteDocument originalDocument = new EidasRemoteDocument();
            asList.add(originalDocument);
            dto.setRemoteOriginalDocuments(asList);

        }

        EidasRemoteDocument signedDocument = new EidasRemoteDocument();
        dto.setRemoteSignedDocument(signedDocument);
        dto.setPolicy(null);
        dto.setDocumentId("ID_TEST");
        dto.setUuid(UUID.randomUUID().toString());

        return dto;
    }

    private static EidasDataToValidateMetadata remoteDocumentAsByteArr(EidasDataToValidateMetadata dto,
            InputStream fileOriginal, InputStream fileWithSignature) throws IOException {
        if (fileOriginal != null) {
            dto.getRemoteOriginalDocuments().get(0).setBytes(IOUtils.toByteArray(fileOriginal));
        }
        dto.getRemoteSignedDocument().setBytes(IOUtils.toByteArray(fileWithSignature));
        return dto;
    }

    private static EidasDataToValidateMetadata remoteDocumentAsURI(EidasDataToValidateMetadata dto, URI fileOriginal,
            URI fileWithSignature) throws IOException {
        if (fileOriginal != null) {
            dto.getRemoteOriginalDocuments().get(0).setUri(fileOriginal);
        }
        dto.getRemoteSignedDocument().setUri(fileWithSignature);
        return dto;
    }

}
