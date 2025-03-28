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
package org.sobeam.server.transport.lwm2m.ota.sql;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.sobeam.server.common.data.Device;
import org.sobeam.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.sobeam.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.sobeam.server.common.data.kv.KvEntry;
import org.sobeam.server.common.data.kv.TsKvEntry;
import org.sobeam.server.common.data.ota.OtaPackageUpdateStatus;
import org.sobeam.server.transport.lwm2m.ota.AbstractOtaLwM2MIntegrationTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.sobeam.rest.client.utils.RestJsonConverter.toTimeseries;
import static org.sobeam.server.common.data.ota.OtaPackageUpdateStatus.DOWNLOADED;
import static org.sobeam.server.common.data.ota.OtaPackageUpdateStatus.DOWNLOADING;
import static org.sobeam.server.common.data.ota.OtaPackageUpdateStatus.FAILED;
import static org.sobeam.server.common.data.ota.OtaPackageUpdateStatus.INITIATED;
import static org.sobeam.server.common.data.ota.OtaPackageUpdateStatus.QUEUED;
import static org.sobeam.server.common.data.ota.OtaPackageUpdateStatus.UPDATED;
import static org.sobeam.server.common.data.ota.OtaPackageUpdateStatus.UPDATING;
import static org.sobeam.server.common.data.ota.OtaPackageUpdateStatus.VERIFIED;
import static org.sobeam.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.NONE;

@Slf4j
public class OtaLwM2MIntegrationTest extends AbstractOtaLwM2MIntegrationTest {

    private List<OtaPackageUpdateStatus> expectedStatuses;

    @Test
    public void testFirmwareUpdateWithClientWithoutFirmwareOtaInfoFromProfile() throws Exception {
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITH_PARAMS, getBootstrapServerCredentialsNoSec(NONE));
        createDeviceProfile(transportConfiguration);
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsNoSec(createNoSecClientCredentials(this.CLIENT_ENDPOINT_WITHOUT_FW_INFO));
        final Device device = createDevice(deviceCredentials, this.CLIENT_ENDPOINT_WITHOUT_FW_INFO);
        createNewClient(SECURITY_NO_SEC,  null, false, this.CLIENT_ENDPOINT_WITHOUT_FW_INFO);
        awaitObserveReadAll(0, device.getId().getId().toString());

        device.setFirmwareId(createFirmware().getId());
        final Device savedDevice = doPost("/api/device", device, Device.class);

        Thread.sleep(1000);

        assertThat(savedDevice).as("saved device").isNotNull();
        assertThat(getDeviceFromAPI(device.getId().getId())).as("fetched device").isEqualTo(savedDevice);

        List<TsKvEntry> ts = toTimeseries(doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" +
                savedDevice.getId().getId() + "/values/timeseries?keys=fw_state", new TypeReference<>() {}));
        List<OtaPackageUpdateStatus> statuses = ts.stream().map(KvEntry::getValueAsString).map(OtaPackageUpdateStatus::valueOf).collect(Collectors.toList());
        List<OtaPackageUpdateStatus> expectedStatuses = Collections.singletonList(FAILED);

        Assert.assertEquals(expectedStatuses, statuses);
    }

    @Test
    public void testFirmwareUpdateByObject5() throws Exception {
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITH_PARAMS_OTA, getBootstrapServerCredentialsNoSec(NONE));
        createDeviceProfile(transportConfiguration);
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsNoSec(createNoSecClientCredentials(this.CLIENT_ENDPOINT_OTA5));
        final Device device = createDevice(deviceCredentials, this.CLIENT_ENDPOINT_OTA5);
        createNewClient(SECURITY_NO_SEC, null, false, this.CLIENT_ENDPOINT_OTA5);
        awaitObserveReadAll(9, device.getId().getId().toString());

        device.setFirmwareId(createFirmware().getId());
        final Device savedDevice = doPost("/api/device", device, Device.class);

        assertThat(savedDevice).as("saved device").isNotNull();
        assertThat(getDeviceFromAPI(device.getId().getId())).as("fetched device").isEqualTo(savedDevice);

        expectedStatuses = Arrays.asList(QUEUED, INITIATED, DOWNLOADING, DOWNLOADED, UPDATING, UPDATED);
        List<TsKvEntry> ts = await("await on timeseries")
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> toTimeseries(doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" +
                        savedDevice.getId().getId() + "/values/timeseries?orderBy=ASC&keys=fw_state&startTs=0&endTs=" +
                        System.currentTimeMillis(), new TypeReference<>() {
                })), this::predicateForStatuses);
        log.warn("Object5: Got the ts: {}", ts);
    }

    /**
     * This is the example how to use the AWAITILITY instead Thread.sleep()
     * Test will finish as fast as possible, but will await until TIMEOUT if a build machine is busy or slow
     * Check the detailed log output to learn how Awaitility polling the API and when exactly expected result appears
     * */
    @Test
    public void testSoftwareUpdateByObject9() throws Exception {
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITH_PARAMS_OTA, getBootstrapServerCredentialsNoSec(NONE));
        createDeviceProfile(transportConfiguration);
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsNoSec(createNoSecClientCredentials(this.CLIENT_ENDPOINT_OTA9));
        final Device device = createDevice(deviceCredentials, this.CLIENT_ENDPOINT_OTA9);
        createNewClient(SECURITY_NO_SEC, null, false, this.CLIENT_ENDPOINT_OTA9);
        awaitObserveReadAll(9, device.getId().getId().toString());

        device.setSoftwareId(createSoftware().getId());
        final Device savedDevice = doPost("/api/device", device, Device.class); //sync call

        assertThat(savedDevice).as("saved device").isNotNull();
        assertThat(getDeviceFromAPI(device.getId().getId())).as("fetched device").isEqualTo(savedDevice);

        expectedStatuses = List.of(
                QUEUED, INITIATED, DOWNLOADING, DOWNLOADING, DOWNLOADING, DOWNLOADED, VERIFIED, UPDATED);

        List<TsKvEntry> ts = await("await on timeseries")
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> getSwStateTelemetryFromAPI(device.getId().getId()), this::predicateForStatuses);
        log.warn("Object9: Got the ts: {}", ts);
    }

    private Device getDeviceFromAPI(UUID deviceId) throws Exception {
        final Device device = doGet("/api/device/" + deviceId, Device.class);
        log.trace("Fetched device by API for deviceId {}, device is {}", deviceId, device);
        return device;
    }

    private List<TsKvEntry> getSwStateTelemetryFromAPI(UUID deviceId) throws Exception {
        final List<TsKvEntry> tsKvEntries = toTimeseries(doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + deviceId + "/values/timeseries?orderBy=ASC&keys=sw_state&startTs=0&endTs=" + System.currentTimeMillis(), new TypeReference<>() {
        }));
        log.warn("Fetched telemetry by API for deviceId {}, list size {}, tsKvEntries {}", deviceId, tsKvEntries.size(), tsKvEntries);
        return tsKvEntries;
    }

    private boolean predicateForStatuses (List<TsKvEntry> ts) {
        List<OtaPackageUpdateStatus> statuses = ts.stream().sorted(Comparator
                .comparingLong(TsKvEntry::getTs)).map(KvEntry::getValueAsString)
                .map(OtaPackageUpdateStatus::valueOf)
                .collect(Collectors.toList());
        log.warn("{}", statuses);
        return statuses.containsAll(expectedStatuses);
    }
}
