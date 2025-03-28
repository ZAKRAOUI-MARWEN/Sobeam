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
package org.sobeam.server.transport.lwm2m;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.eclipse.leshan.client.LeshanClient;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.core.ResponseCode;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.sobeam.common.util.JacksonUtil;
import org.sobeam.common.util.SoBeamThreadFactory;
import org.sobeam.server.common.data.Device;
import org.sobeam.server.common.data.DeviceProfile;
import org.sobeam.server.common.data.DeviceProfileProvisionType;
import org.sobeam.server.common.data.DeviceProfileType;
import org.sobeam.server.common.data.DeviceTransportType;
import org.sobeam.server.common.data.ResourceType;
import org.sobeam.server.common.data.TbResource;
import org.sobeam.server.common.data.device.credentials.lwm2m.LwM2MBootstrapClientCredentials;
import org.sobeam.server.common.data.device.credentials.lwm2m.LwM2MClientCredential;
import org.sobeam.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.sobeam.server.common.data.device.credentials.lwm2m.NoSecBootstrapClientCredential;
import org.sobeam.server.common.data.device.credentials.lwm2m.NoSecClientCredential;
import org.sobeam.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.sobeam.server.common.data.device.profile.DeviceProfileData;
import org.sobeam.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.sobeam.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.sobeam.server.common.data.device.profile.lwm2m.OtherConfiguration;
import org.sobeam.server.common.data.device.profile.lwm2m.TelemetryMappingConfiguration;
import org.sobeam.server.common.data.device.profile.lwm2m.bootstrap.AbstractLwM2MBootstrapServerCredential;
import org.sobeam.server.common.data.device.profile.lwm2m.bootstrap.LwM2MBootstrapServerCredential;
import org.sobeam.server.common.data.device.profile.lwm2m.bootstrap.NoSecLwM2MBootstrapServerCredential;
import org.sobeam.server.common.data.query.EntityData;
import org.sobeam.server.common.data.query.EntityDataPageLink;
import org.sobeam.server.common.data.query.EntityDataQuery;
import org.sobeam.server.common.data.query.EntityKey;
import org.sobeam.server.common.data.query.EntityKeyType;
import org.sobeam.server.common.data.query.SingleEntityFilter;
import org.sobeam.server.common.data.security.DeviceCredentials;
import org.sobeam.server.common.data.security.DeviceCredentialsType;
import org.sobeam.server.common.transport.util.JsonUtils;
import org.sobeam.server.dao.service.DaoSqlTest;
import org.sobeam.server.service.ws.telemetry.cmd.v2.EntityDataCmd;
import org.sobeam.server.service.ws.telemetry.cmd.v2.EntityDataUpdate;
import org.sobeam.server.service.ws.telemetry.cmd.v2.LatestValueCmd;
import org.sobeam.server.transport.AbstractTransportIntegrationTest;
import org.sobeam.server.transport.lwm2m.client.LwM2MTestClient;
import org.sobeam.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.sobeam.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.eclipse.leshan.client.object.Security.noSec;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.sobeam.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_BOOTSTRAP_STARTED;
import static org.sobeam.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_BOOTSTRAP_SUCCESS;
import static org.sobeam.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_INIT;
import static org.sobeam.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_STARTED;
import static org.sobeam.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_SUCCESS;
import static org.sobeam.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_UPDATE_STARTED;
import static org.sobeam.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_UPDATE_SUCCESS;
import static org.sobeam.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType;
import static org.sobeam.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.NONE;

@TestPropertySource(properties = {
        "transport.lwm2m.enabled=true",
})
@Slf4j
@DaoSqlTest
public abstract class AbstractLwM2MIntegrationTest extends AbstractTransportIntegrationTest {

    @SpyBean
    LwM2mUplinkMsgHandler defaultLwM2mUplinkMsgHandlerTest;

    @Autowired
    private LwM2mClientContext clientContextTest;

