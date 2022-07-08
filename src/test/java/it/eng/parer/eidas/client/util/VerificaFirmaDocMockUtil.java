package it.eng.parer.eidas.client.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.uuid.Generators;

import it.eng.parer.eidas.model.DataToValidateDTOExt;
import it.eng.parer.eidas.model.EidasMetadataToValidate;
import it.eng.parer.eidas.model.RemoteDocumentExt;

public class VerificaFirmaDocMockUtil {

    private final static Logger LOG = LoggerFactory.getLogger(VerificaFirmaDocMockUtil.class);

    public static DataToValidateDTOExt createMockDto(InputStream fileWithSignature) throws IOException {
        return createMockDto(null, fileWithSignature, false);
    }

    public static DataToValidateDTOExt createMockDto(InputStream fileOriginal, InputStream fileWithSignature)
            throws IOException {
        return createMockDto(fileOriginal, fileWithSignature, false);
    }

    public static DataToValidateDTOExt createMockDto(InputStream fileOriginal, InputStream fileWithSignature,
            boolean saveJson) throws IOException {
        DataToValidateDTOExt dto = new DataToValidateDTOExt();
        List<RemoteDocumentExt> asList = new ArrayList<>();

        if (fileOriginal != null) {
            RemoteDocumentExt originalDocument = new RemoteDocumentExt();
            originalDocument.setBytes(IOUtils.toByteArray(fileOriginal));
            asList.add(originalDocument);
            dto.setOriginalDocumentsExt(asList);

        }

        RemoteDocumentExt signedDocument = new RemoteDocumentExt();
        signedDocument.setBytes(IOUtils.toByteArray(fileWithSignature));
        dto.setSignedDocumentExt(signedDocument);
        dto.setPolicy(null);
        dto.setIdComponente("ID_TEST");
        dto.setUuid(Generators.randomBasedGenerator().generate().toString());
        if (saveJson) {
            File tmpF = File.createTempFile(UUID.randomUUID().toString(), ".json");
            ObjectMapper Obj = new ObjectMapper();
            String jsonStr = Obj.writeValueAsString(dto);
            FileUtils.writeStringToFile(tmpF, jsonStr, Charset.defaultCharset().name(), false);

            LOG.info("saving JSON ..... as file {}", tmpF.getAbsolutePath());
        }
        return dto;
    }

    public static EidasMetadataToValidate createMockMetadata() throws IOException {
        EidasMetadataToValidate metadata = new EidasMetadataToValidate();
        metadata.setControlloCatenaTrustIgnorato(true);
        metadata.setControlloCertificatoIgnorato(true);
        metadata.setControlloCrittograficoIgnorato(true);
        metadata.setControlloRevocaIgnorato(false);
        metadata.setDataDiRiferimento(new Date());
        metadata.setIdDocuments("ID1");
        metadata.setVerificaAllaDataDiFirma(false);

        return metadata;
    }

}
