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

import static org.apache.xmlgraphics.util.MimeConstants.MIME_PDF;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.Result;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.FopFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import eu.europa.esig.dss.DSSXmlErrorListener;
import eu.europa.esig.dss.DomUtils;
import eu.europa.esig.dss.detailedreport.jaxb.XmlDetailedReport;
import eu.europa.esig.dss.diagnostic.jaxb.XmlDiagnosticData;
import eu.europa.esig.dss.simplereport.jaxb.XmlSimpleReport;
import eu.europa.esig.dss.utils.Utils;
import it.eng.parer.eidas.web.bean.VerificaFirmaResultBean;
import jakarta.annotation.PostConstruct;

@Component
public class ReportRenderingHelper {

    private static final Logger LOG = LoggerFactory.getLogger(ReportRenderingHelper.class);

    private static final String NAV_NEXT = "next";

    private Marshaller simpleReportMarshaller;
    private Marshaller detailedReportMarshaller;
    private Marshaller diagnosticDataReportMarshaller;

    private Templates templateSimpleReport;
    private Templates templateDetailedReport;

    private FopFactory fopFactory;
    private FOUserAgent foUserAgent;
    private Templates templateSimpleReportPdf;
    private Templates templateDetailedReportPdf;

    @PostConstruct
    public void init() throws JAXBException, TransformerConfigurationException, IOException {
        TransformerFactory transformerFactory = DomUtils.getSecureTransformerFactory();

        try (InputStream is = ReportRenderingHelper.class
                .getResourceAsStream("/xslt/html/simple-report-bootstrap4.xslt")) {
            templateSimpleReport = transformerFactory.newTemplates(new StreamSource(is));
        }

        try (InputStream is = ReportRenderingHelper.class
                .getResourceAsStream("/xslt/html/detailed-report-bootstrap4.xslt")) {
            templateDetailedReport = transformerFactory.newTemplates(new StreamSource(is));
        }

        JAXBContext simpleJaxbContext = JAXBContext.newInstance(XmlSimpleReport.class);
        simpleReportMarshaller = simpleJaxbContext.createMarshaller();

        JAXBContext detailedJaxbContext = JAXBContext.newInstance(XmlDetailedReport.class);
        detailedReportMarshaller = detailedJaxbContext.createMarshaller();

        JAXBContext diagnosticDataJaxbContext = JAXBContext.newInstance(XmlDiagnosticData.class);
        diagnosticDataReportMarshaller = diagnosticDataJaxbContext.createMarshaller();
        diagnosticDataReportMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        FopFactoryBuilder builder = new FopFactoryBuilder(new File(".").toURI());
        builder.setAccessibility(true);

        fopFactory = builder.build();

        foUserAgent = fopFactory.newFOUserAgent();
        foUserAgent.setCreator("Parer DSS Webapp");
        foUserAgent.setAccessibility(true);

        InputStream simpleIS = ReportRenderingHelper.class.getResourceAsStream("/xslt/pdf/simple-report.xslt");
        templateSimpleReportPdf = transformerFactory.newTemplates(new StreamSource(simpleIS));
        Utils.closeQuietly(simpleIS);

        InputStream detailedIS = ReportRenderingHelper.class.getResourceAsStream("/xslt/pdf/detailed-report.xslt");
        templateDetailedReportPdf = transformerFactory.newTemplates(new StreamSource(detailedIS));
        Utils.closeQuietly(detailedIS);
    }

    /**
     * Scrive nell'outputStream il PDF del simple report
     *
     * @param simpleReport
     *            simpleReport in xml
     * @param os
     *            outputstream
     * 
     * @throws FOPException
     *             errore su trasformazione lib FOP
     * @throws TransformerException
     *             errore generico sulla trasformazione applicando xslt
     */
    public void generateSimpleReportPdf(String simpleReport, OutputStream os)
            throws FOPException, TransformerException {
        Fop fop = fopFactory.newFop(MIME_PDF, foUserAgent, os);
        Result res = new SAXResult(fop.getDefaultHandler());
        Transformer transformer = templateSimpleReportPdf.newTransformer();
        transformer.setErrorListener(new DSSXmlErrorListener());
        transformer.transform(new StreamSource(new StringReader(simpleReport)), res);
    }

    /**
     * Scrive nell'outputStream il PDF del detailed report
     *
     * @param detailedReport
     *            detailedReport in xml
     * @param os
     *            outputstream su cui scriverlo
     * 
     * @throws FOPException
     *             errore su trasformazione lib FOP
     * @throws TransformerException
     *             errore generico sulla trasformazione applicando xslt
     */
    public void generateDetailedReportPdf(String detailedReport, OutputStream os)
            throws FOPException, TransformerException {
        Fop fop = fopFactory.newFop(MIME_PDF, foUserAgent, os);
        Result res = new SAXResult(fop.getDefaultHandler());
        Transformer transformer = templateDetailedReportPdf.newTransformer();
        transformer.setErrorListener(new DSSXmlErrorListener());
        transformer.transform(new StreamSource(new StringReader(detailedReport)), res);
    }

