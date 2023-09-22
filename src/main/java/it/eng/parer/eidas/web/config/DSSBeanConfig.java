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

package it.eng.parer.eidas.web.config;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore.PasswordProtection;

import javax.sql.DataSource;

import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.ResourceUtils;

import eu.europa.esig.dss.alert.ExceptionOnStatusAlert;
import eu.europa.esig.dss.asic.cades.signature.ASiCWithCAdESService;
import eu.europa.esig.dss.asic.xades.signature.ASiCWithXAdESService;
import eu.europa.esig.dss.cades.signature.CAdESService;
import eu.europa.esig.dss.jades.signature.JAdESService;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.service.crl.JdbcCacheCRLSource;
import eu.europa.esig.dss.service.crl.OnlineCRLSource;
import eu.europa.esig.dss.service.http.commons.CommonsDataLoader;
import eu.europa.esig.dss.service.http.commons.FileCacheDataLoader;
import eu.europa.esig.dss.service.http.commons.SSLCertificateLoader;
import eu.europa.esig.dss.service.http.proxy.ProxyConfig;
import eu.europa.esig.dss.service.ocsp.JdbcCacheOCSPSource;
import eu.europa.esig.dss.service.ocsp.OnlineOCSPSource;
import eu.europa.esig.dss.service.x509.aia.JdbcCacheAIASource;
import eu.europa.esig.dss.spi.client.http.DSSFileLoader;
import eu.europa.esig.dss.spi.client.http.IgnoreDataLoader;
import eu.europa.esig.dss.spi.client.jdbc.JdbcCacheConnector;
import eu.europa.esig.dss.spi.tsl.TrustedListsCertificateSource;
import eu.europa.esig.dss.spi.x509.KeyStoreCertificateSource;
import eu.europa.esig.dss.spi.x509.aia.DefaultAIASource;
import eu.europa.esig.dss.spi.x509.aia.OnlineAIASource;
import eu.europa.esig.dss.spi.x509.tsp.TSPSource;
import eu.europa.esig.dss.token.KeyStoreSignatureTokenConnection;
import eu.europa.esig.dss.tsl.function.OfficialJournalSchemeInformationURI;
import eu.europa.esig.dss.tsl.job.TLValidationJob;
import eu.europa.esig.dss.tsl.source.LOTLSource;
import eu.europa.esig.dss.validation.CRLFirstRevocationDataLoadingStrategyFactory;
import eu.europa.esig.dss.validation.CertificateVerifier;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.ws.signature.common.RemoteDocumentSignatureServiceImpl;
import eu.europa.esig.dss.ws.signature.common.RemoteMultipleDocumentsSignatureServiceImpl;
import eu.europa.esig.dss.ws.signature.common.RemoteTrustedListSignatureServiceImpl;
import eu.europa.esig.dss.ws.validation.common.RemoteDocumentValidationService;
import eu.europa.esig.dss.xades.signature.XAdESService;
import it.eng.parer.eidas.core.bean.CommonsDataLoaderExt;
import it.eng.parer.eidas.core.bean.OCSPDataLoaderExt;
import it.eng.parer.eidas.core.service.CustomRemoteDocumentValidationImpl;
import it.eng.parer.eidas.core.service.ICustomRemoteDocumentValidation;

@Configuration
// @PropertySource("classpath:dss.properties")
@Import({ SchedulingConfig.class })
@ComponentScan(basePackages = { "eu.europa.esig.dss.web.job", "eu.europa.esig.dss.web.service" })
@ImportResource({ "${tsp-source}" })
public class DSSBeanConfig {

    private static final Logger LOG = LoggerFactory.getLogger(DSSBeanConfig.class);

    @Value("${default.validation.policy}")
    private String defaultValidationPolicy;

    @Value("${current.lotl.url}")
    private String lotlUrl;

    @Value("${lotl.country.code}")
    private String lotlCountryCode;

    @Value("${current.oj.url}")
    private String ojUrl;

    @Value("${oj.content.keystore.type}")
    private String ksType;

    @Value("${oj.content.keystore.filename}")
    private String ksFilename;

    @Value("${oj.content.keystore.password}")
    private String ksPassword;

    @Value("${dss.tsa.url}")
    private String tsaUrl;

    @Value("${dss.server.signing.keystore.type}")
    private String serverSigningKeystoreType;

    @Value("${dss.server.signing.keystore.filename}")
    private String serverSigningKeystoreFilename;

    @Value("${dss.server.signing.keystore.password}")
    private String serverSigningKeystorePassword;

    @Value("${dss.dataloader.timeoutconnection:60000}")
    private int timeoutConnection;

    @Value("${dss.dataloader.timeoutsocket:60000}")
    private int timeoutSocket;

    @Value("${dss.dataloader.connectionsmaxtotal:20}")
    private int connectionsMaxTotal;

