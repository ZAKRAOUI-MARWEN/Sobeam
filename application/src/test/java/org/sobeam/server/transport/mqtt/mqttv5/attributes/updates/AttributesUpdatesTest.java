/**
 * Copyright © 2024 The Sobeam Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sobeam.server.transport.mqtt.mqttv5.attributes.updates;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.sobeam.server.common.data.TransportPayloadType;
import org.sobeam.server.dao.service.DaoSqlTest;
import org.sobeam.server.transport.mqtt.MqttTestConfigProperties;
import org.sobeam.server.transport.mqtt.mqttv5.attributes.AbstractAttributesMqttV5Test;

@Slf4j
@DaoSqlTest
public class AttributesUpdatesTest extends AbstractAttributesMqttV5Test {

    @Before
    public void beforeTest() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Subscribe to attribute updates")
                .transportPayloadType(TransportPayloadType.JSON)
                .build();
        processBeforeTest(configProperties);
    }

    @Test
    public void testAttributeMqttV5SimpleClientUpdates() throws Exception {
        processAttributesUpdatesTest();
    }
}
