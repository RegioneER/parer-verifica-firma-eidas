package it.eng.parer.eidas.client.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.RestTemplate;

import eu.europa.esig.dss.ws.validation.dto.WSReportsDTO;
import it.eng.parer.eidas.client.util.RestTemplateErrorHandler;
import it.eng.parer.eidas.client.util.VerificaFirmaDocMockUtil;
import it.eng.parer.eidas.model.DataToValidateDTOExt;
import it.eng.parer.eidas.model.EidasMetadataToValidate;
import it.eng.parer.eidas.model.EidasWSReportsDTOTree;
import it.eng.parer.eidas.model.exception.EidasParerException;

@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled
public class RestTemplateVerificaFirmaTest {

    private static final Logger LOG = LoggerFactory.getLogger(RestTemplateVerificaFirmaTest.class);

    final String uri = "http://localhost:8090/v1/report-verifica";
    final String uriv2 = "http://localhost:8090/v2/report-verifica";

    RestTemplate restTemplate;

    @BeforeAll
    public void init() {
        restTemplate = new RestTemplate();
    }

    @Test
    @Order(1)
    public void testPADES() throws Exception {
        // data
        InputStream fileWithSignature = ResourceUtils.getURL("classpath:PADES/PADES_3FIRME.pdf").openStream();
        // DataToValidateDTO Mock
        DataToValidateDTOExt dto = VerificaFirmaDocMockUtil.createMockDto(fileWithSignature);
        callValidateFirmeOK(dto, 3);
    }

    @Test
    @Order(2)
    public void testMultipartPADES() throws Exception {
        // data
        InputStream fileWithSignature = ResourceUtils.getURL("classpath:PADES/PADES_3FIRME.pdf").openStream();
        Path path = Files.createTempFile("pades", ".tmp");
        Files.copy(fileWithSignature, path, StandardCopyOption.REPLACE_EXISTING);
        // DataToValidateDTO Mock
        EidasMetadataToValidate metadata = VerificaFirmaDocMockUtil.createMockMetadata();
        callValidateMultipartFirmeOK(metadata, path.toFile(), null, 3);
        path.toFile().delete();
    }

    @Test
    @Order(3)
    public void testXADES() throws Exception {
        // data
        InputStream fileOriginal = ResourceUtils.getURL("classpath:ORIGINAL/XASES-BASEB_ORIGINAL.xml").openStream();
        InputStream fileWithSignature = ResourceUtils.getURL("classpath:XADES/XADES-BASEB.xml").openStream();
        // DataToValidateDTO Mock
        DataToValidateDTOExt dto = VerificaFirmaDocMockUtil.createMockDto(fileOriginal, fileWithSignature);
        callValidateFirmeOK(dto, 1);

    }

    @Test
    @Order(4)
    public void testMultipartXADES() throws Exception {
        // data
        InputStream fileOriginal = ResourceUtils.getURL("classpath:ORIGINAL/XASES-BASEB_ORIGINAL.xml").openStream();
        Path pathOriginal = Files.createTempFile("orig", ".tmp");
        Files.copy(fileOriginal, pathOriginal, StandardCopyOption.REPLACE_EXISTING);
        InputStream fileWithSignature = ResourceUtils.getURL("classpath:XADES/XADES-BASEB.xml").openStream();
        Path pathSig = Files.createTempFile("sig", ".tmp");
        Files.copy(fileWithSignature, pathSig, StandardCopyOption.REPLACE_EXISTING);
        // DataToValidateDTO Mock
        EidasMetadataToValidate metadata = VerificaFirmaDocMockUtil.createMockMetadata();
        callValidateMultipartFirmeOK(metadata, pathSig.toFile(), pathOriginal.toFile(), 1);

    }

    @Test
    @Order(5)
    public void testCADES() throws Exception {
        InputStream fileWithSignature = ResourceUtils.getURL("classpath:CADES/CADES.p7m").openStream();
        // DataToValidateDTO Mock
        DataToValidateDTOExt dto = VerificaFirmaDocMockUtil.createMockDto(fileWithSignature);
        callValidateFirmeOK(dto, 1);

    }

    @Test
    @Order(6)
    public void testASIC() throws Exception {
        InputStream fileWithSignature = ResourceUtils.getURL("classpath:ASIC/ASIC.asice").openStream();
        // DataToValidateDTO Mock
        DataToValidateDTOExt dto = VerificaFirmaDocMockUtil.createMockDto(fileWithSignature);
        callValidateFirmeOK(dto, 1);
    }

    // https://www.baeldung.com/spring-rest-template-error-handling
    @Test
    @Order(7)
    public void givenEidasParerException() throws IOException {
        InputStream fileWithSignature = ResourceUtils.getURL("classpath:NOT_VALID/m7m_ok.pdf.m7m").openStream();

        DataToValidateDTOExt dto = VerificaFirmaDocMockUtil.createMockDto(fileWithSignature);
        restTemplate.setErrorHandler(new RestTemplateErrorHandler());

        assertThrows(EidasParerException.class, () -> {
            restTemplate.postForObject(uri, dto, EidasWSReportsDTOTree.class);
        });
    }

    private void callValidateFirmeOK(DataToValidateDTOExt dto, int countSigns) throws IOException, JAXBException {
        EidasWSReportsDTOTree resp = restTemplate.postForObject(uri, dto, EidasWSReportsDTOTree.class);
        assertNumeroDiFirmeOK(resp.getReport(), countSigns);
    }

