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
package org.sobeam.server.transport.coap.provision;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.sobeam.common.util.JacksonUtil;
import org.sobeam.server.common.data.CoapDeviceType;
import org.sobeam.server.common.data.Device;
import org.sobeam.server.common.data.DeviceProfileProvisionType;
import org.sobeam.server.common.data.TransportPayloadType;
import org.sobeam.server.common.data.security.DeviceCredentials;
import org.sobeam.server.common.msg.EncryptionUtil;
import org.sobeam.server.common.msg.session.FeatureType;
import org.sobeam.server.dao.device.DeviceCredentialsService;
import org.sobeam.server.dao.device.DeviceService;
import org.sobeam.server.dao.device.provision.ProvisionResponseStatus;
import org.sobeam.server.dao.service.DaoSqlTest;
import org.sobeam.server.transport.coap.AbstractCoapIntegrationTest;
import org.sobeam.server.transport.coap.CoapTestClient;
import org.sobeam.server.transport.coap.CoapTestConfigProperties;

@Slf4j
@DaoSqlTest
public class CoapProvisionJsonDeviceTest extends AbstractCoapIntegrationTest {

    @Autowired
    DeviceCredentialsService deviceCredentialsService;

    @Autowired
    DeviceService deviceService;

    @After
    public void afterTest() throws Exception {
        processAfterTest();
    }

    @Test
    public void testProvisioningDisabledDevice() throws Exception {
        processTestProvisioningDisabledDevice();
    }

    @Test
    public void testProvisioningCheckPreProvisionedDevice() throws Exception {
        processTestProvisioningCheckPreProvisionedDevice();
    }

    @Test
    public void testProvisioningCreateNewDeviceWithoutCredentials() throws Exception {
        processTestProvisioningCreateNewDeviceWithoutCredentials();
    }

    @Test
    public void testProvisioningCreateNewDeviceWithAccessToken() throws Exception {
        processTestProvisioningCreateNewDeviceWithAccessToken();
    }

    @Test
    public void testProvisioningCreateNewDeviceWithCert() throws Exception {
        processTestProvisioningCreateNewDeviceWithCert();
    }

    @Test
    public void testProvisioningWithBadKeyDevice() throws Exception {
        processTestProvisioningWithBadKeyDevice();
    }


    private void processTestProvisioningDisabledDevice() throws Exception {
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .deviceName("Test Provision device")
                .coapDeviceType(CoapDeviceType.DEFAULT)
                .transportPayloadType(TransportPayloadType.JSON)
                .build();
        processBeforeTest(configProperties);
        JsonNode response = JacksonUtil.fromBytes(createCoapClientAndPublish());
        Assert.assertTrue(response.hasNonNull("errorMsg"));
        Assert.assertTrue(response.hasNonNull("status"));
        Assert.assertEquals("Provision data was not found!", response.get("errorMsg").asText());
        Assert.assertEquals(ProvisionResponseStatus.NOT_FOUND.name(), response.get("status").asText());
    }


