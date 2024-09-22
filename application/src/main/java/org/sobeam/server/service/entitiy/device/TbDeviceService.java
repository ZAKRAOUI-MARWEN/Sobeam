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
package org.sobeam.server.service.entitiy.device;

import com.google.common.util.concurrent.ListenableFuture;
import org.sobeam.server.common.data.Customer;
import org.sobeam.server.common.data.Device;
import org.sobeam.server.common.data.Tenant;
import org.sobeam.server.common.data.User;
import org.sobeam.server.common.data.edge.Edge;
import org.sobeam.server.common.data.exception.SobeamException;
import org.sobeam.server.common.data.id.CustomerId;
import org.sobeam.server.common.data.id.DeviceId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.security.DeviceCredentials;
import org.sobeam.server.dao.device.claim.ClaimResult;
import org.sobeam.server.dao.device.claim.ReclaimResult;

public interface TbDeviceService {

    Device save(Device device, String accessToken, User user) throws Exception;

    Device saveDeviceWithCredentials(Device device, DeviceCredentials deviceCredentials, User user) throws SobeamException;

    void delete(Device device, User user);

    Device assignDeviceToCustomer(TenantId tenantId, DeviceId deviceId, Customer customer, User user) throws SobeamException;

    Device unassignDeviceFromCustomer(Device device, Customer customer, User user) throws SobeamException;

    Device assignDeviceToPublicCustomer(TenantId tenantId, DeviceId deviceId, User user) throws SobeamException;

    DeviceCredentials getDeviceCredentialsByDeviceId(Device device, User user) throws SobeamException;

    DeviceCredentials updateDeviceCredentials(Device device, DeviceCredentials deviceCredentials, User user) throws SobeamException;

    ListenableFuture<ClaimResult> claimDevice(TenantId tenantId, Device device, CustomerId customerId, String secretKey, User user);

    ListenableFuture<ReclaimResult> reclaimDevice(TenantId tenantId, Device device, User user);

    Device assignDeviceToTenant(Device device, Tenant newTenant, User user);

    Device assignDeviceToEdge(TenantId tenantId, DeviceId deviceId, Edge edge, User user) throws SobeamException;

    Device unassignDeviceFromEdge(Device device, Edge edge, User user) throws SobeamException;
}
