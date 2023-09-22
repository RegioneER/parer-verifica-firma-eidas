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

package it.eng.parer.eidas.web.config;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import it.eng.parer.eidas.web.converter.CustomJaxb2RootElementHttpMessageConverter;

//https://docs.spring.io/spring-boot/docs/1.5.2.RELEASE/reference/htmlsingle/#boot-features-external-config-application-property-files
//SEE 24.6.4 YAML shortcomings
//@PropertySource("classpath:application.yaml")
//https://stackoverflow.com/questions/51008382/why-spring-boot-application-doesnt-require-enablewebmvc
//@EnableWebMvc
@Configuration
@ComponentScan("it.eng.parer.eidas.core")
@PropertySource("classpath:git.properties")
public class AppConfiguration implements WebMvcConfigurer {

    @Autowired
    Environment env;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // static resources
        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
        // swagger
        registry.addResourceHandler("swagger-ui.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    /**
     * Note: introdotto l'introspector Jaxb in modo tale da gestire correttamente sia in fase di serialiazzione che
     * deserializzazione il report DSS (Jaxb Object)
     *
     * @return MappingJackson2HttpMessageConverter
     */
    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        TypeFactory typeFactory = TypeFactory.defaultInstance();
        AnnotationIntrospector introspector = new JaxbAnnotationIntrospector(typeFactory);
        // make deserializer use JAXB annotations (only)
        mapper.getDeserializationConfig().with(introspector);
        // make serializer use JAXB annotations (only)
        mapper.getSerializationConfig().with(introspector);
        // since spring boot 2.5.0 (need com.fasterxml.jackson.datatype.jsr310.JavaTimeModule)
        mapper.registerModule(new JavaTimeModule());
        //
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter(
                mapper);
        // Aggiungo i media type text/plain per supportare client come jmeter
        List<MediaType> supportedMediaTypes = new ArrayList<>(
                mappingJackson2HttpMessageConverter.getSupportedMediaTypes());
        supportedMediaTypes.add(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8));
        supportedMediaTypes.add(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.ISO_8859_1));

        mappingJackson2HttpMessageConverter.setSupportedMediaTypes(supportedMediaTypes);

        return mappingJackson2HttpMessageConverter;
    }

    /*
     * Since spring boot 3.x.
     * 
     * Con l'introduzione dello standard jakarta.* necessario introdurre apposito converter per la gestione di
     * marshalling/unmarshalling del dto EidasWSReportsDTOTree.zs
     * 
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
        messageConverters.add(new CustomJaxb2RootElementHttpMessageConverter());
    }

    @Bean
    public OpenAPI eidasOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Verifica firma EIDAS")
                        .description("Microserivice per verifica firma basato su librerie EIDAS (dss)")
                        .version((StringUtils.isNotBlank(getClass().getPackage().getImplementationVersion())
                                ? getClass().getPackage().getImplementationVersion() : "")))
                .externalDocs(
                        new ExternalDocumentation().description("DSS on GitHub").url("https://github.com/esig/dss"));
    }
}
