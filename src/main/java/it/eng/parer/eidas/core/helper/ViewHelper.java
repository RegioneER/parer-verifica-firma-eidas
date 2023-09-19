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

package it.eng.parer.eidas.core.helper;

import java.util.Arrays;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private Logger log = LoggerFactory.getLogger(ViewHelper.class);

    @Autowired
    Environment env;

    // default : all
    @Value("${parer.eidas.admin-ui.propstoskip:}")
    String propsToSkip;

    public void convertAppPropertiesAsMap(Model model) {
        Properties props = new Properties();
        MutablePropertySources propSrcs = ((AbstractEnvironment) env).getPropertySources();
        StreamSupport.stream(propSrcs.spliterator(), false).filter(MapPropertySource.class::isInstance)
                .map(ps -> ((EnumerablePropertySource<?>) ps).getPropertyNames()).flatMap(Arrays::<String> stream)
                .filter(propName -> !propName.matches(propsToSkip))
                .forEach(propName -> props.setProperty(propName, getProperty(propName)));
        model.addAttribute("app", new TreeMap<>(props));
    }

    private String getProperty(String key) {
        try {
            return env.getProperty(key);
        } catch (IllegalArgumentException e) {
            log.warn(
                    "Errore durante il recupero della variabile d'ambiente {}. All'interno del valore c'è forse un placeholder tipo ${var}?",
                    key, e);
            return "non disponibile";
        }
    }
}
