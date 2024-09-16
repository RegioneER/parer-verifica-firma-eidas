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
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
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
import eu.europa.esig.dss.spi.x509.aia.AIASource;
import eu.europa.esig.dss.spi.x509.aia.DefaultAIASource;
import eu.europa.esig.dss.spi.x509.revocation.crl.CRLSource;
import eu.europa.esig.dss.spi.x509.revocation.ocsp.OCSPSource;
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
import it.eng.parer.eidas.core.bean.CommonsDataHttpClient;
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

    @Value("${dss.server.signing.keystore.type}")
    private String serverSigningKeystoreType;

    @Value("${dss.server.signing.keystore.filename}")
    private String serverSigningKeystoreFilename;

    @Value("${dss.server.signing.keystore.password}")
    private String serverSigningKeystorePassword;

    /* from 5.6 */
    @Value("${current.oj.url}")
    private String currentOjUrl;

    /* custom DataSource possibile null in case DB is disable by configuration */
    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    private ProxyConfig proxyConfig;

    @Autowired
    private TSPSource tspSource;

    /* from 5.12 */
    @Value("${cache.crl.default.next.update:0}")
    private long crlDefaultNextUpdate;

    @Value("${cache.crl.max.next.update:0}")
    private long crlMaxNextUpdate;

    @Value("${cache.ocsp.default.next.update:0}")
    private long ocspDefaultNextUpdate;

    @Value("${cache.ocsp.max.next.update:0}")
    private long ocspMaxNextUpdate;

    /* from 5.13 */
    @Value("${cache.expiration:0}")
    private long cacheExpiration;

    /* custom */
    @Value("${revoke.data.loading.strategy.crl-first.enabled:false}")
    private boolean revokeDataLoadingStratCrlFirst;

    @Value("${revoke.removeExpired.enabled:true}")
    private boolean revokeRemoveExpired;

    /* in ms (5m) */
    @Value("${dataloader.timeoutconnection:300000}")
    private int timeoutConnection;

    /* in ms (5m) */
    @Value("${dataloader.timeoutsocket:300000}")
    private int timeoutSocket;

    @Value("${dataloader.connectionsmaxtotal:40}")
    private int connectionsMaxTotal;

    @Value("${dataloader.connectionsmaxperroute:4}")
    private int connectionsMaxPerRoute;

    /* in ms (5m) */
    @Value("${dataloader.connectiontimetolive:300000}")
    private int connectionTimeToLive;

    /* in ms (5m) */
    @Value("${dataloader.ldaptimeoutconnection:300000}")
    private String ldapTimeoutConnection;

    @Value("${cache.enabled:true}")
    private boolean cacheEnabled;

    // default empty
    @Value("${cache.file.path:}")
    private String cacheFilePath;

    /** CUSTOM HTTP CLIENT ! **/
    @Bean(initMethod = "init", destroyMethod = "destroy")
    public CommonsDataHttpClient dataHttpClient() {
        CommonsDataHttpClient dataClient = new CommonsDataHttpClient();
        // NOTA timeout impostabile (da configurazione!)
        dataClient.setTimeoutConnection(timeoutConnection);
        dataClient.setConnectionsMaxTotal(connectionsMaxTotal);
        dataClient.setTimeoutSocket(timeoutSocket);
        //
        dataClient.setConnectionsMaxPerRoute(connectionsMaxPerRoute);
        dataClient.setConnectionTimeToLive(connectionTimeToLive);
        //
        return dataClient;
    }

    @Bean
    public CommonsDataLoaderExt dataLoader() {
        CommonsDataLoaderExt dataLoader = new CommonsDataLoaderExt();
        dataLoader.setCommonsDataHttpClient(dataHttpClient());
        dataLoader.setProxyConfig(proxyConfig);
        //
        dataLoader.setLdapTimeoutConnection(ldapTimeoutConnection);
        return dataLoader;
    }

    @Bean
    public OCSPDataLoaderExt ocspDataLoader() {
        OCSPDataLoaderExt ocspDataLoader = new OCSPDataLoaderExt();
        ocspDataLoader.setCommonsDataHttpClient(dataHttpClient());
        ocspDataLoader.setProxyConfig(proxyConfig);
        ocspDataLoader.setLdapTimeoutConnection(ldapTimeoutConnection);
        return ocspDataLoader;
    }

    /* from 5.13 */
    @Bean
    public FileCacheDataLoader fileCacheDataLoader() {
        FileCacheDataLoader fileCacheDataLoader = initFileCacheDataLoader();
        fileCacheDataLoader.setCacheExpirationTime(cacheExpiration * 1000); // to millis
        return fileCacheDataLoader;
    }

    private FileCacheDataLoader initFileCacheDataLoader() {
        FileCacheDataLoader fileCacheDataLoader = new FileCacheDataLoader();
        fileCacheDataLoader.setDataLoader(dataLoader());
        // Per default uses "java.io.tmpdir" property
        // fileCacheDataLoader.setFileCacheDirectory(new File("/tmp"));
        if (StringUtils.isNotBlank(cacheFilePath)) {
            fileCacheDataLoader.setFileCacheDirectory(new File(cacheFilePath));
        }
        return fileCacheDataLoader;
    }

    @Bean
    public OnlineCRLSource onlineCRLSource() {
        OnlineCRLSource onlineCRLSource = new OnlineCRLSource();
        onlineCRLSource.setDataLoader(dataLoader());
        return onlineCRLSource;
    }

    /*
     * initMethod = "initTable" esecuzione CREATE table gestita in fase di creazione del bean gestione logica doppio
     * "source" JDBC vs FILE destroyMethod = "destroyTable" = esecuzione DROP TABLE non desisedarata corretto che
     * vengano create le tabelle ma non si vuole dropparle non appena il processo viene interrotto
     *
     * Visit
     * https://github.com/esig/dss-demonstrations/blob/master/dss-demo-webapp/src/main/java/eu/europa/esig/dss/web/
     * config/DSSBeanConfig.java
     *
     */
    // @Bean(initMethod = "initTable", destroyMethod = "destroyTable")
    @Bean
    public CRLSource defineCRLSource() {
        if (cacheEnabled) {
            if (dataSource != null) {
                JdbcCacheCRLSource jdbcCacheCRLSource = new JdbcCacheCRLSource();
                jdbcCacheCRLSource.setJdbcCacheConnector(jdbcCacheConnector());
                jdbcCacheCRLSource.setProxySource(onlineCRLSource());
                jdbcCacheCRLSource.setDefaultNextUpdateDelay(crlDefaultNextUpdate); // 0 (get new one every time)
                jdbcCacheCRLSource.setMaxNextUpdateDelay(crlMaxNextUpdate); // 0 (get new one every time)
                // default = true
                // questo permette di mantenere il dato su DB aggiornandolo se risulta *expired*
                jdbcCacheCRLSource.setRemoveExpired(revokeRemoveExpired);
                // create table if not exits
                try {
                    jdbcCacheCRLSource.initTable();
                } catch (SQLException e) {
                    throw new DSSException("Errore inizializzazione CRL JDBC cache", e);
                }
                return jdbcCacheCRLSource;
            }
            OnlineCRLSource onlineCRLSource = onlineCRLSource();
            FileCacheDataLoader fileCacheDataLoader = initFileCacheDataLoader();
            fileCacheDataLoader.setCacheExpirationTime(crlMaxNextUpdate * 1000); // to millis
            onlineCRLSource.setDataLoader(fileCacheDataLoader);
            return onlineCRLSource;
        } else {
            return onlineCRLSource();
        }

    }

    @Bean
    public OnlineOCSPSource onlineOCSPSource() {
        OnlineOCSPSource onlineOCSPSource = new OnlineOCSPSource();
        onlineOCSPSource.setDataLoader(ocspDataLoader());
        return onlineOCSPSource;
    }

    /*
     * initMethod = "initTable" esecuzione CREATE table gestita in fase di creazione del bean gestione logica doppio
     * "source" JDBC vs FILE destroyMethod = "destroyTable" = esecuzione DROP TABLE non desisedarata corretto che
     * vengano create le tabelle ma non si vuole dropparle non appena il processo viene interrotto
     *
     * Visit
     * https://github.com/esig/dss-demonstrations/blob/master/dss-demo-webapp/src/main/java/eu/europa/esig/dss/web/
     * config/DSSBeanConfig.java
     *
     *
     */
    // @Bean(initMethod = "initTable", destroyMethod = "destroyTable")
    @Bean
    public OCSPSource defineOCSPSource() {
        if (cacheEnabled) {
            if (dataSource != null) {
                JdbcCacheOCSPSource jdbcCacheOCSPSource = new JdbcCacheOCSPSource();
                jdbcCacheOCSPSource.setJdbcCacheConnector(jdbcCacheConnector());
                jdbcCacheOCSPSource.setProxySource(onlineOCSPSource());
                jdbcCacheOCSPSource.setDefaultNextUpdateDelay(ocspDefaultNextUpdate); // 0 (get new one every time)
                jdbcCacheOCSPSource.setMaxNextUpdateDelay(ocspMaxNextUpdate); // 0 (get new one every time)
                // questo permette di mantenere il dato su DB aggiornandolo se risulta *expired*
                jdbcCacheOCSPSource.setRemoveExpired(revokeRemoveExpired);
                try {
                    jdbcCacheOCSPSource.initTable();
                } catch (SQLException e) {
                    throw new DSSException("Errore inizializzazione OCSP JDBC cache", e);
                }
                return jdbcCacheOCSPSource;
            }
            OnlineOCSPSource onlineOCSPSource = onlineOCSPSource();
            FileCacheDataLoader fileCacheDataLoader = initFileCacheDataLoader();
            fileCacheDataLoader.setDataLoader(ocspDataLoader());
            fileCacheDataLoader.setCacheExpirationTime(ocspMaxNextUpdate * 1000); // to millis
            onlineOCSPSource.setDataLoader(fileCacheDataLoader);
            return onlineOCSPSource;
        } else {
            return onlineOCSPSource();
        }
    }

    /*
     * initMethod = "initTable" esecuzione CREATE table gestita in fase di creazione del bean gestione logica doppio
     * "source" JDBC vs FILE destroyMethod = "destroyTable" = esecuzione DROP TABLE non desisedarata corretto che
     * vengano create le tabelle ma non si vuole dropparle non appena il processo viene interrotto
     *
     *
     * Visit
     * https://github.com/esig/dss-demonstrations/blob/master/dss-demo-webapp/src/main/java/eu/europa/esig/dss/web/
     * config/DSSBeanConfig.java
     *
     */
    // @Bean(initMethod = "initTable", destroyMethod = "destroyTable")
    @Bean
    public AIASource defineAIASource() {
        if (cacheEnabled) {
            if (dataSource != null) {
                JdbcCacheAIASource jdbcCacheAIASource = new JdbcCacheAIASource();
                jdbcCacheAIASource.setJdbcCacheConnector(jdbcCacheConnector());
                jdbcCacheAIASource.setProxySource(onlineAIASource());
                return jdbcCacheAIASource;
            }
            FileCacheDataLoader fileCacheDataLoader = fileCacheDataLoader();
            return new DefaultAIASource(fileCacheDataLoader);
        } else {
            return onlineAIASource();
        }
    }

    @Bean
    public AIASource onlineAIASource() {
        return new DefaultAIASource(dataLoader());
    }

    /* from 5.8 */
    @Bean
    public CertificateVerifier certificateVerifier() {
        CommonCertificateVerifier certificateVerifier = new CommonCertificateVerifier();
        /* manage source */
        certificateVerifier.setCrlSource(defineCRLSource());
        certificateVerifier.setOcspSource(defineOCSPSource());
        certificateVerifier.setAIASource(defineAIASource());
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
            return new KeyStoreCertificateSource(ResourceUtils.getURL(ksFilename).openStream(), ksType,
                    ksPassword.toCharArray());
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
        FileCacheDataLoader onlineFileLoader = new FileCacheDataLoader();
        onlineFileLoader.setCacheExpirationTime(0);
        onlineFileLoader.setDataLoader(dataLoader());
        onlineFileLoader.setFileCacheDirectory(tlCacheDirectory());
        return onlineFileLoader;
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
        CommonsDataLoaderExt dataLoader = new CommonsDataLoaderExt();
        dataLoader.setCommonsDataHttpClient(dataHttpClient());
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

    @Bean
    public JdbcCacheConnector jdbcCacheConnector() {
        return new JdbcCacheConnector(dataSource);
    }

}
