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
package org.sobeam.server.transport.mqtt.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.sobeam.server.common.data.id.CustomerId;
import org.sobeam.server.common.data.id.DeviceId;
import org.sobeam.server.common.data.id.DeviceProfileId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.transport.TransportService;
import org.sobeam.server.common.transport.auth.TransportDeviceInfo;
import org.sobeam.server.transport.mqtt.MqttTransportContext;

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
public class GatewaySessionHandlerTest {

    @Mock
    private TransportService transportService;

    @Mock
    private DeviceSessionCtx deviceSessionCtx;

    @Mock
    private MqttTransportContext transportContext;

    private GatewaySessionHandler handler;

    @BeforeEach
    public void setup() {
        lenient().when(deviceSessionCtx.getSessionId()).thenReturn(UUID.randomUUID());
        lenient().doNothing().when(transportService).recordActivity(any());
        lenient().when(transportContext.getTransportService()).thenReturn(transportService);
        lenient().when(deviceSessionCtx.getContext()).thenReturn(transportContext);

        var deviceInfo = new TransportDeviceInfo();
        deviceInfo.setDeviceId(new DeviceId(UUID.randomUUID()));
        lenient().when(deviceSessionCtx.getDeviceInfo()).thenReturn(deviceInfo);
        handler = new GatewaySessionHandler(deviceSessionCtx, UUID.randomUUID(), true);
        lenient().when(handler.getNodeId()).thenReturn("nodeId");
    }

    @Test
    public void shouldRecordActivityWhenOnGatewayPing() throws Exception {
        // Given
        ConcurrentHashMap<String, GatewayDeviceSessionContext> devices = new ConcurrentHashMap<>();
        TransportDeviceInfo deviceInfo = new TransportDeviceInfo();
        deviceInfo.setDeviceId(new DeviceId(UUID.randomUUID()));
        deviceInfo.setTenantId(new TenantId(UUID.randomUUID()));
        deviceInfo.setCustomerId(new CustomerId(UUID.randomUUID()));
        deviceInfo.setDeviceName("device1");
        deviceInfo.setDeviceType("default");
        deviceInfo.setDeviceProfileId(new DeviceProfileId(UUID.randomUUID()));
        deviceInfo.setAdditionalInfo("{\"gateway\": true, \"overwriteDeviceActivity\": true}");
        lenient().when(deviceSessionCtx.getDeviceInfo()).thenReturn(deviceInfo);
        GatewayDeviceSessionContext gatewayDeviceSessionContext = new GatewayDeviceSessionContext(handler, deviceInfo, null, null, transportService);
        devices.put("device1", gatewayDeviceSessionContext);
        lenient().when(handler.getNodeId()).thenReturn("nodeId");
        Field devicesField = AbstractGatewaySessionHandler.class.getDeclaredField("devices");
        devicesField.setAccessible(true);
        devicesField.set(handler, devices);

        // When
        handler.onGatewayPing();

        // Then
        verify(transportService).recordActivity(gatewayDeviceSessionContext.getSessionInfo());
    }

    @Test
    public void shouldNotRecordActivityWhenNoDevicesOnGatewayPing() throws Exception {
        // Given
        ConcurrentHashMap<String, GatewayDeviceSessionContext> devices = new ConcurrentHashMap<>();
        Field devicesField = AbstractGatewaySessionHandler.class.getDeclaredField("devices");
        devicesField.setAccessible(true);
        devicesField.set(handler, devices);

        // When
        handler.onGatewayPing();

        // Then
        verify(transportService, never()).recordActivity(any());
    }

    @Test
    public void givenGatewaySessionHandler_WhenCreateWeakMap_thenConcurrentReferenceHashMapClass() {
        GatewaySessionHandler gsh = mock(GatewaySessionHandler.class);
        willCallRealMethod().given(gsh).createWeakMap();

        assertThat(gsh.createWeakMap()).isInstanceOf(ConcurrentReferenceHashMap.class);
    }

}
