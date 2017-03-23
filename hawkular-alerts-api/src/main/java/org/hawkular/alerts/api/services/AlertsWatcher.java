/*
 * Copyright 2015-2017 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.alerts.api.services;

import org.hawkular.alerts.api.model.event.Event;

/**
 * A interface to be notified on generated or modified events or alerts.
 *
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
public interface AlertsWatcher {

    /**
     *
     * @param tenantId
     * @return
     */
    boolean isWatchable(String tenantId);

    /**
     * Watch a generated/modified event/alert.
     *
     * @param event Event watchable
     */
    void watch(Event event);
}
