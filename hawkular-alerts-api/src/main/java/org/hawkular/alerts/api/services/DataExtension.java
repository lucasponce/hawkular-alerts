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

import java.util.TreeSet;

import org.hawkular.alerts.api.model.data.Data;

/**
 * An extension that will process received Data.
 *
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
public interface DataExtension {

    /**
     * The extension processes the supplied Data and returns Data to be forwarded, if any.
     *
     * @param data The Data to be processed by the extension.
     * @return The set of Data to be forwarded to the next extension, or core engine if this is the final extension.
     */
    TreeSet<Data> processData(TreeSet<Data> data);

}