    //  Lwm2m Server
    public static final int port = 5685;
    public static final int securityPort = 5686;
    public static final int portBs = 5687;
    public static final int securityPortBs = 5688;
    public static final int[] SERVERS_PORT_NUMBERS = {port, securityPort, portBs, securityPortBs};

    public static final String host = "localhost";
    public static final String hostBs = "localhost";
    public static final Integer shortServerId = 123;
    public static final Integer shortServerIdBs0 = 0;
    public static final int serverId = 1;
    public static final int serverIdBs = 0;

    public static final String COAP = "coap://";
    public static final String COAPS = "coaps://";
    public static final String URI = COAP + host + ":" + port;
    public static final String SECURE_URI = COAPS + host + ":" + securityPort;
    public static final String URI_BS = COAP + hostBs + ":" + portBs;
    public static final String SECURE_URI_BS = COAPS + hostBs + ":" + securityPortBs;
    public static final Security SECURITY_NO_SEC = noSec(URI, shortServerId);

    protected final String OBSERVE_ATTRIBUTES_WITHOUT_PARAMS =
            "    {\n" +
                    "    \"keyName\": {},\n" +
                    "    \"observe\": [],\n" +
                    "    \"attribute\": [],\n" +
                    "    \"telemetry\": [],\n" +
                    "    \"attributeLwm2m\": {}\n" +
                    "  }";
    public static  String OBSERVE_ATTRIBUTES_WITH_PARAMS =

            "    {\n" +
                    "    \"keyName\": {\n" +
                    "      \"/3_1.2/0/9\": \"batteryLevel\"\n" +
                    "    },\n" +
                    "    \"observe\": [],\n" +
                    "    \"attribute\": [\n" +
                    "    ],\n" +
                    "    \"telemetry\": [\n" +
                    "      \"/3_1.2/0/9\"\n" +
                    "    ],\n" +
                    "    \"attributeLwm2m\": {}\n" +
                    "  }";

    public static final String CLIENT_LWM2M_SETTINGS =
            "     {\n" +
                    "    \"edrxCycle\": null,\n" +
                    "    \"powerMode\": \"DRX\",\n" +
                    "    \"fwUpdateResource\": null,\n" +
                    "    \"fwUpdateStrategy\": 1,\n" +
                    "    \"psmActivityTimer\": null,\n" +
                    "    \"swUpdateResource\": null,\n" +
                    "    \"swUpdateStrategy\": 1,\n" +
                    "    \"pagingTransmissionWindow\": null,\n" +
                    "    \"clientOnlyObserveAfterConnect\": 1\n" +
                    "  }";
    protected final Set<Lwm2mTestHelper.LwM2MClientState> expectedStatusesRegistrationLwm2mSuccess = new HashSet<>(Arrays.asList(ON_INIT, ON_REGISTRATION_STARTED, ON_REGISTRATION_SUCCESS));
    protected final Set<Lwm2mTestHelper.LwM2MClientState> expectedStatusesRegistrationLwm2mSuccessUpdate = new HashSet<>(Arrays.asList(ON_INIT, ON_REGISTRATION_STARTED, ON_REGISTRATION_SUCCESS, ON_UPDATE_STARTED, ON_UPDATE_SUCCESS));
    protected final Set<Lwm2mTestHelper.LwM2MClientState> expectedStatusesRegistrationBsSuccess = new HashSet<>(Arrays.asList(ON_BOOTSTRAP_STARTED, ON_BOOTSTRAP_SUCCESS, ON_REGISTRATION_STARTED, ON_REGISTRATION_SUCCESS));
    protected DeviceProfile deviceProfile;
    protected ScheduledExecutorService executor;
    protected LwM2MTestClient lwM2MTestClient;
    private String[] resources;
    protected String deviceId;
    protected boolean isWriteAttribute = false;
    protected boolean supportFormatOnly_SenMLJSON_SenMLCBOR = false;

    @Before
    public void startInit() throws Exception {
        init();
    }

    @After
    public void after() {
        clientDestroy();
        executor.shutdownNow();
    }

