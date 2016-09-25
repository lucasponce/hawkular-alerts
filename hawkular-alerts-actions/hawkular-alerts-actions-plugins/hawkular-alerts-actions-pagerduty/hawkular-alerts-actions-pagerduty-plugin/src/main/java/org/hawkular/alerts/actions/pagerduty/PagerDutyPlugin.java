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
package org.hawkular.alerts.actions.pagerduty;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hawkular.alerts.actions.api.ActionMessage;
import org.hawkular.alerts.actions.api.ActionPluginListener;
import org.hawkular.alerts.actions.api.ActionPluginSender;
import org.hawkular.alerts.actions.api.ActionResponseMessage;
import org.hawkular.alerts.actions.api.MsgLogger;
import org.hawkular.alerts.actions.api.Plugin;
import org.hawkular.alerts.actions.api.Sender;
import org.hawkular.alerts.api.json.JsonUtil;
import org.hawkular.alerts.api.model.action.Action;
import org.hawkular.alerts.api.model.event.Alert;
import org.hawkular.alerts.api.model.event.Event;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.squareup.pagerduty.incidents.NotifyResult;
import com.squareup.pagerduty.incidents.PagerDuty;
import com.squareup.pagerduty.incidents.Trigger;

import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;

/**
 * Listens to pagerduty bus notifications and interacts with PagerDuty REST API.
 *
 * @author Thomas Segismont
 */
@Plugin(name = "pagerduty")
public class PagerDutyPlugin implements ActionPluginListener {
    static final String API_KEY_PROPERTY = "org.hawkular.actions.pagerduty.api.key";
    static final String API_KEY = System.getProperty(API_KEY_PROPERTY);

    private final MsgLogger msgLog = MsgLogger.LOGGER;

    Map<String, String> defaultProperties = new HashMap<>();

    PagerDuty pagerDuty;

    @Sender
    ActionPluginSender sender;

    private static final String MESSAGE_PROCESSED = "PROCESSED";
    private static final String MESSAGE_FAILED = "FAILED";

    public PagerDutyPlugin() {
        defaultProperties.put("description", "Default PagerDuty plugin description");
        setup();
    }

    @Override
    public Set<String> getProperties() {
        return defaultProperties.keySet();
    }

    @Override
    public Map<String, String> getDefaultProperties() {
        return defaultProperties;
    }

    @Override
    public void process(ActionMessage msg) throws Exception {
        if (pagerDuty == null) {
            msgLog.errorCannotProcessMessage("pagerduty", "Plugin is not started");
            return;
        }
        Trigger trigger;
        NotifyResult result = null;
        try {
            trigger = new Trigger.Builder(prepareMessage(msg)).build();
            result = pagerDuty.notify(trigger);
        } catch (Exception e) {
            msgLog.errorCannotProcessMessage("pagerduty", e.getMessage());
        }
        if (result != null && !"success".equals(result.status())) {
            msgLog.errorCannotProcessMessage("pagerduty", result.message());
            Action failedAction = msg.getAction();
            failedAction.setResult(MESSAGE_FAILED);
            sendResult(failedAction);
        } else {
            msgLog.infoActionReceived("pagerduty", msg.toString());
            Action successAction = msg.getAction();
            successAction.setResult(MESSAGE_PROCESSED);
            sendResult(successAction);
        }
    }

    void setup() {
        if (isBlank(API_KEY)) {
            String msg = "Configure " + API_KEY;
            msgLog.errorCannotBeStarted("pagerduty", msg);
            return;
        }
        try {
            InstanceCreator<NotifyResult> notifyResultCreator = buildNotifyResultCreator();
            Gson gson = new GsonBuilder().registerTypeAdapter(NotifyResult.class, notifyResultCreator).create();

            RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint("https://events.pagerduty.com")
                    .setConverter(new GsonConverter(gson)).build();

            pagerDuty = PagerDuty.create(API_KEY, restAdapter);
        } catch (Exception e) {
            msgLog.errorCannotBeStarted("pagerduty", e.getLocalizedMessage());
        }
    }


    InstanceCreator<NotifyResult> buildNotifyResultCreator() {
        Constructor<NotifyResult> constructor;
        try {
            constructor = NotifyResult.class.getDeclaredConstructor(
                    String.class,
                    String.class, String.class
            );
            if (!constructor.isAccessible()) {
                constructor.setAccessible(true);
            }
        } catch (Exception e) {
            throw new RuntimeException("Pager Duty Java client is not compatible", e);
        }
        NotifyResult notifyResult;
        try {
            notifyResult = constructor.newInstance("1", "2", "3");
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Pager Duty Java client is not compatible", e);
        }
        if (!(
                "1".equals(notifyResult.status())
                        && "2".equals(notifyResult.message())
                        && "3".equals(notifyResult.incidentKey())
        )) {
            throw new RuntimeException("Pager Duty Java client is not compatible");
        }
        return type -> {
            try {
                return constructor.newInstance("", "", "");
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String prepareMessage(ActionMessage msg) {
        String preparedMsg = null;
        Event event = msg.getAction() != null ? msg.getAction().getEvent() : null;

        if (event != null) {
            if (event instanceof Alert) {
                Alert alert = (Alert) event;
                preparedMsg = "Alert : " + alert.getTriggerId() + " at " + alert.getCtime() + " -- Severity: " +
                        alert.getSeverity().toString();
            } else {
                preparedMsg = "Event [" + event.getCategory() + "] " + event.getText() + " at " + event.getCtime();
            }
        } else {
            preparedMsg = "Message received without data at " + System.currentTimeMillis();
            msgLog.warnMessageReceivedWithoutPayload("pagerduty");
        }
        return preparedMsg;
    }

    private void sendResult(Action action) {
        if (sender == null) {
            throw new IllegalStateException("ActionPluginSender is not present in the plugin");
        }
        if (action == null) {
            throw new IllegalStateException("Action to update result must be not null");
        }
        ActionResponseMessage newMessage = sender.createMessage(ActionResponseMessage.Operation.RESULT);
        newMessage.getPayload().put("action", JsonUtil.toJson(action));
        try {
            sender.send(newMessage);
        } catch (Exception e) {
            msgLog.error("Error sending ActionResponseMessage", e);
        }
    }
}
