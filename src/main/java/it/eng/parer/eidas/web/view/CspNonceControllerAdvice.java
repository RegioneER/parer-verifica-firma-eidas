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

package it.eng.parer.eidas.web.view;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import it.eng.parer.eidas.web.config.security.CspNonceFilter;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Espone ai template Thymeleaf il nonce generato da {@link CspNonceFilter} per la
 * Content-Security-Policy corrente, in modo da poter autorizzare esplicitamente gli script inline
 * necessari (es. {@code th:attr="nonce=${cspNonce}"}).
 */
@ControllerAdvice
public class CspNonceControllerAdvice {

    @ModelAttribute(CspNonceFilter.CSP_NONCE_ATTRIBUTE)
    public String cspNonce(HttpServletRequest request) {
        Object nonce = request.getAttribute(CspNonceFilter.CSP_NONCE_ATTRIBUTE);
        return nonce != null ? nonce.toString() : "";
    }

}
