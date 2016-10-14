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
package org.hawkular.alerts.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

import org.hawkular.alerts.api.json.JsonUtil;
import org.hawkular.alerts.api.model.condition.AvailabilityCondition;
import org.hawkular.alerts.api.model.condition.AvailabilityConditionEval;
import org.hawkular.alerts.api.model.condition.CompareCondition;
import org.hawkular.alerts.api.model.condition.CompareConditionEval;
import org.hawkular.alerts.api.model.condition.ConditionEval;
import org.hawkular.alerts.api.model.condition.EventCondition;
import org.hawkular.alerts.api.model.condition.ExternalCondition;
import org.hawkular.alerts.api.model.condition.ExternalConditionEval;
import org.hawkular.alerts.api.model.condition.MissingCondition;
import org.hawkular.alerts.api.model.condition.RateCondition;
import org.hawkular.alerts.api.model.condition.RateConditionEval;
import org.hawkular.alerts.api.model.condition.StringCondition;
import org.hawkular.alerts.api.model.condition.StringConditionEval;
import org.hawkular.alerts.api.model.condition.ThresholdCondition;
import org.hawkular.alerts.api.model.condition.ThresholdConditionEval;
import org.hawkular.alerts.api.model.condition.ThresholdRangeCondition;
import org.hawkular.alerts.api.model.condition.ThresholdRangeConditionEval;
import org.hawkular.alerts.api.model.dampening.Dampening;
import org.hawkular.alerts.api.model.data.AvailabilityType;
import org.hawkular.alerts.api.model.data.Data;
import org.hawkular.alerts.api.model.event.Alert;
import org.hawkular.alerts.api.model.event.Event;
import org.hawkular.alerts.api.model.event.EventCategory;
import org.hawkular.alerts.api.model.event.EventType;
import org.hawkular.alerts.api.model.trigger.Match;
import org.hawkular.alerts.api.model.trigger.Mode;
import org.hawkular.alerts.api.model.trigger.Trigger;
import org.hawkular.alerts.engine.impl.DroolsRulesEngineImpl;
import org.hawkular.alerts.engine.service.RulesEngine;
import org.hawkular.alerts.engine.util.MissingState;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Basic test of RulesEngine implementation.
 *
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
public class RulesEngineTest {
    private static final Logger log = Logger.getLogger(RulesEngineTest.class);

    RulesEngine rulesEngine = new DroolsRulesEngineImpl();
    List<Alert> alerts = new ArrayList<>();
    Set<Dampening> pendingTimeouts = new HashSet<>();
    Map<Trigger, List<Set<ConditionEval>>> autoResolvedTriggers = new HashMap<>();
    Set<Trigger> disabledTriggers = new CopyOnWriteArraySet<>();
    Set<Data> datums = new HashSet<Data>();
    Set<Event> inputEvents = new HashSet<>();
    List<Event> outputEvents = new ArrayList<>();
    Set<MissingState> missingStates = new HashSet<>();

    @Before
    public void before() {
        rulesEngine.addGlobal("log", log);
        rulesEngine.addGlobal("alerts", alerts);
        rulesEngine.addGlobal("events", outputEvents);
        rulesEngine.addGlobal("pendingTimeouts", pendingTimeouts);
        rulesEngine.addGlobal("autoResolvedTriggers", autoResolvedTriggers);
        rulesEngine.addGlobal("disabledTriggers", disabledTriggers);
        rulesEngine.addGlobal("missingStates", missingStates);
    }

    @After
    public void after() {
        rulesEngine.reset();
        alerts.clear();
        pendingTimeouts.clear();
        datums.clear();
        inputEvents.clear();
        outputEvents.clear();
        missingStates.clear();
    }

    @Test
    public void thresholdTest() {
        // 1 alert
        Trigger t1 = new Trigger("tenant", "trigger-1", "Threshold-LT");
        ThresholdCondition t1c1 = new ThresholdCondition("tenant", "trigger-1", 1, 1,
                "NumericData-01",
                ThresholdCondition.Operator.LT, 10.0);
        // 2 alert3
        Trigger t2 = new Trigger("tenant", "trigger-2", "Threshold-LTE");
        ThresholdCondition t2c1 = new ThresholdCondition("tenant", "trigger-2", 1, 1,
                "NumericData-01",
                ThresholdCondition.Operator.LTE, 10.0);
        // 1 alert
        Trigger t3 = new Trigger("tenant", "trigger-3", "Threshold-GT");
        ThresholdCondition t3c1 = new ThresholdCondition("tenant", "trigger-3", 1, 1,
                "NumericData-01",
                ThresholdCondition.Operator.GT, 10.0);
        // 2 alerts
        Trigger t4 = new Trigger("tenant", "trigger-4", "Threshold-GTE");
        ThresholdCondition t4c1 = new ThresholdCondition("tenant", "trigger-4", 1, 1,
                "NumericData-01",
                ThresholdCondition.Operator.GTE, 10.0);

        datums.add(Data.forNumeric("tenant", "NumericData-01", 1, 10.0));
        datums.add(Data.forNumeric("tenant", "NumericData-01", 2, 5.0));
        datums.add(Data.forNumeric("tenant", "NumericData-01", 3, 15.0));

        // default dampening

        t1.setEnabled(true);
        t2.setEnabled(true);
        t3.setEnabled(true);
        t4.setEnabled(true);

        rulesEngine.addFact(t1);
        rulesEngine.addFact(t1c1);
        rulesEngine.addFact(t2);
        rulesEngine.addFact(t2c1);
        rulesEngine.addFact(t3);
        rulesEngine.addFact(t3c1);
        rulesEngine.addFact(t4);
        rulesEngine.addFact(t4c1);

        rulesEngine.addData(datums);

        rulesEngine.fire();

        assertEquals(alerts.toString(), 6, alerts.size());
        Collections.sort(alerts, (Alert a1, Alert a2) -> a1.getTriggerId().compareTo(a2.getTriggerId()));

        Alert a = alerts.get(0);
        assertEquals(a.getTriggerId(), "trigger-1", a.getTriggerId());
        assertEquals(a.getEvalSets().toString(), 1, a.getEvalSets().size());
        Set<ConditionEval> eval = a.getEvalSets().get(0);
        assertEquals(eval.toString(), 1, eval.size());
        ThresholdConditionEval e = (ThresholdConditionEval) eval.iterator().next();
        assertEquals(e.toString(), 1, e.getConditionSetIndex());
        assertEquals(e.toString(), 1, e.getConditionSetSize());
        assertEquals("trigger-1", e.getTriggerId());
        assertTrue(e.isMatch());
        Double v = e.getValue();
        assertTrue(e.toString(), v.equals(5.0D));
        assertEquals(e.getCondition().toString(), "NumericData-01", e.getCondition().getDataId());

        a = alerts.get(1);
        assertEquals(a.getTriggerId(), "trigger-2", a.getTriggerId());
        assertEquals(a.getEvalSets().toString(), 1, a.getEvalSets().size());
        eval = a.getEvalSets().get(0);
        assertEquals(eval.toString(), 1, eval.size());
        e = (ThresholdConditionEval) eval.iterator().next();
        assertEquals(e.toString(), 1, e.getConditionSetIndex());
        assertEquals(e.toString(), 1, e.getConditionSetSize());
        assertEquals("trigger-2", e.getTriggerId());
        assertTrue(e.isMatch());
        v = e.getValue();
        assertTrue(e.toString(), v.equals(5.0D) || v.equals(10.0D));
        assertEquals(e.getCondition().toString(), "NumericData-01", e.getCondition().getDataId());

        a = alerts.get(2);
        assertEquals(a.getTriggerId(), "trigger-2", a.getTriggerId());
        assertEquals(a.getEvalSets().toString(), 1, a.getEvalSets().size());
        eval = a.getEvalSets().get(0);
        assertEquals(eval.toString(), 1, eval.size());
        e = (ThresholdConditionEval) eval.iterator().next();
        assertEquals(e.toString(), 1, e.getConditionSetIndex());
        assertEquals(e.toString(), 1, e.getConditionSetSize());
        assertEquals("trigger-2", e.getTriggerId());
        assertTrue(e.isMatch());
        assertTrue(!v.equals(e.getValue()));
        v = e.getValue();
        assertTrue(e.toString(), v.equals(5.0D) || v.equals(10.0D));
        assertEquals(e.getCondition().toString(), "NumericData-01", e.getCondition().getDataId());

        assertTrue(e.getCondition().getDataId().equals("NumericData-01"));

        a = alerts.get(3);
        assertEquals(a.getTriggerId(), "trigger-3", a.getTriggerId());
        assertEquals(a.getEvalSets().toString(), 1, a.getEvalSets().size());
        eval = a.getEvalSets().get(0);
        assertEquals(eval.toString(), 1, eval.size());
        e = (ThresholdConditionEval) eval.iterator().next();
        assertEquals(e.toString(), 1, e.getConditionSetIndex());
        assertEquals(e.toString(), 1, e.getConditionSetSize());
        assertEquals("trigger-3", e.getTriggerId());
        assertTrue(e.isMatch());
        v = e.getValue();
        assertTrue(e.toString(), v.equals(15.0D));
        assertEquals(e.getCondition().toString(), "NumericData-01", e.getCondition().getDataId());

        a = alerts.get(4);
        assertEquals(a.getTriggerId(), "trigger-4", a.getTriggerId());
        assertEquals(a.getEvalSets().toString(), 1, a.getEvalSets().size());
        eval = a.getEvalSets().get(0);
        assertEquals(eval.toString(), 1, eval.size());
        e = (ThresholdConditionEval) eval.iterator().next();
        assertEquals(e.toString(), 1, e.getConditionSetIndex());
        assertEquals(e.toString(), 1, e.getConditionSetSize());
        assertEquals("trigger-4", e.getTriggerId());
        assertTrue(e.isMatch());
        v = e.getValue();
        assertTrue(e.toString(), v.equals(15.0D) || v.equals(10.0D));
        assertEquals(e.getCondition().toString(), "NumericData-01", e.getCondition().getDataId());

        a = alerts.get(5);
        assertEquals(a.getTriggerId(), "trigger-4", a.getTriggerId());
        assertEquals(a.getEvalSets().toString(), 1, a.getEvalSets().size());
        eval = a.getEvalSets().get(0);
        assertEquals(eval.toString(), 1, eval.size());
        e = (ThresholdConditionEval) eval.iterator().next();
        assertEquals(e.toString(), 1, e.getConditionSetIndex());
        assertEquals(e.toString(), 1, e.getConditionSetSize());
        assertEquals("trigger-4", e.getTriggerId());
        assertTrue(e.isMatch());
        assertTrue(!v.equals(e.getValue()));
        v = e.getValue();
        assertTrue(e.toString(), v.equals(15.0D) || v.equals(10.0D));
        assertEquals(e.getCondition().toString(), "NumericData-01", e.getCondition().getDataId());
    }

