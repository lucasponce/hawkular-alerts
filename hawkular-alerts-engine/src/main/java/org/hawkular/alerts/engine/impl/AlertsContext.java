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
package org.hawkular.alerts.engine.impl;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.hawkular.alerts.api.services.ActionListener;
import org.hawkular.alerts.api.services.DefinitionsEvent;
import org.hawkular.alerts.api.services.DefinitionsEvent.Type;
import org.hawkular.alerts.api.services.DefinitionsListener;
import org.hawkular.alerts.engine.service.PartitionManager;
import org.jboss.logging.Logger;

/**
 * Register DefinitionListener and ActionListener instances.
 * Store initialization state of the whole Alerts engine.
 *
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
@Startup
@Singleton
public class AlertsContext {
    private final Logger log = Logger.getLogger(AlertsContext.class);

    private Map<DefinitionsListener, Set<Type>> definitionListeners = new HashMap<>();
    private Map<DefinitionsListener, Set<Type>> distributedListeners = new HashMap<>();

    List<ActionListener> actionsListeners = new CopyOnWriteArrayList<>();

    private boolean distributed = false;
    @EJB
    PartitionManager partitionManager;

    @PostConstruct
    void init() {
        if (partitionManager != null) {
            distributed = partitionManager.isDistributed();
        }
        if (distributed) {
            partitionManager.registerDefinitionsEventsListener((definitionsEvents) -> {
                Set<DefinitionsEvent.Type> notificationTypes = definitionsEvents.stream()
                        .map(n -> n.getType())
                        .collect(Collectors.toSet());
                distributedListeners.entrySet().stream()
                        .filter(e -> shouldNotify(e.getValue(), notificationTypes))
                        .forEach(e -> {
                            log.debugf("Notified Distributed Listener %s of %s", e.getKey(), notificationTypes);
                            e.getKey().onChange(definitionsEvents.stream()
                                    .filter(de -> e.getValue().contains(de.getType()))
                                    .collect(Collectors.toSet()));
                        });
            });
        }
    }

    public void registerDefinitionListener(DefinitionsListener listener, boolean distributed, Type eventType,
                                           Type... eventTypes) {
        EnumSet<Type> types = EnumSet.of(eventType, eventTypes);
        if (log.isDebugEnabled()) {
            log.debug("Registering listeners " + listener + " for event types " + types);
        }
        if (distributed) {
            distributedListeners.put(listener, types);
        }
       definitionListeners.put(listener, types);
    }

    public Map<DefinitionsListener, Set<Type>> getDefinitionListeners() {
        return definitionListeners;
    }

    public void registerActionListener(ActionListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("ActionListener must not be null");
        }
        actionsListeners.add(listener);
    }

    public List<ActionListener> getActionsListeners() {
        return actionsListeners;
    }

    public void notifyListeners(Set<DefinitionsEvent> notifications) {
        Set<DefinitionsEvent.Type> notificationTypes = notifications.stream()
                .map(n -> n.getType())
                .collect(Collectors.toSet());
        if (log.isDebugEnabled()) {
            log.debugf("Notifying applicable listeners %s of events %s",
                    definitionListeners, notifications);
        }
        definitionListeners.entrySet().stream()
                .filter(e -> shouldNotify(e.getValue(), notificationTypes))
                .forEach(e -> {
                    log.debugf("Notified Listener %s of %s", e.getKey(), notificationTypes);
                    e.getKey().onChange(notifications.stream()
                            .filter(de -> e.getValue().contains(de.getType()))
                            .collect(Collectors.toSet()));
                });
        if (distributed) {
            partitionManager.notifyDefinitionsEvents(notifications);
        }
    }

    private boolean shouldNotify(Set<DefinitionsEvent.Type> listenerTypes, Set<DefinitionsEvent.Type> eventTypes) {
        HashSet<Type> intersection = new HashSet<>(listenerTypes);
        intersection.retainAll(eventTypes);
        return !intersection.isEmpty();
    }
}
