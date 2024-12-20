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
package org.sobeam.server.service.edge.rpc.constructor.tenant;

import org.springframework.stereotype.Component;
import org.sobeam.common.util.JacksonUtil;
import org.sobeam.server.common.data.Tenant;
import org.sobeam.server.common.data.TenantProfile;
import org.sobeam.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.sobeam.server.gen.edge.v1.EdgeVersion;
import org.sobeam.server.gen.edge.v1.TenantProfileUpdateMsg;
import org.sobeam.server.gen.edge.v1.TenantUpdateMsg;
import org.sobeam.server.gen.edge.v1.UpdateMsgType;
import org.sobeam.server.queue.util.TbCoreComponent;

@Component
@TbCoreComponent
public class TenantMsgConstructorV2 implements TenantMsgConstructor {

    @Override
    public TenantUpdateMsg constructTenantUpdateMsg(UpdateMsgType msgType, Tenant tenant) {
        return TenantUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(tenant)).build();
    }

    @Override
    public TenantProfileUpdateMsg constructTenantProfileUpdateMsg(UpdateMsgType msgType, TenantProfile tenantProfile, EdgeVersion edgeVersion) {
        tenantProfile = JacksonUtil.clone(tenantProfile);
        // clear all config
        var configuration = tenantProfile.getDefaultProfileConfiguration();
        configuration.setRpcTtlDays(0);
        configuration.setMaxJSExecutions(0);
        configuration.setMaxREExecutions(0);
        configuration.setMaxDPStorageDays(0);
        configuration.setMaxTbelExecutions(0);
        configuration.setQueueStatsTtlDays(0);
        configuration.setMaxTransportMessages(0);
        configuration.setDefaultStorageTtlDays(0);
        configuration.setMaxTransportDataPoints(0);
        configuration.setRuleEngineExceptionsTtlDays(0);
        configuration.setMaxRuleNodeExecutionsPerMessage(0);
        tenantProfile.getProfileData().setConfiguration(configuration);

        return TenantProfileUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(tenantProfile)).build();
    }

}