    @Test
    public void thresholdRangeTest() {
        Trigger t1 = new Trigger("tenant", "trigger-1", "NumericData-01-");
        // should fire 2 alerts
        ThresholdRangeCondition t1c1 = new ThresholdRangeCondition("tenant", "trigger-1", 1, 1,
                "NumericData-01",
                ThresholdRangeCondition.Operator.INCLUSIVE,
                ThresholdRangeCondition.Operator.INCLUSIVE,
                10.0, 15.0,
                true);
        // should fine 0 alerts
        Trigger t2 = new Trigger("tenant", "trigger-2", "NumericData-01");
        ThresholdRangeCondition t2c1 = new ThresholdRangeCondition("tenant", "trigger-2", 1, 1,
                "NumericData-01",
                ThresholdRangeCondition.Operator.EXCLUSIVE,
                ThresholdRangeCondition.Operator.EXCLUSIVE,
                10.0, 15.0,
                true);
        // should fire 1 alert
        Trigger t3 = new Trigger("tenant", "trigger-3", "NumericData-01");
        ThresholdRangeCondition t3c1 = new ThresholdRangeCondition("tenant", "trigger-3", 1, 1,
                "NumericData-01",
                ThresholdRangeCondition.Operator.INCLUSIVE,
                ThresholdRangeCondition.Operator.INCLUSIVE,
                10.0, 15.0,
                false);

        datums.add(Data.forNumeric("tenant", "NumericData-01", 1, 10.0));
        datums.add(Data.forNumeric("tenant", "NumericData-01", 2, 5.0));
        datums.add(Data.forNumeric("tenant", "NumericData-01", 3, 15.0));

        // default dampening

        t1.setEnabled(true);
        t2.setEnabled(true);
        t3.setEnabled(true);

        rulesEngine.addFact(t1);
        rulesEngine.addFact(t1c1);
        rulesEngine.addFact(t2);
        rulesEngine.addFact(t2c1);
        rulesEngine.addFact(t3);
        rulesEngine.addFact(t3c1);

        rulesEngine.addData(datums);

        rulesEngine.fire();

        assertEquals(alerts.toString(), 3, alerts.size());
        Collections.sort(alerts, (Alert a1, Alert a2) -> a1.getTriggerId().compareTo(a2.getTriggerId()));
        Alert a = alerts.get(0);
        assertTrue(a.getTriggerId(), a.getTriggerId().equals("trigger-1"));
        assertEquals(1, a.getEvalSets().size());
        Set<ConditionEval> evals = a.getEvalSets().get(0);
        assertEquals(evals.toString(), 1, evals.size());
        ThresholdRangeConditionEval e = (ThresholdRangeConditionEval) evals.iterator().next();
        assertEquals(1, e.getConditionSetIndex());
        assertEquals(1, e.getConditionSetSize());
        assertTrue(e.toString(), e.getTriggerId().equals("trigger-1"));
        assertTrue(e.isMatch());
        Double v = e.getValue();
        assertTrue(e.toString(), v.equals(10.0D) || v.equals(15.0D));
        assertTrue(e.getCondition().toString(), e.getCondition().getDataId().equals("NumericData-01"));

        a = alerts.get(1);
        assertTrue(a.getTriggerId(), a.getTriggerId().equals("trigger-1"));
        assertEquals(1, a.getEvalSets().size());
        evals = a.getEvalSets().get(0);
        assertEquals(evals.toString(), 1, evals.size());
        e = (ThresholdRangeConditionEval) evals.iterator().next();
        assertEquals(1, e.getConditionSetIndex());
        assertEquals(1, e.getConditionSetSize());
        assertTrue(e.toString(), e.getTriggerId().equals("trigger-1"));
        assertTrue(e.isMatch());
        assertTrue(!v.equals(e.getValue()));
        v = e.getValue();
        assertTrue(e.toString(), v.equals(10.0D) || v.equals(15.0D));
        assertTrue(e.getCondition().toString(), e.getCondition().getDataId().equals("NumericData-01"));

        a = alerts.get(2);
        assertTrue(a.getTriggerId(), a.getTriggerId().equals("trigger-3"));
        assertEquals(1, a.getEvalSets().size());
        evals = a.getEvalSets().get(0);
        assertEquals(evals.toString(), 1, evals.size());
        e = (ThresholdRangeConditionEval) evals.iterator().next();
        assertEquals(1, e.getConditionSetIndex());
        assertEquals(1, e.getConditionSetSize());
        assertTrue(e.toString(), e.getTriggerId().equals("trigger-3"));
        assertTrue(e.isMatch());
        v = e.getValue();
        assertTrue(e.toString(), v.equals(5.0D));
        assertTrue(e.getCondition().toString(), e.getCondition().getDataId().equals("NumericData-01"));
    }

    @Test
    public void compareTest() {
        Trigger t1 = new Trigger("tenant", "trigger-1", "Compare-D1-LT-Half-D2");
        CompareCondition t1c1 = new CompareCondition("tenant", "trigger-1", 1, 1,
                "NumericData-01",
                CompareCondition.Operator.LT, 0.5, "NumericData-02");
        Trigger t2 = new Trigger("tenant", "trigger-2", "Compare-D1-LTE-Half-D2");
        CompareCondition t2c1 = new CompareCondition("tenant", "trigger-2", 1, 1,
                "NumericData-01",
                CompareCondition.Operator.LTE, 0.5, "NumericData-02");
        Trigger t3 = new Trigger("tenant", "trigger-3", "Compare-D1-GT-Half-D2");
        CompareCondition t3c1 = new CompareCondition("tenant", "trigger-3", 1, 1,
                "NumericData-01",
                CompareCondition.Operator.GT, 0.5, "NumericData-02");
        Trigger t4 = new Trigger("tenant", "trigger-4", "Compare-D1-GTE-Half-D2");
        CompareCondition t4c1 = new CompareCondition("tenant", "trigger-4", 1, 1,
                "NumericData-01",
                CompareCondition.Operator.GTE, 0.5, "NumericData-02");

        // default dampening

        t1.setEnabled(true);
        t2.setEnabled(true);
        t3.setEnabled(true);
        t4.setEnabled(true);

        rulesEngine.addFact(t1);
        rulesEngine.addFact(t1c1);
        rulesEngine.addFact(t2);
        rulesEngine.addFact(t2c1);
        rulesEngine.addFact(t3);
        rulesEngine.addFact(t3c1);
        rulesEngine.addFact(t4);
        rulesEngine.addFact(t4c1);

        // note that for compare conditions both datums need to be in WM for the same rules execution.  This
        // has some subtleties.  Because one rule execution will only see the the oldest datum for a specific
        // dataId, we need several rule executions to test all of the above triggers.

        // Test LT (also LTE)
        datums.add(Data.forNumeric("tenant", "NumericData-01", 1, 10.0));
        datums.add(Data.forNumeric("tenant", "NumericData-02", 2, 30.0));

        rulesEngine.addData(datums);

        rulesEngine.fire();

        assertEquals(alerts.toString(), 2, alerts.size());
        Collections.sort(alerts, (Alert a1, Alert a2) -> a1.getTriggerId().compareTo(a2.getTriggerId()));

        Alert a = alerts.get(0);
        assertTrue(a.getTriggerId(), a.getTriggerId().equals("trigger-1"));
        assertEquals(1, a.getEvalSets().size());
        Set<ConditionEval> evals = a.getEvalSets().get(0);
        assertEquals(evals.toString(), 1, evals.size());
        CompareConditionEval e = (CompareConditionEval) evals.iterator().next();
        assertEquals(1, e.getConditionSetIndex());
        assertEquals(1, e.getConditionSetSize());
        assertTrue(e.toString(), e.getTriggerId().equals("trigger-1"));
        assertTrue(e.isMatch());
        Double v1 = e.getValue1();
        Double v2 = e.getValue2();
        assertTrue(e.toString(), v1.equals(10.0D));
        assertTrue(e.toString(), v2.equals(30.0D));
        assertTrue(e.getCondition().toString(), e.getCondition().getDataId().equals("NumericData-01"));
        assertTrue(e.getCondition().toString(), e.getCondition().getData2Id().equals("NumericData-02"));

        a = alerts.get(1);
        assertTrue(a.getTriggerId(), a.getTriggerId().equals("trigger-2"));
        assertEquals(1, a.getEvalSets().size());
        evals = a.getEvalSets().get(0);
        assertEquals(evals.toString(), 1, evals.size());
        e = (CompareConditionEval) evals.iterator().next();
        assertEquals(1, e.getConditionSetIndex());
        assertEquals(1, e.getConditionSetSize());
        assertTrue(e.toString(), e.getTriggerId().equals("trigger-2"));
        assertTrue(e.isMatch());
        v1 = e.getValue1();
        v2 = e.getValue2();
        assertTrue(e.toString(), v1.equals(10.0D));
        assertTrue(e.toString(), v2.equals(30.0D));
        assertTrue(e.getCondition().toString(), e.getCondition().getDataId().equals("NumericData-01"));
        assertTrue(e.getCondition().toString(), e.getCondition().getData2Id().equals("NumericData-02"));

        // Test LTE + GTE
        datums.clear();
        alerts.clear();
        datums.add(Data.forNumeric("tenant", "NumericData-01", 3, 10.0));
        datums.add(Data.forNumeric("tenant", "NumericData-02", 4, 20.0));

        rulesEngine.addData(datums);

        rulesEngine.fire();

        assertEquals(alerts.toString(), 2, alerts.size());
        Collections.sort(alerts, (Alert a1, Alert a2) -> a1.getTriggerId().compareTo(a2.getTriggerId()));

        a = alerts.get(0);
        assertTrue(a.getTriggerId(), a.getTriggerId().equals("trigger-2"));
        assertEquals(1, a.getEvalSets().size());
        evals = a.getEvalSets().get(0);
        assertEquals(evals.toString(), 1, evals.size());
        e = (CompareConditionEval) evals.iterator().next();
        assertEquals(1, e.getConditionSetIndex());
        assertEquals(1, e.getConditionSetSize());
        assertTrue(e.toString(), e.getTriggerId().equals("trigger-2"));
        assertTrue(e.isMatch());
        v1 = e.getValue1();
        v2 = e.getValue2();
        assertTrue(e.toString(), v1.equals(10.0D));
        assertTrue(e.toString(), v2.equals(20.0D));
        assertTrue(e.getCondition().toString(), e.getCondition().getDataId().equals("NumericData-01"));
        assertTrue(e.getCondition().toString(), e.getCondition().getData2Id().equals("NumericData-02"));

        a = alerts.get(1);
        assertTrue(a.getTriggerId(), a.getTriggerId().equals("trigger-4"));
        assertEquals(1, a.getEvalSets().size());
        evals = a.getEvalSets().get(0);
        assertEquals(evals.toString(), 1, evals.size());
        e = (CompareConditionEval) evals.iterator().next();
        assertEquals(1, e.getConditionSetIndex());
        assertEquals(1, e.getConditionSetSize());
        assertTrue(e.toString(), e.getTriggerId().equals("trigger-4"));
        assertTrue(e.isMatch());
        v1 = e.getValue1();
        v2 = e.getValue2();
        assertTrue(e.toString(), v1.equals(10.0D));
        assertTrue(e.toString(), v2.equals(20.0D));
        assertTrue(e.getCondition().toString(), e.getCondition().getDataId().equals("NumericData-01"));
        assertTrue(e.getCondition().toString(), e.getCondition().getData2Id().equals("NumericData-02"));

        // Test GT (also GTE)
        datums.clear();
        alerts.clear();
        datums.add(Data.forNumeric("tenant", "NumericData-01", 5, 15.0));
        datums.add(Data.forNumeric("tenant", "NumericData-02", 6, 20.0));

        rulesEngine.addData(datums);

        rulesEngine.fire();

        assertEquals(alerts.toString(), 2, alerts.size());
        Collections.sort(alerts, (Alert a1, Alert a2) -> a1.getTriggerId().compareTo(a2.getTriggerId()));

        a = alerts.get(0);
        assertTrue(a.getTriggerId(), a.getTriggerId().equals("trigger-3"));
        assertEquals(1, a.getEvalSets().size());
        evals = a.getEvalSets().get(0);
        assertEquals(evals.toString(), 1, evals.size());
        e = (CompareConditionEval) evals.iterator().next();
        assertEquals(1, e.getConditionSetIndex());
        assertEquals(1, e.getConditionSetSize());
        assertTrue(e.toString(), e.getTriggerId().equals("trigger-3"));
        assertTrue(e.isMatch());
        v1 = e.getValue1();
        v2 = e.getValue2();
        assertTrue(e.toString(), v1.equals(15.0D));
        assertTrue(e.toString(), v2.equals(20.0D));
        assertTrue(e.getCondition().toString(), e.getCondition().getDataId().equals("NumericData-01"));
        assertTrue(e.getCondition().toString(), e.getCondition().getData2Id().equals("NumericData-02"));

        a = alerts.get(1);
        assertTrue(a.getTriggerId(), a.getTriggerId().equals("trigger-4"));
        assertEquals(1, a.getEvalSets().size());
        evals = a.getEvalSets().get(0);
        assertEquals(evals.toString(), 1, evals.size());
        e = (CompareConditionEval) evals.iterator().next();
        assertEquals(1, e.getConditionSetIndex());
        assertEquals(1, e.getConditionSetSize());
        assertTrue(e.toString(), e.getTriggerId().equals("trigger-4"));
        assertTrue(e.isMatch());
        v1 = e.getValue1();
        v2 = e.getValue2();
        assertTrue(e.toString(), v1.equals(15.0D));
        assertTrue(e.toString(), v2.equals(20.0D));
        assertTrue(e.getCondition().toString(), e.getCondition().getDataId().equals("NumericData-01"));
        assertTrue(e.getCondition().toString(), e.getCondition().getData2Id().equals("NumericData-02"));
    }

