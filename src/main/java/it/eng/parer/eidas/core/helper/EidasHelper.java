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

package it.eng.parer.eidas.core.helper;

import static it.eng.parer.eidas.core.util.Constants.BUILD_VERSION;
import static it.eng.parer.eidas.core.util.Constants.DSS_VERSION;
import static it.eng.parer.eidas.core.util.Constants.TMP_FILE_SUFFIX;
import static org.apache.tika.metadata.TikaCoreProperties.RESOURCE_NAME_KEY;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import eu.europa.esig.dss.enumerations.MimeTypeEnum;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.spi.DSSUtils;
import it.eng.parer.eidas.model.EidasDataToValidateMetadata;
import it.eng.parer.eidas.model.EidasRemoteDocument;
import it.eng.parer.eidas.model.exception.EidasParerException;
import it.eng.parer.eidas.model.exception.ParerError;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

@Component
public class EidasHelper {

    private static final Logger LOG = LoggerFactory.getLogger(EidasHelper.class);

    private static final Detector TIKA_DETECTOR = TikaConfig.getDefaultConfig().getDetector();

    private static final FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions
            .asFileAttribute(PosixFilePermissions.fromString("rw-------"));

    private static final int BUFFER_SIZE = 2048;

    private static final String LOG_BASE64_ERROR = "Errore verifica ASCII-Armor";

    @Autowired
    Environment env;

    @Autowired
    BuildProperties buildProperties;

    // default 60 s
    @Value("${parer.eidas.webclient.timeout:360}")
    long webClientTimeout;

    // default 5 times
    @Value("${parer.eidas.webclient.backoff:10}")
    long webClientBackoff;

    // default 3 s
    @Value("${parer.eidas.webclient.backofftime:3}")
    long webClientBackoffTime;

    public String buildversion() {
        return env.getProperty(BUILD_VERSION);
    }

    public String dssversion() {
        return buildProperties.get(DSS_VERSION);
    }

    /**
     * Metodo rivisto e corretto a partire da quello esistente su sacerws.
     *
     * @param document
     *            documento DSS, sia esso un inMemory sia con path assoluto
     * 
     * @return il mime type identificato o null se il contenuto non risulta leggibile.
     */
    public String detectMimeType(DSSDocument document) {
        String mimeType = null;
        // Uso il TikaInputStream per riconoscere il mimetype application/msword (SUE
        // #25694)
        try (InputStream is = TikaInputStream.get(document.openStream());) {
            Metadata metadata = new Metadata();
            metadata.set(RESOURCE_NAME_KEY, null);
            MediaType mime = TIKA_DETECTOR.detect(is, metadata);
            mimeType = mime.toString();
            // text/plain vs application/xml
            if (mime.compareTo(MediaType.TEXT_PLAIN) == 0) {
                mimeType = validateXML(mimeType, is);
            }
        } catch (IOException ex) {
            LOG.debug("Impossibile leggere il DSSDocument durante il calcolo del MimeType", ex);
        }
        return mimeType;
    }

    private String validateXML(String currentMimeType, InputStream is) {
        try {
            // check if is a valid XML
            // a new one beacause is NOT THREAD SAFE !
            getSecureSchemaFactory().parse(is);
            // change with correct type
            return MimeTypeEnum.XML.getMimeTypeString();
        } catch (Exception ex) {
            LOG.debug("Calcolato mime : {}, non risulta XML valido", currentMimeType, ex);
        }
        return currentMimeType;
    }

    private DocumentBuilder getSecureSchemaFactory() throws ParserConfigurationException {
        DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();
        dbfactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        dbfactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        dbfactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return dbfactory.newDocumentBuilder();
    }

    /**
     * Cancellazione dei file temporanei
     *
     * @param signedDocumentExt
     *            documento DSS, sia esso un inMemory sia con path assoluto
     * @param originalDocumentsExt
     *            documento DSS, sia esso un inMemory sia con path assoluto
     * @param policyExt
     *            documento DSS, policy (standard / custom)
     *
     */
    public void deleteTmpDocExtFiles(EidasRemoteDocument signedDocumentExt,
            List<EidasRemoteDocument> originalDocumentsExt, EidasRemoteDocument policyExt) {
        // signed file
        deleteTmpFile(signedDocumentExt.getAbsolutePath());

        // original file
        if (originalDocumentsExt != null && !originalDocumentsExt.isEmpty()) {
            for (EidasRemoteDocument originalFileExt : originalDocumentsExt) {
                deleteTmpFile(originalFileExt.getAbsolutePath());
            }
        }

        // policy file
        if (policyExt != null) {
            deleteTmpFile(policyExt.getAbsolutePath());
        }
    }

