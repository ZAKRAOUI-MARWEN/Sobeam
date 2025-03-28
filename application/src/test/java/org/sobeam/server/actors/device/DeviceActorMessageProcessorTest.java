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
package org.sobeam.server.actors.device;

import org.junit.Before;
import org.junit.Test;
import org.sobeam.common.util.LinkedHashMapRemoveEldest;
import org.sobeam.server.actors.ActorSystemContext;
import org.sobeam.server.common.data.id.DeviceId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.dao.device.DeviceService;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;

public class DeviceActorMessageProcessorTest {

    public static final long MAX_CONCURRENT_SESSIONS_PER_DEVICE = 10L;
    ActorSystemContext systemContext;
    DeviceService deviceService;
    TenantId tenantId = TenantId.SYS_TENANT_ID;
    DeviceId deviceId = DeviceId.fromString("78bf9b26-74ef-4af2-9cfb-ad6cf24ad2ec");

    DeviceActorMessageProcessor processor;

    @Before
    public void setUp() {
        systemContext = mock(ActorSystemContext.class);
        deviceService = mock(DeviceService.class);
        willReturn(MAX_CONCURRENT_SESSIONS_PER_DEVICE).given(systemContext).getMaxConcurrentSessionsPerDevice();
        willReturn(deviceService).given(systemContext).getDeviceService();
        processor = new DeviceActorMessageProcessor(systemContext, tenantId, deviceId);
    }

    @Test
    public void givenSystemContext_whenNewInstance_thenVerifySessionMapMaxSize() {
        assertThat(processor.sessions, instanceOf(LinkedHashMapRemoveEldest.class));
        assertThat(processor.sessions.getMaxEntries(), is(MAX_CONCURRENT_SESSIONS_PER_DEVICE));
        assertThat(processor.sessions.getRemovalConsumer(), notNullValue());
    }
}