    @AfterClass
    public static void afterClass() {
        awaitServersDestroy();
    }

    private void init() throws Exception {
        executor = Executors.newScheduledThreadPool(10, SoBeamThreadFactory.forName("test-lwm2m-scheduled"));
        loginTenantAdmin();
        for (String resourceName : this.resources) {
            TbResource lwModel = new TbResource();
            lwModel.setResourceType(ResourceType.LWM2M_MODEL);
            lwModel.setTitle(resourceName);
            lwModel.setFileName(resourceName);
            lwModel.setTenantId(tenantId);
            byte[] bytes = IOUtils.toByteArray(AbstractLwM2MIntegrationTest.class.getClassLoader().getResourceAsStream("lwm2m/" + resourceName));
            lwModel.setData(bytes);
            lwModel = doPostWithTypedResponse("/api/resource", lwModel, new TypeReference<>() {
            });
            Assert.assertNotNull(lwModel);
        }
    }

    public void basicTestConnectionObserveTelemetry(Security security,
                                                    LwM2MDeviceCredentials deviceCredentials,
                                                    String endpoint,
                                                    boolean queueMode) throws Exception {
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITH_PARAMS, getBootstrapServerCredentialsNoSec(NONE));
        createDeviceProfile(transportConfiguration);
        Device device = createDevice(deviceCredentials, endpoint);

