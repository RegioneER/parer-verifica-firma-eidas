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