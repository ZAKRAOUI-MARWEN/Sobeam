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
package org.sobeam.server.service.edge.rpc.constructor.device;

import org.springframework.stereotype.Component;
import org.sobeam.common.util.JacksonUtil;
import org.sobeam.server.common.data.Device;
import org.sobeam.server.common.data.DeviceProfile;
import org.sobeam.server.common.data.security.DeviceCredentials;
import org.sobeam.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.sobeam.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.sobeam.server.gen.edge.v1.DeviceUpdateMsg;
import org.sobeam.server.gen.edge.v1.UpdateMsgType;
import org.sobeam.server.queue.util.TbCoreComponent;

@Component
@TbCoreComponent
public class DeviceMsgConstructorV2 extends BaseDeviceMsgConstructor {

    @Override
    public DeviceUpdateMsg constructDeviceUpdatedMsg(UpdateMsgType msgType, Device device) {
        return DeviceUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(device))
                .setIdMSB(device.getId().getId().getMostSignificantBits())
                .setIdLSB(device.getId().getId().getLeastSignificantBits()).build();
    }

    @Override
    public DeviceCredentialsUpdateMsg constructDeviceCredentialsUpdatedMsg(DeviceCredentials deviceCredentials) {
        return DeviceCredentialsUpdateMsg.newBuilder().setEntity(JacksonUtil.toString(deviceCredentials)).build();
    }

    @Override
    public DeviceProfileUpdateMsg constructDeviceProfileUpdatedMsg(UpdateMsgType msgType, DeviceProfile deviceProfile) {
        return DeviceProfileUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(deviceProfile))
                .setIdMSB(deviceProfile.getId().getId().getMostSignificantBits())
                .setIdLSB(deviceProfile.getId().getId().getLeastSignificantBits()).build();
    }

}
