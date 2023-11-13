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
