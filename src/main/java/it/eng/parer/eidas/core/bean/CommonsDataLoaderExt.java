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

package it.eng.parer.eidas.core.bean;

import java.io.IOException;

import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.esig.dss.service.http.commons.CommonsDataLoader;

public class CommonsDataLoaderExt extends CommonsDataLoader implements CustomDataLoaderExt {

    private static final long serialVersionUID = -272512490031055464L;

    private static final Logger LOG = LoggerFactory.getLogger(CommonsDataLoaderExt.class);

    // ** LDAP

    private String ldapTimeoutConnection = TIMEOUT_LDAP_CONNECTION;

    /** The Commons HTTP dataHttpClient */
    private CommonsDataHttpClient dataHttpClient;

    /**
     * Sovrascritto il metodo del padre in quanto non implementava un TIMEOUT nelle chiamate LDAP
     *
     * This method retrieves data using LDAP protocol. - CRL from given LDAP url, e.g.
     * ldap://ldap.infonotary.com/dc=identity-ca,dc=infonotary,dc=com - ex URL from AIA
     * ldap://xadessrv.plugtests.net/CN=LevelBCAOK,OU=Plugtests_2015-2016,O=ETSI,C=FR?cACertificate;binary
     *
     * @param urlString URL LDAP resource
     *
     * @return byte[]
     */
    @Override
    public byte[] ldapGet(final String urlString) {
	return customLdapGet(urlString);
    }

    public String getLdapTimeoutConnection() {
	return ldapTimeoutConnection;
    }

    public void setLdapTimeoutConnection(String ldapTimeoutConnection) {
	this.ldapTimeoutConnection = ldapTimeoutConnection;
    }

    /* HTTP GET */
    @Override
    protected byte[] httpGet(String url) {
	return customHttpGet(url);
    }

    /* HTTP POST */
    @Override
    public byte[] post(String url, byte[] content) {
	return customPost(url, content);
    }

    @Override
    public byte[] execute(CloseableHttpClient client, HttpUriRequest httpRequest)
	    throws IOException {
	return super.execute(client, httpRequest);
    }

    @Override
    public void setCommonsDataHttpClient(CommonsDataHttpClient dataHttpClient) {
	this.dataHttpClient = dataHttpClient;
    }

    @Override
    public CommonsDataHttpClient getCommonsDataHttpClient() {
	return dataHttpClient;
    }

    @Override
    public Logger logger() {
	return LOG;
    }

}
