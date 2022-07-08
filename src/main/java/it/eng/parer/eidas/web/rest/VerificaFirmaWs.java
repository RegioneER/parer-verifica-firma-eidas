package it.eng.parer.eidas.web.rest;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
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
import it.eng.parer.eidas.core.service.IErrorCodeManager;
import it.eng.parer.eidas.core.service.IVerificaFirma;
import it.eng.parer.eidas.core.util.Constants;
import it.eng.parer.eidas.model.DataToValidateDTOExt;
import it.eng.parer.eidas.model.EidasMetadataToValidate;
import it.eng.parer.eidas.model.EidasWSReportsDTOTree;
import it.eng.parer.eidas.web.bean.RestExceptionResponse;

@Tag(name = "Verifica", description = "Report verifica firma")
@RestController
@Validated
@RequestMapping("/")
public class VerificaFirmaWs {

    @Autowired
    IVerificaFirma verificaFirma;

    @Autowired
    IErrorCodeManager errorCodeManager;

    /**
     * Versione 1 in cui si accetta in ingresso il JSON con i byte array dei file da validare
     *
     * @param dataToValidateDTOExt
     *            oggetto json con i dati da validare
     * @param request
     *            oggetto standard contente la request
     * 
     * @return EidasWSReportsDTOTree oggetto custom tree con risultati della verifica
     */
    @Operation(summary = "Report Verifica", method = "Effettua la verifica del file passato in input. Accetta un JSON con i metadati relativi alla verifica. La risorsa ottenuta da questa chiamata è il report di verifica", tags = {
            "Verifica", "v1" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Esito verifica documento firmato", content = {
                    @Content(mediaType = "application/xml", schema = @Schema(implementation = EidasWSReportsDTOTree.class)) }),
            @ApiResponse(responseCode = "500", description = "Documento firmato non riconosciuto", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = RestExceptionResponse.class)) }) })
    @PostMapping(value = {
            "v1/report-verifica" }, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public EidasWSReportsDTOTree validateJson(
            @Parameter(description = "DSS DataToValidate Json", required = true) @Valid @RequestBody(required = true) DataToValidateDTOExt dataToValidateDTOExt,
            HttpServletRequest request) {
        // LOG UUID
        MDC.put(Constants.UUID_LOG_MDC, dataToValidateDTOExt.getUuid());
        return verificaFirma.validateSignatureOnJson(dataToValidateDTOExt, request);
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
    @Operation(summary = "Report Verifica", method = "Effettua la verifica del file passato in input. Accetta chiamata POST con multipart/form-data. La risorsa ottenuta da questa chiamata è il report di verifica", tags = {
            "Verifica", "v2" })
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
            "v2/report-verifica" }, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_XML_VALUE)
    public EidasWSReportsDTOTree validateMultipart(
            @Parameter(description = "File firmato", required = true) @RequestPart(name = "signedFile", required = true) MultipartFile signedFile,
            @Parameter(description = "Metadati", required = false, schema = @Schema(type = "string", format = "binary")) @RequestPart(name = "metadata", required = false) @Valid Optional<EidasMetadataToValidate> metadata,
            @Parameter(description = "File originale/i", required = false) @RequestPart(name = "originalFiles", required = false) MultipartFile[] originalFiles,
            @Parameter(description = "File custom policy", required = false) @RequestPart(name = "customValidationFile", required = false) MultipartFile customValidationFile,
            HttpServletRequest request) {
        // LOG UUID
        // optional value
        EidasMetadataToValidate localMetadata = metadata.orElse(new EidasMetadataToValidate());
        MDC.put(Constants.UUID_LOG_MDC, localMetadata.getUuid());
        return verificaFirma.validateSignatureOnMultipart(localMetadata, request, signedFile, originalFiles,
                customValidationFile);
    }
}
