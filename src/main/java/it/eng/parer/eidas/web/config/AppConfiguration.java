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

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.module.jaxb.JaxbAnnotationModule;

@Configuration
@ComponentScan("it.eng.parer.eidas.core")
@PropertySource("classpath:git.properties")
public class AppConfiguration implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // static resources
        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
        // swagger
        registry.addResourceHandler("swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    @Bean
    public ObjectMapper jaxbObjectMapper() {
        return JsonMapper.builder().addModule(new JaxbAnnotationModule())
                // JavaTimeModule NON serve più: incluso in jackson-databind 3.x
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).build();
    }

    @Bean
    public OpenAPI eidasOpenAPI() {
        return new OpenAPI().info(new Info().title("Verifica firma EIDAS")
                .description("Microserivice per verifica firma basato su librerie EIDAS (dss)")
                .version((StringUtils.isNotBlank(getClass().getPackage().getImplementationVersion())
                        ? getClass().getPackage().getImplementationVersion()
                        : "")))
                .externalDocs(new ExternalDocumentation().description("DSS on GitHub")
                        .url("https://github.com/esig/dss"));
    }
}