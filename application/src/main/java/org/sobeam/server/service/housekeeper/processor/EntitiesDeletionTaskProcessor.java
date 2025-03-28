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
package org.sobeam.server.service.housekeeper.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.sobeam.server.common.data.EntityType;
import org.sobeam.server.common.data.housekeeper.EntitiesDeletionHousekeeperTask;
import org.sobeam.server.common.data.housekeeper.HousekeeperTaskType;
import org.sobeam.server.common.data.id.EntityId;
import org.sobeam.server.common.data.id.EntityIdFactory;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.dao.entity.EntityDaoService;
import org.sobeam.server.dao.entity.EntityServiceRegistry;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class EntitiesDeletionTaskProcessor extends HousekeeperTaskProcessor<EntitiesDeletionHousekeeperTask> {

    private final EntityServiceRegistry entityServiceRegistry;

    @Override
    public void process(EntitiesDeletionHousekeeperTask task) throws Exception {
        EntityType entityType = task.getEntityType();
        TenantId tenantId = task.getTenantId();
        EntityDaoService entityService = entityServiceRegistry.getServiceByEntityType(entityType);

        for (UUID entityUuid : task.getEntities()) {
            EntityId entityId = EntityIdFactory.getByTypeAndUuid(entityType, entityUuid);
            entityService.deleteEntity(tenantId, entityId, true);
        }
        log.debug("[{}] Deleted {} {}s", tenantId, task.getEntities().size(), entityType.getNormalName().toLowerCase());
    }

    @Override
    public HousekeeperTaskType getTaskType() {
        return HousekeeperTaskType.DELETE_ENTITIES;
    }

}
