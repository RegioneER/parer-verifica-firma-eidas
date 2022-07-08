package it.eng.parer.eidas.core.token;

import java.io.IOException;
import java.security.KeyStore.PasswordProtection;

import org.springframework.util.ResourceUtils;

/**
 * KeyStoreSignatureTokenConnection TSP check file tsp-config.xml
 * 
 * KeyStoreSignatureTokenConnection extend for managing ResourceUtils.getURL(ksFile) as classpath file
 */

public class EidasKeyStoreSignatureTokenConnection extends eu.europa.esig.dss.token.KeyStoreSignatureTokenConnection {

    public EidasKeyStoreSignatureTokenConnection(String ksFile, String ksType, PasswordProtection ksPassword)
            throws IOException {
        super(ResourceUtils.getURL(ksFile).openStream(), ksType, ksPassword);
    }

}
