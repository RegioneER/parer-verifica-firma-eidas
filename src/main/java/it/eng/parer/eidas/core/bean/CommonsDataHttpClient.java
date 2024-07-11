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

package it.eng.parer.eidas.core.bean;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.net.ssl.HostnameVerifier;

import org.apache.hc.client5.http.HttpRequestRetryStrategy;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.Credentials;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.auth.BasicScheme;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.service.http.commons.CommonsHttpClientResponseHandler;
import eu.europa.esig.dss.service.http.commons.HostConnection;
import eu.europa.esig.dss.service.http.commons.UserCredentials;
import eu.europa.esig.dss.spi.exception.DSSExternalResourceException;
import eu.europa.esig.dss.utils.Utils;

public class CommonsDataHttpClient implements Serializable {

    private static final long serialVersionUID = 5684332248649660441L;

    private static final Logger LOG = LoggerFactory.getLogger(CommonsDataHttpClient.class);

    /** The default connection timeout (1 minute) */
    private static final Timeout TIMEOUT_CONNECTION = toTimeoutMilliseconds(60000);

    /** The default socket timeout (1 minute) */
    private static final Timeout TIMEOUT_SOCKET = toTimeoutMilliseconds(60000);

    /** The default value of maximum connections in time (20) */
    private static final int CONNECTIONS_MAX_TOTAL = 20;

    /** The default value of maximum connections per route (2) */
    private static final int CONNECTIONS_MAX_PER_ROUTE = 2;

    /** The default connection total time to live (TTL) (1 minute) */
    private static final TimeValue CONNECTION_TIME_TO_LIVE = toTimeValueMilliseconds(60000);

    /** The timeout connection */
    private Timeout timeoutConnection = TIMEOUT_CONNECTION;

    /** The connection request timeout */
    private Timeout timeoutConnectionRequest = TIMEOUT_CONNECTION;

    /** The server response timeout */
    private Timeout timeoutResponse = TIMEOUT_CONNECTION;

    /** The timeout socket */
    private Timeout timeoutSocket = TIMEOUT_SOCKET;

    /** Connection keep alive timeout */
    private TimeValue connectionKeepAlive = CONNECTION_TIME_TO_LIVE;

    /** Maximum connections number in time */
    private int connectionsMaxTotal = CONNECTIONS_MAX_TOTAL;

    /** Maximum connections number per route */
    private int connectionsMaxPerRoute = CONNECTIONS_MAX_PER_ROUTE;

    /** The finite connection total time to live (TTL) */
    private TimeValue connectionTimeToLive = CONNECTION_TIME_TO_LIVE;

    /** Defines if the redirection is enabled */
    private boolean redirectsEnabled = true;

    /** Defines if the default system network properties shall be used */
    private boolean useSystemProperties = false;

    /** Contains rules credentials for authentication to different resources */
    private Map<HostConnection, UserCredentials> authenticationMap;

    /**
     * Used SSL protocol
     */
    private String sslProtocol;

    /**
     * Keystore for SSL.
     */
    private DSSDocument sslKeystore;

    /**
     * Keystore's type.
     */
    private String sslKeystoreType = KeyStore.getDefaultType();

    /**
     * Keystore's password.
     */
    private char[] sslKeystorePassword = new char[] {};

    /**
     * Defines if the keyStore shall be loaded as a trusted material
     */
    private boolean loadKeyStoreAsTrustMaterial = false;

    /**
     * TrustStore for SSL.
     */
    private DSSDocument sslTruststore;

    /**
     * Trust store's type
     */
    private String sslTruststoreType = KeyStore.getDefaultType();

    /**
     * Truststore's password.
     */
    private char[] sslTruststorePassword = new char[] {};

    /**
     * The trust strategy
     */
    private transient TrustStrategy trustStrategy;

    /**
     * Array of supported SSL protocols
     */
    private String[] supportedSSLProtocols;

    /**
     * Array of supported SSL Cipher Suites
     */
    private String[] supportedSSLCipherSuites;

