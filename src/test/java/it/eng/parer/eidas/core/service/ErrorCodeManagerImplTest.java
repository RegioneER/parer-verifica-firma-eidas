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

package it.eng.parer.eidas.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import it.eng.parer.eidas.model.exception.ParerError.ErrorCode;

/**
 * Test di unità per {@link ErrorCodeManagerImpl}.
 * <p>
 * Non richiede contesto Spring: nessuna dipendenza iniettata.
 */
class ErrorCodeManagerImplTest {

    private ErrorCodeManagerImpl manager;

    @BeforeEach
    void setUp() {
        manager = new ErrorCodeManagerImpl();
    }

    // --- get() ---

    @Test
    void get_escludeNOT_FOUND() {
        List<ErrorCode> codes = manager.get();
        assertFalse(codes.contains(ErrorCode.NOT_FOUND));
    }

    @Test
    void get_contieneEsattamenteTuttiGliAltriCodici() {
        List<ErrorCode> codes = manager.get();
        long expected = Stream.of(ErrorCode.values()).filter(e -> !e.equals(ErrorCode.NOT_FOUND))
                .count();
        assertEquals(expected, codes.size());
    }

    // --- getSummary() ---

    @ParameterizedTest
    @MethodSource("provideGetSummaryArgs")
    void getSummary_contieneDecodificaTipologia(String urlFriendly, String expectedPart) {
        String result = manager.getSummary(urlFriendly);
        assertTrue(result.contains(expectedPart), "getSummary('" + urlFriendly
                + "') deve contenere '" + expectedPart + "' ma era: " + result);
    }

    static Stream<Arguments> provideGetSummaryArgs() {
        return Stream.of(Arguments.of("unhandled-exception", "generico"),
                Arguments.of("validation-error", "validation"), Arguments.of("io-exception", "I/O"),
                Arguments.of("bad-name-on-multipartfile-and-metadata", "request"),
                Arguments.of("eidas-exception", "dss lib"),
                Arguments.of("metadata-exception", "dss lib"),
                // NOT_FOUND è escluso da get() ma fromUrlFriendly lo restituisce come fallback
                Arguments.of("not-found", "non gestito"));
    }

    @Test
    void getSummary_codiceSconosciuto_restituisceNOT_FOUND() {
        // fromUrlFriendly restituisce NOT_FOUND come fallback (non lancia eccezione)
        String result = manager.getSummary("codice-inesistente");
        assertTrue(result.contains("NOT_FOUND"),
                "Un codice sconosciuto deve fare fallback su NOT_FOUND, era: " + result);
    }

    // --- getDescription() ---

    @ParameterizedTest
    @MethodSource("provideGetDescriptionArgs")
    void getDescription_restituisceMessaggioCorretto(String urlFriendly, String expectedMsg) {
        String result = manager.getDescription(urlFriendly);
        assertEquals(expectedMsg, result);
    }

    static Stream<Arguments> provideGetDescriptionArgs() {
        return Stream.of(Arguments.of("unhandled-exception", "Errore generico di sistema"),
                Arguments.of("eidas-exception", "Verifica del documento firmato non riuscita"),
                Arguments.of("io-exception", "Errore generico di sistema (I/O)"),
                Arguments.of("bad-name-on-multipartfile-and-metadata",
                        "Errore su chiamata multipart/form-data, nome file elemento non trovato su metadati"),
                Arguments.of("metadata-exception",
                        "Errore sui metadati, assente uno o più campi obbligatori"),
                Arguments.of("validation-error",
                        "Errore di validazione, il formato di uno o più campi risulta errato"));
    }

    @Test
    void getDescription_codiceSconosciuto_restituisceMessaggioFallback() {
        // NOT_FOUND cade nel case default con messaggio "Errore non trovato <code>"
        String result = manager.getDescription("codice-inesistente");
        assertTrue(result.startsWith("Errore non trovato"),
                "Codice sconosciuto deve produrre messaggio di fallback, era: " + result);
    }

}
