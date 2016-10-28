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

import java.io.IOException;
import java.util.Collection;
import java.util.TreeSet;
import java.util.function.Predicate;

import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.drools.compiler.compiler.DroolsParserException;
import org.drools.core.event.DebugAgendaEventListener;
import org.drools.core.event.DebugRuleRuntimeEventListener;
import org.hawkular.alerts.api.model.data.Data;
import org.hawkular.alerts.api.model.event.Event;
import org.hawkular.alerts.engine.service.RulesEngine;
import org.jboss.logging.Logger;
import org.kie.api.KieBase;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.KieServices;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.ObjectFilter;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.internal.KnowledgeBaseFactory;
import org.kie.internal.conf.MultithreadEvaluationOption;
import org.kie.internal.utils.KieHelper;

/**
 * An implementation of RulesEngine based on drools framework.
 *
 * This implementations has an approach of fixed rules based on filesystem.
 *
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
@Singleton
@TransactionAttribute(value= TransactionAttributeType.NOT_SUPPORTED)
public class DroolsRulesEngineImpl implements RulesEngine {
    // private final MsgLogger msgLog = MsgLogger.LOGGER;
    private final Logger log = Logger.getLogger(DroolsRulesEngineImpl.class);
    private static final String SESSION_NAME = "hawkular-alerts-engine-session";
    private static final long PERF_BATCHING_THRESHOLD= 3000L; // 3 seconds
    private static final long PERF_FIRING_THRESHOLD = 5000L; // 5 seconds

    private KieServices ks;
    private KieContainer kc;
    private KieSession kSession;

    TreeSet<Data> pendingData = new TreeSet<>();
    TreeSet<Event> pendingEvents = new TreeSet<>();

    public DroolsRulesEngineImpl() {
        log.debug("Creating instance.");
        buildKsession();
        if (log.isEnabled(Logger.Level.TRACE)) {
            kSession.addEventListener(new DebugAgendaEventListener());
            kSession.addEventListener(new DebugRuleRuntimeEventListener());
        }
    }

    private void buildKsession() {
        try {
            buildKieSession("org/hawkular/alerts/engine/rules/ConditionMatch.drl");
        } catch (Exception e) {
            log.fatal("Rules cannot be found", e);
        }
    }

    private void buildKieSession( String uri )
            throws DroolsParserException, IOException, Exception {

        final KieBaseConfiguration kieBaseConfiguration = KnowledgeBaseFactory.newKnowledgeBaseConfiguration();
        // kieBaseConfiguration.setOption(MultithreadEvaluationOption.YES);
        // kieBaseConfiguration.setOption(EventProcessingOption.STREAM);

        final KieBase kieBase = new KieHelper().setClassLoader(getClass().getClassLoader())
                .addFromClassPath(uri)
                .build(kieBaseConfiguration);

        kSession = kieBase.newKieSession();
        // Repeated just to focus on the agenda, not on all hawkular scenario
        kSession.addEventListener(new DebugAgendaEventListener());
        kSession.addEventListener(new DebugRuleRuntimeEventListener());
    }

    @Override
    public void addFact(Object fact) {
        if (fact instanceof Data || fact instanceof Event) {
            throw new IllegalArgumentException(fact.toString());
        }
        kSession.insert(fact);
        if (log.isDebugEnabled()) {
            log.debugf("addFact( %s )", fact.toString());
            log.debug("==> Begin Dump");
            for (FactHandle f : kSession.getFactHandles()) {
                Object sessionObject = kSession.getObject(f);
                log.debugf("Fact:  %s",sessionObject.toString());
            }
            log.debug("==> End Dump");
        }
    }

    @Override
    public void addFacts(Collection facts) {
        for (Object fact : facts) {
            if (fact instanceof Data || fact instanceof Event) {
                throw new IllegalArgumentException(fact.toString());
            }
        }
        for (Object fact : facts) {
            if (log.isDebugEnabled()) {
                log.debugf("Insert %s", fact);
            }
            kSession.insert(fact);
        }
        if (log.isDebugEnabled()) {
            log.debugf("addFacts( %s )", facts.toString());
            log.debug("==> Begin Dump");
            for (FactHandle f : kSession.getFactHandles()) {
                Object sessionObject = kSession.getObject(f);
                log.debugf("Fact:  %s", sessionObject.toString());
            }
            log.debug("==> End Dump");
        }
    }

    @Override
    public void addData(Data data) {
        pendingData.add(data);
    }

    @Override
    public void addData(Collection<Data> data) {
        pendingData.addAll(data);
    }

    @Override
    public void addEvent(Event event) {
        pendingEvents.add(event);
    }

    @Override
    public void addEvents(Collection<Event> events) {
        pendingEvents.addAll(events);
    }

    @Override
    public void addGlobal(String name, Object global) {
        if (log.isDebugEnabled()) {
            log.debugf("Add Global %s = %s ", name, global);
        }
        kSession.setGlobal(name, global);
    }

    @Override
    public void clear() {
        for (FactHandle factHandle : kSession.getFactHandles()) {
            if (log.isDebugEnabled()) {
                log.debugf("Delete %s", factHandle);
            }
            kSession.delete(factHandle);
        }
    }

    @Override
    public void fire() {
        // The rules engine requires that for any DataId only the oldest Data instance is processed in one
        // execution of the rules.  So, if we find multiple Data instances for the same Id, defer all but
        // the oldest to a subsequent run. Note that pendingData is already sorted by (id ASC, timestamp ASC) so
        // the iterator will present Data with the same id together, and time-ordered.
        int initialPendingData = pendingData.size();
        int initialPendingEvents = pendingEvents.size();
        int fireCycle = 0;
        long startFiring = System.currentTimeMillis();
        while (!pendingData.isEmpty() || !pendingEvents.isEmpty()) {
            log.debugf("Firing rules... PendingData [%s] PendingEvents [%s]", initialPendingData, initialPendingEvents);

            TreeSet<Data> batchData = new TreeSet<>(pendingData);
            Data previousData = null;

            pendingData.clear();

            long startBatching = System.currentTimeMillis();
            for (Data data : batchData) {
                if (null == previousData || !data.getId().equals(previousData.getId())) {
                    kSession.insert(data);
                    previousData = data;

                } else {
                    pendingData.add(data);
                    if (log.isTraceEnabled()) {
                        log.tracef("Deferring more recent %s until older %s is processed", data, previousData);
                    }
                }
            }
            long batchingTime = System.currentTimeMillis() - startBatching;
            log.debugf("Batching Data [%s] took [%s]", batchData.size(), batchingTime);
            if (batchingTime > PERF_BATCHING_THRESHOLD) {
                log.warnf("Batching Data [%s] took [%s] ms exceeding [%s] ms",
                        batchData.size(), batchingTime, PERF_BATCHING_THRESHOLD);
            }

            if (!pendingData.isEmpty() && log.isDebugEnabled()) {
                log.debugf("Deferring [%s] Datum(s) to next firing !!", pendingData.size());
            }

            batchData.clear();


            TreeSet<Event> batchEvent = new TreeSet<Event>(pendingEvents);
            Event previousEvent = null;

            pendingEvents.clear();

            startBatching = System.currentTimeMillis();
            for (Event event : batchEvent) {
                if (null == previousEvent
                        || (null != event.getDataId() && !event.getDataId().equals(previousEvent.getDataId()))) {
                    kSession.insert(event);
                    previousEvent = event;
                } else {
                    pendingEvents.add(event);
                    if (log.isTraceEnabled()) {
                        log.tracef("Deferring more recent %s until older %s is processed ", event, previousEvent);
                    }
                }
            }
            batchingTime = System.currentTimeMillis() - startBatching;
            log.debugf("Batching Events [%s] took [%s]", batchEvent.size(), batchingTime);
            if (batchingTime > PERF_BATCHING_THRESHOLD) {
                log.warnf("Batching Events [%s] took [%s] ms exceeding [%s] ms",
                        batchEvent.size(), batchingTime, PERF_BATCHING_THRESHOLD);
            }

            if (log.isDebugEnabled()) {
                log.debugf("Firing cycle [%s] - with these facts: ", fireCycle);
                for (FactHandle fact : kSession.getFactHandles()) {
                    Object o = kSession.getObject(fact);
                    log.debug("Fact:  " + o.toString());
                }
            }

            kSession.fireAllRules();
            fireCycle++;
        }
        long firingTime = System.currentTimeMillis() - startFiring;
        if (log.isDebugEnabled()) {
            log.debugf("Firing took [%s] ms", firingTime);
        }
        if (firingTime > PERF_FIRING_THRESHOLD) {
            log.warnf("Firing rules... PendingData [%s] PendingEvents [%s] took [%s] ms exceeding [%s] ms",
                    initialPendingData, initialPendingEvents, firingTime, PERF_FIRING_THRESHOLD);
        }
    }

    @Override
    public void fireNoData() {
        kSession.fireAllRules();
    }

    @Override
    public Object getFact(Object o) {
        Object result = null;
        FactHandle factHandle = kSession.getFactHandle(o);
        if (null != factHandle) {
            result = kSession.getObject(factHandle);
        }
        if (log.isDebugEnabled()) {
            log.debugf("getFact( %s )", o.toString());
            log.debug("==> Begin Dump");
            for (FactHandle fact : kSession.getFactHandles()) {
                Object sessionObject = kSession.getObject(fact);
                log.debugf("Fact:  %s", sessionObject.toString());
            }
            log.debug("==> End Dump");
        }
        return result;
    }

    @Override
    public void removeFact(Object fact) {
        FactHandle factHandle = kSession.getFactHandle(fact);
        if (factHandle != null) {
            if (log.isDebugEnabled()) {
                log.debugf("Delete %s", factHandle);
            }
            kSession.delete(factHandle);
        }
        if (log.isDebugEnabled()) {
            log.debugf("removeFact( %s )", fact.toString());
            log.debug("==> Begin Dump");
            for (FactHandle f : kSession.getFactHandles()) {
                Object sessionObject = kSession.getObject(f);
                log.debugf("Fact:  %s", sessionObject.toString());
            }
            log.debug("==> End Dump");
        }
    }

    @Override
    public void updateFact(Object fact) {
        FactHandle factHandle = kSession.getFactHandle(fact);
        if (factHandle != null) {
            if (log.isDebugEnabled()) {
                log.debugf("Update %s", factHandle);
            }
            kSession.update(factHandle, fact);
        }
        if (log.isDebugEnabled()) {
            log.debugf("updateFact( %s )", fact.toString());
            log.debug("==> Begin Dump");
            for (FactHandle f : kSession.getFactHandles()) {
                Object sessionObject = kSession.getObject(f);
                log.debugf("Fact:  %s", sessionObject.toString());
            }
            log.debug("==> End Dump");
        }
    }

    @Override
    public void removeFacts(Collection facts) {
        for (Object fact : facts) {
            removeFact(fact);
        }
    }

    @Override
    public void removeFacts(Predicate<Object> factFilter) {
        Collection<FactHandle> handles = kSession.getFactHandles(new ObjectFilter() {
            @Override
            public boolean accept(Object object) {
                return factFilter.test(object);
            }
        });

        if (null == handles) {
            return;
        }

        for (FactHandle h : handles) {
            removeFact(h);
        }
    }

    @Override
    public void removeGlobal(String name) {
        if (log.isDebugEnabled()) {
            log.debugf("Remove Global %s", name);
        }
        kSession.setGlobal(name, null);
    }

    @Override
    public void reset() {
        log.debug("Reset session");
        kSession.dispose();
        buildKsession();
    }
}
