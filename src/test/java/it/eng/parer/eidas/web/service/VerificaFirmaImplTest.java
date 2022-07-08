package it.eng.parer.eidas.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.ResourceUtils;

import eu.europa.esig.dss.ws.validation.dto.WSReportsDTO;
import it.eng.parer.eidas.client.util.VerificaFirmaDocMockUtil;
import it.eng.parer.eidas.core.service.IVerificaFirma;
import it.eng.parer.eidas.model.DataToValidateDTOExt;
import it.eng.parer.eidas.model.EidasWSReportsDTOTree;
import it.eng.parer.eidas.web.VerificaFirmaEidasApplication;

@SpringBootTest(classes = VerificaFirmaEidasApplication.class, properties = { "logging.level.root=INFO",
        "logging.level.it.eng.parer=INFO", "spring.datasource.url=jdbc:h2:mem:eidasdb-test;DB_CLOSE_DELAY=-1" })
public class VerificaFirmaImplTest {

    /**
     * Note: @MockBean non funzionante (da verificare -> non è quindi possibile invocare il servizio REST in quanto gli
     * spring non risolve correttamente le implementazioni delle interaccie @Service / @Autowired)
     */
    // @Autowired
    // private MockMvc mvc;
    @Autowired
    private IVerificaFirma service;

    /**
     * OLD CODE // ObjectMapper mapper = new ObjectMapper(); // String input = mapper.writeValueAsString(dto); //
     * Files.write(Paths.get("/tmp/input-eidas.json"), input.getBytes());
     *
     */
    @Test
    public void testPADESNow() throws Exception {

        // data
        InputStream fileWithSignature = ResourceUtils.getURL("classpath:PADES/PADES_BES.PDF").openStream();
        // DataToValidateDTO Mock
        DataToValidateDTOExt dto = VerificaFirmaDocMockUtil.createMockDto(fileWithSignature);
        callValidateFirmeCount(dto, 3);

    }

    @Test
    public void testXADESNow() throws Exception {

        // data
        InputStream fileOriginal = ResourceUtils.getURL("classpath:ORIGINAL/XASES-BASEB_ORIGINAL.xml").openStream();
        InputStream fileWithSignature = ResourceUtils.getURL("classpath:XADES/XADES-BASEB.xml").openStream();
        // DataToValidateDTO Mock
        DataToValidateDTOExt dto = VerificaFirmaDocMockUtil.createMockDto(fileOriginal, fileWithSignature);
        callValidateFirmeCount(dto, 1);

    }

    @Test
    public void testCADESNow() throws Exception {

        // data
        InputStream fileWithSignature = ResourceUtils.getURL("classpath:CADES/CADES.p7m").openStream();
        // DataToValidateDTO Mock
        DataToValidateDTOExt dto = VerificaFirmaDocMockUtil.createMockDto(fileWithSignature);
        callValidateFirmeCount(dto, 1);

    }

    @Test
    public void testASICNow() throws Exception {

        // data
        InputStream fileWithSignature = ResourceUtils.getURL("classpath:ASIC/ASIC.asice").openStream();
        // DataToValidateDTO Mock
        DataToValidateDTOExt dto = VerificaFirmaDocMockUtil.createMockDto(fileWithSignature);
        callValidateFirmeCount(dto, 1);

    }

    /**
     * Questo test attualmente non può funzionare perché viene restituita un'eccezione in caso di documento senza firme.
     * Forse potrebbe essere utile pensare di restituire un report con almeno il mime type calcolato?
     * 
     * @throws IOException
     *             in caso di file non trovato
     */
    @Test
    @Disabled
    public void testMimeTypeOffice() throws IOException {
        InputStream fileWithSignature = ResourceUtils.getURL("classpath:NOT_SIGNED/office_non_firmato").openStream();
        DataToValidateDTOExt dto = VerificaFirmaDocMockUtil.createMockDto(fileWithSignature);
        EidasWSReportsDTOTree result = service.validateSignature(dto);

        String expetedMimeType = "application/msword";
        String actualMimeType = result.getMimeType();
        assertEquals(expetedMimeType, actualMimeType);

    }

    private void callValidateFirmeCount(DataToValidateDTOExt dto, int countSigns) {
        // now (date)
        EidasWSReportsDTOTree result = service.validateSignature(dto);
        assertNumeroDiFirme(result.getReport(), countSigns);
    }

    private void assertNumeroDiFirme(WSReportsDTO result, int firmeAttese) {
        assertNotNull(result.getSimpleReport().getSignaturesCount());
        assertEquals(firmeAttese, result.getSimpleReport().getSignaturesCount());
    }
}
