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
