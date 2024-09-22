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
package org.sobeam.server.service.queue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.sobeam.server.common.data.TenantProfile;
import org.sobeam.server.common.data.exception.TenantNotFoundException;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.dao.tenant.TbTenantProfileCache;
import org.sobeam.server.queue.discovery.TenantRoutingInfo;
import org.sobeam.server.queue.discovery.TenantRoutingInfoService;

@Slf4j
@Service
@ConditionalOnExpression("'${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core' || '${service.type:null}'=='tb-rule-engine'")
public class DefaultTenantRoutingInfoService implements TenantRoutingInfoService {

    private final TbTenantProfileCache tenantProfileCache;

    public DefaultTenantRoutingInfoService(TbTenantProfileCache tenantProfileCache) {
        this.tenantProfileCache = tenantProfileCache;
    }

    @Override
    public TenantRoutingInfo getRoutingInfo(TenantId tenantId) {
        TenantProfile tenantProfile = tenantProfileCache.get(tenantId);
        if (tenantProfile != null) {
            return new TenantRoutingInfo(tenantId, tenantProfile.getId(), tenantProfile.isIsolatedTbRuleEngine());
        } else {
            throw new TenantNotFoundException(tenantId);
        }
    }
}
