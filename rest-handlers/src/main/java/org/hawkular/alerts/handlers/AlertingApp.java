package org.hawkular.alerts.handlers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.hawkular.alerts.actions.standalone.StandaloneActionPluginRegister;
import org.hawkular.alerts.alerters.standalone.StandaloneAlerterPluginRegister;
import org.hawkular.alerts.engine.StandaloneAlerts;
import org.hawkular.alerts.handlers.util.AlertingThreadFactory;
import org.hawkular.commons.log.MsgLogger;
import org.hawkular.commons.log.MsgLogging;
import org.hawkular.commons.properties.HawkularProperties;
import org.hawkular.handlers.BaseApplication;

/**
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
public class AlertingApp implements BaseApplication {
    private static final MsgLogger log = MsgLogging.getMsgLogger(AlertingApp.class);
    private static final String BASE_URL = "hawkular-alerts.base-url";
    private static final String BASE_URL_DEFAULT = "/hawkular/alerts";

    AlertingThreadFactory threadFactory;
    ExecutorService executor;
    String baseUrl = HawkularProperties.getProperty(BASE_URL, BASE_URL_DEFAULT);

    @Override
    public void start() {
        threadFactory = new AlertingThreadFactory();
        executor = Executors.newCachedThreadPool(threadFactory);
        StandaloneAlerts.setExecutor(executor);
        StandaloneAlerts.start();
        StandaloneActionPluginRegister.setExecutor(executor);
        StandaloneActionPluginRegister.start();
        StandaloneAlerterPluginRegister.setExecutor(executor);
        StandaloneAlerterPluginRegister.start();
        log.infof("Alerting app started on [ %s ] ", baseUrl());
    }

    @Override
    public void stop() {
        StandaloneActionPluginRegister.stop();
        StandaloneAlerts.stop();
        log.infof("Alerting app stopped", baseUrl());
    }

    @Override
    public String baseUrl() {
        return baseUrl;
    }
}
