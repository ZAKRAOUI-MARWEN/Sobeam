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

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.sobeam.server.common.data.EntityType;
import org.sobeam.server.common.data.id.WidgetTypeId;
import org.sobeam.server.common.data.sync.ie.WidgetTypeExportData;
import org.sobeam.server.common.data.widget.WidgetTypeDetails;
import org.sobeam.server.queue.util.TbCoreComponent;
import org.sobeam.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.Set;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class WidgetTypeExportService extends BaseEntityExportService<WidgetTypeId, WidgetTypeDetails, WidgetTypeExportData> {

    @Override
    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, WidgetTypeDetails widgetTypeDetails, WidgetTypeExportData exportData) {
        if (widgetTypeDetails.getTenantId() == null || widgetTypeDetails.getTenantId().isNullUid()) {
            throw new IllegalArgumentException("Export of system Widget Type is not allowed");
        }
        imageService.inlineImages(widgetTypeDetails);
    }

    @Override
    protected WidgetTypeExportData newExportData() {
        return new WidgetTypeExportData();
    }

    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.WIDGET_TYPE);
    }

}