    /**
     * The hostname verifier
     */
    private transient HostnameVerifier hostnameVerifier = new DefaultHostnameVerifier();

    /**
     * The connection retry strategy
     */
    private transient HttpRequestRetryStrategy retryStrategy;

    /**
     * Defines whether the preemptive basic authentication should be used
     */
    private boolean preemptiveAuthentication;

    /**
     * Processes the HTTP dataHttpClient response and returns byte array in case of success Default:
     * {@code CommonsHttpClientResponseHandler}
     */
    private transient HttpClientResponseHandler<byte[]> httpClientResponseHandler = new CommonsHttpClientResponseHandler();

    /**
     * Standard dataHttpClient
     */
    private transient CloseableHttpClient client;

    /**
     * The default constructor for CommonsDataLoader.
     */
    public CommonsDataHttpClient() {
        // empty
    }

    /**
     * init method
     */
    public void init() {
        if (client == null) {
            client = createHttpClient();
        }
    }

    /**
     * destroy method
     * 
     * @throws IOException
     *             generic exception
     */
    public void destroy() throws IOException {
        if (client != null) {
            client.close();
        }
    }

    /**
     * Gets the connection timeout.
     *
     * @return the value (millis)
     */
    public int getTimeoutConnection() {
        return timeoutConnection.toMillisecondsIntBound();
    }

    /**
     * Sets the connection timeout in milliseconds.
     *
     * A negative value is interpreted as undefined (use system default).
     *
     * @param timeoutConnection
     *            the value (millis)
     */
    public void setTimeoutConnection(final int timeoutConnection) {
        this.timeoutConnection = toTimeoutMilliseconds(timeoutConnection);
    }

    /**
     * Gets the connection request timeout.
     *
     * @return the value (millis)
     */
    public int getTimeoutConnectionRequest() {
        return timeoutConnectionRequest.toMillisecondsIntBound();
    }

    /**
     * Sets the connection request in milliseconds.
     *
     * A negative value is interpreted as undefined (use system default).
     *
     * @param timeoutConnectionRequest
     *            the value (millis)
     */
    public void setTimeoutConnectionRequest(int timeoutConnectionRequest) {
        this.timeoutConnectionRequest = toTimeoutMilliseconds(timeoutConnectionRequest);
    }

    /**
     * Gets the server response timeout.
     *
     * @return the value (millis)
     */
    public int getTimeoutResponse() {
        return timeoutResponse.toMillisecondsIntBound();
    }

    /**
     * Sets the server response timeout in milliseconds.
     *
     * A negative value is interpreted as undefined (use system default).
     *
     * @param timeoutResponse
     *            the value (millis)
     */
    public void setTimeoutResponse(int timeoutResponse) {
        this.timeoutResponse = toTimeoutMilliseconds(timeoutResponse);
    }

    /**
     * Gets the socket timeout.
     *
     * @return the value (millis)
     */
    public int getTimeoutSocket() {
        return timeoutSocket.toMillisecondsIntBound();
    }

    /**
     * Sets the socket timeout in milliseconds.
     *
     * A negative value is interpreted as undefined (use system default).
     *
     * @param timeoutSocket
     *            the value (millis)
     */
    public void setTimeoutSocket(final int timeoutSocket) {
        this.timeoutSocket = toTimeoutMilliseconds(timeoutSocket);
    }

    /**
     * Gets the connection keep alive timeout.
     *
     * @return the value (millis)
     */
    public int getConnectionKeepAlive() {
        return connectionKeepAlive.toMillisecondsIntBound();
    }

    /**
     * Sets the connection keep alive timeout in milliseconds.
     *
     * @param connectionKeepAlive
     *            the value (millis)
     */
    public void setConnectionKeepAlive(int connectionKeepAlive) {
        this.connectionKeepAlive = toTimeValueMilliseconds(connectionKeepAlive);
    }