    @Test
    public void compareTest2() {
        Trigger t1 = new Trigger("tenant", "trigger-1", "Compare-D1-LT-Half-D2");
        CompareCondition t1c1 = new CompareCondition("tenant", "trigger-1", 1, 1,
                "NumericData-01",
                CompareCondition.Operator.LT, 0.5, "NumericData-02");
        Dampening t1d = Dampening.forRelaxedCount("tenant", "trigger-1", Mode.FIRING, 2, 3);

        t1.setEnabled(true);

        rulesEngine.addFact(t1);
        rulesEngine.addFact(t1c1);
        rulesEngine.addFact(t1d);

        // Test Adding 1 datum at a time, with some repetition

        // T1: D1=10, D2 unset : no condition eval
        datums.add(Data.forNumeric("tenant", "NumericData-01", 1, 10.0));
        rulesEngine.addData(datums);
        rulesEngine.fire();
        assertEquals(alerts.toString(), 0, alerts.size());

        // T2: D1=15, D2 unset : no condition eval
        datums.clear();
        datums.add(Data.forNumeric("tenant", "NumericData-01", 2, 15.0));
        rulesEngine.addData(datums);
        rulesEngine.fire();
        assertEquals(alerts.toString(), 0, alerts.size());

        // T3: D1=15, D2=25 : Eval False, dampening 0 for 1
        datums.clear();
        datums.add(Data.forNumeric("tenant", "NumericData-02", 3, 25.0));
        rulesEngine.addData(datums);
        rulesEngine.fire();
        assertEquals(alerts.toString(), 0, alerts.size());

        // T4: D1=15, D2=50 : Eval True, dampening 1 for 2
        datums.clear();
        datums.add(Data.forNumeric("tenant", "NumericData-02", 4, 50.0));
        rulesEngine.addData(datums);
        rulesEngine.fire();
        assertEquals(alerts.toString(), 0, alerts.size());

        // T5: D1=20, D2=50 : Eval True, dampening 2 for 3 -> fire
        datums.clear();
        datums.add(Data.forNumeric("tenant", "NumericData-01", 5, 20.0));
        rulesEngine.addData(datums);
        rulesEngine.fire();
        assertEquals(alerts.toString(), 1, alerts.size());

        Alert a = alerts.get(0);
        assertTrue(a.getTriggerId(), a.getTriggerId().equals("trigger-1"));
        assertEquals(2, a.getEvalSets().size());
        Set<ConditionEval> evals = a.getEvalSets().get(0);
        assertEquals(evals.toString(), 1, evals.size());
        CompareConditionEval e = (CompareConditionEval) evals.iterator().next();
        assertEquals(1, e.getConditionSetIndex());
        assertEquals(1, e.getConditionSetSize());
        assertTrue(e.toString(), e.getTriggerId().equals("trigger-1"));
        assertTrue(e.isMatch());
        Double v1 = e.getValue1();
        Double v2 = e.getValue2();
        assertTrue(e.toString(), v1.equals(15.0D));
        assertTrue(e.toString(), v2.equals(50.0D));
        assertTrue(e.getCondition().toString(), e.getCondition().getDataId().equals("NumericData-01"));
        assertTrue(e.getCondition().toString(), e.getCondition().getData2Id().equals("NumericData-02"));

        evals = a.getEvalSets().get(1);
        assertEquals(evals.toString(), 1, evals.size());
        e = (CompareConditionEval) evals.iterator().next();
        assertEquals(1, e.getConditionSetIndex());
        assertEquals(1, e.getConditionSetSize());
        assertTrue(e.toString(), e.getTriggerId().equals("trigger-1"));
        assertTrue(e.isMatch());
        v1 = e.getValue1();
        v2 = e.getValue2();
        assertTrue(e.toString(), v1.equals(20.0D));
        assertTrue(e.toString(), v2.equals(50.0D));
        assertTrue(e.getCondition().toString(), e.getCondition().getDataId().equals("NumericData-01"));
        assertTrue(e.getCondition().toString(), e.getCondition().getData2Id().equals("NumericData-02"));
    }

    @Test
    public void multipleConditionsOnSameDataIdWithAnyMatching() {
        Trigger t1 = new Trigger("tenant", "trigger-1", "Multiple Conditions in ANY");
        t1.setFiringMatch(Match.ANY);
        t1.setEventType(EventType.EVENT);
        t1.setEnabled(true);
        ThresholdCondition t1c1 = new ThresholdCondition("tenant", "trigger-1", 2, 1,
                "HeapUsed",
                ThresholdCondition.Operator.LT, 10.0);
        ThresholdCondition t1c2 = new ThresholdCondition("tenant", "trigger-1", 2, 2,
                "HeapUsed",
                ThresholdCondition.Operator.GT, 20.0);

        // Default dampening

        rulesEngine.addFacts(Arrays.asList(t1, t1c1, t1c2));

        datums.add(Data.forNumeric("tenant", "HeapUsed", 1, 9.0));
        datums.add(Data.forNumeric("tenant", "HeapUsed", 2, 11.0));
        datums.add(Data.forNumeric("tenant", "HeapUsed", 3, 21.0));
        datums.add(Data.forNumeric("tenant", "HeapUsed", 4, 19.0));

        rulesEngine.addData(datums);

        rulesEngine.fire();

        assertEquals(2, outputEvents.size());
    }

    @Test
    public void StringTest() {
        // StringData-01 Triggers
        // 2 alerts
        Trigger t1 = new Trigger("tenant", "trigger-1", "String-StartsWith");
        StringCondition t1c1 = new StringCondition("tenant", "trigger-1", 1, 1,
                "StringData-01",
                StringCondition.Operator.STARTS_WITH, "Fred", false);
        // 1 alert
        Trigger t2 = new Trigger("tenant", "trigger-2", "String-Equal");
        StringCondition t3c1 = new StringCondition("tenant", "trigger-2", 1, 1,
                "StringData-01",
                StringCondition.Operator.EQUAL, "Fred", false);
        // 1 alert
        Trigger t3 = new Trigger("tenant", "trigger-3", "String-Contains");
        StringCondition t5c1 = new StringCondition("tenant", "trigger-3", 1, 1,
                "StringData-01",
                StringCondition.Operator.CONTAINS, "And", false);
        // 1 alert
        Trigger t4 = new Trigger("tenant", "trigger-4", "String-Match");
        StringCondition t6c1 = new StringCondition("tenant", "trigger-4", 1, 1,
                "StringData-01",
                StringCondition.Operator.MATCH, "Fred.*Barney", false);

        // StringData-02 Triggers
        // 1 alert
        Trigger t5 = new Trigger("tenant", "trigger-5", "String-EndsWith");
        StringCondition t2c1 = new StringCondition("tenant", "trigger-5", 1, 1,
                "StringData-02",
                StringCondition.Operator.ENDS_WITH, "Fred", false);
        // 1 alert
        Trigger t6 = new Trigger("tenant", "trigger-6", "String-StartsWith");
        StringCondition t4c1 = new StringCondition("tenant", "trigger-6", 1, 1,
                "StringData-02", // note
                StringCondition.Operator.NOT_EQUAL, "Fred", false);

        datums.add(Data.forString("tenant", "StringData-01", 1, "Fred"));
        datums.add(Data.forString("tenant", "StringData-01", 2, "Fred And Barney"));

        datums.add(Data.forString("tenant", "StringData-02", 1, "Barney And Fred"));

        // default dampening

        t1.setEnabled(true);
        t2.setEnabled(true);
        t3.setEnabled(true);
        t4.setEnabled(true);
        t5.setEnabled(true);
        t6.setEnabled(true);

        rulesEngine.addFact(t1);
        rulesEngine.addFact(t1c1);
        rulesEngine.addFact(t2);
        rulesEngine.addFact(t2c1);
        rulesEngine.addFact(t3);
        rulesEngine.addFact(t3c1);
        rulesEngine.addFact(t4);
        rulesEngine.addFact(t4c1);
        rulesEngine.addFact(t5);
        rulesEngine.addFact(t5c1);
        rulesEngine.addFact(t6);
        rulesEngine.addFact(t6c1);

        rulesEngine.addData(datums);

        rulesEngine.fire();

        assertEquals(alerts.toString(), 7, alerts.size());
        Collections.sort(alerts, (Alert a1, Alert a2) -> a1.getTriggerId().compareTo(a2.getTriggerId()));

        Alert a = alerts.get(0);
        assertTrue(a.getTriggerId(), a.getTriggerId().equals("trigger-1"));
        assertEquals(1, a.getEvalSets().size());
        Set<ConditionEval> evals = a.getEvalSets().get(0);
        assertEquals(1, evals.size());
        StringConditionEval e = (StringConditionEval) evals.iterator().next();
        assertEquals(1, e.getConditionSetIndex());
        assertEquals(1, e.getConditionSetSize());
        assertEquals("trigger-1", e.getTriggerId());
        assertTrue(e.isMatch());
        String v = e.getValue();
        assertEquals("Fred", v);
        assertEquals("StringData-01", e.getCondition().getDataId());

        a = alerts.get(1);
        assertTrue(a.getTriggerId(), a.getTriggerId().equals("trigger-1"));
        assertEquals(1, a.getEvalSets().size());
        evals = a.getEvalSets().get(0);
        assertEquals(1, evals.size());
        e = (StringConditionEval) evals.iterator().next();
        assertEquals(1, e.getConditionSetIndex());
        assertEquals(1, e.getConditionSetSize());
        assertEquals("trigger-1", e.getTriggerId());
        assertTrue(e.isMatch());
        v = e.getValue();
        assertEquals("Fred And Barney", v);
        assertEquals("StringData-01", e.getCondition().getDataId());

        a = alerts.get(2);
        assertTrue(a.getTriggerId(), a.getTriggerId().equals("trigger-2"));
        assertEquals(1, a.getEvalSets().size());
        evals = a.getEvalSets().get(0);
        assertEquals(1, evals.size());
        e = (StringConditionEval) evals.iterator().next();
        assertEquals(1, e.getConditionSetIndex());
        assertEquals(1, e.getConditionSetSize());
        assertEquals("trigger-2", e.getTriggerId());
        assertTrue(e.isMatch());
        v = e.getValue();
        assertEquals("Fred", v);
        assertEquals("StringData-01", e.getCondition().getDataId());

        a = alerts.get(3);
        assertTrue(a.getTriggerId(), a.getTriggerId().equals("trigger-3"));
        assertEquals(1, a.getEvalSets().size());
        evals = a.getEvalSets().get(0);
        assertEquals(1, evals.size());
        e = (StringConditionEval) evals.iterator().next();
        assertEquals(1, e.getConditionSetIndex());
        assertEquals(1, e.getConditionSetSize());
        assertEquals("trigger-3", e.getTriggerId());
        assertTrue(e.isMatch());
        v = e.getValue();
        assertEquals("Fred And Barney", v);
        assertEquals("StringData-01", e.getCondition().getDataId());

        a = alerts.get(4);
        assertTrue(a.getTriggerId(), a.getTriggerId().equals("trigger-4"));
        assertEquals(1, a.getEvalSets().size());
        evals = a.getEvalSets().get(0);
        assertEquals(1, evals.size());
        e = (StringConditionEval) evals.iterator().next();
        assertEquals(1, e.getConditionSetIndex());
        assertEquals(1, e.getConditionSetSize());
        assertEquals("trigger-4", e.getTriggerId());
        assertTrue(e.isMatch());
        v = e.getValue();
        assertEquals("Fred And Barney", v);
        assertEquals("StringData-01", e.getCondition().getDataId());

        a = alerts.get(5);
        assertTrue(a.getTriggerId(), a.getTriggerId().equals("trigger-5"));
        assertEquals(1, a.getEvalSets().size());
        evals = a.getEvalSets().get(0);
        assertEquals(1, evals.size());
        e = (StringConditionEval) evals.iterator().next();
        assertEquals(1, e.getConditionSetIndex());
        assertEquals(1, e.getConditionSetSize());
        assertEquals("trigger-5", e.getTriggerId());
        assertTrue(e.isMatch());
        v = e.getValue();
        assertEquals("Barney And Fred", v);
        assertEquals("StringData-02", e.getCondition().getDataId());

        a = alerts.get(6);
        assertTrue(a.getTriggerId(), a.getTriggerId().equals("trigger-6"));
        assertEquals(1, a.getEvalSets().size());
        evals = a.getEvalSets().get(0);
        assertEquals(1, evals.size());
        e = (StringConditionEval) evals.iterator().next();
        assertEquals(1, e.getConditionSetIndex());
        assertEquals(1, e.getConditionSetSize());
        assertEquals("trigger-6", e.getTriggerId());
        assertTrue(e.isMatch());
        v = e.getValue();
        assertEquals("Barney And Fred", v);
        assertEquals("StringData-02", e.getCondition().getDataId());
    }

