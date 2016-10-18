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
package org.hawkular.alerts.api.services;

import java.util.Map;

/**
 * Interface that allows to check main status of Hawkular Alerting system
 *
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
public interface StatusService {

    /**
     * @return true if system has initialized Cassandra backend correctly
     *         false otherwise
     */
    boolean isStarted();

    /**
     * @return true if system is running on a distributed scenario.
     *         false if system is running on a standalone scenario.
     */
    boolean isDistributed();

    /**
     * Show additional information about distributed status.
     * In distributed scenarios
     *  - getDistributedStatus().get("currentNode") will store a string with the identifier of the current node
     *  - getDistributedStatus().get("members") will store a string with a list comma identifiers of the nodes of the topology
     *    at the moment of the call
     * In standalone scenarios getDistributedStatus() will return an empty map.
     *
     * @return Map with currentNode and members information for distributed scenarios
     */
    Map<String, String> getDistributedStatus();
}
