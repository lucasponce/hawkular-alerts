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
package org.hawkular.alerter.prometheus

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

import java.text.SimpleDateFormat

import groovyx.net.http.ContentType
import groovyx.net.http.RESTClient
import org.junit.Ignore
import org.junit.Test

/**
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
class PrometheusITest {

    def hawkular, response

    PrometheusITest() {
        hawkular = new RESTClient(System.getProperty('hawkular.base-uri') ?: 'http://127.0.0.1:8080/hawkular/alerts/', ContentType.JSON)
        hawkular.handler.failure = { it }
        /*
            Basic header is used only for hawkular-services distribution.
            This header is ignored on standalone scenarios.

            User: jdoe
            Password: password
            String encodedCredentials = Base64.getMimeEncoder()
                .encodeToString("$testUser:$testPasword".getBytes("utf-8"))
        */
        hawkular.defaultRequestHeaders.Authorization = "Basic amRvZTpwYXNzd29yZA=="
        hawkular.headers.put("Hawkular-Tenant", "test")
    }

    /*
        These tests are ignored as they need an external prometheus instance.
        Used for manual testing
     */

    @Ignore
    @Test
    void prometheusAlerterQueryTest() {
        def start = String.valueOf(System.currentTimeMillis())
        // load external trigger
        def definitions = new File("src/test/resources/prometheus-query-trigger.json").text
        response = hawkular.post(path: "import/all", body: definitions)
        assertTrue(response.status < 300)

        for ( int i=0; i < 60; ++i ) {
            Thread.sleep(1000);

            response = hawkular.get(path: "", query: [startTime:start,triggerIds:"prom-trigger"] )
            /*
                We should have only 1 alert
             */
            if ( response.status == 200 && response.data.size() == 1 ) {
                break;
            }
            assertEquals(200, response.status)
            System.out.println("NOTE!!!! May need to manually perform a get request on Prometheus to ge this to pass...")
        }
        assertEquals(200, response.status)
        assertEquals(1, response.data.size())

        response = hawkular.delete(path: "triggers/prom-trigger")
        assertEquals(200, response.status)
    }

    @Ignore
    @Test
    void prometheusAlerterAlertTest() {
        def start = String.valueOf(System.currentTimeMillis())
        // load external trigger
        def definitions = new File("src/test/resources/prometheus-alert-trigger.json").text
        response = hawkular.post(path: "import/all", body: definitions)
        assertTrue(response.status < 300)

        // now manually make a get request on prometheus to get this to work...
        for ( int i=0; i < 60; ++i ) {
            Thread.sleep(1000);

            response = hawkular.get(path: "", query: [startTime:start,triggerIds:"prom-alert-trigger"] )
            /*
                We should have only 1 alert
             */
            if ( response.status == 200 && response.data.size() == 1 ) {
                break;
            }
            assertEquals(200, response.status)
            System.out.println("NOTE!!!! Did you remember to configure prometheus to read the test rules file?")
        }
        assertEquals(200, response.status)
        assertEquals(1, response.data.size())

        response = hawkular.delete(path: "triggers/prom-alert-trigger")
        assertEquals(200, response.status)
    }

}
