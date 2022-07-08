package it.eng.parer.eidas.web.config;

import java.io.PrintStream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.Banner;
import org.springframework.core.env.Environment;

public class CustomBanner implements Banner {

    @Override
    public void printBanner(Environment environment, Class<?> sourceClass, PrintStream out) {
        out.println("================================================================");
        out.println("@EIDAS " + (StringUtils.isNotBlank(getClass().getPackage().getImplementationVersion())
                ? "v." + getClass().getPackage().getImplementationVersion() : " on localhost "));
        out.println("================================================================");

    }
}
