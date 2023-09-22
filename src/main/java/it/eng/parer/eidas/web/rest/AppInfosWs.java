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

package it.eng.parer.eidas.web.rest;

import static it.eng.parer.eidas.web.util.EndPointCostants.RESOURCE_INFOS;
import static it.eng.parer.eidas.web.util.EndPointCostants.URL_ADMIN_BASE;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Tag(name = "Info", description = "Informazioni applicativo")
@RestController
@RequestMapping(URL_ADMIN_BASE)
public class AppInfosWs {

    private Map<String, Map<String, String>> infos = Collections.synchronizedMap(new LinkedHashMap<>());

    private static final String ENV_FILTER_GIT = "git";
    private static final String ENV_FILTER_DSS = "dss";
    private static final String ENV_FILTER_SPRING = "spring";
    private static final String ENV_FILTER_OTHERS = "others";

    /* constants */
    private static final String ETAG = "v1.0";

    @Autowired
    Environment env;

    // default : all
    @Value("${parer.eidas.admin-ui.propstoskip:}")
    String propsToSkip;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Operation(summary = "Info", method = "Informazioni applicativo")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Informazioni applicativo", content = {
            @Content(mediaType = "application/json") }) })
    @GetMapping(value = { RESOURCE_INFOS }, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Map<String, String>>> infos(HttpServletRequest request) {

        MutablePropertySources propSrcs = ((AbstractEnvironment) env).getPropertySources();

        // infos
        // git
        Properties gitprops = new Properties();
        StreamSupport.stream(propSrcs.spliterator(), false).filter(MapPropertySource.class::isInstance)
                .map(ps -> ((EnumerablePropertySource) ps).getPropertyNames()).flatMap(Arrays::<String> stream)
                .filter(propName -> !propName.matches(propsToSkip))
                .filter(propName -> propName.startsWith(ENV_FILTER_GIT))
                .forEach(propName -> gitprops.setProperty(propName, getProperty(propName)));

        infos.put(ENV_FILTER_GIT, new TreeMap<>((Map) gitprops));

        // dss
        Properties dssprops = new Properties();
        StreamSupport.stream(propSrcs.spliterator(), false).filter(MapPropertySource.class::isInstance)
                .map(ps -> ((EnumerablePropertySource) ps).getPropertyNames()).flatMap(Arrays::<String> stream)
                .filter(propName -> !propName.matches(propsToSkip))
                .filter(propName -> propName.startsWith(ENV_FILTER_DSS))
                .forEach(propName -> dssprops.setProperty(propName, getProperty(propName)));

        infos.put(ENV_FILTER_DSS, new TreeMap<>((Map) dssprops));

        // spring
        Properties springprops = new Properties();
        StreamSupport.stream(propSrcs.spliterator(), false).filter(MapPropertySource.class::isInstance)
                .map(ps -> ((EnumerablePropertySource) ps).getPropertyNames()).flatMap(Arrays::<String> stream)
                .filter(propName -> !propName.matches(propsToSkip))
                .filter(propName -> propName.startsWith(ENV_FILTER_SPRING))
                .forEach(propName -> springprops.setProperty(propName, getProperty(propName)));

        infos.put(ENV_FILTER_SPRING, new TreeMap<>((Map) springprops));

        // others
        List<String> allcurrkeys = infos.values().stream().map(Map::keySet).flatMap(Collection::stream)
                .collect(Collectors.toList());
        Properties othersprops = new Properties();
        StreamSupport.stream(propSrcs.spliterator(), false).filter(MapPropertySource.class::isInstance)
                .map(ps -> ((EnumerablePropertySource) ps).getPropertyNames()).flatMap(Arrays::<String> stream)
                .filter(propName -> !propName.matches(propsToSkip)).filter(propName -> !allcurrkeys.contains(propName))
                .forEach(propName -> othersprops.setProperty(propName, getProperty(propName)));

        infos.put(ENV_FILTER_OTHERS, new TreeMap<>((Map) othersprops));

        return ResponseEntity.ok().lastModified(LocalDateTime.now().atZone(ZoneId.systemDefault())).eTag(ETAG)
                .body(infos);
    }

    private String getProperty(String key) {
        try {
            return env.getProperty(key);
        } catch (IllegalArgumentException e) {
            return "non disponibile";
        }
    }
}