    public void deleteTmpFile(String fileName) {
        if (StringUtils.isNotBlank(fileName)) {
            File tmpFile = new File(fileName);
            if (tmpFile.exists()) {
                boolean result = FileUtils.deleteQuietly(new File(fileName));
                if (!result) {
                    LOG.warn("Impossibile cancellare il file temporaneo {}", fileName);
                }
            }
        }
    }

    /**
     * Verifica contenuto array di byte. 1. verifica se ASCII armor 2. verifica se base64 encoded
     * 
     * @param dataToValidateMetadata
     *            metadati forniti al servizio di validazione
     * 
     * @param prefix
     *            prefisso del file da utilizzare per la generazione del risultato
     * @param signedBytes
     *            array di byte con contenuto del file sottoposto a verifica
     * 
     * @return file risultato dell'elaborazione o null se non rientra nei due casi
     */
    public Path verifyAndExtractFileContent(EidasDataToValidateMetadata dataToValidateMetadata, String prefix,
            byte[] signedBytes) {

        // detect ASCII armor
        Path asciiarmor = detectASCIIArmor(dataToValidateMetadata, prefix, signedBytes);
        if (asciiarmor != null) {
            return asciiarmor;
        }
        // detect Base64 encoded
        Path decodedBase64 = detectBase64Encoded(dataToValidateMetadata, prefix, signedBytes);
        if (decodedBase64 != null) {
            return decodedBase64;
        }

        return null; // no detection
    }

    /**
     * Verifica contenuto path relativo al file su disco. 1. verifica se ASCII armor 2. verifica se base64 encoded
     * 
     * @param dataToValidateMetadata
     *            metadati forniti al servizio di validazione
     * 
     * @param prefix
     *            prefisso del file da utilizzare per la generazione del risultato
     * @param signedFile
     *            path file sottoposto a verifica
     * 
     * @return file risultato dell'elaborazione o null se non rientra nei due casi
     */
    public Path verifyAndExtractFileContent(EidasDataToValidateMetadata dataToValidateMetadata, String prefix,
            Path signedFile) {

        // detect ASCII armor
        Path asciiarmor = detectASCIIArmor(dataToValidateMetadata, prefix, signedFile);
        if (asciiarmor != null) {
            return asciiarmor;
        }
        // detect Base64 encoded
        Path decodedBase64 = detectBase64Encoded(dataToValidateMetadata, prefix, signedFile);
        if (decodedBase64 != null) {
            return decodedBase64;
        }

        return null; // no detection
    }

    /*
     * Verifica se i byte[] relativi al file (passati via json) fanno riferimento ad un ASCII-Armor, in quel caso si
     * ottiene come risultato il Path di un nuovo file contenente la decodifica in Base64 del file originale.
     *
     */
    private Path detectASCIIArmor(EidasDataToValidateMetadata dataToValidateMetadata, String prefix,
            byte[] signedBytes) {
        try (InputStream is = new ByteArrayInputStream(signedBytes)) {
            return detectASCIIArmor(dataToValidateMetadata, prefix, is);
        } catch (IOException e) {
            throw new EidasParerException(dataToValidateMetadata, e).withCode(ParerError.ErrorCode.IO_ERROR)
                    .withMessage(LOG_BASE64_ERROR);
        }
    }

    /*
     *
     * Verifica se il file passato è un ASCII-Armor, in quel caso si ottiene come risultato il Path di un nuovo file
     * contenente la decodifica in Base64 del file originale.
     * 
     */
    private Path detectASCIIArmor(EidasDataToValidateMetadata dataToValidateMetadata, String prefix, Path signedFile) {
        try (InputStream is = new FileInputStream(signedFile.toFile())) {
            return detectASCIIArmor(dataToValidateMetadata, prefix, is);
        } catch (IOException e) {
            throw new EidasParerException(dataToValidateMetadata, e).withCode(ParerError.ErrorCode.IO_ERROR)
                    .withMessage(LOG_BASE64_ERROR);
        }
    }

