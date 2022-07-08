package it.eng.parer.eidas.core.bean;

import java.util.Hashtable;
import java.util.StringTokenizer;

import javax.naming.Context;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.service.http.commons.LdapURLUtils;
import eu.europa.esig.dss.spi.exception.DSSExternalResourceException;
import eu.europa.esig.dss.utils.Utils;

public interface CustomDataLoaderExt {

    static final Logger LOG = LoggerFactory.getLogger(CustomDataLoaderExt.class);

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

}