        SingleEntityFilter sef = new SingleEntityFilter();
        sef.setSingleEntity(device.getId());
        LatestValueCmd latestCmd = new LatestValueCmd();
        latestCmd.setKeys(Collections.singletonList(new EntityKey(EntityKeyType.TIME_SERIES, "batteryLevel")));
        EntityDataQuery edq = new EntityDataQuery(sef, new EntityDataPageLink(1, 0, null, null),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        EntityDataCmd cmd = new EntityDataCmd(1, edq, null, latestCmd, null);
        getWsClient().send(cmd);
        getWsClient().waitForReply();

        getWsClient().registerWaitForUpdate();
        createNewClient(security, null, false, endpoint, null, queueMode);
        deviceId = device.getId().getId().toString();
        awaitObserveReadAll(0, deviceId);
        String msg = getWsClient().waitForUpdate();

        EntityDataUpdate update = JacksonUtil.fromString(msg, EntityDataUpdate.class);
        Assert.assertEquals(1, update.getCmdId());
        List<EntityData> eData = update.getUpdate();
        Assert.assertNotNull(eData);
        Assert.assertEquals(1, eData.size());
        Assert.assertEquals(device.getId(), eData.get(0).getEntityId());
        Assert.assertNotNull(eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES));
        var tsValue = eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES).get("batteryLevel");
        Assert.assertThat(Long.parseLong(tsValue.getValue()), instanceOf(Long.class));
        int expectedMax = 50;
        int expectedMin = 5;
        Assert.assertTrue(expectedMax >= Long.parseLong(tsValue.getValue()));
        Assert.assertTrue(expectedMin <= Long.parseLong(tsValue.getValue()));


    }

    protected void createDeviceProfile(Lwm2mDeviceProfileTransportConfiguration transportConfiguration) throws Exception {
        deviceProfile = new DeviceProfile();
        deviceProfile.setName("LwM2M");
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTenantId(tenantId);
        deviceProfile.setTransportType(DeviceTransportType.LWM2M);
        deviceProfile.setProvisionType(DeviceProfileProvisionType.DISABLED);
        deviceProfile.setDescription(deviceProfile.getName());

        DeviceProfileData deviceProfileData = new DeviceProfileData();
        deviceProfileData.setConfiguration(new DefaultDeviceProfileConfiguration());
        deviceProfileData.setProvisionConfiguration(new DisabledDeviceProfileProvisionConfiguration(null));
        deviceProfileData.setTransportConfiguration(transportConfiguration);
        deviceProfile.setProfileData(deviceProfileData);

        deviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        Assert.assertNotNull(deviceProfile);
    }

    protected Device createDevice(LwM2MDeviceCredentials credentials, String endpoint) throws Exception {
        Device device = new Device();
        device.setName(endpoint);
        device.setDeviceProfileId(deviceProfile.getId());
        device.setTenantId(tenantId);
        device = doPost("/api/device", device, Device.class);
        Assert.assertNotNull(device);

        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + device.getId().getId().toString() + "/credentials", DeviceCredentials.class);
        Assert.assertEquals(device.getId(), deviceCredentials.getDeviceId());
        deviceCredentials.setCredentialsType(DeviceCredentialsType.LWM2M_CREDENTIALS);
        deviceCredentials.setCredentialsValue(JacksonUtil.toString(credentials));
        doPost("/api/device/credentials", deviceCredentials).andExpect(status().isOk());
        return device;
    }

    public NoSecClientCredential createNoSecClientCredentials(String endpoint) {
        NoSecClientCredential clientCredentials = new NoSecClientCredential();
        clientCredentials.setEndpoint(endpoint);
        return clientCredentials;
    }

    public void setResources(String[] resources) {
        this.resources = resources;
    }

    public void createNewClient(Security security, Security securityBs, boolean isRpc,
                                String endpoint) throws Exception {
        this.createNewClient(security, securityBs, isRpc, endpoint, null, false);
    }

    public void createNewClient(Security security, Security securityBs, boolean isRpc,
                                String endpoint, Integer clientDtlsCidLength) throws Exception {
        this.createNewClient(security, securityBs, isRpc, endpoint, clientDtlsCidLength, false);
    }

    public void createNewClient(Security security, Security securityBs, boolean isRpc,
                                String endpoint, Integer clientDtlsCidLength, boolean queueMode) throws Exception {
        this.clientDestroy();
        lwM2MTestClient = new LwM2MTestClient(this.executor, endpoint);

        try (ServerSocket socket = new ServerSocket(0)) {
            int clientPort = socket.getLocalPort();
            lwM2MTestClient.init(security, securityBs, clientPort, isRpc,
                    this.defaultLwM2mUplinkMsgHandlerTest, this.clientContextTest, isWriteAttribute,
                    clientDtlsCidLength, queueMode, supportFormatOnly_SenMLJSON_SenMLCBOR);
        }
    }

    private void clientDestroy() {
        try {
            if (lwM2MTestClient != null) {
                lwM2MTestClient.destroy();
                awaitClientDestroy(lwM2MTestClient.getLeshanClient());
            }
        } catch (Exception e) {
            log.error("Failed client Destroy", e);
        }
    }

    protected Lwm2mDeviceProfileTransportConfiguration getTransportConfiguration(String observeAttr, List<LwM2MBootstrapServerCredential> bootstrapServerCredentials) {
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = new Lwm2mDeviceProfileTransportConfiguration();
        TelemetryMappingConfiguration observeAttrConfiguration = JacksonUtil.fromString(observeAttr, TelemetryMappingConfiguration.class);
        OtherConfiguration clientLwM2mSettings = JacksonUtil.fromString(CLIENT_LWM2M_SETTINGS, OtherConfiguration.class);
        transportConfiguration.setBootstrapServerUpdateEnable(true);
        transportConfiguration.setObserveAttr(observeAttrConfiguration);
        transportConfiguration.setClientLwM2mSettings(clientLwM2mSettings);
        transportConfiguration.setBootstrap(bootstrapServerCredentials);
        return transportConfiguration;
    }

    protected List<LwM2MBootstrapServerCredential> getBootstrapServerCredentialsNoSec(LwM2MProfileBootstrapConfigType bootstrapConfigType) {
        List<LwM2MBootstrapServerCredential> bootstrap = new ArrayList<>();
        switch (bootstrapConfigType) {
            case BOTH:
                bootstrap.add(getBootstrapServerCredentialNoSec(false));
                bootstrap.add(getBootstrapServerCredentialNoSec(true));
                break;
            case BOOTSTRAP_ONLY:
                bootstrap.add(getBootstrapServerCredentialNoSec(true));
                break;
            case LWM2M_ONLY:
                bootstrap.add(getBootstrapServerCredentialNoSec(false));
                break;
            case NONE:
        }
        return bootstrap;
    }

    private AbstractLwM2MBootstrapServerCredential getBootstrapServerCredentialNoSec(boolean isBootstrap) {
        AbstractLwM2MBootstrapServerCredential bootstrapServerCredential = new NoSecLwM2MBootstrapServerCredential();
        bootstrapServerCredential.setServerPublicKey("");
        bootstrapServerCredential.setShortServerId(isBootstrap ? shortServerIdBs0 : shortServerId);
        bootstrapServerCredential.setBootstrapServerIs(isBootstrap);
        bootstrapServerCredential.setHost(isBootstrap ? hostBs : host);
        bootstrapServerCredential.setPort(isBootstrap ? portBs : port);
        return bootstrapServerCredential;
    }

    protected LwM2MDeviceCredentials getDeviceCredentialsNoSec(LwM2MClientCredential clientCredentials) {
        LwM2MDeviceCredentials credentials = new LwM2MDeviceCredentials();
        credentials.setClient(clientCredentials);
        LwM2MBootstrapClientCredentials bootstrapCredentials = new LwM2MBootstrapClientCredentials();
        NoSecBootstrapClientCredential serverCredentials = new NoSecBootstrapClientCredential();
        bootstrapCredentials.setBootstrapServer(serverCredentials);
        bootstrapCredentials.setLwm2mServer(serverCredentials);
        credentials.setBootstrap(bootstrapCredentials);
        return credentials;
    }

    private static void awaitServersDestroy() {
        await("One of servers ports number is not free")
                .atMost(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> isServerPortsAvailable() == null);
    }

    private static String isServerPortsAvailable() {
        for (int port : SERVERS_PORT_NUMBERS) {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                serverSocket.close();
                Assert.assertEquals(true, serverSocket.isClosed());
            } catch (IOException e) {
                log.warn(String.format("Port %n still in use", port));
                return (String.format("Port %n still in use", port));
            }
        }
        return null;
    }

    private static void awaitClientDestroy(LeshanClient leshanClient) {
        await("Destroy LeshanClient: delete All is registered Servers.")
                .atMost(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> leshanClient.getRegisteredServers().size() == 0);
    }

    protected  void awaitObserveReadAll(int cntObserve, String deviceIdStr) throws Exception {
        await("ObserveReadAll after start client/test: countObserve " + cntObserve)
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> cntObserve == getCntObserveAll(deviceIdStr));
    }

    protected Integer getCntObserveAll(String deviceIdStr) throws Exception {
        String actualResultBefore = sendObserve("ObserveReadAll", null, deviceIdStr);
        ObjectNode rpcActualResultBefore = JacksonUtil.fromString(actualResultBefore, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResultBefore.get("result").asText());
        JsonElement element = JsonUtils.parse(rpcActualResultBefore.get("value").asText());
        return element.isJsonArray() ? ((JsonArray)element).size() : null;
    }

    protected void sendCancelObserveAllWithAwait(String deviceIdStr) throws Exception {
        String actualResultCancelAll = sendObserve("ObserveCancelAll", null, deviceIdStr);
        ObjectNode rpcActualResultCancelAll = JacksonUtil.fromString(actualResultCancelAll, ObjectNode.class);
        assertEquals(ResponseCode.CONTENT.getName(), rpcActualResultCancelAll.get("result").asText());
        awaitObserveReadAll(0, deviceId);
    }

    protected String sendObserve(String method, String params, String deviceIdStr) throws Exception {
        String sendRpcRequest;
        if (params == null) {
            sendRpcRequest = "{\"method\": \"" + method + "\"}";
        }
        else {
            sendRpcRequest = "{\"method\": \"" + method + "\", \"params\": {\"id\": \"" + params + "\"}}";
        }
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceIdStr, sendRpcRequest, String.class, status().isOk());
    }

}