    @Test
    public void StringTestIgnoreCase() {
        // StringData-01 Triggers
        // 2 alerts
        Trigger t1 = new Trigger("tenant", "trigger-1", "String-StartsWith");
        StringCondition t1c1 = new StringCondition("tenant", "trigger-1", 1, 1,
                "StringData-01",
                StringCondition.Operator.STARTS_WITH, "FRED", true);
        // 1 alert
        Trigger t2 = new Trigger("tenant", "trigger-2", "String-Equal");
        StringCondition t3c1 = new StringCondition("tenant", "trigger-2", 1, 1,
                "StringData-01",
                StringCondition.Operator.EQUAL, "FRED", true);
        // 1 alert
        Trigger t3 = new Trigger("tenant", "trigger-3", "String-Contains");
        StringCondition t5c1 = new StringCondition("tenant", "trigger-3", 1, 1,
                "StringData-01",
                StringCondition.Operator.CONTAINS, "AND", true);
        // 1 alert
        Trigger t4 = new Trigger("tenant", "trigger-4", "String-Match");
        StringCondition t6c1 = new StringCondition("tenant", "trigger-4", 1, 1,
                "StringData-01",
                StringCondition.Operator.MATCH, "FRED.*barney", true);

        // StringData-02 Triggers
        // 1 alert
        Trigger t5 = new Trigger("tenant", "trigger-5", "String-EndsWith");
        StringCondition t2c1 = new StringCondition("tenant", "trigger-5", 1, 1,
                "StringData-02",
                StringCondition.Operator.ENDS_WITH, "FRED", true);
        // 1 alert
        Trigger t6 = new Trigger("tenant", "trigger-6", "String-StartsWith");
        StringCondition t4c1 = new StringCondition("tenant", "trigger-6", 1, 1,
                "StringData-02", // note
                StringCondition.Operator.NOT_EQUAL, "FRED", true);

        datums.add(Data.forString("tenant", "StringData-01", 1, "Fred"));
        datums.add(Data.forString("tenant", "StringData-01", 2, "Fred And Barney"));

        datums.add(Data.forString("tenant", "StringData-02", 1, "Barney And Fred"));

        // default dampening

        t1.setEnabled(true);
        t2.setEnabled(true);
        t3.setEnabled(true);
        t4.setEnabled(true);
        t5.setEnabled(true);
        t6.setEnabled(true);

        rulesEngine.addFact(t1);
        rulesEngine.addFact(t1c1);
        rulesEngine.addFact(t2);
        rulesEngine.addFact(t2c1);
        rulesEngine.addFact(t3);
        rulesEngine.addFact(t3c1);
        rulesEngine.addFact(t4);
        rulesEngine.addFact(t4c1);
        rulesEngine.addFact(t5);
        rulesEngine.addFact(t5c1);
        rulesEngine.addFact(t6);
        rulesEngine.addFact(t6c1);

        rulesEngine.addData(datums);

        rulesEngine.fire();

        assertEquals(alerts.toString(), 7, alerts.size());
        Collections.sort(alerts, (Alert a1, Alert a2) -> a1.getTriggerId().compareTo(a2.getTriggerId()));

        Alert a = alerts.get(0);
        assertEquals("trigger-1", a.getTriggerId());
        assertEquals(1, a.getEvalSets().size());
        Set<ConditionEval> evals = a.getEvalSets().get(0);
        assertEquals(1, evals.size());
        StringConditionEval e = (StringConditionEval) evals.iterator().next();
        assertEquals(1, e.getConditionSetIndex());
        assertEquals(1, e.getConditionSetSize());
        assertEquals("trigger-1", e.getTriggerId());
        assertTrue(e.isMatch());
        String v = e.getValue();
        assertEquals("Fred", v);
        assertEquals("StringData-01", e.getCondition().getDataId());

        a = alerts.get(1);
        assertEquals("trigger-1", a.getTriggerId());
        assertEquals(1, a.getEvalSets().size());
        evals = a.getEvalSets().get(0);
        assertEquals(1, evals.size());
        e = (StringConditionEval) evals.iterator().next();
        assertEquals(1, e.getConditionSetIndex());
        assertEquals(1, e.getConditionSetSize());
        assertEquals("trigger-1", e.getTriggerId());
        assertTrue(e.isMatch());
        v = e.getValue();
        assertEquals("Fred And Barney", v);
        assertEquals("StringData-01", e.getCondition().getDataId());

        a = alerts.get(2);
        assertEquals("trigger-2", a.getTriggerId());
        assertEquals(1, a.getEvalSets().size());
        evals = a.getEvalSets().get(0);
        assertEquals(1, evals.size());
        e = (StringConditionEval) evals.iterator().next();
        assertEquals(1, e.getConditionSetIndex());
        assertEquals(1, e.getConditionSetSize());
        assertEquals("trigger-2", e.getTriggerId());
        assertTrue(e.isMatch());
        v = e.getValue();
        assertEquals("Fred", v);
        assertEquals("StringData-01", e.getCondition().getDataId());

        a = alerts.get(3);
        assertEquals("trigger-3", a.getTriggerId());
        assertEquals(1, a.getEvalSets().size());
        evals = a.getEvalSets().get(0);
        assertEquals(1, evals.size());
        e = (StringConditionEval) evals.iterator().next();
        assertEquals(1, e.getConditionSetIndex());
        assertEquals(1, e.getConditionSetSize());
        assertEquals("trigger-3", e.getTriggerId());
        assertTrue(e.isMatch());
        v = e.getValue();
        assertEquals("Fred And Barney", v);
        assertEquals("StringData-01", e.getCondition().getDataId());

        a = alerts.get(4);
        assertEquals("trigger-4", a.getTriggerId());
        assertEquals(1, a.getEvalSets().size());
        evals = a.getEvalSets().get(0);
        assertEquals(1, evals.size());
        e = (StringConditionEval) evals.iterator().next();
        assertEquals(1, e.getConditionSetIndex());
        assertEquals(1, e.getConditionSetSize());
        assertEquals("trigger-4", e.getTriggerId());
        assertTrue(e.isMatch());
        v = e.getValue();
        assertEquals("Fred And Barney", v);
        assertEquals("StringData-01", e.getCondition().getDataId());

        a = alerts.get(5);
        assertEquals("trigger-5", a.getTriggerId());
        assertEquals(1, a.getEvalSets().size());
        evals = a.getEvalSets().get(0);
        assertEquals(1, evals.size());
        e = (StringConditionEval) evals.iterator().next();
        assertEquals(1, e.getConditionSetIndex());
        assertEquals(1, e.getConditionSetSize());
        assertEquals("trigger-5", e.getTriggerId());
        assertTrue(e.isMatch());
        v = e.getValue();
        assertEquals("Barney And Fred", v);
        assertEquals("StringData-02", e.getCondition().getDataId());

        a = alerts.get(6);
        assertEquals("trigger-6", a.getTriggerId());
        assertEquals(1, a.getEvalSets().size());
        evals = a.getEvalSets().get(0);
        assertEquals(1, evals.size());
        e = (StringConditionEval) evals.iterator().next();
        assertEquals(1, e.getConditionSetIndex());
        assertEquals(1, e.getConditionSetSize());
        assertEquals("trigger-6", e.getTriggerId());
        assertTrue(e.isMatch());
        v = e.getValue();
        assertEquals("Barney And Fred", v);
        assertEquals("StringData-02", e.getCondition().getDataId());
    }

