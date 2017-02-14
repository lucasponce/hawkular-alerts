package org.hawkular.alerts.elasticsearch;

import org.elasticsearch.common.inject.AbstractModule;

/**
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
public class HawkularAlertsModule extends AbstractModule {

    protected void configure() {
        bind(HawkularAlertsListener.class).asEagerSingleton();
    }
}
