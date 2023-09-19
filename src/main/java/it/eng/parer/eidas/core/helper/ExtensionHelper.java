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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.CRLNumber;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import eu.europa.esig.dss.diagnostic.jaxb.XmlCertificate;
import eu.europa.esig.dss.diagnostic.jaxb.XmlCertificateRevocation;
import eu.europa.esig.dss.enumerations.RevocationType;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.AdvancedSignature;
import eu.europa.esig.dss.validation.timestamp.TimestampToken;
import eu.europa.esig.dss.ws.validation.dto.WSReportsDTO;
import it.eng.parer.eidas.model.EidasWSReportsDTOTree;
import it.eng.parer.eidas.model.ExtensionsDTO;

@Component
public class ExtensionHelper {

    private static final Logger LOG = LoggerFactory.getLogger(ExtensionHelper.class);

    /**
     * Metodo che, con l'ausilio delle librerie bouncy, estrae attraverso gli opportuini OID le estensioni delle firma
     * analizzata.
     * 
     * @param wsdto
     *            oggetto standard report eidas
     * @param dto
     *            oggetto custom tree contenente i risultati della verifica
     */
    public void createExtensions(WSReportsDTO wsdto, EidasWSReportsDTOTree dto) {
        try {
            for (XmlCertificate certificate : wsdto.getDiagnosticData().getUsedCertificates()) {
                // Informazioni sul certificato
                byte[] base64Encoded = certificate.getBase64Encoded();
                // check if not null
                if (base64Encoded != null) {
                    ExtensionsDTO certificateExtension = new ExtensionsDTO();
                    certificateExtension.setAuthorityKeyIdentifier(extractAuthorityKeyIdentifier(base64Encoded));
                    LOG.debug("AuthorityKeyIdentifier {}, id {}", certificateExtension.getAuthorityKeyIdentifier(),
                            certificate.getId());

                    certificateExtension.setSubjectKeyIdentifier(extractSubjectKeyIdentifier(base64Encoded));
                    LOG.debug("SubjectKeyIdentifier {}, id {}", certificateExtension.getSubjectKeyIdentifier(),
                            certificate.getId());
                    // add on map
                    dto.getExtensions().put(certificate.getId(), certificateExtension);

                }
                // Estrazione del CrlNumber
                // solo CRL
                for (XmlCertificateRevocation certificateRevocation : certificate.getRevocations().stream()
                        .filter(r -> r.getRevocation().getType().equals(RevocationType.CRL)).toList()) {
                    byte[] base64EncodedCrl = certificateRevocation.getRevocation().getBase64Encoded();
                    // check if not null
                    if (base64EncodedCrl != null) {
                        ExtensionsDTO crlExtension = new ExtensionsDTO();
                        Long crlNumber = getCrlNumber(base64EncodedCrl);
                        crlExtension.setCrlNumber(crlNumber);
                        dto.getExtensions().put(certificateRevocation.getRevocation().getId(), crlExtension);
                        LOG.debug("CrlNumber {}, id {}", crlNumber, certificateRevocation.getRevocation().getId());
                    }
                }
            }
        } catch (Exception e) {
            LOG.debug("Errore durante il calcolo Authority Key Identifier / " + "Subject Key Identifier / "
                    + "CRL Number  (OID 2.5.29.35, 2.5.29.14, 2.5.29.20)", e);
        }
    }

    /**
     * Ottieni l'identificativo del certificato della CA che ha emesso il certificato passato in input.
     *
     * @param certificate
     *            certificato ssl
     * 
     * @return identificativo codificato in HEX
     * 
     * @throws CertificateException
     *             errore sul processamento degli stream
     */
    private String extractAuthorityKeyIdentifier(byte[] certificate) throws CertificateException {
        String keyIdentifierHex = null;
        CertificateFactory instance = CertificateFactory.getInstance("X509");
        X509Certificate cert = (X509Certificate) instance.generateCertificate(new ByteArrayInputStream(certificate));
        byte[] extensionValue = cert.getExtensionValue("2.5.29.35");
        if (extensionValue != null) {
            byte[] octets = ASN1OctetString.getInstance(extensionValue).getOctets();
            AuthorityKeyIdentifier authorityKeyIdentifier = AuthorityKeyIdentifier.getInstance(octets);
            byte[] keyIdentifier = authorityKeyIdentifier.getKeyIdentifier();
            keyIdentifierHex = Hex.encodeHexString(keyIdentifier);
        }
        return keyIdentifierHex;

    }

