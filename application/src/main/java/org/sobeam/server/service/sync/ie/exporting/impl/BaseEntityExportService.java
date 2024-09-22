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
package org.sobeam.server.service.sync.ie.exporting.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.sobeam.common.util.JacksonUtil;
import org.sobeam.server.common.data.EntityType;
import org.sobeam.server.common.data.ExportableEntity;
import org.sobeam.server.common.data.exception.SobeamException;
import org.sobeam.server.common.data.id.EntityId;
import org.sobeam.server.common.data.sync.ie.EntityExportData;
import org.sobeam.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.Collection;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

public abstract class BaseEntityExportService<I extends EntityId, E extends ExportableEntity<I>, D extends EntityExportData<E>> extends DefaultEntityExportService<I, E, D> {

    @Override
    protected void setAdditionalExportData(EntitiesExportCtx<?> ctx, E entity, D exportData) throws SobeamException {
        setRelatedEntities(ctx, entity, (D) exportData);
        super.setAdditionalExportData(ctx, entity, exportData);
    }

    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, E mainEntity, D exportData) {
    }

    protected D newExportData() {
        return (D) new EntityExportData<E>();
    }

    public abstract Set<EntityType> getSupportedEntityTypes();

    protected void replaceUuidsRecursively(EntitiesExportCtx<?> ctx, JsonNode node, Set<String> skippedRootFields, Pattern includedFieldsPattern) {
        JacksonUtil.replaceUuidsRecursively(node, skippedRootFields, includedFieldsPattern, uuid -> getExternalIdOrElseInternalByUuid(ctx, uuid), true);
    }

    protected Stream<UUID> toExternalIds(Collection<UUID> internalIds, Function<UUID, EntityId> entityIdCreator,
                                         EntitiesExportCtx<?> ctx) {
        return internalIds.stream().map(entityIdCreator)
                .map(entityId -> getExternalIdOrElseInternal(ctx, entityId))
                .map(EntityId::getId);
    }

}
