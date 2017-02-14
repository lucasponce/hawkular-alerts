package org.hawkular.alerts.elasticsearch;

import java.util.Collection;

import org.elasticsearch.common.collect.ImmutableList;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.plugins.AbstractPlugin;

/**
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
public class HawkularAlertsPlugin extends AbstractPlugin {

    private static final Collection<Class<? extends Module>> modules = ImmutableList
            .<Class<? extends Module>>of(HawkularAlertsModule.class);

    @Inject
    public HawkularAlertsPlugin() { }

    public String name() {
        return "hawkular-alerting";
    }

    public String description() {
        return "Hawkular Alerting";
    }

    @Override
    public Collection<Class<? extends Module>> modules() {
        return modules;
    }
}
