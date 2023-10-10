/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.eng.parer.eidas.web.util;

public class EndPointCostants {

    private EndPointCostants() {
        throw new IllegalStateException("Utility class");
    }

    public static final String URL_ADMIN_BASE = "/admin";
    public static final String URL_API_BASE = "/api";

    public static final String RESOURCE_ERRORS = "/errors";
    public static final String RESOURCE_REPORT_VERIFICA = "/report-verifica";
    public static final String RESOURCE_INFOS = "/infos";

    public static final String URL_ADMIN_INFOS = URL_ADMIN_BASE + RESOURCE_INFOS;
    public static final String URL_REPORT_VERIFICA = URL_API_BASE + RESOURCE_REPORT_VERIFICA;
    public static final String URL_ERRORS = URL_API_BASE + RESOURCE_ERRORS;

    // ROLES
    public static final String ROLE_ADMIN = "ADMIN";

}