    @Test
    public void availabilityTest() {
        Trigger t1 = new Trigger("tenant", "trigger-1", "Avail-DOWN");
        AvailabilityCondition t1c1 = new AvailabilityCondition("tenant", "trigger-1", 1, 1,
                "AvailData-01", AvailabilityCondition.Operator.NOT_UP);

        datums.add(Data.forAvailability("tenant", "AvailData-01", 1, AvailabilityType.DOWN));
        datums.add(Data.forAvailability("tenant", "AvailData-01", 2, AvailabilityType.UNAVAILABLE));
        datums.add(Data.forAvailability("tenant", "AvailData-01", 3, AvailabilityType.UP));

        // default dampening

        t1.setEnabled(true);

        rulesEngine.addFact(t1);
        rulesEngine.addFact(t1c1);

        rulesEngine.addData(datums);

        rulesEngine.fire();

        assertEquals(alerts.toString(), 2, alerts.size());

        Alert a = alerts.get(0);
        assertEquals("trigger-1", a.getTriggerId());
        assertEquals(1, a.getEvalSets().size());
        Set<ConditionEval> evals = a.getEvalSets().get(0);
        assertEquals(1, evals.size());
        AvailabilityConditionEval e = (AvailabilityConditionEval) evals.iterator().next();
        assertEquals(1, e.getConditionSetIndex());
        assertEquals(1, e.getConditionSetSize());
        assertEquals("trigger-1", e.getTriggerId());
        assertTrue(e.isMatch());
        AvailabilityType v = e.getValue();
        assertEquals(AvailabilityType.DOWN, v);
        assertEquals("AvailData-01", e.getCondition().getDataId());

        a = alerts.get(1);
        assertEquals("trigger-1", a.getTriggerId());
        assertEquals(1, a.getEvalSets().size());
        evals = a.getEvalSets().get(0);
        assertEquals(1, evals.size());
        e = (AvailabilityConditionEval) evals.iterator().next();
        assertEquals(1, e.getConditionSetIndex());
        assertEquals(1, e.getConditionSetSize());
        assertEquals("trigger-1", e.getTriggerId());
        assertTrue(e.isMatch());
        v = e.getValue();
        assertEquals(AvailabilityType.UNAVAILABLE, v);
        assertEquals("AvailData-01", e.getCondition().getDataId());
    }

    @Test
    public void externalTest() {
        Trigger t1 = new Trigger("tenant", "trigger-1", "External-Metrics");
        ExternalCondition t1c1 = new ExternalCondition("tenant", "trigger-1", Mode.FIRING, 1, 1,
                "ExternalData-01", "HawkularMetrics", "metric:5:avg(foo > 100.5)");

        datums.add(Data.forString("tenant", "ExternalData-01", 1, "Ignored"));

        // default dampening

        t1.setEnabled(true);

        rulesEngine.addFact(t1);
        rulesEngine.addFact(t1c1);

        rulesEngine.addData(datums);

        rulesEngine.fire();

        assertEquals(alerts.toString(), 1, alerts.size());

        Alert a = alerts.get(0);
        assertEquals("trigger-1", a.getTriggerId());
        assertEquals(1, a.getEvalSets().size());
        Set<ConditionEval> evals = a.getEvalSets().get(0);
        assertEquals(1, evals.size());
        ExternalConditionEval e = (ExternalConditionEval) evals.iterator().next();
        assertEquals(1, e.getConditionSetIndex());
        assertEquals(1, e.getConditionSetSize());
        assertEquals("trigger-1", e.getTriggerId());
        assertTrue(e.isMatch());
        String v = e.getValue();
        assertEquals("Ignored", v);
        assertEquals("ExternalData-01", e.getCondition().getDataId());
    }

    @Test
    public void eventTest() {
        Trigger t1 = new Trigger("tenant", "trigger-1", "Events Test");
        t1.setEventType(EventType.EVENT);
        EventCondition t1c1 = new EventCondition("tenant", "trigger-1", "myapp.war", "text == 'DOWN'");

        Event appDownEvent = new Event("tenant", UUID.randomUUID().toString(), "myapp.war",
                EventCategory.DEPLOYMENT.name(), "DOWN");
        inputEvents.add(appDownEvent);

        t1.setEnabled(true);

        rulesEngine.addFact(t1);
        rulesEngine.addFact(t1c1);

        rulesEngine.addEvents(inputEvents);

        rulesEngine.fire();

        assertEquals(outputEvents.toString(), 1, outputEvents.size());
    }

    @Test
    public void rateTest() {
        // 1 alert
        Trigger t1 = new Trigger("tenant", "trigger-1", "Rate-Increasing");
        RateCondition t1c1 = new RateCondition("tenant", "trigger-1",
                "CounterUp",
                RateCondition.Direction.INCREASING,
                RateCondition.Period.MINUTE,
                RateCondition.Operator.GT, 20.0);
        // 1 alert
        Trigger t2 = new Trigger("tenant", "trigger-2", "Rate-Decreasing");
        RateCondition t2c1 = new RateCondition("tenant", "trigger-2",
                "CounterDown",
                RateCondition.Direction.DECREASING,
                RateCondition.Period.HOUR,
                RateCondition.Operator.GT, 2000.0);

        long t1minute = 60000L * 1;
        long t3minute = t1minute * 3;
        long t5minute = t1minute * 5;
        datums.add(Data.forNumeric("tenant", "CounterUp", t1minute, 10.0)); // minute 1
        datums.add(Data.forNumeric("tenant", "CounterUp", t3minute, 20.0)); // minute 3 (rate = 5 per minute)
        datums.add(Data.forNumeric("tenant", "CounterUp", t5minute, 100.0)); // minute 5 (rate = 40 per minute)

        datums.add(Data.forNumeric("tenant", "CounterDown", t1minute, 100.0)); // minute 1
        datums.add(Data.forNumeric("tenant", "CounterDown", t3minute, 90.0)); // minute 3 (rate = 300 per hour)
        datums.add(Data.forNumeric("tenant", "CounterDown", t5minute, 10.0)); // minute 5 (rate = 2400 per hour)

        // default dampening

        t1.setEnabled(true);
        t2.setEnabled(true);

        rulesEngine.addFact(t1);
        rulesEngine.addFact(t1c1);
        rulesEngine.addFact(t2);
        rulesEngine.addFact(t2c1);

        rulesEngine.addData(datums);

        rulesEngine.fire();

        assertEquals(alerts.toString(), 2, alerts.size());
        Collections.sort(alerts, (Alert a1, Alert a2) -> a1.getTriggerId().compareTo(a2.getTriggerId()));

        Alert a = alerts.get(0);
        assertEquals(a.getTriggerId(), "trigger-1", a.getTriggerId());
        assertEquals(a.getEvalSets().toString(), 1, a.getEvalSets().size());
        Set<ConditionEval> eval = a.getEvalSets().get(0);
        assertEquals(eval.toString(), 1, eval.size());
        RateConditionEval e = (RateConditionEval) eval.iterator().next();
        assertEquals(e.toString(), 1, e.getConditionSetIndex());
        assertEquals(e.toString(), 1, e.getConditionSetSize());
        assertEquals("trigger-1", e.getTriggerId());
        assertTrue(e.isMatch());
        assertTrue(e.toString(), e.getTime() == t5minute);
        assertTrue(e.toString(), e.getPreviousTime() == t3minute);
        assertTrue(e.toString(), e.getValue().equals(100.0));
        assertTrue(e.toString(), e.getPreviousValue().equals(20.0));
        assertTrue(e.toString(), e.getRate().equals(40.0));
        assertEquals(e.getCondition().toString(), "CounterUp", e.getCondition().getDataId());

        a = alerts.get(1);
        assertEquals(a.getTriggerId(), "trigger-2", a.getTriggerId());
        assertEquals(a.getEvalSets().toString(), 1, a.getEvalSets().size());
        eval = a.getEvalSets().get(0);
        assertEquals(eval.toString(), 1, eval.size());
        e = (RateConditionEval) eval.iterator().next();
        assertEquals(e.toString(), 1, e.getConditionSetIndex());
        assertEquals(e.toString(), 1, e.getConditionSetSize());
        assertEquals("trigger-2", e.getTriggerId());
        assertTrue(e.isMatch());
        assertTrue(e.toString(), e.getTime() == t5minute);
        assertTrue(e.toString(), e.getPreviousTime() == t3minute);
        assertTrue(e.toString(), e.getValue().equals(10.0));
        assertTrue(e.toString(), e.getPreviousValue().equals(90.0));
        assertTrue(e.toString(), e.getRate().equals(2400.0));
        assertEquals(e.getCondition().toString(), "CounterDown", e.getCondition().getDataId());
    }

    @Test
    public void multipleEventConditions() {
        Trigger t1 = new Trigger("tenant", "trigger-1", "Events Test");
        t1.setEventType(EventType.EVENT);
        EventCondition t1c1 = new EventCondition("tenant", "trigger-1", Mode.FIRING, 3, 1, "myapp.war",
                "text == 'DOWN'");
        EventCondition t1c2 = new EventCondition("tenant", "trigger-1", Mode.FIRING, 3, 2, "datacenter1",
                "text starts 'ERROR'");
        EventCondition t1c3 = new EventCondition("tenant", "trigger-1", Mode.FIRING, 3, 3, "datacenter2",
                "text starts 'WARN'");

        /*
            On multiple conditions timestamps on input events are important.
         */
        Event appDownEvent1 = new Event("tenant", UUID.randomUUID().toString(), 1, "myapp.war",
                EventCategory.DEPLOYMENT.name(), "DOWN");
        Event logErrorEvent1 = new Event("tenant", UUID.randomUUID().toString(), 2, "datacenter1",
                EventCategory.LOG.name(), "ERROR [Time] This is a sample as app logging");
        Event logWarnEvent1 = new Event("tenant", UUID.randomUUID().toString(), 3, "datacenter2",
                EventCategory.LOG.name(), "WARN [Time] This is a sample as app logging");

        Event appDownEvent2 = new Event("tenant", UUID.randomUUID().toString(), 4, "myapp.war",
                EventCategory.DEPLOYMENT.name(), "UP");
        Event logErrorEvent2 = new Event("tenant", UUID.randomUUID().toString(), 5, "datacenter1",
                EventCategory.LOG.name(), "ERROR [Time] This is a sample as app logging 2");
        Event logWarnEvent2 = new Event("tenant", UUID.randomUUID().toString(), 6, "datacenter2",
                EventCategory.LOG.name(), "WARN [Time] This is a sample as app logging 2");

        Event appDownEvent3 = new Event("tenant", UUID.randomUUID().toString(), 7, "myapp.war",
                EventCategory.DEPLOYMENT.name(), "UP");
        Event logErrorEvent3 = new Event("tenant", UUID.randomUUID().toString(), 8, "datacenter1",
                EventCategory.LOG.name(), "ERROR [Time] This is a sample as app logging 3");
        Event logWarnEvent3 = new Event("tenant", UUID.randomUUID().toString(), 9, "datacenter2",
                EventCategory.LOG.name(), "WARN [Time] This is a sample as app logging 3");

        inputEvents.add(appDownEvent1);
        inputEvents.add(logErrorEvent1);
        inputEvents.add(logWarnEvent1);

        inputEvents.add(appDownEvent2);
        inputEvents.add(logErrorEvent2);
        inputEvents.add(logWarnEvent2);

        inputEvents.add(appDownEvent3);
        inputEvents.add(logErrorEvent3);
        inputEvents.add(logWarnEvent3);

        t1.setEnabled(true);

        rulesEngine.addFact(t1);
        rulesEngine.addFact(t1c1);
        rulesEngine.addFact(t1c2);
        rulesEngine.addFact(t1c3);

        rulesEngine.addEvents(inputEvents);

        rulesEngine.fire();

        /*
            Trigger1
                Conditions
                    "myapp.war" text == 'DOWN' AND
                    "datacenter1" text starts 'ERROR' AND
                    "datacenter2" text starts 'WARN'

            Incoming data:

            t1. "myapp.war" "DOWN"
            t2. "datacenter1" "ERROR [Time] This is a sample as app logging"
            t3. "datacenter2" "WARN [Time] This is a sample as app logging"

            --> EVENT

            t4. "myapp.war" "UP"
            t5. "datacenter1" "ERROR [Time] This is a sample as app logging"
            t6. "datacenter2" "WARN [Time] This is a sample as app logging"

            --> NOT EVENT

            t7. "myapp.war" "UP"
            t8. "datacenter1" "ERROR [Time] This is a sample as app logging"
            t9. "datacenter2" "WARN [Time] This is a sample as app logging"

            --> NOT EVENT
         */
        assertEquals(outputEvents.toString(), 1, outputEvents.size());
    }

