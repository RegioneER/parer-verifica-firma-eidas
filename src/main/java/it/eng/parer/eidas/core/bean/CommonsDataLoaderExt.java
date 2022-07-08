package it.eng.parer.eidas.core.bean;

import eu.europa.esig.dss.service.http.commons.CommonsDataLoader;
import it.eng.parer.eidas.core.util.Constants;

public class CommonsDataLoaderExt extends CommonsDataLoader implements CustomDataLoaderExt {

    private static final long serialVersionUID = -272512490031055464L;

    private String ldapTimeoutConnection = Constants.TIMEOUT_LDAP_CONNECTION;

    /**
     * Sovrascritto il metodo del padre in quanto non implementava un TIMEOUT nelle chiamate LDAP
     * 
     * This method retrieves data using LDAP protocol. - CRL from given LDAP url, e.g.
     * ldap://ldap.infonotary.com/dc=identity-ca,dc=infonotary,dc=com - ex URL from AIA
     * ldap://xadessrv.plugtests.net/CN=LevelBCAOK,OU=Plugtests_2015-2016,O=ETSI,C=FR?cACertificate;binary
     *
     * @param urlString
     *            URL LDAP resource
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

}
