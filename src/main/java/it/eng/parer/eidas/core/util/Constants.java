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

    public static final String WRITE_FILE = "it.eng.parer.eidas.core.validation.writefile";
    public static final String WRITE_FILE_DEFAULT_VAL = "false";
    public static final String BUILD_VERSION = "git.build.version";
    public static final String BUILD_TIME = "git.commit.time";
    public static final String DSS_VERSION = "dss.version";

    public static final String DATE_FORMAT_TIMESTAMP_TYPE = "dd-MM-yyyy'T'HH:mm:ss.SSSZ";

    /* default configuration */
    public static final String TIMEOUT_LDAP_CONNECTION = "6000";

    public static final String TMP_FILE_SUFFIX = "-eidasvf.tmp";

}
