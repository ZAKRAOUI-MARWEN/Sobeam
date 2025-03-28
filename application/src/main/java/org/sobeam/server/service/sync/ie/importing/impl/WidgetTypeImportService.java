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
package org.sobeam.server.service.sync.ie.importing.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.sobeam.server.common.data.EntityType;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.id.WidgetTypeId;
import org.sobeam.server.common.data.sync.ie.WidgetTypeExportData;
import org.sobeam.server.common.data.widget.WidgetTypeDetails;
import org.sobeam.server.dao.widget.WidgetTypeService;
import org.sobeam.server.queue.util.TbCoreComponent;
import org.sobeam.server.service.sync.vc.data.EntitiesImportCtx;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class WidgetTypeImportService extends BaseEntityImportService<WidgetTypeId, WidgetTypeDetails, WidgetTypeExportData> {

    private final WidgetTypeService widgetTypeService;

    @Override
    protected void setOwner(TenantId tenantId, WidgetTypeDetails widgetsBundle, IdProvider idProvider) {
        widgetsBundle.setTenantId(tenantId);
    }

    @Override
    protected WidgetTypeDetails prepare(EntitiesImportCtx ctx, WidgetTypeDetails widgetsBundle, WidgetTypeDetails old, WidgetTypeExportData exportData, IdProvider idProvider) {
        return widgetsBundle;
    }

    @Override
    protected WidgetTypeDetails saveOrUpdate(EntitiesImportCtx ctx, WidgetTypeDetails widgetsBundle, WidgetTypeExportData exportData, IdProvider idProvider) {
        return widgetTypeService.saveWidgetType(widgetsBundle);
    }

    @Override
    protected boolean compare(EntitiesImportCtx ctx, WidgetTypeExportData exportData, WidgetTypeDetails prepared, WidgetTypeDetails existing) {
        return true;
    }

    @Override
    protected WidgetTypeDetails deepCopy(WidgetTypeDetails widgetsBundle) {
        return new WidgetTypeDetails(widgetsBundle);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.WIDGET_TYPE;
    }

}
