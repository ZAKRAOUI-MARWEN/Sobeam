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
package org.sobeam.server.transport.mqtt.mqttv3.claim;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.sobeam.server.common.data.TransportPayloadType;
import org.sobeam.server.dao.service.DaoSqlTest;
import org.sobeam.server.gen.transport.TransportApiProtos;
import org.sobeam.server.transport.mqtt.MqttTestConfigProperties;
import org.sobeam.server.transport.mqtt.mqttv3.MqttTestClient;

@Slf4j
@DaoSqlTest
public class MqttClaimProtoDeviceTest extends MqttClaimDeviceTest {

    @Before
    public void beforeTest() throws Exception {
        MqttTestConfigProperties configProperties = MqttTestConfigProperties.builder()
                .deviceName("Test Claim device")
                .gatewayName("Test Claim gateway")
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .build();
        processBeforeTest(configProperties);
    }

    @Test
    public void testClaimingDevice() throws Exception {
        processTestClaimingDevice(false);
    }

    @Test
    public void testClaimingDeviceWithoutSecretAndDuration() throws Exception {
        processTestClaimingDevice(true);
    }

    @Test
    public void testGatewayClaimingDevice() throws Exception {
        processTestGatewayClaimingDevice("Test claiming gateway device Proto", false);
    }

    @Test
    public void testGatewayClaimingDeviceWithoutSecretAndDuration() throws Exception {
        processTestGatewayClaimingDevice("Test claiming gateway device empty payload Proto", true);
    }

    protected void processTestClaimingDevice(boolean emptyPayload) throws Exception {
        MqttTestClient client = new MqttTestClient();
        client.connectAndWait(accessToken);
        byte[] payloadBytes;
        if (emptyPayload) {
            payloadBytes = getClaimDevice(0, emptyPayload).toByteArray();
        } else {
            payloadBytes = getClaimDevice(60000, emptyPayload).toByteArray();
        }
        byte[] failurePayloadBytes = getClaimDevice(1, emptyPayload).toByteArray();
        validateClaimResponse(emptyPayload, client, payloadBytes, failurePayloadBytes);
    }

    protected void processTestGatewayClaimingDevice(String deviceName, boolean emptyPayload) throws Exception {
        processProtoTestGatewayClaimDevice(deviceName, emptyPayload);
    }

    private TransportApiProtos.ClaimDevice getClaimDevice(long duration, boolean emptyPayload) {
        TransportApiProtos.ClaimDevice.Builder claimDeviceBuilder = TransportApiProtos.ClaimDevice.newBuilder();
        if (!emptyPayload) {
            claimDeviceBuilder.setSecretKey("value");
        }
        if (duration > 0) {
            claimDeviceBuilder.setSecretKey("value");
            claimDeviceBuilder.setDurationMs(duration);
        }
        return claimDeviceBuilder.build();
    }


}
