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
package org.sobeam.server.dao.sql.usagerecord;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.sobeam.server.common.data.ApiUsageState;
import org.sobeam.server.common.data.EntityType;
import org.sobeam.server.common.data.id.EntityId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.dao.DaoUtil;
import org.sobeam.server.dao.model.sql.ApiUsageStateEntity;
import org.sobeam.server.dao.sql.JpaAbstractDao;
import org.sobeam.server.dao.usagerecord.ApiUsageStateDao;
import org.sobeam.server.dao.util.SqlDao;

import java.util.UUID;

/**
 * @author Andrii Shvaika
 */
@Component
@SqlDao
public class JpaApiUsageStateDao extends JpaAbstractDao<ApiUsageStateEntity, ApiUsageState> implements ApiUsageStateDao {

    private final ApiUsageStateRepository apiUsageStateRepository;

    public JpaApiUsageStateDao(ApiUsageStateRepository apiUsageStateRepository) {
        this.apiUsageStateRepository = apiUsageStateRepository;
    }

    @Override
    protected Class<ApiUsageStateEntity> getEntityClass() {
        return ApiUsageStateEntity.class;
    }

    @Override
    protected JpaRepository<ApiUsageStateEntity, UUID> getRepository() {
        return apiUsageStateRepository;
    }

    @Override
    public ApiUsageState findTenantApiUsageState(UUID tenantId) {
        return DaoUtil.getData(apiUsageStateRepository.findByTenantId(tenantId));
    }

    @Override
    public ApiUsageState findApiUsageStateByEntityId(EntityId entityId) {
        return DaoUtil.getData(apiUsageStateRepository.findByEntityIdAndEntityType(entityId.getId(), entityId.getEntityType().name()));
    }

    @Override
    public void deleteApiUsageStateByTenantId(TenantId tenantId) {
        apiUsageStateRepository.deleteApiUsageStateByTenantId(tenantId.getId());
    }

    @Override
    public void deleteApiUsageStateByEntityId(EntityId entityId) {
        apiUsageStateRepository.deleteByEntityIdAndEntityType(entityId.getId(), entityId.getEntityType().name());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.API_USAGE_STATE;
    }

}