    /**
     * Gets the maximum connections number.
     *
     * @return the value (millis)
     */
    public int getConnectionsMaxTotal() {
        return connectionsMaxTotal;
    }

    /**
     * Sets the maximum connections number.
     *
     * @param connectionsMaxTotal
     *            maximum number of connections
     */
    public void setConnectionsMaxTotal(int connectionsMaxTotal) {
        this.connectionsMaxTotal = connectionsMaxTotal;
    }

    /**
     * Gets the maximum connections number per route.
     *
     * @return maximum number of connections per one route
     */
    public int getConnectionsMaxPerRoute() {
        return connectionsMaxPerRoute;
    }

    /**
     * Sets the maximum connections number per route.
     *
     * @param connectionsMaxPerRoute
     *            maximum number of connections per one route
     */
    public void setConnectionsMaxPerRoute(int connectionsMaxPerRoute) {
        this.connectionsMaxPerRoute = connectionsMaxPerRoute;
    }

    /**
     * Gets the finite connection time to live.
     *
     * @return connection time to live (millis)
     */
    public int getConnectionTimeToLive() {
        return connectionTimeToLive.toMillisecondsIntBound();
    }

    /**
     * Sets the finite connection total time to live (TTL) in milliseconds.
     *
     * @param connectionTimeToLive
     *            the finite connection time to live (millis)
     */
    public void setConnectionTimeToLive(int connectionTimeToLive) {
        this.connectionTimeToLive = toTimeValueMilliseconds(connectionTimeToLive);
    }

    /**
     * Gets if redirect is enabled.
     *
     * @return true if http redirects are allowed
     */
    public boolean isRedirectsEnabled() {
        return redirectsEnabled;
    }

    /**
     * Sets if redirect should be enabled.
     *
     * @param redirectsEnabled
     *            true if http redirects are allowed
     */
    public void setRedirectsEnabled(boolean redirectsEnabled) {
        this.redirectsEnabled = redirectsEnabled;
    }

    /**
     * Gets if the default system network properties shall be used
     *
     * @return TRUE if the default system network properties shall be used, FALSE otherwise
     */
    public boolean isUseSystemProperties() {
        return useSystemProperties;
    }

    /**
     * Sets if the default system network properties shall be used
     *
     * Default: FALSE (system properties are not used)
     *
     * NOTE: all other configured property may override the default behavior!
     *
     * @param useSystemProperties
     *            if the default system network properties shall be used
     */
    public void setUseSystemProperties(boolean useSystemProperties) {
        this.useSystemProperties = useSystemProperties;
    }

    /**
     * This method sets the SSL protocol to be used
     *
     * @param sslProtocol
     *            the ssl protocol to be used
     */
    public void setSslProtocol(String sslProtocol) {
        this.sslProtocol = sslProtocol;
    }

    /**
     * Sets the SSL KeyStore
     *
     * @param sslKeyStore
     *            {@link DSSDocument}
     */
    public void setSslKeystore(DSSDocument sslKeyStore) {
        this.sslKeystore = sslKeyStore;
    }

    /**
     * Sets if the KeyStore shall be considered as a trust material (used for SSL connection)
     *
     * @param loadKeyStoreAsTrustMaterial
     *            if the KeyStore shall be considered as a trust material
     */
    public void setKeyStoreAsTrustMaterial(boolean loadKeyStoreAsTrustMaterial) {
        this.loadKeyStoreAsTrustMaterial = loadKeyStoreAsTrustMaterial;
    }

    /**
     * Sets the SSL KeyStore type
     *
     * @param sslKeystoreType
     *            {@link String}
     */
    public void setSslKeystoreType(String sslKeystoreType) {
        this.sslKeystoreType = sslKeystoreType;
    }

    /**
     * Sets the KeyStore password. Please note that the password shall be the same for the keystore and the extraction
     * of a corresponding key.
     *
     * @param sslKeystorePassword
     *            char array representing the password
     */
    public void setSslKeystorePassword(char[] sslKeystorePassword) {
        this.sslKeystorePassword = sslKeystorePassword;
    }