    @Test
    public void chainedEventsRules() {
        Trigger t1 = new Trigger("tenant", "trigger-1", "A.war");
        t1.setEventType(EventType.EVENT);
        EventCondition t1c1 = new EventCondition("tenant", "trigger-1", Mode.FIRING, "A.war", "text == 'DOWN'");

        Trigger t2 = new Trigger("tenant", "trigger-2", "B.war");
        t2.setEventType(EventType.EVENT);
        EventCondition t2c1 = new EventCondition("tenant", "trigger-2", Mode.FIRING, "B.war", "text == 'DOWN'");

        Trigger t3 = new Trigger("tenant", "trigger-3", "A.war and B.war DOWN");
        EventCondition t3c1 = new EventCondition("tenant", "trigger-3", Mode.FIRING, 2, 1, "trigger-1");
        EventCondition t3c2 = new EventCondition("tenant", "trigger-3", Mode.FIRING, 2, 2, "trigger-2");

        Event appADownEvent1 = new Event("tenant", UUID.randomUUID().toString(), 1, "A.war",
                EventCategory.DEPLOYMENT.name(), "DOWN");

        Event appBDownEvent1 = new Event("tenant", UUID.randomUUID().toString(), 1, "B.war",
                EventCategory.DEPLOYMENT.name(), "DOWN");

        Event appADownEvent2 = new Event("tenant", UUID.randomUUID().toString(), 2, "A.war",
                EventCategory.DEPLOYMENT.name(), "DOWN");

        Event appBDownEvent2 = new Event("tenant", UUID.randomUUID().toString(), 2, "B.war",
                EventCategory.DEPLOYMENT.name(), "DOWN");

        Event appADownEvent3 = new Event("tenant", UUID.randomUUID().toString(), 3, "A.war",
                EventCategory.DEPLOYMENT.name(), "DOWN");

        Event appBDownEvent3 = new Event("tenant", UUID.randomUUID().toString(), 3, "B.war",
                EventCategory.DEPLOYMENT.name(), "DOWN");

        inputEvents.add(appADownEvent1);
        inputEvents.add(appBDownEvent1);
        inputEvents.add(appADownEvent2);
        inputEvents.add(appBDownEvent2);
        inputEvents.add(appADownEvent3);
        inputEvents.add(appBDownEvent3);

        t1.setEnabled(true);
        t2.setEnabled(true);
        t3.setEnabled(true);

        rulesEngine.addFact(t1);
        rulesEngine.addFact(t1c1);
        rulesEngine.addFact(t2);
        rulesEngine.addFact(t2c1);
        rulesEngine.addFact(t3);
        rulesEngine.addFact(t3c1);
        rulesEngine.addFact(t3c2);

        rulesEngine.addEvents(inputEvents);

        rulesEngine.fire();

        assertEquals(outputEvents.toString(), 6, outputEvents.size());

        /*
            Expected inference:

                Alert 1:
                    id=trigger-1-Event-timestamp-1
                    id=trigger-2-Event-timestamp-1

                Alert 2:
                    id=trigger-1-Event-timestamp-2 ==> NEW Event from trigger-1
                    id=trigger-2-Event-timestamp-1

                Alert 3:
                    id=trigger-1-Event-timestamp-2
                    id=trigger-2-Event-timestamp-2 ==> NEW Event from trigger-2

                Alert 4:
                    id=trigger-1-Event-timestamp-3 ==> NEW Event from trigger-1
                    id=trigger-2-Event-timestamp-2

                Alert 5:
                    id=trigger-1-Event-timestamp-3
                    id=trigger-2-Event-timestamp-3 ==> NEW Event from trigger-2
         */
        assertEquals(alerts.toString(), 5, alerts.size());
    }

    @Test
    public void dampeningStrictTest() {
        Trigger t1 = new Trigger("tenant", "trigger-1", "Avail-DOWN");
        AvailabilityCondition t1c1 = new AvailabilityCondition("tenant", "trigger-1", 1, 1,
                "AvailData-01", AvailabilityCondition.Operator.DOWN);

        Dampening t1d = Dampening.forStrict("tenant", "trigger-1", Mode.FIRING, 3);

        datums.add(Data.forAvailability("tenant", "AvailData-01", 1, AvailabilityType.DOWN));
        datums.add(Data.forAvailability("tenant", "AvailData-01", 2, AvailabilityType.UNAVAILABLE));
        datums.add(Data.forAvailability("tenant", "AvailData-01", 3, AvailabilityType.UP));
        datums.add(Data.forAvailability("tenant", "AvailData-01", 4, AvailabilityType.DOWN));
        datums.add(Data.forAvailability("tenant", "AvailData-01", 5, AvailabilityType.DOWN));
        datums.add(Data.forAvailability("tenant", "AvailData-01", 6, AvailabilityType.DOWN));
        datums.add(Data.forAvailability("tenant", "AvailData-01", 7, AvailabilityType.UP));

        t1.setEnabled(true);

        rulesEngine.addFact(t1);
        rulesEngine.addFact(t1c1);
        rulesEngine.addFact(t1d);

        rulesEngine.addData(datums);

        rulesEngine.fire();

        assertEquals(alerts.toString(), 1, alerts.size());

        Alert a = alerts.get(0);
        assertEquals("trigger-1", a.getTriggerId());
        assertEquals(3, a.getEvalSets().size());
        long expectedTimestamp = 4;
        for (Set<ConditionEval> evalSet : a.getEvalSets()) {
            assertEquals(1, evalSet.size());
            AvailabilityConditionEval e = (AvailabilityConditionEval) evalSet.iterator().next();
            assertEquals(1, e.getConditionSetIndex());
            assertEquals(1, e.getConditionSetSize());
            assertEquals("trigger-1", e.getTriggerId());
            assertTrue(e.isMatch());
            assertEquals(expectedTimestamp++, e.getDataTimestamp());
            AvailabilityType v = e.getValue();
            assertEquals(AvailabilityType.DOWN, v);
            assertEquals("AvailData-01", e.getCondition().getDataId());
        }
    }

    @Test
    public void dampeningRelaxedCountTest() {
        Trigger t1 = new Trigger("tenant", "trigger-1", "Avail-DOWN");
        AvailabilityCondition t1c1 = new AvailabilityCondition("tenant", "trigger-1", 1, 1,
                "AvailData-01", AvailabilityCondition.Operator.DOWN);

        Dampening t1d = Dampening.forRelaxedCount("tenant", "trigger-1", Mode.FIRING, 3, 5);

        datums.add(Data.forAvailability("tenant", "AvailData-01", 1, AvailabilityType.DOWN));
        datums.add(Data.forAvailability("tenant", "AvailData-01", 2, AvailabilityType.UNAVAILABLE));
        datums.add(Data.forAvailability("tenant", "AvailData-01", 3, AvailabilityType.UP));
        datums.add(Data.forAvailability("tenant", "AvailData-01", 4, AvailabilityType.DOWN));
        datums.add(Data.forAvailability("tenant", "AvailData-01", 5, AvailabilityType.DOWN));
        datums.add(Data.forAvailability("tenant", "AvailData-01", 6, AvailabilityType.DOWN));
        datums.add(Data.forAvailability("tenant", "AvailData-01", 7, AvailabilityType.UP));

        t1.setEnabled(true);

        rulesEngine.addFact(t1);
        rulesEngine.addFact(t1c1);
        rulesEngine.addFact(t1d);

        rulesEngine.addData(datums);

        rulesEngine.fire();

        assertEquals(alerts.toString(), 1, alerts.size());

        Alert a = alerts.get(0);
        assertEquals("trigger-1", a.getTriggerId());
        assertEquals(3, a.getEvalSets().size());
        int i = 0;
        long[] expectedTimestamps = new long[] { 1, 4, 5 };
        for (Set<ConditionEval> evalSet : a.getEvalSets()) {
            assertEquals(1, evalSet.size());
            AvailabilityConditionEval e = (AvailabilityConditionEval) evalSet.iterator().next();
            assertEquals(1, e.getConditionSetIndex());
            assertEquals(1, e.getConditionSetSize());
            assertEquals("trigger-1", e.getTriggerId());
            assertTrue(e.isMatch());
            assertEquals(expectedTimestamps[i++], e.getDataTimestamp());
            AvailabilityType v = e.getValue();
            assertEquals(AvailabilityType.DOWN, v);
            assertEquals("AvailData-01", e.getCondition().getDataId());
        }
    }

    @Test
    public void dampeningRelaxedTimeTest() {
        Trigger t1 = new Trigger("tenant", "trigger-1", "Avail-DOWN");
        AvailabilityCondition t1c1 = new AvailabilityCondition("tenant", "trigger-1", 1, 1,
                "AvailData-01", AvailabilityCondition.Operator.DOWN);

        Dampening t1d = Dampening.forRelaxedTime("tenant", "trigger-1", Mode.FIRING, 2, 500L);

        t1.setEnabled(true);

        rulesEngine.addFact(t1);
        rulesEngine.addFact(t1c1);
        rulesEngine.addFact(t1d);

        datums.add(Data.forAvailability("tenant", "AvailData-01", 1, AvailabilityType.DOWN));

        rulesEngine.addData(datums);
        rulesEngine.fire();

        assertEquals(alerts.toString(), 0, alerts.size());

        try {
            Thread.sleep(750L);
        } catch (InterruptedException e1) {
        }

        datums.clear();
        datums.add(Data.forAvailability("tenant", "AvailData-01", 2, AvailabilityType.DOWN));
        datums.add(Data.forAvailability("tenant", "AvailData-01", 3, AvailabilityType.UP));
        datums.add(Data.forAvailability("tenant", "AvailData-01", 4, AvailabilityType.DOWN));
        datums.add(Data.forAvailability("tenant", "AvailData-01", 5, AvailabilityType.UP));

        rulesEngine.addData(datums);
        rulesEngine.fire();

        assertEquals(alerts.toString(), 1, alerts.size());

        Alert a = alerts.get(0);
        assertEquals("trigger-1", a.getTriggerId());
        assertEquals(2, a.getEvalSets().size());
        int i = 0;
        long[] expectedTimestamps = new long[] { 2, 4 };
        for (Set<ConditionEval> evalSet : a.getEvalSets()) {
            assertEquals(1, evalSet.size());
            AvailabilityConditionEval e = (AvailabilityConditionEval) evalSet.iterator().next();
            assertEquals(1, e.getConditionSetIndex());
            assertEquals(1, e.getConditionSetSize());
            assertEquals("trigger-1", e.getTriggerId());
            assertTrue(e.isMatch());
            assertEquals(expectedTimestamps[i++], e.getDataTimestamp());
            AvailabilityType v = e.getValue();
            assertEquals(AvailabilityType.DOWN, v);
            assertEquals("AvailData-01", e.getCondition().getDataId());
        }
    }

