package it.eng.parer.eidas.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import it.eng.parer.eidas.web.config.CustomBanner;

@SpringBootApplication
public class VerificaFirmaEidasApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(VerificaFirmaEidasApplication.class);
        application.setBanner(new CustomBanner());
        application.run(args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(VerificaFirmaEidasApplication.class);
    }
}
