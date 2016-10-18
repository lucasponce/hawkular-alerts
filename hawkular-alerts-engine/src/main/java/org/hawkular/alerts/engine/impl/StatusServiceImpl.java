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
package org.hawkular.alerts.engine.impl;

import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.hawkular.alerts.api.services.StatusService;
import org.hawkular.alerts.engine.service.PartitionManager;

import com.datastax.driver.core.Session;

/**
 * An implementation of {@link org.hawkular.alerts.api.services.StatusService}.
 *
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
@Local(StatusService.class)
@Stateless
@TransactionAttribute(value = TransactionAttributeType.NOT_SUPPORTED)
public class StatusServiceImpl implements StatusService {

    @EJB
    PartitionManager partitionManager;

    @Inject
    @CassClusterSession
    Session session;

    @Override
    public boolean isStarted() {
        return session != null && !session.isClosed();
    }

    @Override
    public boolean isDistributed() {
        return partitionManager.isDistributed();
    }

    @Override
    public Map<String, String> getDistributedStatus() {
        return partitionManager.getStatus();
    }
}