    @Test
    public void dampeningStrictTimeTest() {
        Trigger t1 = new Trigger("tenant", "trigger-1", "Avail-DOWN");
        AvailabilityCondition t1c1 = new AvailabilityCondition("tenant", "trigger-1", 1, 1,
                "AvailData-01", AvailabilityCondition.Operator.DOWN);

        Dampening t1d = Dampening.forStrictTime("tenant", "trigger-1", Mode.FIRING, 250L);

        t1.setEnabled(true);

        rulesEngine.addFact(t1);
        rulesEngine.addFact(t1c1);
        rulesEngine.addFact(t1d);

        long start = System.currentTimeMillis();
        int i = 0;
        while ((alerts.size() == 0) && ((System.currentTimeMillis() - start) < 500)) {
            rulesEngine.addData(Data.forAvailability("tenant", "AvailData-01", ++i, AvailabilityType.DOWN));
            rulesEngine.fire();
        }

        assertEquals(alerts.toString(), 1, alerts.size());

        Alert a = alerts.get(0);
        assertTrue(String.valueOf((alerts.get(0).getCtime() - start)), (alerts.get(0).getCtime() - start) >= 250);
        assertEquals("trigger-1", a.getTriggerId());
        assertTrue(String.valueOf(a.getEvalSets().size()), a.getEvalSets().size() >= 2);
        for (Set<ConditionEval> evalSet : a.getEvalSets()) {
            assertEquals(1, evalSet.size());
            AvailabilityConditionEval e = (AvailabilityConditionEval) evalSet.iterator().next();
            assertEquals(1, e.getConditionSetIndex());
            assertEquals(1, e.getConditionSetSize());
            assertEquals("trigger-1", e.getTriggerId());
            assertTrue(e.isMatch());
            AvailabilityType v = e.getValue();
            assertEquals(AvailabilityType.DOWN, v);
            assertEquals("AvailData-01", e.getCondition().getDataId());
        }
    }

    @Test
    public void dampeningStrictTimeoutTest() {
        Trigger t1 = new Trigger("tenant", "trigger-1", "Avail-DOWN");
        AvailabilityCondition t1c1 = new AvailabilityCondition("tenant", "trigger-1", 1, 1,
                "AvailData-01", AvailabilityCondition.Operator.DOWN);

        Dampening t1d = Dampening.forStrictTimeout("tenant", "trigger-1", Mode.FIRING, 200L);

        t1.setEnabled(true);

        rulesEngine.addFact(t1);
        rulesEngine.addFact(t1c1);
        rulesEngine.addFact(t1d);

        assertTrue(alerts.isEmpty());
        assertTrue(pendingTimeouts.isEmpty());

        rulesEngine.addData(Data.forAvailability("tenant", "AvailData-01", 1, AvailabilityType.DOWN));
        rulesEngine.fire();

        assertTrue(alerts.isEmpty());
        assertEquals(String.valueOf(pendingTimeouts), 1, pendingTimeouts.size());

        Dampening pendingTimeout = pendingTimeouts.iterator().next();
        pendingTimeout.setSatisfied(true);
        rulesEngine.updateFact(pendingTimeout);

        rulesEngine.fireNoData();

        assertEquals(alerts.toString(), 1, alerts.size());

        Alert a = alerts.get(0);
        assertEquals("trigger-1", a.getTriggerId());
        assertEquals(1, a.getEvalSets().size());
        Set<ConditionEval> evalSet = a.getEvalSets().get(0);
        assertEquals(1, evalSet.size());
        AvailabilityConditionEval e = (AvailabilityConditionEval) evalSet.iterator().next();
        assertEquals(1, e.getConditionSetIndex());
        assertEquals(1, e.getConditionSetSize());
        assertEquals("trigger-1", e.getTriggerId());
        assertTrue(e.isMatch());
        AvailabilityType v = e.getValue();
        assertEquals(AvailabilityType.DOWN, v);
        assertEquals("AvailData-01", e.getCondition().getDataId());
    }

    @Test
    public void multiConditionTest() {
        Trigger t1 = new Trigger("tenant", "trigger-1", "Two-Conditions");
        ThresholdCondition t1c1 = new ThresholdCondition("tenant", "trigger-1", 2, 1, "NumericData-01",
                ThresholdCondition.Operator.LT, 10.0);
        ThresholdRangeCondition t1c2 = new ThresholdRangeCondition("tenant", "trigger-1", 2, 2, "NumericData-02",
                ThresholdRangeCondition.Operator.INCLUSIVE, ThresholdRangeCondition.Operator.EXCLUSIVE, 100.0, 200.0,
                true);

        // default dampening

        t1.setEnabled(true);

        rulesEngine.addFact(t1);
        rulesEngine.addFact(t1c1);
        rulesEngine.addFact(t1c2);

        // break up the arrivals of the relevant datums so that we get a more complicated series of evaluations.
        // remember that for any batch of datums:
        //   1) one datum for a specific dataId will will be processed at a time
        //   2) only the most recent conditionEvals will be used in a condition set tuple for a multi-condition trigger

        datums.add(Data.forNumeric("tenant", "NumericData-01", 1, 10.0)); // eval(d1,t1)=no match,
        datums.add(Data.forNumeric("tenant", "NumericData-01", 2, 5.0));  // eval(d1,t2)=   match, replaces eval(d1,t1)
        datums.add(Data.forNumeric("tenant", "NumericData-01", 3, 15.0)); // eval(d1,t3)=no match, replaces eval(d1,t2)

        rulesEngine.addData(datums);
        rulesEngine.fire();

        assertEquals(alerts.toString(), 0, alerts.size());

        datums.clear();
        // eval(d2,t4) = no match, tuple(eval(d1,t3), eval(d2,t4)) = false
        datums.add(Data.forNumeric("tenant", "NumericData-02", 4, 10.0));
        // eval(d2,t5) =    match, tuple(eval(d1,t3), eval(d2,t5)) = false
        datums.add(Data.forNumeric("tenant", "NumericData-02", 5, 150.0));

        rulesEngine.addData(datums);
        rulesEngine.fire();

        assertEquals(alerts.toString(), 0, alerts.size());

        datums.clear();
        // eval(d1,t6) =    match, tuple(eval(d1,t6), eval(d2,t5)) = true
        datums.add(Data.forNumeric("tenant", "NumericData-01", 6, 8.0));

        rulesEngine.addData(datums);
        rulesEngine.fire();

        assertEquals(alerts.toString(), 1, alerts.size());

        Alert a = alerts.get(0);
        assertEquals("trigger-1", a.getTriggerId());
        assertEquals(1, a.getEvalSets().size());
        Set<ConditionEval> evals = a.getEvalSets().get(0);
        assertEquals(2, evals.size());
        List<ConditionEval> evalsList = new ArrayList<>(evals);
        Collections.sort(
                evalsList,
                (ConditionEval c1, ConditionEval c2) -> Integer.compare(c1.getConditionSetIndex(),
                        c2.getConditionSetIndex()));
        Iterator<ConditionEval> i = evalsList.iterator();
        ThresholdConditionEval e = (ThresholdConditionEval) i.next();
        assertEquals(2, e.getConditionSetSize());
        assertEquals(1, e.getConditionSetIndex());
        assertEquals("trigger-1", e.getTriggerId());
        assertTrue(e.isMatch());
        Double v = e.getValue();
        assertTrue(v.equals(8.0));
        assertEquals("NumericData-01", e.getCondition().getDataId());

        ThresholdRangeConditionEval e2 = (ThresholdRangeConditionEval) i.next();
        assertEquals(2, e2.getConditionSetSize());
        assertEquals(2, e2.getConditionSetIndex());
        assertEquals("trigger-1", e2.getTriggerId());
        assertTrue(e2.isMatch());
        v = e2.getValue();
        assertTrue(v.equals(150.0));
        assertEquals("NumericData-02", e2.getCondition().getDataId());
    }

    @Test
    public void matchAnyTest() {
        Trigger t1 = new Trigger("tenant", "trigger-1", "Any-Two-Conditions");
        t1.setFiringMatch(Match.ANY);

        ThresholdCondition t1c1 = new ThresholdCondition("tenant", "trigger-1", 2, 1, "X",
                ThresholdCondition.Operator.GT, 100.0);
        ThresholdCondition t1c2 = new ThresholdCondition("tenant", "trigger-1", 2, 2, "Y",
                ThresholdCondition.Operator.GT, 200.0);

        Dampening t1d = Dampening.forStrict("tenant", "trigger-1", Mode.FIRING, 2);

        t1.setEnabled(true);

        rulesEngine.addFact(t1);
        rulesEngine.addFact(t1c1);
        rulesEngine.addFact(t1c2);
        rulesEngine.addFact(t1d);

        // for clarity deliver datums independently

        datums.add(Data.forNumeric("tenant", "X", 1, 125.0));  // match, dampening eval true (X), no alert
        rulesEngine.addData(datums);
        rulesEngine.fire();
        assertEquals(alerts.toString(), 0, alerts.size());

        datums.clear();
        datums.add(Data.forNumeric("tenant", "X", 2, 50.0));   // no match, dampening reset
        rulesEngine.addData(datums);
        rulesEngine.fire();
        assertEquals(alerts.toString(), 0, alerts.size());

        datums.clear();
        datums.add(Data.forNumeric("tenant", "Y", 3, 300.0));  // match, dampening eval true (Y), no alert
        rulesEngine.addData(datums);
        rulesEngine.fire();
        assertEquals(alerts.toString(), 0, alerts.size());

        datums.clear();
        datums.add(Data.forNumeric("tenant", "X", 4, 110.0));  // match, dampening eval true (X,Y), alert! damp reset
        rulesEngine.addData(datums);
        rulesEngine.fire();
        assertEquals(alerts.toString(), 1, alerts.size());

        Alert a = alerts.get(0);
        assertEquals("trigger-1", a.getTriggerId());
        assertEquals(2, a.getEvalSets().size());
        Set<ConditionEval> evals = a.getEvalSets().get(0);
        assertEquals(2, evals.size());
        List<ConditionEval> evalsList = new ArrayList<>(evals);
        Collections.sort(
                evalsList,
                (ConditionEval c1, ConditionEval c2) -> Integer.compare(c1.getConditionSetIndex(),
                        c2.getConditionSetIndex()));
        Iterator<ConditionEval> i = evalsList.iterator();
        ThresholdConditionEval e = (ThresholdConditionEval) i.next();
        assertEquals(2, e.getConditionSetSize());
        assertEquals(1, e.getConditionSetIndex());
        assertEquals("trigger-1", e.getTriggerId());
        assertTrue(!e.isMatch());
        Double v = e.getValue();
        assertTrue(v.equals(50.0));
        assertEquals("X", e.getCondition().getDataId());

        ThresholdConditionEval e2 = (ThresholdConditionEval) i.next();
        assertEquals(2, e2.getConditionSetSize());
        assertEquals(2, e2.getConditionSetIndex());
        assertEquals("trigger-1", e2.getTriggerId());
        assertTrue(e2.isMatch());
        v = e2.getValue();
        assertTrue(v.equals(300.0));
        assertEquals("Y", e2.getCondition().getDataId());

        evals = a.getEvalSets().get(1);
        assertEquals(2, evals.size());
        evalsList = new ArrayList<>(evals);
        Collections.sort(
                evalsList,
                (ConditionEval c1, ConditionEval c2) -> Integer.compare(c1.getConditionSetIndex(),
                        c2.getConditionSetIndex()));
        i = evalsList.iterator();
        e = (ThresholdConditionEval) i.next();
        assertEquals(2, e.getConditionSetSize());
        assertEquals(1, e.getConditionSetIndex());
        assertEquals("trigger-1", e.getTriggerId());
        assertTrue(e.isMatch());
        v = e.getValue();
        assertTrue(v.equals(110.0));
        assertEquals("X", e.getCondition().getDataId());

        e2 = (ThresholdConditionEval) i.next();
        assertEquals(2, e2.getConditionSetSize());
        assertEquals(2, e2.getConditionSetIndex());
        assertEquals("trigger-1", e2.getTriggerId());
        assertTrue(e2.isMatch());
        v = e2.getValue();
        assertTrue(v.equals(300.0));
        assertEquals("Y", e2.getCondition().getDataId());

        alerts.clear();
        datums.clear();
        datums.add(Data.forNumeric("tenant", "Y", 5, 150.0));  // match, dampening eval true (X), no alert
        rulesEngine.addData(datums);
        rulesEngine.fire();
        assertEquals(alerts.toString(), 0, alerts.size());
    }

