/*
 * Copyright 2015-2017 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkular.alerts.extensions;

import static org.hawkular.alerts.api.services.DistributedEvent.*;
import static org.hawkular.alerts.api.util.Util.isEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;

import org.hawkular.alerts.api.model.condition.Condition;
import org.hawkular.alerts.api.model.condition.ExternalCondition;
import org.hawkular.alerts.api.model.event.Event;
import org.hawkular.alerts.api.model.trigger.FullTrigger;
import org.hawkular.alerts.api.model.trigger.Trigger;
import org.hawkular.alerts.api.model.trigger.TriggerKey;
import org.hawkular.alerts.api.services.DefinitionsService;
import org.hawkular.alerts.api.services.DistributedEvent;
import org.hawkular.alerts.api.services.EventExtension;
import org.hawkular.alerts.api.services.ExtensionsService;
import org.hawkular.alerts.api.services.PropertiesService;
import org.hawkular.commons.log.MsgLogger;
import org.hawkular.commons.log.MsgLogging;

/**
 * This EventExtension is responsible of the following tasks:
 *
 * - It register a DistributedListener into the Alerting engine.
 *   It will process ExternalConditions tagged with HawkularExtension=AggregatedEvents into CEP rules.
 *
 * - All events tagged with HawkularExtension=AggregatedEvents will be filtered out and processed asynchronously by
 *   the extension applying aggregated rules defined in the ExternalCondition expressions.
 *
 * - The result of the processing might generate new events that are sent into the Alerting engine.
 *
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
public class EventsAggregationExtension implements EventExtension {
    private final MsgLogger log = MsgLogging.getMsgLogger(EventsAggregationExtension.class);

    private static final String ENGINE_EXTENSIONS = "hawkular-alerts.engine-extensions";
    private static final String ENGINE_EXTENSIONS_ENV = "ENGINE_EXTENSIONS";
    private static final String ENGINE_EXTENSIONS_DEFAULT = "true";
    boolean engineExtensions;

    private static final String TAG_NAME = "HawkularExtension";
    private static final String TAG_VALUE = "EventsAggregation";

    private static final String EVENTS_EXPIRATION = "hawkular-alerts.extension-events-expiration";
    private static final String EVENTS_EXPIRATION_ENV = "EXTENSION_EVENTS_EXPIRATION";
    private static final String EVENTS_EXTENSIONS_DEFAULT = "30m";
    private String defaultExpiration;

    /**
     * Events generated by the extension as a result of a processing are marked using a context property
     * to prevent that these events are re-processed by the extension in a loop.
     */
    private static final String CONTEXT_PROCESSED = "processed";

    private Map<TriggerKey, FullTrigger> activeTriggers = new HashMap<>();

    private PropertiesService properties;

    private DefinitionsService definitions;

    private ExtensionsService extensions;

    private CepEngine cep;

    private ExecutorService executor;

    public void setProperties(PropertiesService properties) {
        this.properties = properties;
    }

    public void setDefinitions(DefinitionsService definitions) {
        this.definitions = definitions;
    }

    public void setExtensions(ExtensionsService extensions) {
        this.extensions = extensions;
    }

    public void setCep(CepEngine cep) {
        this.cep = cep;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public void init() {
        engineExtensions = Boolean.parseBoolean(properties.getProperty(ENGINE_EXTENSIONS, ENGINE_EXTENSIONS_ENV,
                ENGINE_EXTENSIONS_DEFAULT));
        defaultExpiration = properties.getProperty(EVENTS_EXPIRATION, EVENTS_EXPIRATION_ENV, EVENTS_EXTENSIONS_DEFAULT);
        if (engineExtensions) {
            log.info("Registering Distributed Trigger listener");
            definitions.registerDistributedListener(events -> refresh(events));
            extensions.addExtension(this);
        }
    }

    /*
        A refresh() call can be invoked with several events for same trigger
        (i.e. trigger creation, conditions added, trigger enabled, and trigger removed)
        Effectively the last event for the same trigger is what this extension needs to load/unload the trigger
        from the alerter/extension.
     */
    private Set<DistributedEvent> optimizeEvents(Set<DistributedEvent> distEvents) {
        Map<TriggerKey, Operation> map = new HashMap<>();
        distEvents.stream().forEach(event -> map.put(new TriggerKey(event.getTenantId(), event.getTriggerId()),
                event.getOperation()));
        Set<DistributedEvent> optimizedEvents = new HashSet<>();
        map.entrySet().stream().forEach(entry -> optimizedEvents.add(new DistributedEvent(entry.getValue(),
                entry.getKey().getTenantId(), entry.getKey().getTriggerId())));
        return optimizedEvents;
    }

    private void refresh(Set<DistributedEvent> distEvents) {
        final Set<DistributedEvent> optimizedEvents = optimizeEvents(distEvents);
        executor.submit(() -> {
            try {
                for (DistributedEvent distEvent : optimizedEvents) {
                    switch (distEvent.getOperation()) {
                        case REMOVE:
                            activeTriggers.remove(new TriggerKey(distEvent.getTenantId(), distEvent.getTriggerId()));
                            break;
                        case ADD:
                        case UPDATE:
                            Trigger trigger = definitions.getTrigger(distEvent.getTenantId(), distEvent.getTriggerId());
                            if (trigger != null && trigger.getTags().containsKey(TAG_NAME)
                                    && trigger.getTags().get(TAG_NAME).equals(TAG_VALUE)) {
                                log.infof("Found [%s]", trigger.getName());
                                Collection<Condition> conditions = null;
                                List<Condition> activeConditions = new ArrayList<>();
                                try {
                                    if (!trigger.isGroup()) {
                                        conditions = definitions.getTriggerConditions(trigger.getTenantId(),
                                                trigger.getId(), null);
                                        log.infof("Checking [%s] Conditions for enabled trigger [%s]!",
                                                conditions.size(), trigger.getName());
                                    }
                                } catch (Exception e) {
                                    log.error("Failed to fetch Conditions when " +
                                            "scheduling metrics conditions for " + trigger, e);
                                    continue;
                                }
                                if (null == conditions) {
                                    continue;
                                }
                                for (Condition condition : conditions) {
                                    if (condition instanceof ExternalCondition) {
                                        ExternalCondition externalCondition = (ExternalCondition) condition;
                                        if (TAG_VALUE.equals(externalCondition.getAlerterId())) {
                                            activeConditions.add(externalCondition);
                                        }
                                    }
                                }
                                TriggerKey triggerKey = new TriggerKey(trigger.getTenantId(), trigger.getId());
                                if (activeConditions.isEmpty()) {
                                    activeTriggers.remove(triggerKey);
                                } else {
                                    FullTrigger activeTrigger = new FullTrigger();
                                    activeTrigger.setTrigger(trigger);
                                    activeTrigger.setConditions(activeConditions);
                                    activeTriggers.put(triggerKey, activeTrigger);
                                }
                            }
                            break;
                    }
                }
            } catch (Exception e) {
                log.error("Failed to fetch Triggers for external conditions.", e);
            }
            if (activeTriggers.isEmpty()) {
                cep.stop();
            } else {
                cep.updateConditions(defaultExpiration, activeTriggers.values());
            }
        });
    }

    public TreeSet<Event> processEvents(TreeSet<Event> events) {
        if (isEmpty(events)) {
            return events;
        }
        TreeSet<Event> retained = new TreeSet<>();
        TreeSet<Event> filtered = new TreeSet<>();
        for (Event event : events) {
            if (event.getTags() != null && TAG_VALUE.equals(event.getTags().get(TAG_NAME))
                    && event.getContext().get(CONTEXT_PROCESSED) == null) {
                retained.add(event);
            } else {
                if (event.getContext().get(CONTEXT_PROCESSED) != null) {
                    event.getContext().remove(CONTEXT_PROCESSED);
                }
                filtered.add(event);
            }
        }
        if (!retained.isEmpty()) {
            executor.submit(() -> cep.processEvents(retained));
        }
        return filtered;
    }
}
