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
package org.sobeam.server.transport.snmp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.sobeam.common.util.JacksonUtil;
import org.sobeam.server.common.data.Device;
import org.sobeam.server.common.data.device.data.DeviceData;
import org.sobeam.server.common.data.device.data.DeviceTransportConfiguration;
import org.sobeam.server.common.data.id.DeviceId;
import org.sobeam.server.common.data.id.DeviceProfileId;
import org.sobeam.server.common.data.security.DeviceCredentials;
import org.sobeam.server.common.transport.TransportService;
import org.sobeam.server.common.util.ProtoUtils;
import org.sobeam.server.gen.transport.TransportProtos;
import org.sobeam.server.queue.util.TbSnmpTransportComponent;

import java.util.UUID;

@TbSnmpTransportComponent
@Service
@RequiredArgsConstructor
public class ProtoTransportEntityService {
    private final TransportService transportService;

    public Device getDeviceById(DeviceId id) {
        TransportProtos.GetDeviceResponseMsg deviceProto = transportService.getDevice(TransportProtos.GetDeviceRequestMsg.newBuilder()
                .setDeviceIdMSB(id.getId().getMostSignificantBits())
                .setDeviceIdLSB(id.getId().getLeastSignificantBits())
                .build());

        if (deviceProto == null) {
            return null;
        }

        DeviceProfileId deviceProfileId = new DeviceProfileId(new UUID(
                deviceProto.getDeviceProfileIdMSB(), deviceProto.getDeviceProfileIdLSB())
        );

        Device device = new Device();
        device.setId(id);
        device.setDeviceProfileId(deviceProfileId);

        DeviceTransportConfiguration deviceTransportConfiguration = JacksonUtil.fromBytes(
                deviceProto.getDeviceTransportConfiguration().toByteArray(), DeviceTransportConfiguration.class);

        DeviceData deviceData = new DeviceData();
        deviceData.setTransportConfiguration(deviceTransportConfiguration);
        device.setDeviceData(deviceData);

        return device;
    }

    public DeviceCredentials getDeviceCredentialsByDeviceId(DeviceId deviceId) {
        TransportProtos.GetDeviceCredentialsResponseMsg deviceCredentialsResponse = transportService.getDeviceCredentials(
                TransportProtos.GetDeviceCredentialsRequestMsg.newBuilder()
                        .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                        .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                        .build()
        );

        if (deviceCredentialsResponse.hasDeviceCredentialsData()) {
            return ProtoUtils.fromProto(deviceCredentialsResponse.getDeviceCredentialsData());
        } else {
            throw new IllegalArgumentException("Device credentials not found");
        }
    }

    public TransportProtos.GetSnmpDevicesResponseMsg getSnmpDevicesIds(int page, int pageSize) {
        TransportProtos.GetSnmpDevicesRequestMsg requestMsg = TransportProtos.GetSnmpDevicesRequestMsg.newBuilder()
                .setPage(page)
                .setPageSize(pageSize)
                .build();
        return transportService.getSnmpDevicesIds(requestMsg);
    }
}