    @Value("${dss.dataloader.connectionsmaxperroute:2}")
    private int connectionsMaxPerRoute;

    /* custom */
    @Value("${dss.dataloader.ldaptimeoutconnection:60000}")
    private String ldapTimeoutConnection;

    /* from 5.6 */
    @Value("${current.oj.url}")
    private String currentOjUrl;

    @Autowired
    private DataSource dataSource;

    @Autowired(required = false)
    private ProxyConfig proxyConfig;

    @Autowired
    private TSPSource tspSource;

    /* from 5.6 */
    @Value("${dss.cachedCRLSource.defaultNextUpdateDelay:180}")
    private long defaultNextUpdateDelay;

    /* custom */
    @Value("${dss.revoke.data.loading.strategy.crl-first.enabled:true}")
    private boolean revokeDataLoadingStratCrlFirst;

    @Value("${dss.revoke.removeExpired.enabled:true}")
    private boolean revokeRemoveExpired;

    @Bean
    public CommonsDataLoaderExt dataLoader() {
        CommonsDataLoaderExt dataLoader = new CommonsDataLoaderExt();
        dataLoader.setProxyConfig(proxyConfig);
        // NOTA timeout impostabile (da configurazione!)
        dataLoader.setTimeoutConnection(timeoutConnection);
        dataLoader.setConnectionsMaxTotal(connectionsMaxTotal);
        dataLoader.setTimeoutSocket(timeoutSocket);
        //
        dataLoader.setConnectionsMaxPerRoute(connectionsMaxPerRoute);
        //
        dataLoader.setLdapTimeoutConnection(ldapTimeoutConnection);
        return dataLoader;
    }

    @Bean
    public OCSPDataLoaderExt ocspDataLoader() {
        OCSPDataLoaderExt ocspDataLoader = new OCSPDataLoaderExt();
        ocspDataLoader.setProxyConfig(proxyConfig);
        // NOTA timeout impostabile (da configurazione!)
        ocspDataLoader.setTimeoutConnection(timeoutConnection);
        ocspDataLoader.setConnectionsMaxTotal(connectionsMaxTotal);
        ocspDataLoader.setTimeoutSocket(timeoutSocket);
        //
        ocspDataLoader.setConnectionsMaxPerRoute(connectionsMaxPerRoute);
        //
        ocspDataLoader.setLdapTimeoutConnection(ldapTimeoutConnection);
        return ocspDataLoader;
    }

    @Bean
    public FileCacheDataLoader fileCacheDataLoader() {
        FileCacheDataLoader fileCacheDataLoader = new FileCacheDataLoader();
        fileCacheDataLoader.setDataLoader(dataLoader());
        // Per default uses "java.io.tmpdir" property
        // fileCacheDataLoader.setFileCacheDirectory(new File("/tmp"));
        return fileCacheDataLoader;
    }

    @Bean
    public OnlineCRLSource onlineCRLSource() {
        OnlineCRLSource onlineCRLSource = new OnlineCRLSource();
        onlineCRLSource.setDataLoader(dataLoader());
        return onlineCRLSource;
    }

    /*
     * destroyMethod = "destroyTable" = esecuzione DROP TABLE non desisedarata corretto che vengano create le tabelle ma
     * non si vuole dropparle non appena il processo viene interrotto
     */
    // @Bean(initMethod = "initTable", destroyMethod = "destroyTable")
    @Bean(initMethod = "initTable")
    public JdbcCacheCRLSource cachedCRLSource() {
        JdbcCacheCRLSource jdbcCacheCRLSource = new JdbcCacheCRLSource();
        jdbcCacheCRLSource.setJdbcCacheConnector(jdbcCacheConnector());
        jdbcCacheCRLSource.setProxySource(onlineCRLSource());
        jdbcCacheCRLSource.setDefaultNextUpdateDelay(defaultNextUpdateDelay); // 3 minutes
        // default = true
        // questo permette di mantenere il dato su DB aggiornandolo se risulta *expired*
        jdbcCacheCRLSource.setRemoveExpired(revokeRemoveExpired);
        return jdbcCacheCRLSource;
    }

    @Bean
    public OnlineOCSPSource onlineOcspSource() {
        OnlineOCSPSource onlineOCSPSource = new OnlineOCSPSource();
        onlineOCSPSource.setDataLoader(ocspDataLoader());
        return onlineOCSPSource;
    }

