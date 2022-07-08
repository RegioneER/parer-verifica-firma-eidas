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
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

//https://docs.spring.io/spring-boot/docs/1.5.2.RELEASE/reference/htmlsingle/#boot-features-external-config-application-property-files
//SEE 24.6.4 YAML shortcomings
//@PropertySource("classpath:application.yaml")
//https://stackoverflow.com/questions/51008382/why-spring-boot-application-doesnt-require-enablewebmvc
//@EnableWebMvc
@Configuration
@ComponentScan("it.eng.parer.eidas.core")
@PropertySource("classpath:git.properties")
public class VerificaFirmaEidasAppConfiguration implements WebMvcConfigurer {

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