    private Path detectASCIIArmor(EidasDataToValidateMetadata dataToValidateMetadata, String prefix, InputStream is) {
        final String BEGIN = "-----BEGIN ";
        final String END = "-----END ";

        Path base64NoArmorPath = null;
        // check if signed file is an ascii armor
        if (isASCIIArmor(is)) {
            //
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                base64NoArmorPath = Files.createTempFile(prefix, TMP_FILE_SUFFIX, attr);
                try (OutputStream os = new FileOutputStream(base64NoArmorPath.toFile())) {
                    for (String line; (line = reader.readLine()) != null;) {
                        // skip BEGIN / END
                        if (line.startsWith(BEGIN) || line.startsWith(END)) {
                            continue;
                        }
                        // write
                        os.write(org.apache.commons.codec.binary.Base64.decodeBase64(line));
                    }
                } // writer
            } catch (IOException e) {
                throw new EidasParerException(dataToValidateMetadata, e).withCode(ParerError.ErrorCode.IO_ERROR)
                        .withMessage("Errore generico");
            }
        }

        return base64NoArmorPath;
    }

    private boolean isASCIIArmor(InputStream is) {
        try {
            final String BEGIN = "-----BEGIN ";
            int headerLength = 22;
            byte[] preamble = new byte[headerLength];
            // read bytes
            DSSUtils.readAvailableBytes(is, preamble);
            String preambleString = new String(preamble);
            return preambleString.startsWith(BEGIN);
        } catch (IllegalStateException e) {
            LOG.error("Detect ASCII Armor error / File reading preamble problem {}", e.getMessage());
            return false; // Nota: is not an ascii armor. Try to validate.
        }
    }

    // base64 file detection

    private Path detectBase64Encoded(EidasDataToValidateMetadata dataToValidateMetadata, String prefix,
            byte[] signedBytes) {
        try (InputStream is = new ByteArrayInputStream(signedBytes)) {
            return readBase64EncodedFile(prefix, is);
        } catch (IOException e) {
            throw new EidasParerException(dataToValidateMetadata, e).withCode(ParerError.ErrorCode.IO_ERROR)
                    .withMessage(LOG_BASE64_ERROR);
        }
    }

    private Path detectBase64Encoded(EidasDataToValidateMetadata dataToValidateMetadata, String prefix,
            Path signedFile) {
        try (InputStream is = new FileInputStream(signedFile.toFile())) {
            return readBase64EncodedFile(prefix, is);
        } catch (IOException e) {
            throw new EidasParerException(dataToValidateMetadata, e).withCode(ParerError.ErrorCode.IO_ERROR)
                    .withMessage("Errore verifica Base64");
        }
    }

    private Path readBase64EncodedFile(String prefix, InputStream is) throws IOException {
        //
        Path base64Encoded = Files.createTempFile(prefix, TMP_FILE_SUFFIX, attr);
        try (OutputStream os = new FileOutputStream(base64Encoded.toFile())) {
            //
            try (InputStream reader = java.util.Base64.getDecoder().wrap(is)) {
                byte[] buf = new byte[BUFFER_SIZE];
                int len;
                while ((len = reader.read(buf)) > 0) {
                    os.write(buf, 0, len);
                }
            }
        } // writer
        catch (IOException ignore) {
            LOG.debug("Detect Base64 error", ignore);
            // delete tmp file
            deleteTmpFile(base64Encoded.toAbsolutePath().toString());
            return null;
        }

        return base64Encoded;
    }

    public void getResourceFromURI(URI signedResource, Path localPath) throws IOException {
        try {
            // Attenzione, se al posto dell'uri viene utilizzata una stringa ci possono essere problemi di conversione
            // dei
            // caratteri
            Flux<DataBuffer> dataBuffer = WebClient.create().get().uri(signedResource).retrieve()
                    .bodyToFlux(DataBuffer.class);
            // scarica sul local path provando 5 volte aspettando almeno 3 secondi tra un prova e l'altra
            DataBufferUtils.write(dataBuffer, localPath).timeout(Duration.ofSeconds(webClientTimeout))
                    .retryWhen(Retry.backoff(webClientBackoff, Duration.ofSeconds(webClientBackoffTime))).share()
                    .block();
        } catch (Exception ex) {
            throw new IOException("Impossibile recuperare il documento da URI", ex);
        }
    }
}
