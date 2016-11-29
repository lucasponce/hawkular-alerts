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

import static org.hawkular.alerts.actions.standalone.ServiceNames.Service.ACTIONS_SERVICE;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.hawkular.alerts.actions.api.ActionPluginListener;
import org.hawkular.alerts.actions.api.ActionPluginSender;
import org.hawkular.alerts.actions.api.Plugin;
import org.hawkular.alerts.actions.api.Sender;
import org.hawkular.alerts.api.services.ActionsService;
import org.jboss.vfs.VirtualFile;

/**
 * Helper class to find the classes annotated with ActionPlugin and instantiate them.
 *
 * @author Lucas Ponce
 */
public class ActionPlugins {
    private final MsgLogger msgLog = MsgLogger.LOGGER;
    private ActionsService actions;
    private static ActionPlugins instance;
    private Map<String, ActionPluginListener> plugins;
    private Map<String, ActionPluginSender> senders;

    public static synchronized Map<String, ActionPluginListener> getPlugins() {
        if (instance == null) {
            instance = new ActionPlugins();
        }
        return Collections.unmodifiableMap(instance.plugins);
    }

    public static synchronized Map<String, ActionPluginSender> getSenders() {
        if (instance == null) {
            instance = new ActionPlugins();
        }
        return Collections.unmodifiableMap(instance.senders);
    }

    private ActionPlugins() {
        try {
            plugins = new HashMap<>();
            senders = new HashMap<>();
            init();
            List<URL> webInfUrls = getWebInfUrls();
            for (URL webInfUrl : webInfUrls) {
                List<Class> pluginClasses = findAnnotationInJar(webInfUrl, Plugin.class);
                for (Class pluginClass : pluginClasses) {
                    Annotation actionPlugin = pluginClass.getDeclaredAnnotation(Plugin.class);
                    if (actionPlugin instanceof Plugin) {
                        String name = ((Plugin) actionPlugin).name();
                        Object newInstance = pluginClass.newInstance();
                        if (newInstance instanceof ActionPluginListener) {
                            ActionPluginListener pluginInstance = (ActionPluginListener)newInstance;
                            injectActionPluginSender(name, pluginInstance);
                            plugins.put(name, pluginInstance);
                        } else {
                            throw new IllegalStateException("Plugin [" + name + "] is not instance of " +
                                    "ActionPluginListener");
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private List<URL> getWebInfUrls() throws Exception {
        Enumeration<URL> allUrls = Thread.currentThread().getContextClassLoader().getResources("");
        List<URL> webInfUrls = new ArrayList<>();
        while (allUrls != null && allUrls.hasMoreElements()) {
            URL url = allUrls.nextElement();
            String sUrl = url.toExternalForm();
            int indexPrefix = sUrl.indexOf("hawkular-alerts-actions-");
            if (indexPrefix > 0) {
                int indexSuffix = sUrl.indexOf("-plugin", indexPrefix);
                if (indexSuffix > 0) {
                    msgLog.debugf("Scanning %s", url);
                    webInfUrls.add(url);
                }
            }
        }
        return webInfUrls;
    }

    private List<Class> findAnnotationInJar(URL url, Class annotation) throws Exception {
        if (url == null || annotation == null) {
            throw new IllegalArgumentException("url or annotation must be not null");
        }
        List<Class> plugins = new ArrayList<>();
        URLConnection conn = url.openConnection();
        Object content = conn.getContent();
        if (content instanceof VirtualFile) {
            VirtualFile root = (VirtualFile)content;
            List<VirtualFile> children = root.getChildrenRecursively();
            for (VirtualFile vf : children) {
                String vfName = vf.toURI().toString();
                msgLog.debugf("Searching %s", vfName);
                if (vfName.endsWith(".class")) {
                    int startName = vfName.indexOf(".jar/") + 5;
                    int stopName = vfName.indexOf(".class");
                    String className = vfName.substring(startName, stopName).replace("/", ".");
                    msgLog.debugf("Loading %s", className);
                    Class clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
                    if (clazz.isAnnotationPresent(annotation)) {
                        plugins.add(clazz);
                    }
                }
            }
        }
        return plugins;
    }

    /*
        Search and inject ActionPluginSender inside ActionPluginListener
     */
    private void injectActionPluginSender(String actionPlugin, ActionPluginListener pluginInstance) throws Exception {
        if (pluginInstance == null) {
            throw new IllegalArgumentException("pluginInstance must be not null");
        }
        Field[] fields = pluginInstance.getClass().getDeclaredFields();
        Field sender = null;
        for (Field field : fields) {
            if (field.isAnnotationPresent(Sender.class) &&
                    field.getType().isAssignableFrom(ActionPluginSender.class)) {
                sender = field;
                break;
            }
        }
        if (sender != null) {
            ActionPluginSender standaloneSender = new StandaloneActionPluginSender(actions);
            sender.setAccessible(true);
            sender.set(pluginInstance, standaloneSender);
            senders.put(actionPlugin, standaloneSender);
        }
    }

    private void init() {
        if (actions == null) {
            try {
                InitialContext ctx = new InitialContext();
                actions = (ActionsService)ctx.lookup(ServiceNames.getServiceName(ACTIONS_SERVICE));
            } catch (NamingException e) {
                msgLog.error("Cannot access to JNDI context", e);
            }

        }
    }

}
