package it.eng.parer.eidas.core.helper;

import static org.apache.tika.metadata.TikaMetadataKeys.RESOURCE_NAME_KEY;
import static it.eng.parer.eidas.core.util.Constants.TMP_FILE_SUFFIX;
import static it.eng.parer.eidas.core.util.Constants.DSS_VERSION;
import static it.eng.parer.eidas.core.util.Constants.BUILD_VERSION;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.MimeType;
import eu.europa.esig.dss.spi.DSSUtils;
import it.eng.parer.eidas.model.RemoteDocumentExt;
import it.eng.parer.eidas.model.exception.EidasParerException;
import it.eng.parer.eidas.model.exception.ParerError;

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

    public String buildversion() {
        return env.getProperty(BUILD_VERSION);
    }

    public String dssversion() {
        return buildProperties.get(DSS_VERSION);
    }

    public Path writeTmpFile(DSSDocument doc) {
        //
        Path tmpDoc = null;
        try {
            // tmp file (uuid as file name)
            tmpDoc = Files.createTempFile(UUID.randomUUID().toString(), TMP_FILE_SUFFIX, attr);
            Files.copy(doc.openStream(), tmpDoc, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new EidasParerException().withCode(ParerError.ErrorCode.IO_ERROR).withMessage(ex.getMessage());
        }
        return tmpDoc;
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
            return MimeType.XML.getMimeTypeString();
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
    public void deleteTmpDocExtFiles(RemoteDocumentExt signedDocumentExt, List<RemoteDocumentExt> originalDocumentsExt,
            RemoteDocumentExt policyExt) {
        // signed file
        deleteTmpFile(signedDocumentExt.getAbsolutePath());

        // original file
        if (originalDocumentsExt != null && !originalDocumentsExt.isEmpty()) {
            for (RemoteDocumentExt originalFileExt : originalDocumentsExt) {
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
     * 
     * 
     * @param prefix
     *            prefisso del file da utilizzare per la generazione del risultato
     * @param signedBytes
     *            array di byte con contenuto del file sottoposto a verifica
     * 
     * @return file risultato dell'elaborazione o null se non rientra nei due casi
     */
    public Path verifyAndExtractFileContent(String prefix, byte[] signedBytes) {

        // detect ASCII armor
        Path asciiarmor = detectASCIIArmor(prefix, signedBytes);
        if (asciiarmor != null) {
            return asciiarmor;
        }
        // detect Base64 encoded
        Path decodedBase64 = detectBase64Encoded(prefix, signedBytes);
        if (decodedBase64 != null) {
            return decodedBase64;
        }

        return null; // no detection
    }

    /**
     * Verifica contenuto path relativo al file su disco. 1. verifica se ASCII armor 2. verifica se base64 encoded
     * 
     * 
     * 
     * @param prefix
     *            prefisso del file da utilizzare per la generazione del risultato
     * @param signedFile
     *            path file sottoposto a verifica
     * 
     * @return file risultato dell'elaborazione o null se non rientra nei due casi
     */
    public Path verifyAndExtractFileContent(String prefix, Path signedFile) {

        // detect ASCII armor
        Path asciiarmor = detectASCIIArmor(prefix, signedFile);
        if (asciiarmor != null) {
            return asciiarmor;
        }
        // detect Base64 encoded
        Path decodedBase64 = detectBase64Encoded(prefix, signedFile);
        if (decodedBase64 != null) {
            return decodedBase64;
        }

        return null; // no detection
    }

    /**
     * Verifica se i byte[] relativi al file (passati via json) fanno riferimento ad un ASCII-Armor, in quel caso si
     * ottiene come risultato il Path di un nuovo file contenente la decodifica in Base64 del file originale.
     *
     * @param prefix
     *            prefisso file (tipicamente viene utilizzato uuid relativo alla sessione)
     * @param originalFileName
     *            nome del file originale trasmesso
     * @param signedBytes
     *            documento DSS in byte[]
     * 
     * @return Path del file
     */
    private Path detectASCIIArmor(String prefix, byte[] signedBytes) {
        try (InputStream is = new ByteArrayInputStream(signedBytes)) {
            return detectASCIIArmor(prefix, is);
        } catch (IOException e) {
            LOG.error("Detect ASCII Armor as ByteArrayInputStream", e);
            throw new EidasParerException().withCode(ParerError.ErrorCode.IO_ERROR).withMessage(LOG_BASE64_ERROR);
        }
    }

    /**
     *
     * Verifica se il file passato è un ASCII-Armor, in quel caso si ottiene come risultato il Path di un nuovo file
     * contenente la decodifica in Base64 del file originale.
     *
     * @param prefix
     *            prefisso file (tipicamente viene utilizzato uuid relativo alla sessione)
     * @param originalFileName
     *            nome del file originale trasmesso
     * @param signedFile
     *            documento DSS come {@link File}
     * 
     * @return Path del file
     */
    private Path detectASCIIArmor(String prefix, Path signedFile) {
        try (InputStream is = new FileInputStream(signedFile.toFile())) {
            return detectASCIIArmor(prefix, is);
        } catch (IOException e) {
            LOG.error("Detect ASCII Armor as FileInputStream", e);
            throw new EidasParerException().withCode(ParerError.ErrorCode.IO_ERROR).withMessage(LOG_BASE64_ERROR);
        }
    }

    private Path detectASCIIArmor(String prefix, InputStream is) {
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
                LOG.error("Detect ASCII Armor error", e);
                throw new EidasParerException().withCode(ParerError.ErrorCode.IO_ERROR).withMessage("Errore generico");
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

    private Path detectBase64Encoded(String prefix, byte[] signedBytes) {
        try (InputStream is = new ByteArrayInputStream(signedBytes)) {
            return readBase64EncodedFile(prefix, is);
        } catch (IOException e) {
            LOG.error("Detect ASCII Armor as ByteArrayInputStream", e);
            throw new EidasParerException().withCode(ParerError.ErrorCode.IO_ERROR).withMessage(LOG_BASE64_ERROR);
        }
    }

    private Path detectBase64Encoded(String prefix, Path signedFile) {
        try (InputStream is = new FileInputStream(signedFile.toFile())) {
            return readBase64EncodedFile(prefix, is);
        } catch (IOException e) {
            LOG.error("Detect Base64 as FileInputStream", e);
            throw new EidasParerException().withCode(ParerError.ErrorCode.IO_ERROR)
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

}