    /**
     * Sets the SSL trust store
     *
     * NOTE: different from KeyStore!
     *
     * @param sslTrustStore
     *            {@link DSSDocument}
     */
    public void setSslTruststore(DSSDocument sslTrustStore) {
        this.sslTruststore = sslTrustStore;
    }

    /**
     * Sets the password for SSL truststore
     *
     * @param sslTruststorePassword
     *            char array representing a password string
     */
    public void setSslTruststorePassword(char[] sslTruststorePassword) {
        this.sslTruststorePassword = sslTruststorePassword;
    }

    /**
     * Sets the SSL TrustStore type
     *
     * @param sslTruststoreType
     *            {@link String}
     */
    public void setSslTruststoreType(String sslTruststoreType) {
        this.sslTruststoreType = sslTruststoreType;
    }

    /**
     * Returns the current instance of the authentication map
     *
     * @return a map between {@link HostConnection} and {@link UserCredentials}
     */
    public Map<HostConnection, UserCredentials> getAuthenticationMap() {
        if (authenticationMap == null) {
            authenticationMap = new HashMap<>();
        }
        return authenticationMap;
    }

    /**
     * Sets the authentication map
     *
     * NOTE: this method overrides the current instance of {@code authenticationMap}
     *
     * @param authenticationMap
     *            a map between {@link HostConnection} and {@link UserCredentials}
     */
    public void setAuthenticationMap(Map<HostConnection, UserCredentials> authenticationMap) {
        this.authenticationMap = authenticationMap;
    }

    /**
     * Adds authentication credentials to the existing {@code authenticationMap}
     *
     * @param hostConnection
     *            host connection details
     * @param userCredentials
     *            user login credentials
     * 
     * @return this (for fluent addAuthentication)
     */
    public CommonsDataHttpClient addAuthentication(HostConnection hostConnection, UserCredentials userCredentials) {
        Map<HostConnection, UserCredentials> authenticationMap = getAuthenticationMap();
        authenticationMap.put(hostConnection, userCredentials);
        return this;
    }

    /**
     * Sets whether the preemptive authentication should be used. When set to TRUE, the dataHttpClient sends
     * authentication details (i.e. user credentials) within the initial request to the remote host, instead of sending
     * the credentials only after a request from the host. Please note that the preemptive authentication should not be
     * used over an insecure connection. Default : FALSE (preemptive authentication is not used)
     *
     * @param preemptiveAuthentication
     *            whether the preemptive authentication should be used
     */
    public void setPreemptiveAuthentication(boolean preemptiveAuthentication) {
        this.preemptiveAuthentication = preemptiveAuthentication;
    }

    /**
     * Adds authentication credentials to the existing {@code authenticationMap}
     *
     * @param host
     *            host
     * @param port
     *            port
     * @param scheme
     *            scheme
     * @param login
     *            login
     * @param password
     *            password
     * 
     * @return this (for fluent addAuthentication)
     */
    public CommonsDataHttpClient addAuthentication(final String host, final int port, final String scheme,
            final String login, final char[] password) {
        final HostConnection hostConnection = new HostConnection(host, port, scheme);
        final UserCredentials userCredentials = new UserCredentials(login, password);
        return addAuthentication(hostConnection, userCredentials);
    }

    /**
     * Sets a custom retry strategy
     *
     * @param retryStrategy
     *            {@link HttpRequestRetryStrategy}
     */
    public void setRetryStrategy(final HttpRequestRetryStrategy retryStrategy) {
        this.retryStrategy = retryStrategy;
    }

    /**
     * Gets supported SSL protocols
     *
     * @return an array if {@link String}s
     */
    public String[] getSupportedSSLProtocols() {
        return supportedSSLProtocols;
    }

