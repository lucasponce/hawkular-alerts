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
package org.hawkular.alerts.bus.log;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

/**
 * Common log for INFO, WARN, ERROR and FATAL messages.
 *
 * @author Lucas Ponce
 */
@MessageLogger(projectCode = "HAWKALERT")
@ValidIdRange(min = 210000, max = 219999)
public interface MsgLogger extends BasicLogger {
    MsgLogger LOGGER = Logger.getMessageLogger(MsgLogger.class, MsgLogger.class.getPackage().getName());

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 210001, value = "ActionResponse message received without payload.")
    void warnActionResponseMessageWithoutPayload();

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 210002, value = "Action plugin [%s] registered")
    void infoActionPluginRegistration(String actionPlugin);

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 210003, value = "Action plugin [%s] is already registered")
    void warnActionPluginAlreadyRegistered(String actionPlugin);

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 210005, value = "Cannot connect to hawkular bus")
    void warnCannotConnectToBus();

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 210007, value = "Cannot access to DefinitionsService")
    void warnCannotAccessToDefinitionsService();

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 210008, value = "Error processing action. Description: [%s]")
    void errorProcessingAction(String msg);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 210009, value = "Error accesing to DefinitionsService. Description: [%s]")
    void errorDefinitionsService(String msg);

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 210010, value = "Cannot connect to the broker. Attempt [%s]. Trying in [%s] ms. Error: [%s]")
    void warnCannotConnectBroker(int attempt, int next, String msg);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 210011, value = "Error sending publish message to the bus. Error: [%s]")
    void errorCannotSendPublishMessage(String msg);

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 210012, value = "Init Publish Cache")
    void infoInitPublishCache();

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 210013, value = "Clear Publish Cache")
    void warnClearPublishCache();

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 210014, value = "Publish Cache is disabled")
    void warnDisabledPublishCache();
}
