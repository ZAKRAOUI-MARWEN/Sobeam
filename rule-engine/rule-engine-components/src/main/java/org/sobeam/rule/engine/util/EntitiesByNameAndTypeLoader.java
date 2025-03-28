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
package org.sobeam.rule.engine.util;

import org.sobeam.rule.engine.api.TbContext;
import org.sobeam.server.common.data.BaseData;
import org.sobeam.server.common.data.EntityType;
import org.sobeam.server.common.data.id.EntityId;

import java.util.List;

public class EntitiesByNameAndTypeLoader {

    private static final List<EntityType> AVAILABLE_ENTITY_TYPES = List.of(
            EntityType.DEVICE,
            EntityType.ASSET,
            EntityType.ENTITY_VIEW,
            EntityType.EDGE,
            EntityType.USER);

    public static EntityId findEntityId(TbContext ctx, EntityType entityType, String entityName) {
        BaseData<? extends EntityId> targetEntity;
        switch (entityType) {
            case DEVICE:
                targetEntity = ctx.getDeviceService().findDeviceByTenantIdAndName(ctx.getTenantId(), entityName);
                break;
            case ASSET:
                targetEntity = ctx.getAssetService().findAssetByTenantIdAndName(ctx.getTenantId(), entityName);
                break;
            case ENTITY_VIEW:
                targetEntity = ctx.getEntityViewService().findEntityViewByTenantIdAndName(ctx.getTenantId(), entityName);
                break;
            case EDGE:
                targetEntity = ctx.getEdgeService().findEdgeByTenantIdAndName(ctx.getTenantId(), entityName);
                break;
            case USER:
                targetEntity = ctx.getUserService().findUserByTenantIdAndEmail(ctx.getTenantId(), entityName);
                break;
            default:
                throw new IllegalStateException("Unexpected entity type " + entityType.name());
        }
        if (targetEntity == null) {
            throw new IllegalStateException("Failed to found " + entityType.name() + "  entity by name: '" + entityName + "'!");
        }
        return targetEntity.getId();
    }

    public static void checkEntityType(EntityType entityType) {
        if (!AVAILABLE_ENTITY_TYPES.contains(entityType)) {
            throw new IllegalStateException("Unexpected entity type " + entityType.name());
        }
    }

}
