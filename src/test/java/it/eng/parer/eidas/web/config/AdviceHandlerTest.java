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

package it.eng.parer.eidas.web.config;

import static it.eng.parer.eidas.web.util.EndPointCostants.URL_REPORT_VERIFICA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;

import it.eng.parer.eidas.core.helper.EidasMetadaValidator;
import it.eng.parer.eidas.core.service.IVerificaFirma;
import it.eng.parer.eidas.model.exception.EidasParerException;
import it.eng.parer.eidas.model.exception.ParerError.ErrorCode;
import it.eng.parer.eidas.web.bean.RestExceptionResponse;
import it.eng.parer.eidas.web.rest.VerificaFirmaEndpoint;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.module.jaxb.JaxbAnnotationModule;

/**
 * Test per {@link AdviceHandler}: verifica che ogni {@code @ExceptionHandler} restituisca il codice
 * HTTP e il codice errore attesi. I test sugli handler EidasParerException e generici invocano i
 * metodi direttamente (senza MockMvc) per evitare conflitti di content-negotiation tra il
 * produces=APPLICATION_XML del controller e il produces=APPLICATION_PROBLEM_JSON dell'advice.
 */
@ExtendWith(MockitoExtension.class)
class AdviceHandlerTest {

    private AdviceHandler handler;

    @Mock
    private IVerificaFirma verificaFirma;

    @Mock
    private EidasMetadaValidator eidasMetadaValidator;

    @InjectMocks
    private VerificaFirmaEndpoint controller;

    private MockMvc mvc;
    private ServletWebRequest webRequest;

    @BeforeEach
    void setUp() {
        handler = new AdviceHandler();

        // Imposta il contesto di richiesta necessario a ServletUriComponentsBuilder
        MockHttpServletRequest mockReq = new MockHttpServletRequest("POST", URL_REPORT_VERIFICA);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockReq));
        webRequest = new ServletWebRequest(mockReq);

        // MockMvc usato solo per il test MissingServletRequestPartException (HTTP 400)
        lenient().when(eidasMetadaValidator.supports(any())).thenReturn(false);
        JsonMapper jsonMapper = JsonMapper.builder().addModule(new JaxbAnnotationModule())
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).build();
        mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(handler)
                .setMessageConverters(new JacksonJsonHttpMessageConverter(jsonMapper)).build();
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    // --- EidasParerException (EIDAS_ERROR) → HTTP 500 ---

    @Test
    void handleEidasParerException_perEIDAS_ERROR_restituisce500() {
        EidasParerException ex = new EidasParerException().withCode(ErrorCode.EIDAS_ERROR)
                .withMessage("firma non valida");

        ResponseEntity<RestExceptionResponse> response = handler.handleEidasParerException(ex,
                webRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ErrorCode.EIDAS_ERROR, response.getBody().getCode());
    }

    // --- EidasParerException (IO_ERROR) → HTTP 500 ---

    @Test
    void handleEidasParerException_perIO_ERROR_restituisce500() {
        EidasParerException ex = new EidasParerException().withCode(ErrorCode.IO_ERROR)
                .withMessage("errore I/O");

        ResponseEntity<RestExceptionResponse> response = handler.handleEidasParerException(ex,
                webRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ErrorCode.IO_ERROR, response.getBody().getCode());
    }

    // --- Eccezione generica → HTTP 500 con codice GENERIC_ERROR ---

    @Test
    void handleGenericException_restituisce500() {
        ResponseEntity<RestExceptionResponse> response = handler
                .handleGenericException(new RuntimeException("errore imprevisto"), webRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ErrorCode.GENERIC_ERROR, response.getBody().getCode());
    }

    // --- Multipart senza signedFile → MissingServletRequestPartException → HTTP 400 ---

    @Test
    void handleMissingServletRequestPartException_senzaSignedFile_restituisce400()
            throws Exception {
        mvc.perform(multipart(URL_REPORT_VERIFICA)).andExpect(status().isBadRequest());
    }

}
