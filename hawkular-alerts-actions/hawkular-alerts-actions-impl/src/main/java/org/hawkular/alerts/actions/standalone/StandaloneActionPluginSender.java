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
package org.hawkular.alerts.actions.standalone;

import org.hawkular.alerts.actions.api.ActionPluginSender;
import org.hawkular.alerts.actions.api.ActionResponseMessage;
import org.hawkular.alerts.actions.api.ActionResponseMessage.Operation;
import org.hawkular.alerts.api.json.JsonUtil;
import org.hawkular.alerts.api.model.action.Action;
import org.hawkular.alerts.api.services.ActionsService;
import org.jboss.logging.Logger;

/**
 * Main standalone sender for plugins implementations
 *
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
public class StandaloneActionPluginSender implements ActionPluginSender {
    private final MsgLogger msgLog = MsgLogger.LOGGER;
    private final Logger log = Logger.getLogger(StandaloneActionPluginListener.class);

    private ActionsService actions;

    public StandaloneActionPluginSender(ActionsService actions) {
        this.actions = actions;
    }

    @Override
    public ActionResponseMessage createMessage(Operation operation) {
        if (operation == null) {
            return new StandaloneActionResponseMessage();
        }
        return new StandaloneActionResponseMessage(operation);
    }

    @Override
    public void send(ActionResponseMessage msg) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("Message received: " + msg);
        }
        if (msg != null && msg.getPayload().containsKey("action")) {
            String jsonAction = msg.getPayload().get("action");
            Action updatedAction = JsonUtil.fromJson(jsonAction, Action.class);
            actions.updateResult(updatedAction);
            if (log.isDebugEnabled()) {
                log.debug("Operation message received from plugin [" + updatedAction.getActionPlugin() + "] with " +
                    "payload [" + updatedAction.getResult() + "]");
            }
        } else {
            msgLog.warnActionResponseMessageWithoutPayload();
        }
    }
}
