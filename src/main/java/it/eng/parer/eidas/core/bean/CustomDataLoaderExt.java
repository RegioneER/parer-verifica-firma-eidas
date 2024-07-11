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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Hashtable;
import java.util.StringTokenizer;

import javax.naming.Context;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.BufferedHttpEntity;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.slf4j.Logger;

import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.service.http.commons.LdapURLUtils;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.spi.exception.DSSExternalResourceException;
import eu.europa.esig.dss.utils.Utils;

public interface CustomDataLoaderExt {

    static final String CONTENT_TYPE = "Content-Type";

    static final String TIMEOUT_LDAP_CONNECTION = "6000";

    /**
     * Extendend OCSPDataLoader method, not implemented TIMEOUT on LDPA call
     * 
     * This method retrieves data using LDAP protocol. - CRL from given LDAP url, e.g.
     * ldap://ldap.infonotary.com/dc=identity-ca,dc=infonotary,dc=com - ex URL from AIA
     * ldap://xadessrv.plugtests.net/CN=LevelBCAOK,OU=Plugtests_2015-2016,O=ETSI,C=FR?cACertificate;binary
     *
     * @param urlString
     *            LDAP url string resource
     * 
     * @return byte[]
     */
    public byte[] ldapGet(final String urlString);

    default byte[] customLdapGet(String urlString) {

        urlString = LdapURLUtils.encode(urlString);

        final Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put("com.sun.jndi.ldap.read.timeout", getLdapTimeoutConnection());
        env.put(Context.PROVIDER_URL, urlString);
        try {

            // parse URL according to the template:
            // 'ldap://host:port/DN?attributes?scope?filter?extensions'
            String ldapParams = Utils.substringAfter(urlString, "?");
            StringTokenizer tokenizer = new StringTokenizer(ldapParams, "?");
            String attributeName = (tokenizer.hasMoreTokens()) ? tokenizer.nextToken() : null;

            if (Utils.isStringEmpty(attributeName)) {
                // default was CRL
                attributeName = "certificateRevocationList;binary";
            }

            final DirContext ctx = new InitialDirContext(env);
            final Attributes attributes = ctx.getAttributes(Utils.EMPTY_STRING, new String[] { attributeName });
            if ((attributes == null) || (attributes.size() < 1)) {
                throw new DSSException(
                        String.format("Cannot download binaries from: [%s], no attributes with name: [%s] returned",
                                urlString, attributeName));
            } else {
                final Attribute attribute = attributes.getAll().next();
                final byte[] ldapBytes = (byte[]) attribute.get();
                if (Utils.isArrayNotEmpty(ldapBytes)) {
                    return ldapBytes;
                }
                throw new DSSException(String.format("The retrieved ldap content from url [%s] is empty", urlString));
            }
        } catch (DSSException e) {
            throw e;
        } catch (Exception e) {
            throw new DSSExternalResourceException(
                    String.format("Cannot get data from URL [%s]. Reason : [%s]", urlString, e.getMessage()), e);
        }
    }

    public String getLdapTimeoutConnection();

    /* CUSTOM DATA LOADING HTTP */

    public void setCommonsDataHttpClient(CommonsDataHttpClient dataHttpClient);

    public CommonsDataHttpClient getCommonsDataHttpClient();

    /* apache dataHttpClient management */

    default byte[] customPost(String url, byte[] content) {
        logger().atDebug().log("Fetching data via POST from url {}", url);

        HttpPost httpRequest = null;
        // The length for the InputStreamEntity is needed, because some receivers (on
        // the other side)
        // need this information.
        // To determine the length, we cannot read the content-stream up to the end and
        // re-use it afterwards.
        // This is because, it may not be possible to reset the stream (= go to position
        // 0).
        // So, the solution is to cache temporarily the complete content data (as we do
        // not expect much here) in
        // a byte-array.
        try (final ByteArrayInputStream bis = new ByteArrayInputStream(content);) {
            final URI uri = URI.create(Utils.trim(url));
            httpRequest = new HttpPost(uri);

            final HttpEntity httpEntity = new InputStreamEntity(bis, content.length,
                    toContentTypeExt(getContentType()));
            final HttpEntity requestEntity = new BufferedHttpEntity(httpEntity);
            httpRequest.setEntity(requestEntity);

            return execute(getCommonsDataHttpClient().getHttpClient(), httpRequest);

        } catch (IOException e) {
            throw new DSSExternalResourceException(
                    String.format("Unable to process POST call for url [%s]. Reason : [%s]", url, e.getMessage()), e);

        } finally {
            if (httpRequest != null) {
                httpRequest.cancel();
            }
        }
    }

    default byte[] customHttpGet(String url) {
        logger().atDebug().log("Fetching data via GET from url {}", url);

        HttpGet httpRequest = null;

        try {
            httpRequest = customGetHttpRequest(url);
            return execute(getCommonsDataHttpClient().getHttpClient(), httpRequest);

        } catch (URISyntaxException | IOException e) {
            throw new DSSExternalResourceException(String.format(
                    "Unable to process GET call for url [%s]. Reason : [%s]", url, DSSUtils.getExceptionMessage(e)), e);

        } finally {
            if (httpRequest != null) {
                httpRequest.cancel();
            }
        }
    }

    default HttpGet customGetHttpRequest(String url) throws URISyntaxException {
        final URI uri = new URI(Utils.trim(url));
        HttpGet httpRequest = new HttpGet(uri);
        if (getContentType() != null) {
            httpRequest.setHeader(CONTENT_TYPE, getContentType());
        }
        return httpRequest;
    }

    default ContentType toContentTypeExt(String contentTypeString) {
        return Utils.isStringNotBlank(contentTypeString) ? ContentType.create(contentTypeString) : null;
    }

    public Logger logger();

    /*
     * define standard getter & setter (inherit from {@link CommonsDataLoader})
     */

    public byte[] execute(final CloseableHttpClient client, final HttpUriRequest httpRequest) throws IOException;

    public String getContentType();

}