    /*
     * destroyMethod = "destroyTable" = esecuzione DROP TABLE non desisedarata corretto che vengano create le tabelle ma
     * non si vuole dropparle non appena il processo viene interrotto
     */
    /* from 5.6 */
    // @Bean(initMethod = "initTable", destroyMethod = "destroyTable")
    @Bean(initMethod = "initTable")
    public JdbcCacheOCSPSource cachedOCSPSource() {
        JdbcCacheOCSPSource jdbcCacheOCSPSource = new JdbcCacheOCSPSource();
        jdbcCacheOCSPSource.setJdbcCacheConnector(jdbcCacheConnector());
        jdbcCacheOCSPSource.setProxySource(onlineOcspSource());
        jdbcCacheOCSPSource.setDefaultNextUpdateDelay(defaultNextUpdateDelay); // 3 minutes
        // questo permette di mantenere il dato su DB aggiornandolo se risulta *expired*
        jdbcCacheOCSPSource.setRemoveExpired(revokeRemoveExpired);
        return jdbcCacheOCSPSource;
    }

    /* from 5.8 */
    @Bean
    public CertificateVerifier certificateVerifier() {
        CommonCertificateVerifier certificateVerifier = new CommonCertificateVerifier();
        certificateVerifier.setCrlSource(cachedCRLSource());
        certificateVerifier.setOcspSource(cachedOCSPSource());
        certificateVerifier.setAIASource(cachedAIASource());
        certificateVerifier.setTrustedCertSources(trustedListSource());

        // Default configs
        certificateVerifier.setAlertOnMissingRevocationData(new ExceptionOnStatusAlert());
        certificateVerifier.setCheckRevocationForUntrustedChains(false);

        // Revocation strategy (CRL first)
        if (revokeDataLoadingStratCrlFirst) {
            certificateVerifier
                    .setRevocationDataLoadingStrategyFactory(new CRLFirstRevocationDataLoadingStrategyFactory());
        }

        return certificateVerifier;
    }

    @Bean
    public Resource defaultPolicy() {
        Resource resource = new FileSystemResource(defaultValidationPolicy);
        if (!resource.exists()) {
            resource = new ClassPathResource(defaultValidationPolicy);
        }
        return resource;
    }

    @Bean
    public CAdESService cadesService() {
        CAdESService service = new CAdESService(certificateVerifier());
        service.setTspSource(tspSource);
        return service;
    }

    @Bean
    public XAdESService xadesService() {
        XAdESService service = new XAdESService(certificateVerifier());
        service.setTspSource(tspSource);
        return service;
    }

    @Bean
    public PAdESService padesService() {
        PAdESService service = new PAdESService(certificateVerifier());
        service.setTspSource(tspSource);
        return service;
    }

    @Bean
    public ASiCWithCAdESService asicWithCadesService() {
        ASiCWithCAdESService service = new ASiCWithCAdESService(certificateVerifier());
        service.setTspSource(tspSource);
        return service;
    }

    @Bean
    public ASiCWithXAdESService asicWithXadesService() {
        ASiCWithXAdESService service = new ASiCWithXAdESService(certificateVerifier());
        service.setTspSource(tspSource);
        return service;
    }

    /* from 5.8 */
    @Bean
    public JAdESService jadesService() {
        JAdESService service = new JAdESService(certificateVerifier());
        service.setTspSource(tspSource);
        return service;
    }

    /* from 5.8 */
    @Bean
    public RemoteDocumentSignatureServiceImpl remoteSignatureService() {
        RemoteDocumentSignatureServiceImpl service = new RemoteDocumentSignatureServiceImpl();
        service.setAsicWithCAdESService(asicWithCadesService());
        service.setAsicWithXAdESService(asicWithXadesService());
        service.setCadesService(cadesService());
        service.setXadesService(xadesService());
        service.setPadesService(padesService());
        service.setJadesService(jadesService());
        return service;
    }

    /* from 5.8 */
    @Bean
    public RemoteMultipleDocumentsSignatureServiceImpl remoteMultipleDocumentsSignatureService() {
        RemoteMultipleDocumentsSignatureServiceImpl service = new RemoteMultipleDocumentsSignatureServiceImpl();
        service.setAsicWithCAdESService(asicWithCadesService());
        service.setAsicWithXAdESService(asicWithXadesService());
        service.setXadesService(xadesService());
        service.setJadesService(jadesService());
        return service;
    }

    @Bean
    public RemoteDocumentValidationService remoteValidationService() {
        RemoteDocumentValidationService service = new RemoteDocumentValidationService();
        service.setVerifier(certificateVerifier());
        return service;
    }

    /* from 5.11 */
    @Bean
    public RemoteTrustedListSignatureServiceImpl remoteTrustedListSignatureService() {
        RemoteTrustedListSignatureServiceImpl service = new RemoteTrustedListSignatureServiceImpl();
        service.setXadesService(xadesService());
        return service;
    }

    /* from 5.6 */
    @Bean
    public KeyStoreSignatureTokenConnection remoteToken() throws IOException {
        return new KeyStoreSignatureTokenConnection(ResourceUtils.getURL(serverSigningKeystoreFilename).openStream(),
                serverSigningKeystoreType, new PasswordProtection(serverSigningKeystorePassword.toCharArray()));
    }

