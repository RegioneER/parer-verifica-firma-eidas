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

package it.eng.parer.eidas.web.rest;

import static it.eng.parer.eidas.web.util.EndPointCostants.URL_REPORT_VERIFICA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ResourceUtils;

import eu.europa.esig.dss.ws.validation.dto.WSReportsDTO;
import it.eng.parer.eidas.core.util.Constants;
import it.eng.parer.eidas.model.EidasWSReportsDTOTree;
import it.eng.parer.eidas.model.exception.EidasParerException;
import it.eng.parer.eidas.model.exception.ParerError.ErrorCode;
import it.eng.parer.eidas.util.RestTemplateErrorHandler;
import it.eng.parer.eidas.web.converter.CustomJaxb2RootElementHttpMessageConverter;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:eidasdb-test;DB_CLOSE_DELAY=-1", "logging.level.root=INFO",
        "logging.level.it.eng.parer.eidas=INFO", "logging.level.org.springframework.web.client.RestTemplate=DEBUG" })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VerificaFirmaWsTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeAll
    public void init() throws IOException {
        restTemplate.getRestTemplate().setErrorHandler(new RestTemplateErrorHandler());
        // add custcom converter
        restTemplate.getRestTemplate().getMessageConverters().add(new CustomJaxb2RootElementHttpMessageConverter());
    }

    @Test
    void testVerificaFirmaWsPadesBesMultipart() throws IOException, URISyntaxException {

        // mock
        InputStream fileWithSignature = ResourceUtils.getURL("classpath:PADES/PADES_BES.PDF").openStream();
        Path pathSig = Files.createTempFile("sig", ".tmp");
        Files.copy(fileWithSignature, pathSig, StandardCopyOption.REPLACE_EXISTING);
        //
        HttpEntity<MultiValueMap<String, Object>> entity = prepareMultipartReq(pathSig, Optional.empty());

        EidasWSReportsDTOTree result = restTemplate.postForObject(URL_REPORT_VERIFICA, entity,
                EidasWSReportsDTOTree.class);

        assertNumeroDiFirme(result.getReport(), 3);

    }

    @Test
    void testVerificaFirmaWsXADESBesMultipart() throws Exception {

        // mock
        InputStream fileWithSignature = ResourceUtils.getURL("classpath:XADES/XADES-BASEB.xml").openStream();
        Path pathSig = Files.createTempFile("sig", ".tmp");
        Files.copy(fileWithSignature, pathSig, StandardCopyOption.REPLACE_EXISTING);
        //
        InputStream fileOriginal = ResourceUtils.getURL("classpath:ORIGINAL/XASES-BASEB_ORIGINAL.xml").openStream();
        Path pathOrig = Files.createTempFile("orig", ".tmp");
        Files.copy(fileOriginal, pathOrig, StandardCopyOption.REPLACE_EXISTING);
        //
        HttpEntity<MultiValueMap<String, Object>> entity = prepareMultipartReq(pathSig, Optional.of(pathOrig));

        EidasWSReportsDTOTree result = restTemplate.postForObject(URL_REPORT_VERIFICA, entity,
                EidasWSReportsDTOTree.class);

        assertNumeroDiFirme(result.getReport(), 1);

    }

    @Test
    void testVerificaFirmaWsMultipartWith500Error() throws Exception {

        // mock
        InputStream fileWithSignature = ResourceUtils.getURL("classpath:NOT_VALID/m7m_ok.pdf.m7m").openStream();
        Path pathSig = Files.createTempFile("sig", ".tmp");
        Files.copy(fileWithSignature, pathSig, StandardCopyOption.REPLACE_EXISTING);
        //
        HttpEntity<MultiValueMap<String, Object>> entity = prepareMultipartReq(pathSig, Optional.empty());

        assertThrows(EidasParerException.class,
                () -> restTemplate.postForObject(URL_REPORT_VERIFICA, entity, EidasWSReportsDTOTree.class),
                "Should fail throwing EidasParerException");

    }

    @Test
    void testVerificaFirmaWsMultipartWith400Error() throws Exception {
        //
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(List.of(MediaType.APPLICATION_XML));

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("wrong", null);
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

        EidasParerException ex = assertThrows(EidasParerException.class,
                () -> restTemplate.postForObject(URL_REPORT_VERIFICA, entity, EidasWSReportsDTOTree.class),
                "Should fail throwing EidasParerException");

        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getCode());
        assertEquals(Constants.STD_MSG_VALIDATION_ERROR, ex.getMessage());

    }

    private HttpEntity<MultiValueMap<String, Object>> prepareMultipartReq(Path pathSig, Optional<Path> pathOrig) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(List.of(MediaType.APPLICATION_XML));

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("signedFile", new FileSystemResource(pathSig));
        //
        if (pathOrig.isPresent()) {
            body.add("originalFiles", new FileSystemResource(pathOrig.get()));
        }
        return new HttpEntity<>(body, headers);
    }

    private void assertNumeroDiFirme(WSReportsDTO result, int firmeAttese) {
        assertNotNull(result.getSimpleReport());
        assertEquals(firmeAttese, result.getSimpleReport().getSignaturesCount());
    }

}
