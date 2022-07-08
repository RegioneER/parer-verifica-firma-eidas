package it.eng.parer.eidas.web.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.ResourceUtils;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import it.eng.parer.eidas.core.helper.EidasHelper;
import it.eng.parer.eidas.web.VerificaFirmaEidasApplication;

@SpringBootTest(classes = VerificaFirmaEidasApplication.class, properties = { "logging.level.root=INFO",
        "logging.level.it.eng.parer=INFO", "spring.datasource.url=jdbc:h2:mem:eidasdb-test;DB_CLOSE_DELAY=-1" })
public class EidasHelperTest {

    @Autowired
    private EidasHelper eidasHelper;

    @Test
    public void testMimeTypeOffice() throws IOException {
        String fileWithoutSignature = ResourceUtils.getURL("classpath:NOT_SIGNED/office_non_firmato").getFile();
        DSSDocument input = new FileDocument(fileWithoutSignature);

        String expetedMimeType = "application/msword";
        String actualMimeType = eidasHelper.detectMimeType(input);
        assertEquals(expetedMimeType, actualMimeType);
    }

}