    private void callValidateMultipartFirmeOK(EidasMetadataToValidate metadata, File signedDocument,
            File originalDocument, int countSigns) throws IOException, JAXBException {
        // header
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        //
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("metadata", metadata);
        body.add("signedFile", new FileSystemResource(signedDocument));
        //
        if (originalDocument != null) {
            body.add("originalFiles", new FileSystemResource(originalDocument));
        }

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<EidasWSReportsDTOTree> resp = restTemplate.postForEntity(uriv2, requestEntity,
                EidasWSReportsDTOTree.class);
        assertNumeroDiFirmeOK(resp.getBody().getReport(), countSigns);
    }

    private void assertNumeroDiFirmeOK(WSReportsDTO result, int firmeAttese) {
        assertNotNull(result.getSimpleReport().getSignaturesCount());
        assertEquals(firmeAttese, result.getSimpleReport().getSignaturesCount());
    }

    @Test
    @Order(8)
    public void testReportNoBase64() throws Exception {
        // data
        InputStream fileWithSignature = ResourceUtils.getURL("classpath:PADES/PADES_3FIRME.pdf").openStream();
        // DataToValidateDTO Mock
        DataToValidateDTOExt dto = VerificaFirmaDocMockUtil.createMockDto(fileWithSignature);
        EidasWSReportsDTOTree resp = restTemplate.postForObject(uri, dto, EidasWSReportsDTOTree.class);
        // xml original
        createTempFile(marshalResponse(resp));
        removeRawsOnReports(resp);
        // xml skinny
        createTempFile(marshalResponse(resp));
        assertNotNull(resp);
    }

    // tree no binaries
    private static void removeRawsOnReports(EidasWSReportsDTOTree dto) {
        // root element
        if (!dto.isParent()) {
            removeraw(dto);
        }
        // per ogni figlio (signed)
        for (EidasWSReportsDTOTree child : dto.getChildren()) {
            if (child.isUnsigned() /* ultimo livello documento non firmato */) {
                break;
            }
            removeraw(child);
            if (child.isParent() && child.getChildren() != null && !child.getChildren().isEmpty()) {
                removeRawsOnReports(child);
            }
        }
    }

    private static void removeraw(EidasWSReportsDTOTree dto) {
        // used certificate
        dto.getReport().getDiagnosticData().getUsedCertificates().forEach(usedcert -> {
            // set null
            usedcert.setBase64Encoded(null);
            // set null on revocation
            usedcert.getRevocations().forEach(r -> r.getRevocation().setBase64Encoded(null));
            // set null on chain
            usedcert.getCertificateChain().forEach(c -> c.getCertificate().setBase64Encoded(null));
            // set null on signincertificate
            if (usedcert.getSigningCertificate() != null) {
                usedcert.getSigningCertificate().getCertificate().setBase64Encoded(null);
            }
        });
        // used revocations
        dto.getReport().getDiagnosticData().getUsedRevocations().forEach(usedrev -> {
            // set null
            usedrev.setBase64Encoded(null);
            // set null signcert
            if (usedrev.getSigningCertificate() != null) {
                usedrev.getSigningCertificate().getCertificate().setBase64Encoded(null);
            }
            // set null on chain
            usedrev.getCertificateChain().forEach(r -> r.getCertificate().setBase64Encoded(null));
        });
        // used timestamp
        dto.getReport().getDiagnosticData().getUsedTimestamps().forEach(usedts -> {
            // set null
            usedts.setBase64Encoded(null);
            // set null signigncertificate
            if (usedts.getSigningCertificate() != null) {
                usedts.getSigningCertificate().getCertificate().setBase64Encoded(null);
            }
            // set null on chain
            usedts.getCertificateChain().forEach(c -> c.getCertificate().setBase64Encoded(null));
        });

        // used signatures
        dto.getReport().getDiagnosticData().getSignatures().forEach(useds -> {
            // all founded obj
            // set null on timestamps
            useds.getFoundTimestamps().forEach(t -> t.getTimestamp().setBase64Encoded(null));
            // set null on related cert
            useds.getFoundCertificates().getRelatedCertificates()
                    .forEach(c -> c.getCertificate().setBase64Encoded(null));
            // set null on related revocation
            useds.getFoundRevocations().getRelatedRevocations().forEach(r -> r.getRevocation().setBase64Encoded(null));
            // set null on chain
            useds.getCertificateChain().forEach(c -> c.getCertificate().setBase64Encoded(null));
        });
    }

    private String marshalResponse(Object response) throws JAXBException {
        StringWriter tmpStringWriter = new StringWriter();
        try {
            JAXBContext jc = JAXBContext.newInstance(response.getClass());
            Marshaller tmpGenericMarshaller = jc.createMarshaller();
            tmpGenericMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            JAXBIntrospector introspector = jc.createJAXBIntrospector();
            if (null == introspector.getElementName(response)) {
                JAXBElement jaxbElement = new JAXBElement(new QName("ROOT"), response.getClass(), response);
                tmpGenericMarshaller.marshal(jaxbElement, tmpStringWriter);
            } else {
                tmpGenericMarshaller.marshal(response, tmpStringWriter);
            }

            // tmpGenericMarshaller.marshal(response, tmpStringWriter);
        } catch (JAXBException e) {
            throw e;
        }
        return tmpStringWriter.toString();
    }

    private static String createTempFile(String xml) throws IOException {
        // Since Java 1.7 Files and Path API simplify operations on files
        Path path = Files.createTempFile("report", ".xml");
        File file = path.toFile();
        // writing sample data
        Files.write(path, xml.getBytes(StandardCharsets.UTF_8));
        // This tells JVM to delete the file on JVM exit.
        // Useful for temporary files in tests.
        // file.deleteOnExit();
        return file.getAbsolutePath();
    }

}
