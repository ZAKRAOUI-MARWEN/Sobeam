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
package org.sobeam.server.dao.sql.device;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.sobeam.common.util.SoBeamThreadFactory;
import org.sobeam.server.common.data.Device;
import org.sobeam.server.common.data.DeviceProfile;
import org.sobeam.server.common.data.DeviceProfileType;
import org.sobeam.server.common.data.DeviceTransportType;
import org.sobeam.server.common.data.id.CustomerId;
import org.sobeam.server.common.data.id.DeviceId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.page.PageData;
import org.sobeam.server.common.data.page.PageLink;
import org.sobeam.server.dao.AbstractJpaDaoTest;
import org.sobeam.server.dao.device.DeviceDao;
import org.sobeam.server.dao.device.DeviceProfileDao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
public class JpaDeviceDaoTest extends AbstractJpaDaoTest {

    public static final int COUNT_DEVICES = 40;
    public static final String PREFIX_FOR_DEVICE_NAME = "SEARCH_TEXT_";
    List<UUID> deviceIds;
    UUID tenantId1;
    UUID tenantId2;
    UUID customerId1;
    UUID customerId2;
    @Autowired
    private DeviceDao deviceDao;

    @Autowired
    private DeviceProfileDao deviceProfileDao;

    private DeviceProfile savedDeviceProfile;

    ListeningExecutorService executor;

    @Before
    public void setUp() {
        createDeviceProfile();

        tenantId1 = Uuids.timeBased();
        customerId1 = Uuids.timeBased();
        tenantId2 = Uuids.timeBased();
        customerId2 = Uuids.timeBased();

        deviceIds = createDevices(tenantId1, tenantId2, customerId1, customerId2, COUNT_DEVICES);
    }

    private void createDeviceProfile() {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setName("TEST");
        deviceProfile.setTenantId(TenantId.SYS_TENANT_ID);
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTransportType(DeviceTransportType.DEFAULT);
        deviceProfile.setDescription("Test");
        savedDeviceProfile = deviceProfileDao.save(TenantId.SYS_TENANT_ID, deviceProfile);
    }

    @After
    public void tearDown() throws Exception {
        deviceDao.removeAllByIds(deviceIds);
        deviceProfileDao.removeById(TenantId.SYS_TENANT_ID, savedDeviceProfile.getUuidId());
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    public void testSaveDeviceName0x00_thenSomeDatabaseException() {
        Device device = getDevice(tenantId1, customerId1, "\u0000");
        assertThatThrownBy(() -> deviceIds.add(deviceDao.save(TenantId.fromUUID(tenantId1), device).getUuidId()));
    }

    @Test
    public void testFindDevicesByTenantId() {
        PageLink pageLink = new PageLink(15, 0, PREFIX_FOR_DEVICE_NAME);
        PageData<Device> devices1 = deviceDao.findDevicesByTenantId(tenantId1, pageLink);
        assertEquals(15, devices1.getData().size());

        pageLink = pageLink.nextPageLink();

        PageData<Device> devices2 = deviceDao.findDevicesByTenantId(tenantId1, pageLink);
        assertEquals(5, devices2.getData().size());
    }

    @Test
    public void testFindAsync() throws ExecutionException, InterruptedException, TimeoutException {
        UUID tenantId = Uuids.timeBased();
        UUID customerId = Uuids.timeBased();
        // send to method getDevice() number = 40, because make random name is bad and name "SEARCH_TEXT_40" don't used
        Device device = getDevice(tenantId, customerId, 40);
        deviceIds.add(deviceDao.save(TenantId.fromUUID(tenantId), device).getUuidId());

        UUID uuid = device.getId().getId();
        Device entity = deviceDao.findById(TenantId.fromUUID(tenantId), uuid);
        assertNotNull(entity);
        assertEquals(uuid, entity.getId().getId());

        executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10, SoBeamThreadFactory.forName(getClass().getSimpleName() + "-test-scope")));
        ListenableFuture<Device> future = executor.submit(() -> deviceDao.findById(TenantId.fromUUID(tenantId), uuid));
        Device asyncDevice = future.get(30, TimeUnit.SECONDS);
        assertNotNull("Async device expected to be not null", asyncDevice);
    }

    @Test
    public void testFindDevicesByTenantIdAndIdsAsync() throws ExecutionException, InterruptedException, TimeoutException {
        ListenableFuture<List<Device>> devicesFuture = deviceDao.findDevicesByTenantIdAndIdsAsync(tenantId1, deviceIds);
        List<Device> devices = devicesFuture.get(30, TimeUnit.SECONDS);
        assertEquals(20, devices.size());
    }

    @Test
    public void testFindDevicesByTenantIdAndCustomerIdAndIdsAsync() throws ExecutionException, InterruptedException, TimeoutException {
        ListenableFuture<List<Device>> devicesFuture = deviceDao.findDevicesByTenantIdCustomerIdAndIdsAsync(tenantId1, customerId1, deviceIds);
        List<Device> devices = devicesFuture.get(30, TimeUnit.SECONDS);
        assertEquals(20, devices.size());
    }

    private List<UUID> createDevices(UUID tenantId1, UUID tenantId2, UUID customerId1, UUID customerId2, int count) {
        List<UUID> savedDevicesUUID = new ArrayList<>();
        for (int i = 0; i < count / 2; i++) {
            savedDevicesUUID.add(deviceDao.save(TenantId.fromUUID(tenantId1), getDevice(tenantId1, customerId1, i)).getUuidId());
            savedDevicesUUID.add(deviceDao.save(TenantId.fromUUID(tenantId2), getDevice(tenantId2, customerId2, i + count / 2)).getUuidId());
        }
        return savedDevicesUUID;
    }

    private Device getDevice(UUID tenantId, UUID customerID, Object nameSuffix) {
        return getDevice(tenantId, customerID, Uuids.timeBased(), nameSuffix);
    }

    private Device getDevice(UUID tenantId, UUID customerID, UUID deviceId, Object nameSuffix) {
        Device device = new Device();
        device.setId(new DeviceId(deviceId));
        device.setTenantId(TenantId.fromUUID(tenantId));
        device.setCustomerId(new CustomerId(customerID));
        device.setName(PREFIX_FOR_DEVICE_NAME + nameSuffix);
        device.setDeviceProfileId(savedDeviceProfile.getId());
        return device;
    }
}
