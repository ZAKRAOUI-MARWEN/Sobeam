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
package org.sobeam.server.transport.mqtt.mqttv3.attributes.updates;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.sobeam.server.common.data.Device;
import org.sobeam.server.common.data.DeviceProfile;
import org.sobeam.server.common.data.TransportPayloadType;
import org.sobeam.server.common.data.device.profile.JsonTransportPayloadConfiguration;
import org.sobeam.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.sobeam.server.dao.service.DaoSqlTest;
import org.sobeam.server.transport.mqtt.MqttTestConfigProperties;
import org.sobeam.server.transport.mqtt.mqttv3.attributes.AbstractMqttAttributesIntegrationTest;

import static org.sobeam.server.common.data.device.profile.MqttTopics.DEVICE_ATTRIBUTES_SHORT_JSON_TOPIC;
import static org.sobeam.server.common.data.device.profile.MqttTopics.DEVICE_ATTRIBUTES_SHORT_TOPIC;
import static org.sobeam.server.common.data.device.profile.MqttTopics.DEVICE_ATTRIBUTES_TOPIC;

@Slf4j
@DaoSqlTest
public class MqttAttributesUpdatesIntegrationTest extends AbstractMqttAttributesIntegrationTest {

    @Before
    public void beforeTest() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Subscribe to attribute updates")
                .gatewayName("Gateway Test Subscribe to attribute updates")
                .transportPayloadType(TransportPayloadType.JSON)
                .build();
        processBeforeTest(configProperties);
    }

    @Test
    public void testJsonSubscribeToAttributesUpdatesFromTheServer() throws Exception {
        processJsonTestSubscribeToAttributesUpdates(DEVICE_ATTRIBUTES_TOPIC);
    }

    @Test
    public void testJsonSubscribeToAttributesUpdatesFromTheServerOnCustomTopic() throws Exception {
        Device tmp = savedDevice;
        String customTopic = "v1/devices/me/subscribeattributes";
        JsonTransportPayloadConfiguration jsonTransportPayloadConfiguration = new JsonTransportPayloadConfiguration();
        MqttDeviceProfileTransportConfiguration mqttDeviceProfileTransportConfiguration =
                this.createMqttDeviceProfileTransportConfiguration(jsonTransportPayloadConfiguration, true,
                        "v1/devices/me/telemetry", "v1/devices/me/attributes", customTopic);
        DeviceProfile deviceProfile = this.createDeviceProfile("New device Profile",
                mqttDeviceProfileTransportConfiguration);
        DeviceProfile savedProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        savedDevice.setDeviceProfileId(savedProfile.getId());
        doPost("/api/device", savedDevice);
        processJsonTestSubscribeToAttributesUpdates(customTopic);
        savedDevice = tmp;
    }

    @Test
    public void testJsonSubscribeToAttributesUpdatesFromTheServerOnShortTopic() throws Exception {
        processJsonTestSubscribeToAttributesUpdates(DEVICE_ATTRIBUTES_SHORT_TOPIC);
    }

    @Test
    public void testJsonSubscribeToAttributesUpdatesFromTheServerOnShortJsonTopic() throws Exception {
        processJsonTestSubscribeToAttributesUpdates(DEVICE_ATTRIBUTES_SHORT_JSON_TOPIC);
    }

    @Test
    public void testJsonSubscribeToAttributesUpdatesFromTheServerGateway() throws Exception {
        processJsonGatewayTestSubscribeToAttributesUpdates();
    }

}
