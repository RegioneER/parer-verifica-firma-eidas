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

package it.eng.parer.eidas.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.ResourceUtils;

import it.eng.parer.eidas.model.EidasDataToValidateMetadata;
import it.eng.parer.eidas.model.EidasWSReportsDTOTree;
import it.eng.parer.eidas.util.VerificaFirmaDocMockUtil;
import it.eng.parer.eidas.web.VerificaFirmaEidasApplication;

@SpringBootTest(classes = VerificaFirmaEidasApplication.class, properties = { "logging.level.root=INFO",
        "logging.level.it.eng.parer=INFO", "spring.datasource.url=jdbc:h2:mem:eidasdb-test;DB_CLOSE_DELAY=-1" })
class CustomRemoteDocumentValidationTest {

    /**
     * Note: @MockBean non funzionante (da verificare -> non è quindi possibile invocare il servizio REST in quanto gli
     * spring non risolve correttamente le implementazioni delle interaccie @Service / @Autowired)
     */
    // @Autowired
    // private MockMvc mvc;
    @Autowired
    ICustomRemoteDocumentValidation service;

    @Test
    void testPADESNow() throws Exception {

        // data
        InputStream fileWithSignature = ResourceUtils.getURL("classpath:PADES/PADES_BES.PDF").openStream();
        // DataToValidateDTO Mock
        EidasDataToValidateMetadata dto = VerificaFirmaDocMockUtil.createMockDto(fileWithSignature);
        EidasWSReportsDTOTree result = service.validateSignature(dto, null);
        validateSignaturesCount(result, 3);

    }

    @Test
    void testXADESNow() throws Exception {

        // data
        InputStream fileOriginal = ResourceUtils.getURL("classpath:ORIGINAL/XASES-BASEB_ORIGINAL.xml").openStream();
        InputStream fileWithSignature = ResourceUtils.getURL("classpath:XADES/XADES-BASEB.xml").openStream();
        // DataToValidateDTO Mock
        EidasDataToValidateMetadata dto = VerificaFirmaDocMockUtil.createMockDto(fileOriginal, fileWithSignature);
        EidasWSReportsDTOTree result = service.validateSignature(dto, null);
        validateSignaturesCount(result, 1);

    }

    @Test
    void testCADESNow() throws Exception {

        // data
        InputStream fileWithSignature = ResourceUtils.getURL("classpath:CADES/CADES.p7m").openStream();
        // DataToValidateDTO Mock
        EidasDataToValidateMetadata dto = VerificaFirmaDocMockUtil.createMockDto(fileWithSignature);
        EidasWSReportsDTOTree result = service.validateSignature(dto, null);
        validateSignaturesCount(result, 1);

    }

    @Test
    void testASICNow() throws Exception {

        // data
        InputStream fileWithSignature = ResourceUtils.getURL("classpath:ASIC/ASIC.asice").openStream();
        // DataToValidateDTO Mock
        EidasDataToValidateMetadata dto = VerificaFirmaDocMockUtil.createMockDto(fileWithSignature);
        EidasWSReportsDTOTree result = service.validateSignature(dto, null);
        validateSignaturesCount(result, 1);
    }

    @Test
    void testCADESTNow() throws Exception {

        // data
        InputStream fileWithSignature = ResourceUtils.getURL("classpath:CADES/CADEST.pdf.p7m").openStream();
        // DataToValidateDTO Mock
        EidasDataToValidateMetadata dto = VerificaFirmaDocMockUtil.createMockDto(fileWithSignature);
        EidasWSReportsDTOTree result = service.validateSignature(dto, null);
        validateSignaturesCount(result, 1);
        validateChildrenCount(result, 1);
    }

    /**
     * Questo test attualmente non può funzionare perché viene restituita un'eccezione in caso di documento senza firme.
     * Forse potrebbe essere utile pensare di restituire un report con almeno il mime type calcolato?
     *
     * @throws java.io.IOException
     *             in caso di file non trovato
     * @throws java.net.URISyntaxException
     */
    @Test
    @Disabled(value = "Questo test attualmente non può funzionare perché viene restituita un'eccezione in caso di documento senza firme")
    void testMimeTypeOffice() throws IOException, URISyntaxException {
        InputStream fileWithSignature = ResourceUtils.getURL("classpath:NOT_SIGNED/office_non_firmato").openStream();
        EidasDataToValidateMetadata dto = VerificaFirmaDocMockUtil.createMockDto(fileWithSignature);
        EidasWSReportsDTOTree result = service.validateSignature(dto, null);

        String expetedMimeType = "application/msword";
        String actualMimeType = result.getMimeType();
        assertEquals(expetedMimeType, actualMimeType);

    }

    private void validateSignaturesCount(EidasWSReportsDTOTree result, int countSigns) {
        assertNotNull(result.getReport());
        assertEquals(countSigns, result.getReport().getSimpleReport().getSignaturesCount());
    }

    private void validateChildrenCount(EidasWSReportsDTOTree result, int countChilds) {
        assertEquals(countChilds, result.getChildren().size());
    }

}
