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
package org.hawkular.alerts.bus.init;

import static java.util.Arrays.asList;

import static org.hawkular.alerts.bus.api.PublishCommandMessage.PUBLISH_COMMAND;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.hawkular.alerts.api.model.condition.AvailabilityCondition;
import org.hawkular.alerts.api.model.condition.CompareCondition;
import org.hawkular.alerts.api.model.condition.Condition;
import org.hawkular.alerts.api.services.DefinitionsEvent;
import org.hawkular.alerts.api.services.DefinitionsListener;
import org.hawkular.alerts.api.services.DefinitionsService;
import org.hawkular.alerts.bus.api.PublishCommandMessage;
import org.hawkular.alerts.bus.publish.PublishCommandSender;
import org.jboss.logging.Logger;

/**
 * A helper class to initialize bus callbacks into the alerts engine.
 *
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
@Singleton
@TransactionAttribute(value= TransactionAttributeType.NOT_SUPPORTED)
public class CacheManager {
    private final Logger log = Logger.getLogger(CacheManager.class);

    private final String AVAILABILITY = "availability";
    private final String GAUGE = "gauge";
    private final String COUNTER = "counter";
    private final String COUNTER_RATE = "counter_rate";
    private final String STRING = "string";
    private final String GAUGE_RATE = "gauge_rate";

    //public static final String CACHE_KEY_TRIGGER_UPDATE_TIME = "HawkularAlerts:TriggerUpdateTime";
    //public static final String CACHE_KEY_CONDITION_UPDATE_TIME = "HawkularAlerts:ConditionUpdateTime";
    //public static final String CACHE_KEY_DAMPENING_UPDATE_TIME = "HawkularAlerts:DampeningUpdateTime";

    Set<String> activeDataIds;
    //long activeDataIdsTime = 0L;
    Set<String> activeAvailabityIds;

    //@Resource(lookup = "java:jboss/infinispan/container/hawkular")
    //private CacheContainer container;
    //protected Cache<CacheEntry, Object> cache;

    @EJB
    DefinitionsService definitions;

    @Inject
    PublishCommandSender publishCommandSender;

    @PostConstruct
    public void init() {
        // cache = cacheContainer.getCache();

        updateActiveIds();

        definitions.registerListener(new DefinitionsListener() {
            @Override
            public void onChange(DefinitionsEvent event) {
                updateActiveIds();
            }
        }, DefinitionsEvent.Type.CONDITION_CHANGE);
    }

    public Set<String> getActiveDataIds() {
        return activeDataIds;
    }

    public void setActiveDataIds(Set<String> activeDataIds) {
        this.activeDataIds = activeDataIds;
    }

    public Set<String> getActiveAvailabilityIds() {
        return activeAvailabityIds;
    }

    public void setActiveAvailabilityIds(Set<String> activeAvailabilityIds) {
        this.activeDataIds = activeAvailabilityIds;
    }

    private synchronized void updateActiveIds() {

        //        if (null != activeDataIds) {
        //            if (null == cache) {
        //                log.error("ISPN Cache is null. All data being forwarded to alerting!");
        //                activeDataIds = null;
        //                return;
        //            }
        //
        //            Long updateTime = (Long) cache.get("HawkularAlerts:ConditionUpdateTime");
        //            if (null == updateTime || updateTime <= activeDataIdsTime) {
        //                return;
        //            }
        //
        //            activeDataIdsTime = updateTime;
        //        }

        Set<String> dataIds = null;
        Set<String> availIds = null;
        try {
            Collection<Condition> conditions = definitions.getAllConditions();
            dataIds = new HashSet<>(conditions.size());
            availIds = new HashSet<>(conditions.size());
            for (Condition c : conditions) {
                if (c instanceof AvailabilityCondition) {
                    availIds.add(c.getDataId());
                    if (isNewAvailId(c.getDataId())) {
                        publishCommand(c.getTenantId(), c.getDataId());
                    }
                    continue;
                }
                dataIds.add(c.getDataId());
                if (isNewDataId(c.getDataId())) {
                    publishCommand(c.getTenantId(), c.getDataId());
                }
                if (c instanceof CompareCondition) {
                    String data2Id = ((CompareCondition) c).getData2Id();
                    dataIds.add(data2Id);
                    if (isNewDataId(data2Id)) {
                        publishCommand(c.getTenantId(), data2Id);
                    }
                }
            }
        } catch (Exception e) {
            log.error("FAILED to load conditions to create Id filters. All data being forwarded to alerting!", e);
            activeDataIds = null;
            activeAvailabityIds = null;
            return;
        }

        activeDataIds = Collections.unmodifiableSet(dataIds);
        activeAvailabityIds = Collections.unmodifiableSet(availIds);

        if (log.isDebugEnabled()) {
            log.debug("Updated activeDataIds! " + activeDataIds);
            log.debug("Updated activeAvailIds! " + activeAvailabityIds);
        }
    }

    private boolean isNewAvailId(String availId) {
        if (activeAvailabityIds == null) {
            return true;
        }
        return !activeAvailabityIds.contains(availId);
    }

    private boolean isNewDataId(String dataId) {
        if (activeDataIds == null) {
            return true;
        }
        return !activeDataIds.contains(dataId);
    }

    private void publishCommand(String tenantId, String dataId) throws Exception {
        String type;
        String metricId;
        if (dataId.startsWith(AVAILABILITY)) {
            type = AVAILABILITY;
            metricId = dataId.substring(AVAILABILITY.length() + 1);
        } else if (dataId.startsWith(GAUGE)) {
            type = GAUGE;
            metricId = dataId.substring(GAUGE.length() + 1);
        } else if (dataId.startsWith(COUNTER)) {
            type = COUNTER;
            metricId = dataId.substring(COUNTER.length() + 1);
        } else if (dataId.startsWith(COUNTER_RATE)) {
            type = COUNTER_RATE;
            metricId = dataId.substring(COUNTER_RATE.length() + 1);
        } else if (dataId.startsWith(STRING)) {
            type = STRING;
            metricId = dataId.substring(STRING.length() + 1);
        } else if (dataId.startsWith(GAUGE_RATE)) {
            type = GAUGE_RATE;
            metricId = dataId.substring(GAUGE_RATE.length() + 1);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("DataId " + dataId + " doesn't have a valid metrics type. Skipping publishing command.");
            }
            return;
        }
        PublishCommandMessage msg = new PublishCommandMessage(PUBLISH_COMMAND, tenantId,
                asList(new PublishCommandMessage.MetricKey(type, metricId)));
        publishCommandSender.send(msg);
    }
}
