/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;

import org.hawkular.alerts.api.model.data.Data;
import org.hawkular.alerts.api.model.event.Event;
import org.hawkular.alerts.api.services.DefinitionsService;
import org.hawkular.alerts.engine.service.AlertsEngine;
import org.hawkular.alerts.filter.CacheClient;
import org.jboss.logging.Logger;

/**
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
@Singleton
@Startup
@TransactionAttribute(value = TransactionAttributeType.NOT_SUPPORTED)
public class IncomingDataManager {
    private final Logger log = Logger.getLogger(IncomingDataManager.class);

    private static final String MIN_REPORTING_INTERVAL = "hawkular-alerts.min-reporting-interval";
    private int minRepInterval;

    @Resource
    private ManagedExecutorService executor;

    @EJB
    DataDrivenGroupCacheManager dataDrivenGroupCacheManager;

    @EJB
    DefinitionsService definitionsService;

    @EJB
    AlertsEngine alertsEngine;

    @Inject
    CacheClient dataIdCache;

    public IncomingDataManager() {
        minRepInterval = new Integer(AlertProperties.getProperty(MIN_REPORTING_INTERVAL, "1000"));
    }

    public void bufferData(IncomingData incomingData) {
        executor.submit(() -> {
            Collection<Data> filteredData = filterIncomingData(incomingData);
            while (!filteredData.isEmpty()) {
                TreeSet<Data> batchData = new TreeSet<>(filteredData);
                Data previousData = null;
                filteredData.clear();
                for (Iterator<Data> i = batchData.iterator(); i.hasNext();) {
                    Data data = i.next();
                    if (null == previousData ||
                            !(previousData.getId().equals(data.getId())
                                    && previousData.getTenantId().equals(data.getTenantId()))) {
                        previousData = data;
                    } else {
                        i.remove();
                        if (data.getTimestamp() - previousData.getTimestamp() > minRepInterval) {
                            filteredData.add(data);
                        }
                    }
                }
                log.debugf("Send %s data", batchData);
                sendData(batchData);
            }
        });
    }

    public void bufferEvents(Collection<Event> events) {
        executor.submit(() ->  {
            Collection<Event> incomingEvents = new HashSet<>(events);
            while (!incomingEvents.isEmpty()) {
                TreeSet<Event> batchEvents = new TreeSet<>(incomingEvents);
                Event previousEvent = null;
                incomingEvents.clear();
                for (Iterator<Event> i = batchEvents.iterator(); i.hasNext();) {
                    Event event = i.next();
                    if (null == previousEvent ||
                            !(null != event.getDataId()
                                    && !(event.getDataId().equals(previousEvent.getDataId())
                                            && event.getTenantId().equals(previousEvent.getTenantId())))) {
                        previousEvent = event;
                    } else {
                        i.remove();
                        if (event.getCtime() - previousEvent.getCtime() > minRepInterval) {
                            incomingEvents.add(event);
                        }
                    }
                }
                log.debugf("Send %s events", batchEvents);
                sendEvents(batchEvents);
            }
        });
    }

    private Collection<Data> filterIncomingData(IncomingData incomingData) {
        Collection<Data> data = incomingData.getIncomingData();
        data = incomingData.isRaw() ? dataIdCache.filterData(data) : data;

        // check to see if any data can be used to generate data-driven group members
        checkDataDrivenGroupTriggers(data);

        return data;
    }

    private void sendData(Collection<Data> data) {
        log.debugf("Sending %s data to AlertsEngine.", data.size());
        try {
            alertsEngine.sendData(data);

        } catch (Exception e) {
            log.errorf("Failed sending data: %s", e.getMessage());
        }
    }

    private void sendEvents(Collection<Event> events) {
        log.debugf("Sending %s events to AlertsEngine.", events.size());
        try {
            alertsEngine.sendEvents(events);
        } catch (Exception e) {
            log.errorf("Failed sending events: %s", e.getMessage());
        }
    }

    private void checkDataDrivenGroupTriggers(Collection<Data> data) {
        if (!dataDrivenGroupCacheManager.isCacheActive()) {
            return;
        }
        for (Data d : data) {
            if (isEmpty(d.getSource())) {
                continue;
            }

            String tenantId = d.getTenantId();
            String dataId = d.getId();
            String dataSource = d.getSource();

            Set<String> groupTriggerIds = dataDrivenGroupCacheManager.needsSourceMember(tenantId, dataId, dataSource);

            // Add a trigger members for the source

            for (String groupTriggerId : groupTriggerIds) {
                try {
                    definitionsService.addDataDrivenMemberTrigger(tenantId, groupTriggerId, dataSource);
                } catch (Exception e) {
                    log.errorf("Failed to add Data-Driven Member Trigger for [%s:%s]: %s:", groupTriggerId, d,
                            e.getMessage());
                }
            }
        }
    }

    private boolean isEmpty(String s) {
        return null == s || s.isEmpty();
    }

    public static class IncomingData {
        private Collection<Data> incomingData;
        private boolean raw;

        public IncomingData(Collection<Data> incomingData, boolean raw) {
            super();
            this.incomingData = incomingData;
            this.raw = raw;
        }

        public Collection<Data> getIncomingData() {
            return incomingData;
        }

        public boolean isRaw() {
            return raw;
        }
    }

}
