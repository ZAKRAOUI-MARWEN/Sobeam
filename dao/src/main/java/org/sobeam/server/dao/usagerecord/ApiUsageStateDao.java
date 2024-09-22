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
package org.sobeam.server.dao.usagerecord;

import org.sobeam.server.common.data.ApiUsageState;
import org.sobeam.server.common.data.id.EntityId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.dao.Dao;

import java.util.UUID;

public interface ApiUsageStateDao extends Dao<ApiUsageState> {

    /**
     * Save or update usage record object
     *
     * @param apiUsageState the usage record
     * @return saved usage record entity
     */
    ApiUsageState save(TenantId tenantId, ApiUsageState apiUsageState);

    /**
     * Find usage record by tenantId.
     *
     * @param tenantId the tenantId
     * @return the corresponding usage record
     */
    ApiUsageState findTenantApiUsageState(UUID tenantId);

    ApiUsageState findApiUsageStateByEntityId(EntityId entityId);

    /**
     * Delete usage record by tenantId.
     *
     * @param tenantId the tenantId
     */
    void deleteApiUsageStateByTenantId(TenantId tenantId);

    void deleteApiUsageStateByEntityId(EntityId entityId);
}
