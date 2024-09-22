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

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.sobeam.common.util.JacksonUtil;
import org.sobeam.server.common.data.Device;
import org.sobeam.server.common.data.id.CustomerId;
import org.sobeam.server.common.data.id.DeviceId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.security.DeviceCredentials;
import org.sobeam.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.sobeam.server.gen.edge.v1.DeviceUpdateMsg;
import org.sobeam.server.queue.util.TbCoreComponent;

@Primary
@Component
@TbCoreComponent
public class DeviceEdgeProcessorV2 extends DeviceEdgeProcessor {

    @Override
    protected Device constructDeviceFromUpdateMsg(TenantId tenantId, DeviceId deviceId, DeviceUpdateMsg deviceUpdateMsg) {
        return JacksonUtil.fromString(deviceUpdateMsg.getEntity(), Device.class, true);
    }

    @Override
    protected DeviceCredentials constructDeviceCredentialsFromUpdateMsg(TenantId tenantId, DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg) {
        return JacksonUtil.fromString(deviceCredentialsUpdateMsg.getEntity(), DeviceCredentials.class, true);
    }

    @Override
    protected void setCustomerId(TenantId tenantId, CustomerId customerId, Device device, DeviceUpdateMsg deviceUpdateMsg) {
        CustomerId customerUUID = device.getCustomerId() != null ? device.getCustomerId() : customerId;
        device.setCustomerId(customerUUID);
    }
}
