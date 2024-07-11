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

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class ApacheClientHelper {

    /*
     * Standard client
     */
    // default 60 s
    @Value("${parer.eidas.uriloader.httpclient.timeout:60}")
    long httpClientTimeout;

    // default 60 s
    @Value("${parer.eidas.uriloader.httpclient.timeoutsocket:60}")
    int httpClientSocketTimeout;

    // default 4
    @Value("${parer.eidas.uriloader.httpclient.connectionsmaxperroute:4}")
    int httpClientConnectionsmaxperroute;

    // default 40
    @Value("${parer.eidas.uriloader.httpclient.connectionsmax:40}")
    int httpClientConnectionsmax;

    // default 60s
    @Value("${parer.eidas.uriloader.httpclient.timetolive:60}")
    long httpClientTimeToLive;

    // default false
    @Value("${parer.eidas.uriloader.httpclient.no-ssl-verify:false}")
    boolean noSslVerify;

    private CloseableHttpClient client;

    @PostConstruct
    public void init() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        // client
        HttpClientBuilder httpClientBuilder = HttpClients.custom();

        final RequestConfig.Builder requestConfigBuilder = RequestConfig.custom()
                .setConnectionRequestTimeout(httpClientTimeout, TimeUnit.SECONDS)
                .setResponseTimeout(httpClientTimeout, TimeUnit.SECONDS)
                .setConnectionKeepAlive(TimeValue.ofSeconds(httpClientTimeToLive));

        httpClientBuilder.setConnectionManager(getConnectionManager())
                .setDefaultRequestConfig(requestConfigBuilder.build());

        client = httpClientBuilder.build();
    }

    private HttpClientConnectionManager getConnectionManager()
            throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {

        final PoolingHttpClientConnectionManagerBuilder builder = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultSocketConfig(getSocketConfig()).setMaxConnTotal(httpClientConnectionsmax)
                .setMaxConnPerRoute(httpClientConnectionsmaxperroute);
        // ssl
        if (noSslVerify) {
            builder.setSSLSocketFactory(getSSLConnectionSocketFactoryIgnoreSSLValidation());
        }

        final ConnectionConfig.Builder connectionConfigBuilder = ConnectionConfig.custom()
                .setConnectTimeout(httpClientSocketTimeout, TimeUnit.SECONDS)
                .setTimeToLive(httpClientTimeToLive, TimeUnit.SECONDS);

        final PoolingHttpClientConnectionManager connectionManager = builder.build();
        connectionManager.setDefaultConnectionConfig(connectionConfigBuilder.build());

        return connectionManager;
    }

    private SocketConfig getSocketConfig() {
        SocketConfig.Builder socketConfigBuilder = SocketConfig.custom();
        socketConfigBuilder.setSoTimeout(Timeout.ofSeconds(httpClientTimeout));
        return socketConfigBuilder.build();
    }

    private SSLConnectionSocketFactory getSSLConnectionSocketFactoryIgnoreSSLValidation()
            throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        return SSLConnectionSocketFactoryBuilder.create()
                .setSslContext(SSLContextBuilder.create().loadTrustMaterial(TrustAllStrategy.INSTANCE).build())
                .setHostnameVerifier(NoopHostnameVerifier.INSTANCE).build();
    }

    @PreDestroy
    public void destroy() throws IOException {
        client.close();
    }

    public CloseableHttpClient client() {
        return client;
    }

}
