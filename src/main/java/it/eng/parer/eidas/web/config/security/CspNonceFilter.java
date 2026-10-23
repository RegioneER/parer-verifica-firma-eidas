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

package it.eng.parer.eidas.web.config.security;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Genera un nonce (numero casuale monouso) per ogni richiesta e lo utilizza per valorizzare
 * l'header Content-Security-Policy, così da consentire l'esecuzione dei soli script inline
 * esplicitamente autorizzati (attributo {@code nonce} nei tag &lt;script&gt;) e impedire
 * l'esecuzione di script iniettati da un eventuale attacco XSS lato DOM.
 *
 * Il nonce viene esposto come request attribute {@value #CSP_NONCE_ATTRIBUTE} in modo da poter
 * essere referenziato dai template Thymeleaf (es. {@code th:attr="nonce=${cspNonce}"}).
 */
@Component
public class CspNonceFilter extends OncePerRequestFilter {

    public static final String CSP_NONCE_ATTRIBUTE = "cspNonce";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String nonce = generateNonce();
        request.setAttribute(CSP_NONCE_ATTRIBUTE, nonce);

        response.setHeader("Content-Security-Policy", buildPolicy(nonce));

        filterChain.doFilter(request, response);
    }

    // default policy
    private String buildPolicy(String nonce) {
        return "default-src 'self'; " + "script-src 'self' 'nonce-" + nonce + "'; "
                + "style-src 'self' 'unsafe-inline'; " + "img-src 'self' data:; "
                + "font-src 'self'; " + "object-src 'none'; " + "base-uri 'self'; "
                + "frame-ancestors 'self'; " + "form-action 'self'";
    }

    private String generateNonce() {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

}
