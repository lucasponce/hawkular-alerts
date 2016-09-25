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
package org.hawkular.alerts.actions.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hawkular.alerts.api.model.condition.ConditionEval;
import org.hawkular.alerts.api.model.condition.ThresholdRangeCondition;
import org.hawkular.alerts.api.model.condition.ThresholdRangeConditionEval;
import org.hawkular.alerts.api.model.dampening.Dampening;
import org.hawkular.alerts.api.model.data.Data;
import org.hawkular.alerts.api.model.event.Alert;
import org.hawkular.alerts.api.model.trigger.Mode;
import org.hawkular.alerts.api.model.trigger.Trigger;

/**
 * Provide test data for Active Session Alerts on Web resources
 *
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
public class WebActiveSessionsData extends CommonData {

    public static Trigger trigger;
    public static ThresholdRangeCondition firingCondition;
    public static ThresholdRangeCondition autoResolveCondition;
    public static Dampening firingDampening;

    static {

        Map<String, String> context = new HashMap<>();
        context.put("resourceType", "App Server");
        context.put("resourceName", "thevault~Local");
        context.put("category", "Web Sessions");

        String triggerId = "thevault~local-web-active-sessions-trigger";
        String triggerDescription = "Web Active Sessions for thevault~Local";
        String dataId = "thevault~local-web-active-sessions-data-id";

        trigger = new Trigger(TENANT,
                triggerId,
                triggerDescription,
                context);

        firingCondition = new ThresholdRangeCondition(TENANT, trigger.getId(),
                Mode.FIRING,
                dataId,
                ThresholdRangeCondition.Operator.INCLUSIVE,
                ThresholdRangeCondition.Operator.INCLUSIVE,
                200d,
                5000d,
                false);
        firingCondition.getContext().put("description", "Active Sessions");
        firingCondition.getContext().put("unit", "sessions");

        autoResolveCondition = new ThresholdRangeCondition(TENANT, trigger.getId(),
                Mode.FIRING,
                dataId,
                ThresholdRangeCondition.Operator.EXCLUSIVE,
                ThresholdRangeCondition.Operator.EXCLUSIVE,
                200d,
                5000d,
                true);
        autoResolveCondition.getContext().put("description", "Active Sessions");
        autoResolveCondition.getContext().put("unit", "sessions");

        firingDampening = Dampening.forStrictTimeout(TENANT, trigger.getId(),
                Mode.FIRING,
                10000);
    }

    public static Alert getOpenAlert() {

        List<Set<ConditionEval>> satisfyingEvals = new ArrayList<>();

        Data rtBadData1 = Data.forNumeric(TENANT, firingCondition.getDataId(),
                System.currentTimeMillis(),
                5010d);
        ThresholdRangeConditionEval eval1 = new ThresholdRangeConditionEval(firingCondition, rtBadData1);

        Set<ConditionEval> evalSet1 = new HashSet<>();
        evalSet1.add(eval1);
        satisfyingEvals.add(evalSet1);

        // 5 seconds later
        Data rtBadData2 = Data.forNumeric(TENANT, firingCondition.getDataId(),
                System.currentTimeMillis() + 5000,
                5014d);
        ThresholdRangeConditionEval eval2 = new ThresholdRangeConditionEval(firingCondition, rtBadData2);

        Set<ConditionEval> evalSet2 = new HashSet<>();
        evalSet2.add(eval2);
        satisfyingEvals.add(evalSet2);

        Alert openAlert = new Alert(trigger.getTenantId(), trigger, firingDampening, satisfyingEvals);

        return openAlert;
    }

    public static Alert resolveAlert(Alert unresolvedAlert) {
        List<Set<ConditionEval>> resolvedEvals = new ArrayList<>();

        Data rtGoodData = Data.forNumeric(TENANT, autoResolveCondition.getDataId(),
                System.currentTimeMillis() + 20000,
                1000d);
        ThresholdRangeConditionEval eval1 = new ThresholdRangeConditionEval(autoResolveCondition, rtGoodData);
        Set<ConditionEval> evalSet1 = new HashSet<>();
        evalSet1.add(eval1);
        resolvedEvals.add(evalSet1);

        return resolveAlert(unresolvedAlert, resolvedEvals);
    }

}
