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

package it.eng.parer.eidas.validator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.spi.DSSUtils;

public class CMSNotEtfiValidatorUtil {

    private static final String BEGIN = "-----BEGIN ";

    private CMSNotEtfiValidatorUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static CMSSignedData toCMSSignedDataAsciiArmor(final DSSDocument document) {
        try (InputStream inputStream = document.openStream()) {
            Reader reader = new InputStreamReader(inputStream);
            //
            PemReader pr = new PemReader(reader);
            PemObject pem = pr.readPemObject();
            //
            pr.close();
            reader.close();

            return new CMSSignedData(new ByteArrayInputStream(pem.getContent()));
        } catch (NullPointerException | IOException | CMSException e) {
            throw new DSSException("Not a valid CAdES (ascii armor) file", e);
        }
    }

    public static boolean isAsciiArmor(final DSSDocument dssDocument) {
        int headerLength = 22;
        byte[] preamble = new byte[headerLength];
        DSSUtils.readAvailableBytes(dssDocument, preamble);
        String preambleString = new String(preamble);
        return preambleString.startsWith(BEGIN);
    }

}
