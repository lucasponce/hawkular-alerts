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

import static org.hawkular.alerts.actions.standalone.ServiceNames.Service.DEFINITIONS_SERVICE;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.naming.InitialContext;

import org.hawkular.alerts.actions.api.ActionMessage;
import org.hawkular.alerts.actions.api.ActionPluginListener;
import org.hawkular.alerts.api.model.action.Action;
import org.hawkular.alerts.api.services.ActionListener;
import org.hawkular.alerts.api.services.DefinitionsService;
import org.jboss.logging.Logger;

/**
 * Main standalone listener for plugins implementation.
 *
 * @author Lucas Ponce
 */
public class StandaloneActionPluginListener implements ActionListener {
    private static final String NUM_THREADS = "hawkular-alerts.standalone-actions-threads";
    private final MsgLogger msgLog = MsgLogger.LOGGER;
    private final Logger log = Logger.getLogger(StandaloneActionPluginListener.class);

    private InitialContext ctx;
    private DefinitionsService definitions;

    ExecutorService executorService;

    private Map<String, ActionPluginListener> plugins;

    public StandaloneActionPluginListener(Map<String, ActionPluginListener> plugins) {
        this.plugins = plugins;
        int numThreads = Integer.parseInt(System.getProperty(NUM_THREADS, "10"));
        executorService = Executors.newFixedThreadPool(numThreads, new StandaloneThreadFactory());
    }

    @Override
    public void process(Action action) {
        try {
            init();
            if (plugins.isEmpty()) {
                msgLog.warnNoPluginsFound();
                return;
            }
            if (action == null || action.getActionPlugin() == null) {
                msgLog.warnMessageReceivedWithoutPluginInfo();
                return;
            }
            String actionPlugin = action.getActionPlugin();
            final ActionPluginListener plugin = plugins.get(actionPlugin);
            Set<String> globals = ActionPlugins.getGlobals();
            if (plugin == null && ActionPlugins.getGlobals().isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("Received action [" + actionPlugin +
                            "] but no ActionPluginListener found on this deployment");
                }
                return;
            }

            ActionMessage pluginMessage = new StandaloneActionMessage(action);
            if (plugin != null) {
                executorService.execute(() -> {
                    try {
                        plugin.process(pluginMessage);
                    } catch (Exception e) {
                        log.debug("Error processing action: " + action.getActionPlugin(), e);
                        msgLog.errorProcessingAction(e.getMessage());
                    }
                });
            }
            // Check if the plugin is executed twice
            if (!globals.contains(actionPlugin)) {
                for (String global : globals) {
                    ActionPluginListener globalPlugin = ActionPlugins.getPlugins().get(global);
                    if (globalPlugin != null) {
                        executorService.execute(() -> {
                            try {
                                globalPlugin.process(pluginMessage);
                            } catch (Exception e) {
                                log.debug("Error processing action: " + action.getActionPlugin(), e);
                                msgLog.errorProcessingAction(e.getMessage());
                            }
                        });
                    }
                }
            }

        } catch (Exception e) {
            log.debug("Error processing action: " + action.getActionPlugin(), e);
            msgLog.errorProcessingAction(e.getMessage());
        }
    }

    private void init() throws Exception {
        if (ctx == null) {
            ctx = new InitialContext();
        }
        if (definitions == null) {
            definitions = (DefinitionsService) ctx.lookup(ServiceNames.getServiceName(DEFINITIONS_SERVICE));
        }
    }

    public void close() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    public class StandaloneThreadFactory implements ThreadFactory {
        private int counter = 0;
        private static final String PREFIX = "standalone-action-";

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, PREFIX + counter++);
        }
    }
}
