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

package it.eng.parer.eidas.core.token;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.DefaultCMSSignatureAlgorithmNameGenerator;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.SignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.OperatorException;
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampResponse;
import org.bouncycastle.tsp.TimeStampResponseGenerator;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.tsp.TimeStampTokenGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.TimestampBinary;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.spi.DSSASN1Utils;
import eu.europa.esig.dss.spi.x509.tsp.TSPSource;
import eu.europa.esig.dss.token.KSPrivateKeyEntry;
import eu.europa.esig.dss.token.KeyStoreSignatureTokenConnection;

/**
 * Motck TSP check file tsp-config.xml
 */
public class EidasMockTSPSource implements TSPSource {

    private static final long serialVersionUID = 8863748492343274842L;

    private static final Logger log = LoggerFactory.getLogger(EidasMockTSPSource.class);

    private static SecureRandom random = new SecureRandom();

    private transient KeyStoreSignatureTokenConnection token;

    private String alias;

    public void setToken(KeyStoreSignatureTokenConnection token) {
	this.token = token;
    }

    public void setAlias(String alias) {
	this.alias = alias;
    }

    @Override
    public TimestampBinary getTimeStampResponse(DigestAlgorithm digestAlgorithm, byte[] digest) {
	Objects.requireNonNull(token, "KeyStore token is not defined!");
	try {
	    TimeStampRequestGenerator requestGenerator = new TimeStampRequestGenerator();
	    requestGenerator.setCertReq(true);
	    TimeStampRequest request = requestGenerator
		    .generate(new ASN1ObjectIdentifier(digestAlgorithm.getOid()), digest);

	    KSPrivateKeyEntry ksPK = (KSPrivateKeyEntry) token.getKey(alias);
	    if (ksPK == null) {
		throw new IllegalArgumentException(String.format(
			"Unable to initialize the MockTSPSource! "
				+ "Reason : Unable to retrieve private key from the given keyStore with alias '%s'",
			alias));
	    }

	    log.atDebug().log("Timestamping with {}", ksPK.getCertificate());

	    X509CertificateHolder certificate = new X509CertificateHolder(
		    ksPK.getCertificate().getEncoded());
	    List<X509Certificate> chain = new ArrayList<>();
	    CertificateToken[] certificateChain = ksPK.getCertificateChain();
	    for (CertificateToken token : certificateChain) {
		chain.add(token.getCertificate());
	    }

	    Set<ASN1ObjectIdentifier> accepted = new HashSet<>();
	    accepted.add(TSPAlgorithms.SHA1);
	    accepted.add(TSPAlgorithms.SHA256);
	    accepted.add(TSPAlgorithms.SHA384);
	    accepted.add(TSPAlgorithms.SHA512);

	    AlgorithmIdentifier digestAlgorithmIdentifier = new AlgorithmIdentifier(
		    new ASN1ObjectIdentifier(digestAlgorithm.getOid()));
	    AlgorithmIdentifier encryptionAlg = new AlgorithmIdentifier(
		    PKCSObjectIdentifiers.rsaEncryption);

	    DefaultCMSSignatureAlgorithmNameGenerator sigAlgoGenerator = new DefaultCMSSignatureAlgorithmNameGenerator();
	    String sigAlgoName = sigAlgoGenerator.getSignatureName(digestAlgorithmIdentifier,
		    encryptionAlg);

	    ContentSigner signer = new JcaContentSignerBuilder(sigAlgoName)
		    .build(ksPK.getPrivateKey());

	    SignerInfoGenerator infoGenerator = new SignerInfoGeneratorBuilder(
		    new BcDigestCalculatorProvider()).build(signer, certificate);
	    DigestCalculator digestCalculator = new JcaDigestCalculatorProviderBuilder().build()
		    .get(digestAlgorithmIdentifier);

	    TimeStampTokenGenerator tokenGenerator = new TimeStampTokenGenerator(infoGenerator,
		    digestCalculator, new ASN1ObjectIdentifier("1.2.3.4"));
	    tokenGenerator.addCertificates(new JcaCertStore(chain));

	    TimeStampResponseGenerator responseGenerator = new TimeStampResponseGenerator(
		    tokenGenerator, accepted);
	    TimeStampResponse response = responseGenerator.generate(request,
		    new BigInteger(128, random), new Date());
	    TimeStampToken timeStampToken = response.getTimeStampToken();

	    return new TimestampBinary(DSSASN1Utils.getDEREncoded(timeStampToken));

	} catch (IOException | TSPException | OperatorException | CertificateException e) {
	    throw new DSSException("Unable to generate a timestamp from the Mock", e);
	}
    }

}
