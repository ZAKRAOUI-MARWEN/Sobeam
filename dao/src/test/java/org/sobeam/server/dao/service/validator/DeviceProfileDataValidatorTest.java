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
package org.sobeam.server.dao.service.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.sobeam.common.util.JacksonUtil;
import org.sobeam.server.common.data.DeviceProfile;
import org.sobeam.server.common.data.DeviceProfileType;
import org.sobeam.server.common.data.DeviceTransportType;
import org.sobeam.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.sobeam.server.common.data.device.profile.DeviceProfileData;
import org.sobeam.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.sobeam.server.common.data.device.profile.lwm2m.OtherConfiguration;
import org.sobeam.server.common.data.device.profile.lwm2m.TelemetryMappingConfiguration;
import org.sobeam.server.common.data.device.profile.lwm2m.bootstrap.AbstractLwM2MBootstrapServerCredential;
import org.sobeam.server.common.data.device.profile.lwm2m.bootstrap.LwM2MBootstrapServerCredential;
import org.sobeam.server.common.data.device.profile.lwm2m.bootstrap.NoSecLwM2MBootstrapServerCredential;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.dao.dashboard.DashboardService;
import org.sobeam.server.dao.device.DeviceDao;
import org.sobeam.server.dao.device.DeviceProfileDao;
import org.sobeam.server.dao.device.DeviceProfileService;
import org.sobeam.server.dao.queue.QueueService;
import org.sobeam.server.dao.rule.RuleChainService;
import org.sobeam.server.dao.tenant.TenantService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = DeviceProfileDataValidator.class)
class DeviceProfileDataValidatorTest {

    private static final String OBSERVE_ATTRIBUTES_WITHOUT_PARAMS =
            "    {\n" +
                    "    \"keyName\": {},\n" +
                    "    \"observe\": [],\n" +
                    "    \"attribute\": [],\n" +
                    "    \"telemetry\": [],\n" +
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

    private static final String host = "localhost";
    private static final String hostBs = "localhost";

    private static final int port = 5685;
    private static final int portBs = 5687;

    @MockBean
    DeviceProfileDao deviceProfileDao;
    @MockBean
    DeviceProfileService deviceProfileService;
    @MockBean
    DeviceDao deviceDao;
    @MockBean
    TenantService tenantService;
    @MockBean
    QueueService queueService;
    @MockBean
    RuleChainService ruleChainService;
    @MockBean
    DashboardService dashboardService;
    @SpyBean
    DeviceProfileDataValidator validator;
    TenantId tenantId = TenantId.fromUUID(UUID.fromString("9ef79cdf-37a8-4119-b682-2e7ed4e018da"));

    @BeforeEach
    void setUp() {
        willReturn(true).given(tenantService).tenantExists(tenantId);
    }

    @Test
    void testValidateNameInvocation() {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setName("default");
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTransportType(DeviceTransportType.DEFAULT);
        DeviceProfileData data = new DeviceProfileData();
        data.setTransportConfiguration(new DefaultDeviceProfileTransportConfiguration());
        deviceProfile.setProfileData(data);
        deviceProfile.setTenantId(tenantId);

        validator.validateDataImpl(tenantId, deviceProfile);
        verify(validator).validateString("Device profile name", deviceProfile.getName());
    }
    @Test
    void testValidateDeviceProfile_Lwm2mBootstrap_ShortServerId_Ok() {
        Integer shortServerId = 123;
        Integer shortServerIdBs = 0;
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration =
                getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsNoSec(shortServerId, shortServerIdBs));
        DeviceProfile deviceProfile = getDeviceProfile(transportConfiguration);

        validator.validateDataImpl(tenantId, deviceProfile);
        verify(validator).validateString("Device profile name", deviceProfile.getName());
    }

    private DeviceProfile getDeviceProfile(Lwm2mDeviceProfileTransportConfiguration transportConfiguration) {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setName("default");
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTransportType(DeviceTransportType.LWM2M);
        DeviceProfileData data = new DeviceProfileData();
        data.setTransportConfiguration(transportConfiguration);
        deviceProfile.setProfileData(data);
        deviceProfile.setTenantId(tenantId);
        return deviceProfile;
    }

    private Lwm2mDeviceProfileTransportConfiguration getTransportConfiguration(String observeAttr, List<LwM2MBootstrapServerCredential> bootstrapServerCredentials) {
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = new Lwm2mDeviceProfileTransportConfiguration();
        TelemetryMappingConfiguration observeAttrConfiguration = JacksonUtil.fromString(observeAttr, TelemetryMappingConfiguration.class);
        OtherConfiguration clientLwM2mSettings = JacksonUtil.fromString(CLIENT_LWM2M_SETTINGS, OtherConfiguration.class);
        transportConfiguration.setBootstrapServerUpdateEnable(true);
        transportConfiguration.setObserveAttr(observeAttrConfiguration);
        transportConfiguration.setClientLwM2mSettings(clientLwM2mSettings);
        transportConfiguration.setBootstrap(bootstrapServerCredentials);
        return transportConfiguration;
    }

    private List<LwM2MBootstrapServerCredential> getBootstrapServerCredentialsNoSec(Integer shortServerId, Integer shortServerIdBs){
        List<LwM2MBootstrapServerCredential> bootstrap = new ArrayList<>();
        bootstrap.add(getBootstrapServerCredentialNoSec(false, shortServerId, shortServerIdBs));
        bootstrap.add(getBootstrapServerCredentialNoSec(true, shortServerId, shortServerIdBs));
        return bootstrap;
    }

    private AbstractLwM2MBootstrapServerCredential getBootstrapServerCredentialNoSec(boolean isBootstrap, Integer shortServerId, Integer shortServerIdBs) {
        AbstractLwM2MBootstrapServerCredential bootstrapServerCredential = new NoSecLwM2MBootstrapServerCredential();
        bootstrapServerCredential.setServerPublicKey("");
        bootstrapServerCredential.setShortServerId(isBootstrap ? shortServerIdBs : shortServerId);
        bootstrapServerCredential.setBootstrapServerIs(isBootstrap);
        bootstrapServerCredential.setHost(isBootstrap ? hostBs : host);
        bootstrapServerCredential.setPort(isBootstrap ? portBs : port);
        return bootstrapServerCredential;
    }

}
