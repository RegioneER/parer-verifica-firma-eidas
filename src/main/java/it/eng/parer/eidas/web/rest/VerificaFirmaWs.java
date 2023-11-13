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

import static it.eng.parer.eidas.web.util.EndPointCostants.RESOURCE_REPORT_VERIFICA;
import static it.eng.parer.eidas.web.util.EndPointCostants.URL_API_BASE;

import java.util.Optional;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.eng.parer.eidas.core.helper.EidasMetadaValidator;
import it.eng.parer.eidas.core.service.IErrorCodeManager;
import it.eng.parer.eidas.core.service.IVerificaFirma;
import it.eng.parer.eidas.core.util.Constants;
import it.eng.parer.eidas.model.EidasDataToValidateMetadata;
import it.eng.parer.eidas.model.EidasWSReportsDTOTree;
import it.eng.parer.eidas.web.bean.RestExceptionResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@Tag(name = "Verifica", description = "Report verifica firma")
@RestController
@Validated
@RequestMapping(URL_API_BASE)
public class VerificaFirmaWs {

    /* constants */
    private static final String ETAG = "RVv1.0";

    @Autowired
    IVerificaFirma verificaFirma;

    @Autowired
    IErrorCodeManager errorCodeManager;

    @Autowired
    EidasMetadaValidator eidasMetadaValidator;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.addValidators(eidasMetadaValidator);
    }

    /**
     * Versione 1 in cui si accetta in ingresso il JSON con i byte array dei file da validare
     *
     * @param metadata
     *            oggetto json con i dati da validare
     * @param request
     *            oggetto standard contente la request
     * 
     * @return EidasWSReportsDTOTree oggetto custom tree con risultati della verifica
     */
    @Operation(summary = "Report con verifica firma", method = "Effettua la verifica del file passato in input. Accetta un JSON con i metadati relativi alla verifica. La risorsa ottenuta da questa chiamata è il report di verifica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Esito verifica documento firmato", content = {
                    @Content(mediaType = "application/xml", schema = @Schema(implementation = EidasWSReportsDTOTree.class)) }),
            @ApiResponse(responseCode = "500", description = "Documento firmato non riconosciuto", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionResponse.class)) }) })
    @PostMapping(value = {
            RESOURCE_REPORT_VERIFICA }, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<EidasWSReportsDTOTree> validateJson(
            @Parameter(description = "DSS DataToValidate Json", required = true) @Valid @RequestBody(required = true) EidasDataToValidateMetadata metadata,
            HttpServletRequest request) {
        // LOG UUID
        MDC.put(Constants.UUID_LOG_MDC, metadata.getUuid());
        EidasWSReportsDTOTree body = verificaFirma.validateSignatureOnJson(metadata, request);
        return ResponseEntity.ok().lastModified(body.getEndValidation().toInstant()).eTag(ETAG).body(body);
    }

    /**
     * Versione 2 in cui si accetta in ingresso un multipart/form-data con i file da validare
     *
     * @param signedFile
     *            file firmato
     * @param metadata
     *            oggetto json con i metadati a supporto della verifica firma
     * @param originalFiles
     *            lista file originali
     * @param customValidationFile
     *            file policy standard eidas
     * @param request
     *            oggetto standard contente la request
     * 
     * @return EidasWSReportsDTOTree
     */
    @Operation(summary = "Report con verifica firma", method = "Effettua la verifica del file passato in input. Accetta chiamata POST con multipart/form-data. La risorsa ottenuta da questa chiamata è il report di verifica")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Esito verifica documento firmato", content = {
                    @Content(mediaType = "application/xml", schema = @Schema(implementation = EidasWSReportsDTOTree.class)) }),
            @ApiResponse(responseCode = "400", description = "Parametri errati durante l'esecuzione della procedura", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionResponse.class)) }),
            @ApiResponse(responseCode = "417", description = "File eccede dimensioni consentite", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionResponse.class)) }),
            @ApiResponse(responseCode = "500", description = "Documento firmato non riconosciuto", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionResponse.class)) }) })
    @PostMapping(value = {
            RESOURCE_REPORT_VERIFICA }, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<EidasWSReportsDTOTree> validateMultipart(
            @Parameter(description = "File firmato", required = true) @RequestPart(name = "signedFile", required = true) MultipartFile signedFile,
            @Parameter(description = "Metadati", required = false, schema = @Schema(type = "string", format = "binary")) @RequestPart(name = "metadata", required = false) Optional<EidasDataToValidateMetadata> metadata,
            @Parameter(description = "File originale/i", required = false) @RequestPart(name = "originalFiles", required = false) MultipartFile[] originalFiles,
            @Parameter(description = "File custom policy", required = false) @RequestPart(name = "customValidationFile", required = false) MultipartFile customValidationFile,
            HttpServletRequest request) {
        // optional value
        EidasDataToValidateMetadata localMetadata = metadata.orElse(new EidasDataToValidateMetadata());
        MDC.put(Constants.UUID_LOG_MDC, localMetadata.getUuid());
        EidasWSReportsDTOTree body = verificaFirma.validateSignatureOnMultipart(localMetadata, request, signedFile,
                originalFiles, customValidationFile);
        return ResponseEntity.ok().lastModified(body.getEndValidation().toInstant()).eTag(ETAG).body(body);
    }
}
