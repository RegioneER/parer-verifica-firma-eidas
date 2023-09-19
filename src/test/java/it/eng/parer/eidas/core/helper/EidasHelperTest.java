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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.ResourceUtils;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import it.eng.parer.eidas.web.VerificaFirmaEidasApplication;

/**
 * <p>
 * EidasHelperTest class.
 * </p>
 *
 * @author stefano
 * 
 * @version $Id: $Id
 * 
 * @since 1.10.1
 */
@SpringBootTest(classes = VerificaFirmaEidasApplication.class, properties = { "logging.level.root=INFO",
        "logging.level.it.eng.parer=INFO", "spring.datasource.url=jdbc:h2:mem:eidasdb-test;DB_CLOSE_DELAY=-1" })
class EidasHelperTest {

    @Autowired
    private EidasHelper eidasHelper;

    /**
     * <p>
     * testMimeTypeOffice.
     * </p>
     *
     * @throws java.io.IOException
     *             if any.
     */
    @Test
    void testMimeTypeOffice() throws IOException {
        String fileWithoutSignature = ResourceUtils.getURL("classpath:NOT_SIGNED/office_non_firmato").getFile();
        DSSDocument input = new FileDocument(fileWithoutSignature);

        String expetedMimeType = "application/msword";
        String actualMimeType = eidasHelper.detectMimeType(input);
        assertEquals(expetedMimeType, actualMimeType);
    }

}
