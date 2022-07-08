package it.eng.parer.eidas.web.rest;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
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

@Tag(name = "Errori", description = "Gestione errori applicativi")
@RestController
@RequestMapping("/v1")
public class ParerErrorCodeWs {

    @Autowired
    IErrorCodeManager errorCodeManager;

    @Operation(summary = "Errors", method = "Lista dei codici di errore dell'applicazione", tags = { "errors" })
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Lista codici di errore", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = ParerErrorDoc[].class)) }) })
    @GetMapping(value = { "/errors" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ParerErrorDoc> docs(HttpServletRequest request) {

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

        return docs;
    }

    @Operation(summary = "Documents", method = "Dettaglio codice di errore by code", tags = { "errors" })
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Dettaglio codice di errore", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = ParerErrorDoc.class)) }) })
    @GetMapping(value = { "/errors/{code}" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ParerErrorDoc docs(@PathVariable("code") String errorCode, HttpServletRequest request) {
        ParerErrorDoc doc = new ParerErrorDoc();
        doc.setCode(errorCode);
        doc.setSummary(errorCodeManager.getSummary(errorCode));
        doc.setDescription(errorCodeManager.getDescription(errorCode));
        doc.setLink(request.getRequestURL().toString()); // Nota: fino ad error-code
        return doc;
    }

}
