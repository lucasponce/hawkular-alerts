package org.hawkular.alerts.cache;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.hawkular.commons.log.MsgLogger;
import org.hawkular.commons.log.MsgLogging;
import org.hawkular.commons.properties.HawkularProperties;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

/**
 * Load the DefaultCacheManager from infinispan
 *
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
public class IspnCacheManager {
    private static final MsgLogger log = MsgLogging.getMsgLogger(IspnCacheManager.class);
    private static final String CONFIG_PATH = "hawkular.configuration";
    private static final String ISPN_CONFIG_DISTRIBUTED = "alerting-distributed.xml";
    private static final String ISPN_CONFIG_LOCAL = "alerting-local.xml";
    private static final String ALERTS_DISTRIBUTED = "hawkular-alerts.distributed";
    private static final String ALERTS_DISTRIBUTED_ENV = "HAWKULAR_ALERTS_DISTRIBUTED";
    private static final String ALERTS_DISTRIBUTED_DEFAULT = "false";

    private static EmbeddedCacheManager cacheManager = null;
    private static boolean distributed = false;

    public static EmbeddedCacheManager getCacheManager() {
        if (cacheManager == null) {
            init();
        }
        return cacheManager;
    }

    public static boolean isDistributed() {
        return distributed;
    }

    public static void stop() {
        if (cacheManager != null) {
            cacheManager.stop();
            cacheManager = null;
        }
    }

    private static synchronized void init() {
        if (cacheManager == null) {
            try {
                distributed = Boolean.valueOf(HawkularProperties.getProperty(ALERTS_DISTRIBUTED, ALERTS_DISTRIBUTED_ENV,
                        ALERTS_DISTRIBUTED_DEFAULT));
                String configPath = System.getProperty(CONFIG_PATH);
                InputStream is = null;
                if (configPath != null) {
                    File configFile = new File(configPath, distributed ? ISPN_CONFIG_DISTRIBUTED : ISPN_CONFIG_LOCAL);
                    if (configFile.exists() && configFile.isDirectory()) {
                        is = new FileInputStream(configFile);
                    }
                }
                if (is == null) {
                    is = IspnCacheManager.class.getResourceAsStream("/" + (distributed ? ISPN_CONFIG_DISTRIBUTED : ISPN_CONFIG_LOCAL));
                }
                cacheManager = new DefaultCacheManager(is);
            } catch (IOException e) {
                log.error(e);
            }
        }
    }
}