    @Test
    public void autoResolveTest() {
        // The single trigger has definitions for both FIRING and AUTORESOLVE modes
        Trigger t1 = new Trigger("tenant", "trigger-1", "Avail-DOWN");

        // Firing Mode
        AvailabilityCondition fmt1c1 = new AvailabilityCondition("tenant", "trigger-1", Mode.FIRING, 1, 1,
                "AvailData-01", AvailabilityCondition.Operator.DOWN);
        Dampening fmt1d = Dampening.forStrict("tenant", "trigger-1", Mode.FIRING, 2);

        // AutoResolve Mode
        AvailabilityCondition smt1c1 = new AvailabilityCondition("tenant", "trigger-1", Mode.AUTORESOLVE, 1, 1,
                "AvailData-01", AvailabilityCondition.Operator.UP);
        Dampening smt1d = Dampening.forStrict("tenant", "trigger-1", Mode.AUTORESOLVE, 2);

        datums.add(Data.forAvailability("tenant", "AvailData-01", 1, AvailabilityType.DOWN));
        datums.add(Data.forAvailability("tenant", "AvailData-01", 2, AvailabilityType.UNAVAILABLE));
        datums.add(Data.forAvailability("tenant", "AvailData-01", 3, AvailabilityType.UP));
        datums.add(Data.forAvailability("tenant", "AvailData-01", 4, AvailabilityType.DOWN));
        datums.add(Data.forAvailability("tenant", "AvailData-01", 5, AvailabilityType.DOWN));
        datums.add(Data.forAvailability("tenant", "AvailData-01", 6, AvailabilityType.DOWN));
        datums.add(Data.forAvailability("tenant", "AvailData-01", 7, AvailabilityType.DOWN));
        datums.add(Data.forAvailability("tenant", "AvailData-01", 8, AvailabilityType.UP));

        t1.setEnabled(true);
        t1.setAutoDisable(false);
        t1.setAutoEnable(false);
        t1.setAutoResolve(true);
        t1.setAutoResolveAlerts(true);

        rulesEngine.addFact(t1);
        rulesEngine.addFact(fmt1c1);
        rulesEngine.addFact(fmt1d);
        rulesEngine.addFact(smt1c1);
        rulesEngine.addFact(smt1d);

        // The Trigger should fire on the consecutive DOWN datums at T4,T5. It should then switch to
        // AutoResolve mode and not fire again at the next two consecutive down datums at T6,T7.  T8 should be the
        // first match for the AutoResolve dampening but it should not yet be satisfied until T9 (see below).
        rulesEngine.addData(datums);
        rulesEngine.fire();

        assertEquals(alerts.toString(), 1, alerts.size());
        assertTrue(disabledTriggers.toString(), disabledTriggers.isEmpty());
        assertTrue(autoResolvedTriggers.toString(), autoResolvedTriggers.isEmpty());

        Alert a = alerts.get(0);
        assertTrue(a.getStatus() == Alert.Status.OPEN);
        assertTrue(a.getTriggerId(), a.getTriggerId().equals("trigger-1"));
        assertTrue(a.getEvalSets().toString(), a.getEvalSets().size() == 2);
        long expectedTimestamp = 4;
        for (Set<ConditionEval> evalSet : a.getEvalSets()) {
            assertEquals(evalSet.toString(), 1, evalSet.size());
            AvailabilityConditionEval e = (AvailabilityConditionEval) evalSet.iterator().next();
            assertEquals(e.toString(), 1, e.getConditionSetIndex());
            assertEquals(e.toString(), 1, e.getConditionSetSize());
            assertEquals("trigger-1", e.getTriggerId());
            assertTrue(e.isMatch());
            assertEquals(expectedTimestamp++, e.getDataTimestamp());
            AvailabilityType v = e.getValue();
            assertEquals(AvailabilityType.DOWN, v);
            assertEquals("AvailData-01", e.getCondition().getDataId());
        }

        assertTrue(t1.toString(), t1.getMode() == Mode.AUTORESOLVE);

        alerts.clear();
        datums.clear();
        datums.add(Data.forAvailability("tenant", "AvailData-01", 9, AvailabilityType.UP));

        rulesEngine.addData(datums);
        rulesEngine.fire();

        // The second consecutive UP should satisfy the AutoResolve requirements and return the Trigger
        // in the autoResolvedTriggers global. Note the trigger is set to FIRING mode but in production
        // the real handling reloads the trigger and as such it is reset to FIRING mode in the engine.
        assertTrue(alerts.isEmpty());
        assertTrue(disabledTriggers.isEmpty());
        assertEquals(1, autoResolvedTriggers.size());

        assertTrue(t1.toString(), t1.getMode() == Mode.FIRING);
    }

    @Test
    public void checkEqualityInRulesEngine() throws Exception {

        Trigger t1 = new Trigger("tenant", "trigger-1", "Avail-DOWN");
        AvailabilityCondition fmt1c1 = new AvailabilityCondition("tenant", "trigger-1", Mode.FIRING, 1, 1,
                "AvailData-01", AvailabilityCondition.Operator.DOWN);
        Data adata = Data.forAvailability("tenant", "AvailData-01", System.currentTimeMillis(), AvailabilityType.UP);
        AvailabilityConditionEval fmt1c1eval = new AvailabilityConditionEval(fmt1c1, adata);
        Dampening fmt1d = Dampening.forStrict("tenant", "trigger-1", Mode.FIRING, 2);

        ThresholdCondition fmt1c2 = new ThresholdCondition("tenant", "trigger-1", Mode.FIRING, 1, 1,
                "ThreData-01", ThresholdCondition.Operator.GT, 10d);
        Data ndata = Data.forNumeric("tenant", "ThreData-01", System.currentTimeMillis(), 20d);
        ThresholdConditionEval fmt1c2eval = new ThresholdConditionEval(fmt1c2, ndata);

        rulesEngine.addFact(t1);
        rulesEngine.addFact(fmt1c1);
        rulesEngine.addFact(fmt1c1eval);
        rulesEngine.addFact(fmt1d);
        rulesEngine.addFact(fmt1c2);
        rulesEngine.addFact(fmt1c2eval);

        assertTrue(rulesEngine.getFact(t1) != null);
        assertTrue(rulesEngine.getFact(fmt1c1) != null);
        assertTrue(rulesEngine.getFact(fmt1c1eval) != null);
        assertTrue(rulesEngine.getFact(fmt1d) != null);
        assertTrue(rulesEngine.getFact(fmt1c2) != null);
        assertTrue(rulesEngine.getFact(fmt1c2eval) != null);

        String strt1 = JsonUtil.toJson(t1);
        String strfmt1c1 = JsonUtil.toJson(fmt1c1);
        String strfmt1c1eval = JsonUtil.toJson(fmt1c1eval);
        String strfmt1d = JsonUtil.toJson(fmt1d);
        String strfmt1c2 = JsonUtil.toJson(fmt1c2);
        String strfmt1c2eval = JsonUtil.toJson(fmt1c2eval);

        Trigger jsont1 = JsonUtil.fromJson(strt1, Trigger.class);
        AvailabilityCondition jsonfmt1c1 = JsonUtil.fromJson(strfmt1c1, AvailabilityCondition.class);
        AvailabilityConditionEval jsonfmt1c1eval = JsonUtil.fromJson(strfmt1c1eval, AvailabilityConditionEval.class);
        Dampening jsonfmt1d = JsonUtil.fromJson(strfmt1d, Dampening.class);
        ThresholdCondition jsonfmt1c2 = JsonUtil.fromJson(strfmt1c2, ThresholdCondition.class);
        ThresholdConditionEval jsonfmt1c2eval = JsonUtil.fromJson(strfmt1c2eval, ThresholdConditionEval.class);

        assertTrue(t1.equals(jsont1));
        assertTrue(fmt1c1.equals(jsonfmt1c1));
        assertTrue(fmt1c1eval.equals(jsonfmt1c1eval));
        assertTrue(fmt1d.equals(jsonfmt1d));
        assertTrue(fmt1c2.equals(jsonfmt1c2));
        assertTrue(fmt1c2eval.equals(jsonfmt1c2eval));

        assertTrue(rulesEngine.getFact(jsont1) != null);
        assertTrue(rulesEngine.getFact(jsonfmt1c1) != null);
        assertTrue(rulesEngine.getFact(jsonfmt1c1eval) != null);
        assertTrue(rulesEngine.getFact(jsonfmt1d) != null);
        assertTrue(rulesEngine.getFact(jsonfmt1c2) != null);
        assertTrue(rulesEngine.getFact(jsonfmt1c2eval) != null);
    }

    @Test
    public void missingDataTestNoData() throws Exception {
        //Initial trigger and condition
        Trigger t1 = new Trigger("tenant", "trigger-m", "Missing test Trigger");
        MissingCondition fmt1 = new MissingCondition("tenant", "trigger-m", "data-id", 3000L);

        t1.setEnabled(true);

        // Missing state is generated on reloadTrigger()
        missingStates.add(new MissingState(t1, fmt1));

        rulesEngine.addFact(t1);
        rulesEngine.addFact(fmt1);

        // Rules Invoker thread updates missing states periodically
        MissingState missingState = missingStates.iterator().next();

        rulesEngine.removeFact(missingState);

        // It simulates that we have not data in 4 seconds > of 3 seconds interval defined
        missingState.setPreviousTime(missingState.getTime());
        missingState.setTime(missingState.getTime() + 4000);

        rulesEngine.addFact(missingState);

        // If missing states but not data rules engine should fire
        rulesEngine.fireNoData();

        assertEquals(1, alerts.size());


        // Rules Invoker thread updates missing states periodically
        missingState = missingStates.iterator().next();

        rulesEngine.removeFact(missingState);

        // It simulates that we have not data in 4 seconds > of 3 seconds interval defined
        missingState.setPreviousTime(missingState.getTime());
        missingState.setTime(missingState.getTime() + 4000);

        rulesEngine.addFact(missingState);

        rulesEngine.fireNoData();

        assertEquals(2, alerts.size());
    }

    @Test
    public void missingDataTestWithData() throws Exception {
        Trigger t1 = new Trigger("tenant", "trigger-m", "Missing test Trigger");
        MissingCondition fmt1 = new MissingCondition("tenant", "trigger-m", "data-id", 3000L);

        t1.setEnabled(true);

        // Missing state is generated on reloadTrigger()
        missingStates.add(new MissingState(t1, fmt1));

        rulesEngine.addFact(t1);
        rulesEngine.addFact(fmt1);

        MissingState missingState = missingStates.iterator().next();

        rulesEngine.removeFact(missingState);

        // It simulates that we have not data in 4 seconds > of 3 seconds interval defined
        missingState.setPreviousTime(missingState.getTime());
        missingState.setTime(missingState.getTime() + 4000);

        rulesEngine.addFact(missingState);

        // We add a data, so it should update the missingState and not fire any alert

        Data data = Data.forAvailability("tenant", "data-id", missingState.getTime() + 4001, AvailabilityType.DOWN);
        rulesEngine.addData(data);

        rulesEngine.fire();

        assertEquals(0, alerts.size());
    }
}
