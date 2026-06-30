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

package it.eng.parer.eidas.core.helper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import it.eng.parer.eidas.model.EidasDataToValidateMetadata;
import it.eng.parer.eidas.model.EidasRemoteDocument;

/**
 * Test di unità per {@link EidasMetadaValidator}.
 * <p>
 * Non richiede contesto Spring: il validator non ha dipendenze iniettate.
 */
class EidasMetadaValidatorTest {

    private EidasMetadaValidator validator;

    @BeforeEach
    void setUp() {
        validator = new EidasMetadaValidator();
    }

    // --- supports() ---

    @Test
    void supports_conClasseCorretta_restituesceTrue() {
        assertTrue(validator.supports(EidasDataToValidateMetadata.class));
    }

    @Test
    void supports_conClasseDiversa_restituesceFalse() {
        assertFalse(validator.supports(Object.class));
        assertFalse(validator.supports(String.class));
    }

    // --- validate() ---

    @Test
    void validate_conURINull_registraErrore() {
        EidasDataToValidateMetadata dto = buildDtoWithUri(null);
        Errors errors = validate(dto);

        assertTrue(errors.hasErrors());
    }

    @Test
    void validate_conURIHttp_nessunErrore() throws Exception {
        EidasDataToValidateMetadata dto = buildDtoWithUri(new URI("http://example.com/doc.pdf"));
        Errors errors = validate(dto);

        assertFalse(errors.hasErrors());
    }

    @Test
    void validate_conURIHttps_nessunErrore() throws Exception {
        EidasDataToValidateMetadata dto = buildDtoWithUri(new URI("https://example.com/doc.pdf"));
        Errors errors = validate(dto);

        assertFalse(errors.hasErrors());
    }

    @Test
    void validate_conURIFtp_registraErrore() throws Exception {
        EidasDataToValidateMetadata dto = buildDtoWithUri(new URI("ftp://example.com/doc.pdf"));
        Errors errors = validate(dto);

        assertTrue(errors.hasErrors());
    }

    @Test
    void validate_conURIFileScheme_registraErrore() throws Exception {
        EidasDataToValidateMetadata dto = buildDtoWithUri(new URI("file:///tmp/doc.pdf"));
        Errors errors = validate(dto);

        assertTrue(errors.hasErrors());
    }

    @Test
    void validate_conURIRelativo_registraErrore() throws Exception {
        // URI relativo non ha scheme → toURL() lancia eccezione → isValidUri() → false
        EidasDataToValidateMetadata dto = buildDtoWithUri(new URI("relative/path/doc.pdf"));
        Errors errors = validate(dto);

        assertTrue(errors.hasErrors());
    }

    // --- helpers ---

    private EidasDataToValidateMetadata buildDtoWithUri(URI uri) {
        EidasDataToValidateMetadata dto = new EidasDataToValidateMetadata();
        EidasRemoteDocument doc = new EidasRemoteDocument();
        doc.setUri(uri);
        dto.setRemoteSignedDocument(doc);
        return dto;
    }

    private Errors validate(EidasDataToValidateMetadata dto) {
        Errors errors = new BeanPropertyBindingResult(dto, "dto");
        validator.validate(dto, errors);
        return errors;
    }
}
