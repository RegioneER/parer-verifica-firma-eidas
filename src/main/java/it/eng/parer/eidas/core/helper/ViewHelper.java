package it.eng.parer.eidas.core.helper;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

@Component
public class ViewHelper {

    @Autowired
    Environment env;

    // default : all
    @Value("${it.eng.parer.core.viewhelper.propstoskip:}")
    String propsToSkip;

    public void convertAppPropertiesAsMap(Model model) {
        Properties props = new Properties();
        MutablePropertySources propSrcs = ((AbstractEnvironment) env).getPropertySources();
        StreamSupport.stream(propSrcs.spliterator(), false).filter(ps -> ps instanceof MapPropertySource)
                .map(ps -> ((EnumerablePropertySource) ps).getPropertyNames()).flatMap(Arrays::<String> stream)
                .filter(propName -> !propName.matches(propsToSkip))
                .forEach(propName -> props.setProperty(propName, env.getProperty(propName)));
        model.addAttribute("app", new TreeMap<String, String>((Map) props));
    }

}
