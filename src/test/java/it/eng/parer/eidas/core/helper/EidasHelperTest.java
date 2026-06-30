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

package it.eng.parer.eidas.core.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.util.ResourceUtils;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import it.eng.parer.eidas.core.bean.CommonsDataHttpClient;
import it.eng.parer.eidas.model.EidasDataToValidateMetadata;

/**
 * <p>
 * EidasHelperTest class.
 * </p>
 *
 * @author stefano
 *
 * @version $Id: $Id
 *
 * @since 1.10.1
 */
@ExtendWith(MockitoExtension.class)
class EidasHelperTest {

    @Mock
    private Environment env;

    @Mock
    private BuildProperties buildProperties;

    @Mock
    private CommonsDataHttpClient dataHttpClient;

    @InjectMocks
    private EidasHelper eidasHelper;

    /**
     * <p>
     * testMimeTypeOffice.
     * </p>
     *
     * @throws java.io.IOException if any.
     */
    @Test
    void testMimeTypeOffice() throws IOException {
        String fileWithoutSignature = ResourceUtils
                .getURL("classpath:NOT_SIGNED/office_non_firmato").getFile();
        DSSDocument input = new FileDocument(fileWithoutSignature);

        String expetedMimeType = "application/msword";
        String actualMimeType = eidasHelper.detectMimeType(input);
        assertEquals(expetedMimeType, actualMimeType);
    }

    @Test
    void testMimeTypePdf() throws IOException {
        String file = ResourceUtils.getURL("classpath:PADES/PADES_BES.PDF").getFile();
        DSSDocument input = new FileDocument(file);

        assertEquals("application/pdf", eidasHelper.detectMimeType(input));
    }

    @Test
    void testMimeTypeXml() throws IOException {
        String file = ResourceUtils.getURL("classpath:XADES/XADES-BASEB.xml").getFile();
        DSSDocument input = new FileDocument(file);

        assertEquals("application/xml", eidasHelper.detectMimeType(input));
    }

    @Test
    void testMimeTypeP7m() throws IOException {
        String file = ResourceUtils.getURL("classpath:CADES/CADES.p7m").getFile();
        DSSDocument input = new FileDocument(file);

        assertNotNull(eidasHelper.detectMimeType(input));
    }

    @Test
    void testMimeTypeDocumentoVuoto() {
        DSSDocument input = new InMemoryDocument(new byte[0]);

        // Tika restituisce application/octet-stream per stream vuoti, mai null
        assertEquals("application/octet-stream", eidasHelper.detectMimeType(input));
    }

    // --- verifyAndExtractFileContent ---

    @Test
    void verifyAndExtractFileContent_conASCIIArmor_restituiscePathNonNull() throws Exception {
        // Contenuto ASCII-Armor: la riga inizia con "-----BEGIN "
        String base64Content = java.util.Base64.getEncoder()
                .encodeToString("contenuto di test".getBytes(StandardCharsets.UTF_8));
        String asciiArmor = "-----BEGIN DOCUMENT-----\n" + base64Content
                + "\n-----END DOCUMENT-----\n";
        byte[] input = asciiArmor.getBytes(StandardCharsets.US_ASCII);

        Path result = eidasHelper.verifyAndExtractFileContent(new EidasDataToValidateMetadata(),
                "test-ascii-", input);

        assertNotNull(result);
        assertTrue(Files.exists(result));
        Files.deleteIfExists(result);
    }

    @Test
    void verifyAndExtractFileContent_conBase64_restituiscePathNonNull() throws Exception {
        // Contenuto base64 valido che non inizia con "-----BEGIN "
        byte[] originalContent = "contenuto originale da codificare"
                .getBytes(StandardCharsets.UTF_8);
        byte[] base64Input = java.util.Base64.getEncoder().encode(originalContent);

        Path result = eidasHelper.verifyAndExtractFileContent(new EidasDataToValidateMetadata(),
                "test-b64-", base64Input);

        assertNotNull(result);
        assertTrue(Files.exists(result));
        Files.deleteIfExists(result);
    }

    @Test
    void verifyAndExtractFileContent_conBinarioNonBase64_restituisceNull() throws Exception {
        // Byte binari non decodificabili come base64
        byte[] binary = new byte[] {
                (byte) 0xFF, (byte) 0xFE, (byte) 0xFD, (byte) 0x00, (byte) 0x01, (byte) 0xFF,
                (byte) 0xFE, (byte) 0xFD };

        Path result = eidasHelper.verifyAndExtractFileContent(new EidasDataToValidateMetadata(),
                "test-bin-", binary);

        assertNull(result);
    }

}
