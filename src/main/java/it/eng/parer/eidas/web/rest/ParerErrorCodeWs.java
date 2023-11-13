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

package it.eng.parer.eidas.web.rest;

import static it.eng.parer.eidas.web.util.EndPointCostants.URL_API_BASE;
import static it.eng.parer.eidas.web.util.EndPointCostants.RESOURCE_ERRORS;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.eng.parer.eidas.core.service.IErrorCodeManager;
import it.eng.parer.eidas.model.exception.ParerError;
import it.eng.parer.eidas.model.exception.ParerErrorDoc;
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "Errori", description = "Gestione errori applicativi")
@RestController
@RequestMapping(URL_API_BASE)
public class ParerErrorCodeWs {

    /* constants */
    private static final String ETAG = "v1.0";

    @Autowired
    IErrorCodeManager errorCodeManager;

    @Operation(summary = "Errors", method = "Lista dei codici di errore dell'applicazione")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Lista codici di errore", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = ParerErrorDoc[].class)) }) })
    @GetMapping(value = { RESOURCE_ERRORS }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ParerErrorDoc>> docs(HttpServletRequest request) {

        List<ParerErrorDoc> docs = new ArrayList<>();
        for (ParerError.ErrorCode code : errorCodeManager.get()) {
            ParerErrorDoc doc = new ParerErrorDoc();
            doc.setCode(code.urlFriendly());
            doc.setSummary(errorCodeManager.getSummary(code.urlFriendly()));
            doc.setDescription(errorCodeManager.getDescription(code.urlFriendly()));
            doc.setLink(request.getRequestURL().toString() + "/" + code.urlFriendly());// Nota: /docs/{error-code} - no
            // redirect
            docs.add(doc);
        }

        return ResponseEntity.ok().lastModified(LocalDateTime.now().atZone(ZoneId.systemDefault())).eTag(ETAG)
                .body(docs);
    }

    @Operation(summary = "Documents", method = "Dettaglio codice di errore by code")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Dettaglio codice di errore", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = ParerErrorDoc.class)) }) })
    @GetMapping(value = { RESOURCE_ERRORS + "/{code}" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ParerErrorDoc> docs(@PathVariable("code") String errorCode, HttpServletRequest request) {
        ParerErrorDoc doc = new ParerErrorDoc();
        doc.setCode(errorCode);
        doc.setSummary(errorCodeManager.getSummary(errorCode));
        doc.setDescription(errorCodeManager.getDescription(errorCode));
        doc.setLink(request.getRequestURL().toString()); // Nota: fino ad error-code

        return ResponseEntity.ok().lastModified(LocalDateTime.now().atZone(ZoneId.systemDefault())).eTag(ETAG)
                .body(doc);
    }

}