    /**
     * Sets supported SSL protocols
     *
     * @param supportedSSLProtocols
     *            an array if {@link String}s
     */
    public void setSupportedSSLProtocols(String[] supportedSSLProtocols) {
        this.supportedSSLProtocols = supportedSSLProtocols;
    }

    /**
     * Gets supported SSL Cipher Suites
     *
     * @return an array if {@link String}s
     */
    public String[] getSupportedSSLCipherSuites() {
        return supportedSSLCipherSuites;
    }

    /**
     * Sets supported SSL Cipher Suites
     *
     * @param supportedSSLCipherSuites
     *            an array if {@link String}s
     */
    public void setSupportedSSLCipherSuites(String[] supportedSSLCipherSuites) {
        this.supportedSSLCipherSuites = supportedSSLCipherSuites;
    }

    /**
     * Gets the hostname verifier
     *
     * @return {@link HostnameVerifier}
     */
    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    /**
     * Sets a custom {@code HostnameVerifier}
     *
     * @param hostnameVerifier
     *            {@link HostnameVerifier}
     */
    public void setHostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
    }

    /**
     * Gets the TrustStrategy
     *
     * @return {@link TrustStrategy}
     */
    public TrustStrategy getTrustStrategy() {
        return trustStrategy;
    }

    /**
     * Sets the {@code TrustStrategy}
     *
     * @param trustStrategy
     *            {@link TrustStrategy}
     */
    public void setTrustStrategy(TrustStrategy trustStrategy) {
        this.trustStrategy = trustStrategy;
    }

    /**
     * Returns the {@code HttpClientResponseHandler} response handler
     *
     * @return {@link HttpClientResponseHandler}
     */
    public HttpClientResponseHandler<byte[]> getHttpClientResponseHandler() {
        return httpClientResponseHandler;
    }

    /**
     * Sets the {@code HttpClientResponseHandler<byte[]>} response handler performing a processing of an HTTP
     * dataHttpClient response and returns a byte array in case of success.
     *
     * @param httpClientResponseHandler
     *            {@link HttpClientResponseHandler}
     */
    public void setHttpClientResponseHandler(HttpClientResponseHandler<byte[]> httpClientResponseHandler) {
        Objects.requireNonNull(httpClientResponseHandler, "HttpClientResponseHandler cannot be null!");
        this.httpClientResponseHandler = httpClientResponseHandler;
    }

    /**
     * Gets the {@code HttpHost}
     *
     * @param httpRequest
     *            {@link HttpUriRequest}
     * 
     * @return {@link HttpHost}
     */
    protected HttpHost getHttpHost(final HttpUriRequest httpRequest) {
        try {
            final URI uri = httpRequest.getUri();
            return new HttpHost(uri.getScheme(), uri.getHost(), uri.getPort());
        } catch (URISyntaxException e) {
            throw new DSSExternalResourceException(String.format("Invalid URI : %s", e.getMessage()), e);
        }
    }

    /**
     * Gets the {@code HttpContext}
     *
     * @param httpHost
     *            {@link HttpHost}
     * 
     * @return {@link HttpContext}
     */
    protected HttpContext getHttpContext(HttpHost httpHost) {
        HttpClientContext localContext = HttpClientContext.create();
        localContext = configurePreemptiveAuthentication(localContext, httpHost);
        return localContext;
    }

    /**
     * This method is used to configure preemptive authentication process for {@code HttpClientContext}, when required
     *
     * @param localContext
     *            {@link HttpClientContext}
     * @param httpHost
     *            {@link HttpHost}
     * 
     * @return {@link HttpClientContext}
     */
    protected HttpClientContext configurePreemptiveAuthentication(HttpClientContext localContext, HttpHost httpHost) {
        if (preemptiveAuthentication && Utils.isMapNotEmpty(getAuthenticationMap())) {
            Credentials credentials = getCredentialsProvider().getCredentials(new AuthScope(httpHost), localContext);
            BasicScheme basicScheme = new BasicScheme();
            basicScheme.initPreemptive(credentials);
            localContext.resetAuthExchange(httpHost, basicScheme);
        }
        return localContext;
    }

    /**
     * Closes all the parameters quietly
     *
     * @param httpRequest
     *            {@link HttpUriRequestBase}
     * @param client
     *            {@link CloseableHttpClient}
     */
    protected void closeQuietly(HttpUriRequestBase httpRequest, CloseableHttpClient client) {
        try {
            if (httpRequest != null) {
                httpRequest.cancel();
            }
        } finally {
            Utils.closeQuietly(client);
        }
    }

    private HttpClientConnectionManager getConnectionManager() {
        final PoolingHttpClientConnectionManagerBuilder builder = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(getConnectionSocketFactoryHttps()).setDefaultSocketConfig(getSocketConfig())
                .setMaxConnTotal(getConnectionsMaxTotal()).setMaxConnPerRoute(getConnectionsMaxPerRoute());

        final ConnectionConfig.Builder connectionConfigBuilder = ConnectionConfig.custom()
                .setConnectTimeout(timeoutConnection).setTimeToLive(connectionTimeToLive);

        final PoolingHttpClientConnectionManager connectionManager = builder.build();
        connectionManager.setDefaultConnectionConfig(connectionConfigBuilder.build());

        LOG.atDebug().log("PoolingHttpClientConnectionManager: max total: {}", connectionManager.getMaxTotal());
        LOG.atDebug().log("PoolingHttpClientConnectionManager: max per route: {}",
                connectionManager.getDefaultMaxPerRoute());

        return connectionManager;
    }

    private SocketConfig getSocketConfig() {
        SocketConfig.Builder socketConfigBuilder = SocketConfig.custom();
        socketConfigBuilder.setSoTimeout(timeoutSocket);
        return socketConfigBuilder.build();
    }

    private SSLConnectionSocketFactory getConnectionSocketFactoryHttps() {
        try {
            SSLContextBuilder sslContextBuilder = SSLContextBuilder.create();
            sslContextBuilder.setProtocol(sslProtocol);

            final TrustStrategy trustStrategy = getTrustStrategy();
            if (trustStrategy != null) {
                LOG.atDebug().log("Set the TrustStrategy");
                sslContextBuilder.loadTrustMaterial(null, trustStrategy);
            }

            final KeyStore sslTrustStore = getSSLTrustStore();
            if (sslTrustStore != null) {
                LOG.atDebug().log("Set the SSL trust store as trust materials");
                sslContextBuilder.loadTrustMaterial(sslTrustStore, trustStrategy);
            }

            final KeyStore sslKeystore = getSSLKeyStore();
            if (sslKeystore != null) {
                LOG.atDebug().log("Set the SSL keystore as key materials");
                sslContextBuilder.loadKeyMaterial(sslKeystore, sslKeystorePassword);
                if (loadKeyStoreAsTrustMaterial) {
                    LOG.atDebug().log("Set the SSL keystore as trust materials");
                    sslContextBuilder.loadTrustMaterial(sslKeystore, trustStrategy);
                }
            }

            SSLConnectionSocketFactoryBuilder sslConnectionSocketFactoryBuilder = new SSLConnectionSocketFactoryBuilder();
            return sslConnectionSocketFactoryBuilder.setSslContext(sslContextBuilder.build())
                    .setTlsVersions(getSupportedSSLProtocols()).setCiphers(getSupportedSSLCipherSuites())
                    .setHostnameVerifier(getHostnameVerifier()).build();

        } catch (final Exception e) {
            throw new IllegalArgumentException("Unable to configure the SSLContext/SSLConnectionSocketFactory", e);
        }
    }

    /**
     * Gets the SSL KeyStore
     *
     * @return {@link KeyStore}
     * 
     * @throws IOException
     *             if IOException occurs
     * @throws GeneralSecurityException
     *             if GeneralSecurityException occurs
     */
    protected KeyStore getSSLKeyStore() throws IOException, GeneralSecurityException {
        return loadKeyStore(sslKeystore, sslKeystoreType, sslKeystorePassword);
    }

    /**
     * Gets the SSL Trusted KeyStore
     *
     * @return {@link KeyStore}
     * 
     * @throws IOException
     *             if IOException occurs
     * @throws GeneralSecurityException
     *             if GeneralSecurityException occurs
     */
    protected KeyStore getSSLTrustStore() throws IOException, GeneralSecurityException {
        return loadKeyStore(sslTruststore, sslTruststoreType, sslTruststorePassword);
    }

    private KeyStore loadKeyStore(DSSDocument store, String type, char[] password)
            throws IOException, GeneralSecurityException {
        if (store != null) {
            try (InputStream is = store.openStream()) {
                KeyStore ks = KeyStore.getInstance(type);
                ks.load(is, password);
                return ks;
            }
        } else {
            return null;
        }
    }

    /**
     * Gets the {@code HttpClientBuilder}
     *
     * 
     * @return {@link HttpClientBuilder}
     */
    protected HttpClientBuilder getHttpClientBuilder() {
        HttpClientBuilder httpClientBuilder = HttpClients.custom();

        if (useSystemProperties) {
            httpClientBuilder.useSystemProperties();
        }

        httpClientBuilder = configCredentials(httpClientBuilder);

        final RequestConfig.Builder requestConfigBuilder = RequestConfig.custom()
                .setConnectionRequestTimeout(timeoutConnectionRequest).setResponseTimeout(timeoutResponse)
                .setConnectionKeepAlive(connectionKeepAlive).setRedirectsEnabled(redirectsEnabled);

        httpClientBuilder.setConnectionManager(getConnectionManager())
                .setDefaultRequestConfig(requestConfigBuilder.build()).setRetryStrategy(retryStrategy);

        return httpClientBuilder;
    }

    /**
     * Create the HTTP dataHttpClient
     *
     * @return {@link CloseableHttpClient}
     */
    protected CloseableHttpClient createHttpClient() {
        return getHttpClientBuilder().build();
    }

    /**
     * Defines the Credentials
     *
     * @param httpClientBuilder
     *            {@link HttpClientBuilder}
     * @param url
     *            {@link String}
     * 
     * @return {@link HttpClientBuilder}
     */
    private HttpClientBuilder configCredentials(HttpClientBuilder httpClientBuilder) {
        final BasicCredentialsProvider credentialsProvider = getCredentialsProvider();
        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        return httpClientBuilder;
    }

    /**
     * Builds and returns a {@code BasicCredentialsProvider} configured with {@code authenticationMap}
     *
     * @return {@link BasicCredentialsProvider}
     */
    protected BasicCredentialsProvider getCredentialsProvider() {
        final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        for (final Map.Entry<HostConnection, UserCredentials> entry : getAuthenticationMap().entrySet()) {
            final HostConnection hostConnection = entry.getKey();
            final UserCredentials userCredentials = entry.getValue();
            final AuthScope authscope = new AuthScope(hostConnection.getProtocol(), hostConnection.getHost(),
                    hostConnection.getPort(), hostConnection.getRealm(), hostConnection.getScheme());

            final UsernamePasswordCredentials usernamePasswordCredentials = new UsernamePasswordCredentials(
                    userCredentials.getUsername(), userCredentials.getPassword());
            credentialsProvider.setCredentials(authscope, usernamePasswordCredentials);
        }
        return credentialsProvider;
    }

    private static Timeout toTimeoutMilliseconds(int millis) {
        if (millis < 0) {
            LOG.info("A negative timeout has been provided. Use system default.");
            return null;
        }
        return Timeout.ofMilliseconds(millis);
    }

    private static TimeValue toTimeValueMilliseconds(int millis) {
        return TimeValue.ofMilliseconds(millis);
    }

    /**
     * Standard HTTP Apache Client
     * 
     * @return dataHttpClient
     */
    public CloseableHttpClient getHttpClient() {
        return client;
    }

}
