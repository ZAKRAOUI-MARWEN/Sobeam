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

import com.fasterxml.jackson.databind.JsonNode;
import org.sobeam.server.common.data.Device;
import org.sobeam.server.common.data.DeviceProfile;
import org.sobeam.server.common.data.id.DeviceId;
import org.sobeam.server.common.data.id.DeviceProfileId;
import org.sobeam.server.common.data.security.DeviceCredentials;
import org.sobeam.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.sobeam.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.sobeam.server.gen.edge.v1.DeviceRpcCallMsg;
import org.sobeam.server.gen.edge.v1.DeviceUpdateMsg;
import org.sobeam.server.gen.edge.v1.UpdateMsgType;
import org.sobeam.server.service.edge.rpc.constructor.MsgConstructor;

import java.util.UUID;

public interface DeviceMsgConstructor extends MsgConstructor {

    DeviceUpdateMsg constructDeviceUpdatedMsg(UpdateMsgType msgType, Device device);

    DeviceUpdateMsg constructDeviceDeleteMsg(DeviceId deviceId);

    DeviceCredentialsUpdateMsg constructDeviceCredentialsUpdatedMsg(DeviceCredentials deviceCredentials);

    DeviceProfileUpdateMsg constructDeviceProfileUpdatedMsg(UpdateMsgType msgType, DeviceProfile deviceProfile);

    DeviceProfileUpdateMsg constructDeviceProfileDeleteMsg(DeviceProfileId deviceProfileId);

    DeviceRpcCallMsg constructDeviceRpcCallMsg(UUID deviceId, JsonNode body);

}
