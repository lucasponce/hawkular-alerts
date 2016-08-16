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
package org.hawkular.alerts.bus.publish;

import java.io.IOException;

import javax.enterprise.context.Dependent;
import javax.jms.JMSException;
import javax.jms.QueueConnectionFactory;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.hawkular.alerts.bus.api.PublishCommandMessage;
import org.hawkular.alerts.bus.log.MsgLogger;
import org.hawkular.bus.common.ConnectionContextFactory;
import org.hawkular.bus.common.Endpoint;
import org.hawkular.bus.common.MessageId;
import org.hawkular.bus.common.MessageProcessor;
import org.hawkular.bus.common.producer.ProducerConnectionContext;
import org.jboss.logging.Logger;

/**
 * A wrapper class to send publish command to the metrics queue
 *
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
@Dependent
public class PublishCommandSender {

    public static final int NUM_ATTEMPTS = 10;
    public static final int TIMEOUT = 2000;

    private static final String CONNECTION_FACTORY = "java:/HawkularBusConnectionFactory";
    private static final String METRICS_PUBLISH = "MetricsPublish";

    private final MsgLogger msgLog = MsgLogger.LOGGER;
    private final Logger log = Logger.getLogger(PublishCommandSender.class);

    private QueueConnectionFactory conFactory;
    private ThreadLocal<ConnectionContextFactory> ccf = new ThreadLocal<>();
    private ThreadLocal<ProducerConnectionContext> pcc = new ThreadLocal<>();

    private void init() throws Exception {
        /*
            Init and cache factories and contexts used.
            ActiveMQ recommends to catch MessageProducer and MessageConsumer objects that are wrapped in context:
            http://activemq.apache.org/how-do-i-use-jms-efficiently.html
         */
        if (conFactory == null) {
            try {
                InitialContext ctx = new InitialContext();
                conFactory = (QueueConnectionFactory)ctx.lookup(CONNECTION_FACTORY);
            } catch (NamingException e) {
                throw new IllegalStateException("Cannot get context");
            }
        }

        if (ccf.get() == null) {
            int i = NUM_ATTEMPTS;
            while (ccf.get() == null && i >= 0) {
                try {
                    ccf.set(new ConnectionContextFactory(conFactory));
                } catch (JMSException e) {
                    msgLog.warnCannotConnectBroker(i, TIMEOUT, e.getMessage());
                    try {
                        Thread.sleep(TIMEOUT);
                    } catch(InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
                i--;
            }
            if (ccf.get() == null) {
                throw new IllegalStateException("Cannot connect to the broker.");
            }
        }

        if (pcc.get() == null) {
            pcc.set(ccf.get().createProducerConnectionContext(
                    new Endpoint(Endpoint.Type.QUEUE, METRICS_PUBLISH)));
        }
    }

    public void send(PublishCommandMessage msg) throws Exception {
        init();
        try {
            if (pcc.get() == null) {
                pcc.set(ccf.get().createProducerConnectionContext(
                        new Endpoint(Endpoint.Type.QUEUE, METRICS_PUBLISH)));
            }
            MessageId mid = new MessageProcessor().send(pcc.get(), (PublishCommandMessage)msg);
            if (pcc.get() != null) {
                try {
                    pcc.get().close();
                    pcc.remove();
                } catch (IOException ignored) { }
            }
            if (ccf != null) {
                try {
                    ccf.get().close();
                    ccf.remove();
                } catch (Exception ignored) { }
            }
        } catch (JMSException e) {
            log.debug(e.getMessage(), e);
            msgLog.errorCannotSendPublishMessage(e.getMessage());
        }
    }

}