    /**
     * Produce l'html che descrive il simple report.
     *
     * @param report
     *            simple report della DSS
     * 
     * @return html sotto forma di stringa
     */
    public String generateSimpleReport(XmlSimpleReport report) {
        try {
            String xml = marshallSimpleReport(report);
            return transformSimpleReport(xml);
        } catch (Exception e) {
            LOG.error("Errore durante la creazione del simple report", e);
        }
        return "<div>Errore durante la generazione del report di base</div>";
    }

    /**
     * Produce l'html che descrive il detailed report.
     *
     * @param report
     *            detailed report della DSS
     * 
     * @return html sotto forma di stringa
     */
    public String generateDetailedReport(XmlDetailedReport report) {
        try {
            String xml = marshallDetailedReport(report);
            return transformDetailedReport(xml);
        } catch (Exception e) {
            LOG.error("Errore durante la creazione del detailed report", e);
        }
        return "<div>Errore durante la generazione del report dettagliato</div>";
    }

    /**
     * Produce l'xml che descrive il diagnostic Tree. A differenza degli altri casi qui viene restiuto direttamente
     * l'xml.
     *
     * @param report
     *            diagnostic tree
     * 
     * @return xml del report
     */
    public String generateDiagnosticData(XmlDiagnosticData report) {
        try {
            return marshallDiagnosticDataReport(report);
        } catch (JAXBException e) {
            LOG.error("Errore durante la creazione del diagnostic report", e);
        }
        return "<div>Errore durante la generazione del report diagnostico</div>";
    }

    /**
     * Creazione dell'xml relativo al simple report.
     *
     * @param simpleReport
     *            simple report
     * 
     * @return xml del simple report
     */
    public String marshallSimpleReport(XmlSimpleReport simpleReport) {
        try {
            JAXBElement<XmlSimpleReport> report = new eu.europa.esig.dss.simplereport.jaxb.ObjectFactory()
                    .createSimpleReport(simpleReport);
            StringWriter sw = new StringWriter();
            simpleReportMarshaller.marshal(report, sw);
            return sw.toString();
        } catch (JAXBException e) {
            LOG.error("Errore durante il marshall del simple report", e);
        }
        return "<div>Errore durante il marshall del simple report</div>";
    }

    /**
     * Creazione dell'xml relativo al detailed report.
     *
     * @param detailedReport
     *            detailed report
     * 
     * @return xml del detailed report
     */
    public String marshallDetailedReport(XmlDetailedReport detailedReport) {
        try {
            JAXBElement<XmlDetailedReport> report = new eu.europa.esig.dss.detailedreport.jaxb.ObjectFactory()
                    .createDetailedReport(detailedReport);
            StringWriter sw = new StringWriter();
            detailedReportMarshaller.marshal(report, sw);
            return sw.toString();
        } catch (JAXBException e) {
            LOG.error("Errore durante il marshall del detailed report", e);
        }
        return "<div>Errore durante il marshall del detailed report</div>";
    }

    private String marshallDiagnosticDataReport(XmlDiagnosticData diagnosticData) throws JAXBException {
        JAXBElement<XmlDiagnosticData> report = new eu.europa.esig.dss.diagnostic.jaxb.ObjectFactory()
                .createDiagnosticData(diagnosticData);
        StringWriter sw = new StringWriter();
        diagnosticDataReportMarshaller.marshal(report, sw);
        return sw.toString();
    }

    private String transformSimpleReport(String simpleReportXml) {
        Writer writer = new StringWriter();
        try {
            Transformer transformer = templateSimpleReport.newTransformer();
            transformer.setErrorListener(new DSSXmlErrorListener());
            transformer.transform(new StreamSource(new StringReader(simpleReportXml)), new StreamResult(writer));
        } catch (Exception e) {
            LOG.error("Error while generating simple report", e);
        }
        return writer.toString();
    }

    private String transformDetailedReport(String detailedReportXml) {
        Writer writer = new StringWriter();
        try {
            Transformer transformer = templateDetailedReport.newTransformer();
            transformer.setErrorListener(new DSSXmlErrorListener());
            transformer.transform(new StreamSource(new StringReader(detailedReportXml)), new StreamResult(writer));
        } catch (Exception e) {
            LOG.error("Error while generating detailed report", e);
        }
        return writer.toString();
    }

    public VerificaFirmaResultBean navNextBusta(String dir, VerificaFirmaResultBean current, int livello, int busta) {
        VerificaFirmaResultBean result = null;
        if (dir.equalsIgnoreCase(NAV_NEXT)) {
            // call ricerca by child
            result = current.ricerca(livello, busta + 1);
            if (result != null) {
                return result;
            } else {
                return navNextBusta(dir, current, livello + 1, busta);
            }
        } else {
            // call ricerca by parent
            result = current.getParent().ricerca(livello, busta - 1);
            if (result != null) {
                return result;
            } else {
                return navNextBusta(dir, current, livello - 1, busta);
            }
        }

    }

}
