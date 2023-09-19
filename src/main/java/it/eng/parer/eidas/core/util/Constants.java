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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.eng.parer.eidas.core.util;

/**
 *
 * @author sinatti_s (
 */
public class Constants {

    private Constants() {
        throw new IllegalStateException("Constant class");
    }

    public static final String UUID_LOG_MDC = "uuid";

    public static final String BUILD_VERSION = "git.build.version";
    public static final String BUILD_TIME = "git.commit.time";
    public static final String DSS_VERSION = "dss.version";

    /* default configuration */
    public static final String TIMEOUT_LDAP_CONNECTION = "6000";

    public static final String TMP_FILE_SUFFIX = "-eidasvf.tmp";

    /* default error message on advice handler */
    public static final String STD_MSG_APP_ERROR = "Errore applicativo";
    public static final String STD_MSG_GENERIC_ERROR = "Errore generico";
    public static final String STD_MSG_VALIDATION_ERROR = "Chiamata non valida";

}
