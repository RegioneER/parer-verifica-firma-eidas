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

package it.eng.parer.eidas.core.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import eu.europa.esig.dss.tsl.job.TLValidationJob;
import jakarta.annotation.PostConstruct;

@Service
public class TSLLoaderJob {

    /*
     * Default : disabled
     *
     * If enable the loader is designed for loading cache offline ONLY IF exits directory on filesystem. In case the
     * directory does not exits the DSS logic raise an exception. Check DSSBeanConfig.tlCacheDirectory method.
     */
    @Value("${cron.tl.loader.offline.enabled}")
    private boolean offlineEnabled;

    @Autowired
    private TLValidationJob job;

    @PostConstruct
    public void init() {
        if (offlineEnabled) {
            job.offlineRefresh();
        }
        //
        job.onlineRefresh();
    }

    @Scheduled(cron = "${cron.tl.sched}")
    public void refresh() {
        job.onlineRefresh();
    }

}