    @Bean
    public ICustomRemoteDocumentValidation customRemoteValidationService() {
        ICustomRemoteDocumentValidation service = new CustomRemoteDocumentValidationImpl();
        service.setVerifier(certificateVerifier());
        service.setDefaultValidationPolicy(defaultPolicy());
        return service;
    }

    @Bean(name = "european-trusted-list-certificate-source")
    public TrustedListsCertificateSource trustedListSource() {
        return new TrustedListsCertificateSource();
    }

    /* from 5.6 */
    @Bean
    public KeyStoreCertificateSource ojContentKeyStore() {
        try {
            return new KeyStoreCertificateSource(ResourceUtils.getURL(ksFilename).openStream(), ksType, ksPassword);
        } catch (IOException e) {
            throw new DSSException("Unable to load the file " + ksFilename, e);
        }
    }

    /* from 5.6 */
    @Bean(name = "european-lotl-source")
    public LOTLSource europeanLOTL() {
        LOTLSource lotlSource = new LOTLSource();
        lotlSource.setUrl(lotlUrl);
        lotlSource.setCertificateSource(ojContentKeyStore());
        lotlSource.setSigningCertificatesAnnouncementPredicate(new OfficialJournalSchemeInformationURI(currentOjUrl));
        lotlSource.setPivotSupport(true);
        return lotlSource;
    }

    /* from 5.6 */
    @Bean
    public File tlCacheDirectory() {
        File rootFolder = new File(System.getProperty("java.io.tmpdir"));
        File tslCache = new File(rootFolder, "dss-tsl-loader");
        if (tslCache.mkdirs()) {
            LOG.info("TL Cache folder : {}", tslCache.getAbsolutePath());
        }
        return tslCache;
    }

    /* from 5.6 */
    @Bean
    public DSSFileLoader offlineLoader() {
        FileCacheDataLoader offlineFileLoader = new FileCacheDataLoader();
        offlineFileLoader.setCacheExpirationTime(-1); // negative value means cache never expires (from 5.10)
        offlineFileLoader.setDataLoader(new IgnoreDataLoader());
        offlineFileLoader.setFileCacheDirectory(tlCacheDirectory());
        return offlineFileLoader;
    }

    /* from 5.6 */
    @Bean
    public DSSFileLoader onlineLoader() {
        FileCacheDataLoader offlineFileLoader = new FileCacheDataLoader();
        offlineFileLoader.setCacheExpirationTime(0);
        offlineFileLoader.setDataLoader(dataLoader());
        offlineFileLoader.setFileCacheDirectory(tlCacheDirectory());
        return offlineFileLoader;
    }

    /* from 5.6 */
    @Bean
    public TLValidationJob job() {
        TLValidationJob job = new TLValidationJob();
        job.setTrustedListCertificateSource(trustedListSource());
        job.setListOfTrustedListSources(europeanLOTL());
        job.setOfflineDataLoader(offlineLoader());
        job.setOnlineDataLoader(onlineLoader());
        return job;
    }

    /* from 5.8 */
    @Bean
    public CommonsDataLoader trustAllDataLoader() {
        CommonsDataLoader dataLoader = new CommonsDataLoader();
        dataLoader.setProxyConfig(proxyConfig);
        dataLoader.setTrustStrategy(new TrustAllStrategy());
        return dataLoader;
    }

    /* from 5.8 */
    /* QWAC Validation */

    @Bean
    public SSLCertificateLoader sslCertificateLoader() {
        SSLCertificateLoader sslCertificateLoader = new SSLCertificateLoader();
        sslCertificateLoader.setCommonsDataLoader(trustAllDataLoader());
        return sslCertificateLoader;
    }

    /*
     * destroyMethod = "destroyTable" = esecuzione DROP TABLE non desisedarata corretto che vengano create le tabelle ma
     * non si vuole dropparle non appena il processo viene interrotto
     */
    /* from 5.10.1 */
    // @Bean(initMethod = "initTable", destroyMethod = "destroyTable")
    @Bean(initMethod = "initTable")
    public JdbcCacheAIASource cachedAIASource() {
        JdbcCacheAIASource jdbcCacheAIASource = new JdbcCacheAIASource();
        jdbcCacheAIASource.setJdbcCacheConnector(jdbcCacheConnector());
        jdbcCacheAIASource.setProxySource(onlineAIASource());
        return jdbcCacheAIASource;
    }

    @Bean
    public OnlineAIASource onlineAIASource() {
        return new DefaultAIASource(dataLoader());
    }

    @Bean
    public JdbcCacheConnector jdbcCacheConnector() {
        return new JdbcCacheConnector(dataSource);
    }

}
