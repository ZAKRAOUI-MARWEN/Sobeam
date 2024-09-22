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
package org.sobeam.server.service.edge.rpc.processor.device;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.springframework.stereotype.Component;
import org.sobeam.common.util.JacksonUtil;
import org.sobeam.server.common.data.Device;
import org.sobeam.server.common.data.id.CustomerId;
import org.sobeam.server.common.data.id.DeviceId;
import org.sobeam.server.common.data.id.DeviceProfileId;
import org.sobeam.server.common.data.id.OtaPackageId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.security.DeviceCredentials;
import org.sobeam.server.common.data.security.DeviceCredentialsType;
import org.sobeam.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.sobeam.server.gen.edge.v1.DeviceUpdateMsg;
import org.sobeam.server.queue.util.TbCoreComponent;

import java.util.UUID;

@Component
@TbCoreComponent
public class DeviceEdgeProcessorV1 extends DeviceEdgeProcessor {

    @Override
    protected Device constructDeviceFromUpdateMsg(TenantId tenantId, DeviceId deviceId, DeviceUpdateMsg deviceUpdateMsg) {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setCreatedTime(Uuids.unixTimestamp(deviceId.getId()));
        device.setName(deviceUpdateMsg.getName());
        device.setType(deviceUpdateMsg.getType());
        device.setLabel(deviceUpdateMsg.hasLabel() ? deviceUpdateMsg.getLabel() : null);
        device.setAdditionalInfo(deviceUpdateMsg.hasAdditionalInfo()
                ? JacksonUtil.toJsonNode(deviceUpdateMsg.getAdditionalInfo()) : null);

        UUID deviceProfileUUID = safeGetUUID(deviceUpdateMsg.getDeviceProfileIdMSB(), deviceUpdateMsg.getDeviceProfileIdLSB());
        device.setDeviceProfileId(deviceProfileUUID != null ? new DeviceProfileId(deviceProfileUUID) : null);

        device.setDeviceDataBytes(deviceUpdateMsg.getDeviceDataBytes().toByteArray());

        UUID firmwareUUID = safeGetUUID(deviceUpdateMsg.getFirmwareIdMSB(), deviceUpdateMsg.getFirmwareIdLSB());
        device.setFirmwareId(firmwareUUID != null ? new OtaPackageId(firmwareUUID) : null);
        UUID softwareUUID = safeGetUUID(deviceUpdateMsg.getSoftwareIdMSB(), deviceUpdateMsg.getSoftwareIdLSB());
        device.setSoftwareId(softwareUUID != null ? new OtaPackageId(softwareUUID) : null);

        return device;
    }

    @Override
    protected DeviceCredentials constructDeviceCredentialsFromUpdateMsg(TenantId tenantId, DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg) {
        DeviceCredentials deviceCredentials = new DeviceCredentials();
        deviceCredentials.setDeviceId(new DeviceId(new UUID(deviceCredentialsUpdateMsg.getDeviceIdMSB(), deviceCredentialsUpdateMsg.getDeviceIdLSB())));
        deviceCredentials.setCredentialsType(DeviceCredentialsType.valueOf(deviceCredentialsUpdateMsg.getCredentialsType()));
        deviceCredentials.setCredentialsId(deviceCredentialsUpdateMsg.getCredentialsId());
        deviceCredentials.setCredentialsValue(deviceCredentialsUpdateMsg.hasCredentialsValue()
                ? deviceCredentialsUpdateMsg.getCredentialsValue() : null);
        return deviceCredentials;
    }

    @Override
    protected void setCustomerId(TenantId tenantId, CustomerId customerId, Device device, DeviceUpdateMsg deviceUpdateMsg) {
        CustomerId customerUUID = safeGetCustomerId(deviceUpdateMsg.getCustomerIdMSB(), deviceUpdateMsg.getCustomerIdLSB());
        device.setCustomerId(customerUUID != null ? customerUUID : customerId);
    }
}