    /**
     * Ottieni l'identificativo del certificato passato in input.
     *
     * @param certificate
     *            certificato ssl
     * 
     * @return identificativo codificato in HEX
     * 
     * @throws CertificateException
     *             errore sul processamento degli stream
     */
    private String extractSubjectKeyIdentifier(byte[] certificate) throws CertificateException {
        String keyIdentifierHex = null;
        CertificateFactory instance = CertificateFactory.getInstance("X509");
        X509Certificate cert = (X509Certificate) instance.generateCertificate(new ByteArrayInputStream(certificate));
        byte[] extensionValue = cert.getExtensionValue("2.5.29.14");
        if (extensionValue != null) {
            byte[] octets = ASN1OctetString.getInstance(extensionValue).getOctets();
            SubjectKeyIdentifier subjectKeyIdentifier = SubjectKeyIdentifier.getInstance(octets);
            byte[] keyIdentifier = subjectKeyIdentifier.getKeyIdentifier();
            keyIdentifierHex = Hex.encodeHexString(keyIdentifier);
        }
        return keyIdentifierHex;
    }

    /**
     * Estrai dalla CRL l'estensione relativa al numero della CRL.
     *
     * @param crlCertificate
     *            certificato della CRL
     * 
     * @return numero della crl
     * 
     * @throws CertificateException
     *             da gestire meglio
     * @throws IOException
     *             da gestire meglio
     * @throws CRLException
     *             da gestire meglio
     */
    private Long getCrlNumber(byte[] crlCertificate) throws CertificateException, IOException, CRLException {
        Long result = null;
        CertificateFactory instance = CertificateFactory.getInstance("X509");
        X509CRL crl = (X509CRL) instance.generateCRL(new ByteArrayInputStream(crlCertificate));
        byte[] extensionValue = crl.getExtensionValue("2.5.29.20");
        if (extensionValue != null) {
            byte[] encoded = JcaX509ExtensionUtils.parseExtensionValue(extensionValue).getEncoded();
            CRLNumber clrNumber = CRLNumber.getInstance(encoded);
            if (clrNumber != null) {
                result = clrNumber.getCRLNumber().longValue();
            }
        }
        return result;
    }

    /**
     * Ottieni il base64 delle marche. Il report eidas contiene l'attributo signaturevalue con il base64 della firma
     * mentre, lo stesso non vale per gli ottetti di tipo TimeStampObject (tale valore deve quindi essere ricavato)
     *
     * @param signature
     *            interfaccia della firma
     * @param rootReport
     *            report contentente le estensioni
     */
    public void extractSignatureBytes(AdvancedSignature signature, EidasWSReportsDTOTree rootReport) {

        // Gestione timestamp
        List<TimestampToken> timeStamps = signature.getAllTimestamps();
        if (timeStamps != null) {
            for (TimestampToken timestampToken : timeStamps) {
                String encodedSignedDataValue = extractAndEncodeTimestampBytes(timestampToken);

                ExtensionsDTO timestampExt = new ExtensionsDTO();
                timestampExt.setMarcaBase64(encodedSignedDataValue);
                rootReport.getExtensions().put(timestampToken.getDSSIdAsString(), timestampExt);
                LOG.debug("TimestampToken id {} encodedSignedDataValue {}", timestampToken.getDSSIdAsString(),
                        timestampExt.getMarcaBase64());
            }
        }
    }

    /**
     * Ottieni il timestamp apposto codificato il formato base64.
     *
     * @param timestampToken
     *            token DSS
     * 
     * @return base64 del timestamp apposto
     */
    private String extractAndEncodeTimestampBytes(TimestampToken timestampToken) {
        String encodedTimestamp = "non valorizzata";
        try {
            CMSSignedData cmsSignedData = new CMSSignedData(timestampToken.getEncoded());
            SignerInformationStore signerInfos = cmsSignedData.getSignerInfos();

            Collection<SignerInformation> signers = signerInfos
                    .getSigners(timestampToken.getSignerInformation().getSID());
            if (signers != null && !signers.isEmpty()) {
                SignerInformation signerInfo = signers.iterator().next();
                encodedTimestamp = Utils.toBase64(signerInfo.getSignature());
            }

        } catch (CMSException ex) {
            LOG.debug("Impossibile ottenere le informazioni relative al timestamp", ex);
        }
        return encodedTimestamp;
    }

}