    private void processTestProvisioningCreateNewDeviceWithoutCredentials() throws Exception {
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .deviceName("Test Provision device3")
                .coapDeviceType(CoapDeviceType.DEFAULT)
                .transportPayloadType(TransportPayloadType.JSON)
                .provisionType(DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES)
                .provisionKey("testProvisionKey")
                .provisionSecret("testProvisionSecret")
                .build();
        processBeforeTest(configProperties);
        JsonNode response = JacksonUtil.fromBytes(createCoapClientAndPublish());
        Assert.assertTrue(response.hasNonNull("credentialsType"));
        Assert.assertTrue(response.hasNonNull("status"));

        Device createdDevice = deviceService.findDeviceByTenantIdAndName(tenantId, "Test Provision device");

        Assert.assertNotNull(createdDevice);

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, createdDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.get("credentialsType").asText());
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.get("status").asText());
    }


    private void processTestProvisioningCreateNewDeviceWithAccessToken() throws Exception {
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .deviceName("Test Provision device3")
                .coapDeviceType(CoapDeviceType.DEFAULT)
                .transportPayloadType(TransportPayloadType.JSON)
                .provisionType(DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES)
                .provisionKey("testProvisionKey")
                .provisionSecret("testProvisionSecret")
                .build();
        processBeforeTest(configProperties);
        String requestCredentials = ",\"credentialsType\": \"ACCESS_TOKEN\",\"token\": \"test_token\"";
        JsonNode response = JacksonUtil.fromBytes(createCoapClientAndPublish(requestCredentials));
        Assert.assertTrue(response.hasNonNull("credentialsType"));
        Assert.assertTrue(response.hasNonNull("status"));

        Device createdDevice = deviceService.findDeviceByTenantIdAndName(tenantId, "Test Provision device");

        Assert.assertNotNull(createdDevice);

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, createdDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.get("credentialsType").asText());
        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), "ACCESS_TOKEN");
        Assert.assertEquals(deviceCredentials.getCredentialsId(), "test_token");
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.get("status").asText());
    }


    private void processTestProvisioningCreateNewDeviceWithCert() throws Exception {
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .deviceName("Test Provision device3")
                .coapDeviceType(CoapDeviceType.DEFAULT)
                .transportPayloadType(TransportPayloadType.JSON)
                .provisionType(DeviceProfileProvisionType.ALLOW_CREATE_NEW_DEVICES)
                .provisionKey("testProvisionKey")
                .provisionSecret("testProvisionSecret")
                .build();
        processBeforeTest(configProperties);
        String requestCredentials = ",\"credentialsType\": \"X509_CERTIFICATE\",\"hash\": \"testHash\"";
        JsonNode response = JacksonUtil.fromBytes(createCoapClientAndPublish(requestCredentials));
        Assert.assertTrue(response.hasNonNull("credentialsType"));
        Assert.assertTrue(response.hasNonNull("status"));

        Device createdDevice = deviceService.findDeviceByTenantIdAndName(tenantId, "Test Provision device");

        Assert.assertNotNull(createdDevice);

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, createdDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.get("credentialsType").asText());
        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), "X509_CERTIFICATE");

        String cert = EncryptionUtil.certTrimNewLines(deviceCredentials.getCredentialsValue());
        String sha3Hash = EncryptionUtil.getSha3Hash(cert);

        Assert.assertEquals(deviceCredentials.getCredentialsId(), sha3Hash);

        Assert.assertEquals(deviceCredentials.getCredentialsValue(), "testHash");
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.get("status").asText());
    }

    private void processTestProvisioningCheckPreProvisionedDevice() throws Exception {
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .deviceName("Test Provision device")
                .coapDeviceType(CoapDeviceType.DEFAULT)
                .transportPayloadType(TransportPayloadType.JSON)
                .provisionType(DeviceProfileProvisionType.CHECK_PRE_PROVISIONED_DEVICES)
                .provisionKey("testProvisionKey")
                .provisionSecret("testProvisionSecret")
                .build();
        processBeforeTest(configProperties);
        JsonNode response = JacksonUtil.fromBytes(createCoapClientAndPublish());
        Assert.assertTrue(response.hasNonNull("credentialsType"));
        Assert.assertTrue(response.hasNonNull("status"));

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, savedDevice.getId());

        Assert.assertEquals(deviceCredentials.getCredentialsType().name(), response.get("credentialsType").asText());
        Assert.assertEquals(ProvisionResponseStatus.SUCCESS.name(), response.get("status").asText());
    }

    private void processTestProvisioningWithBadKeyDevice() throws Exception {
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .deviceName("Test Provision device")
                .coapDeviceType(CoapDeviceType.DEFAULT)
                .transportPayloadType(TransportPayloadType.JSON)
                .provisionType(DeviceProfileProvisionType.CHECK_PRE_PROVISIONED_DEVICES)
                .provisionKey("testProvisionKeyOrig")
                .provisionSecret("testProvisionSecret")
                .build();
        processBeforeTest(configProperties);
        JsonNode response = JacksonUtil.fromBytes(createCoapClientAndPublish());
        Assert.assertTrue(response.hasNonNull("errorMsg"));
        Assert.assertTrue(response.hasNonNull("status"));
        Assert.assertEquals("Provision data was not found!", response.get("errorMsg").asText());
        Assert.assertEquals(ProvisionResponseStatus.NOT_FOUND.name(), response.get("status").asText());
    }

    private byte[] createCoapClientAndPublish() throws Exception {
        return createCoapClientAndPublish("");
    }

    private byte[] createCoapClientAndPublish(String deviceCredentials) throws Exception {
        String provisionRequestMsg = createTestProvisionMessage(deviceCredentials);
        client = new CoapTestClient(accessToken, FeatureType.PROVISION);
        return client.postMethod(provisionRequestMsg.getBytes()).getPayload();
    }

    private String createTestProvisionMessage(String deviceCredentials) {
        return "{\"deviceName\":\"Test Provision device\",\"provisionDeviceKey\":\"testProvisionKey\", \"provisionDeviceSecret\":\"testProvisionSecret\"" + deviceCredentials + "}";
    }
}
