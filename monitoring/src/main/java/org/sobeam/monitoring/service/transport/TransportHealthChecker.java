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
package org.sobeam.monitoring.service.transport;

import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.sobeam.common.util.JacksonUtil;
import org.sobeam.monitoring.client.TbClient;
import org.sobeam.monitoring.config.transport.DeviceConfig;
import org.sobeam.monitoring.config.transport.TransportInfo;
import org.sobeam.monitoring.config.transport.TransportMonitoringConfig;
import org.sobeam.monitoring.config.transport.TransportMonitoringTarget;
import org.sobeam.monitoring.config.transport.TransportType;
import org.sobeam.monitoring.service.BaseHealthChecker;
import org.sobeam.monitoring.util.ResourceUtils;
import org.sobeam.server.common.data.Device;
import org.sobeam.server.common.data.DeviceProfile;
import org.sobeam.server.common.data.DeviceProfileType;
import org.sobeam.server.common.data.DeviceTransportType;
import org.sobeam.server.common.data.TbResource;
import org.sobeam.server.common.data.device.credentials.lwm2m.LwM2MBootstrapClientCredentials;
import org.sobeam.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.sobeam.server.common.data.device.credentials.lwm2m.NoSecBootstrapClientCredential;
import org.sobeam.server.common.data.device.credentials.lwm2m.NoSecClientCredential;
import org.sobeam.server.common.data.device.data.DefaultDeviceConfiguration;
import org.sobeam.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.sobeam.server.common.data.device.data.DeviceData;
import org.sobeam.server.common.data.device.data.Lwm2mDeviceTransportConfiguration;
import org.sobeam.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.sobeam.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.sobeam.server.common.data.device.profile.DeviceProfileData;
import org.sobeam.server.common.data.page.PageLink;
import org.sobeam.server.common.data.security.DeviceCredentials;
import org.sobeam.server.common.data.security.DeviceCredentialsType;

@Slf4j
public abstract class TransportHealthChecker<C extends TransportMonitoringConfig> extends BaseHealthChecker<C, TransportMonitoringTarget> {

    public TransportHealthChecker(C config, TransportMonitoringTarget target) {
        super(config, target);
    }

    @Override
    protected void initialize(TbClient tbClient) {
        Device device = getOrCreateDevice(tbClient);
        DeviceCredentials credentials = tbClient.getDeviceCredentialsByDeviceId(device.getId())
                .orElseThrow(() -> new IllegalArgumentException("No credentials found for device " + device.getId()));

        DeviceConfig deviceConfig = new DeviceConfig();
        deviceConfig.setId(device.getId().toString());
        deviceConfig.setName(device.getName());
        deviceConfig.setCredentials(credentials);
        target.setDevice(deviceConfig);
    }

    @Override
    protected String createTestPayload(String testValue) {
        return JacksonUtil.newObjectNode().set(TEST_TELEMETRY_KEY, new TextNode(testValue)).toString();
    }

    @Override
    protected Object getInfo() {
        return new TransportInfo(getTransportType(), target.getBaseUrl(), target.getQueue());
    }

    @Override
    protected String getKey() {
        return getTransportType().name().toLowerCase() + (target.getQueue().equals("Main") ? "" : target.getQueue()) + "Transport";
    }

    protected abstract TransportType getTransportType();


    private Device getOrCreateDevice(TbClient tbClient) {
        TransportType transportType = config.getTransportType();
        String deviceName = String.format("%s (%s) - %s", transportType.getName(), target.getQueue(), target.getBaseUrl());
        Device device = tbClient.getTenantDevice(deviceName).orElse(null);
        if (device != null) {
            return device;
        }

        log.info("Creating new device '{}'", deviceName);
        device = new Device();
        device.setName(deviceName);

        DeviceCredentials credentials = new DeviceCredentials();
        credentials.setCredentialsId(RandomStringUtils.randomAlphabetic(20));
        DeviceData deviceData = new DeviceData();
        deviceData.setConfiguration(new DefaultDeviceConfiguration());

        DeviceProfile deviceProfile = getOrCreateDeviceProfile(tbClient);
        device.setType(deviceProfile.getName());
        device.setDeviceProfileId(deviceProfile.getId());

        if (transportType != TransportType.LWM2M) {
            deviceData.setTransportConfiguration(new DefaultDeviceTransportConfiguration());
            credentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        } else {
            deviceData.setTransportConfiguration(new Lwm2mDeviceTransportConfiguration());
            credentials.setCredentialsType(DeviceCredentialsType.LWM2M_CREDENTIALS);
            LwM2MDeviceCredentials lwm2mCreds = new LwM2MDeviceCredentials();
            NoSecClientCredential client = new NoSecClientCredential();
            client.setEndpoint(credentials.getCredentialsId());
            lwm2mCreds.setClient(client);
            LwM2MBootstrapClientCredentials bootstrap = new LwM2MBootstrapClientCredentials();
            bootstrap.setBootstrapServer(new NoSecBootstrapClientCredential());
            bootstrap.setLwm2mServer(new NoSecBootstrapClientCredential());
            lwm2mCreds.setBootstrap(bootstrap);
            credentials.setCredentialsValue(JacksonUtil.toString(lwm2mCreds));
        }

        return tbClient.saveDeviceWithCredentials(device, credentials).get();
    }

    private DeviceProfile getOrCreateDeviceProfile(TbClient tbClient) {
        TransportType transportType = config.getTransportType();
        String profileName = String.format("%s (%s)", transportType.getName(), target.getQueue());
        DeviceProfile deviceProfile = tbClient.getDeviceProfiles(new PageLink(1, 0, profileName)).getData()
                .stream().findFirst().orElse(null);
        if (deviceProfile != null) {
            return deviceProfile;
        }

        log.info("Creating new device profile '{}'", profileName);
        if (transportType != TransportType.LWM2M) {
            deviceProfile = new DeviceProfile();
            deviceProfile.setType(DeviceProfileType.DEFAULT);
            deviceProfile.setTransportType(DeviceTransportType.DEFAULT);
            DeviceProfileData profileData = new DeviceProfileData();
            profileData.setConfiguration(new DefaultDeviceProfileConfiguration());
            profileData.setTransportConfiguration(new DefaultDeviceProfileTransportConfiguration());
            deviceProfile.setProfileData(profileData);
        } else {
            tbClient.getResources(new PageLink(1, 0, "LwM2M Monitoring id=3 v1.0")).getData()
                    .stream().findFirst()
                    .orElseGet(() -> {
                        TbResource newResource = ResourceUtils.getResource("lwm2m/resource.json", TbResource.class);
                        log.info("Creating LwM2M resource");
                        return tbClient.saveResource(newResource);
                    });
            deviceProfile = ResourceUtils.getResource("lwm2m/device_profile.json", DeviceProfile.class);
        }

        deviceProfile.setName(profileName);
        deviceProfile.setDefaultQueueName(target.getQueue());
        return tbClient.saveDeviceProfile(deviceProfile);
    }

}