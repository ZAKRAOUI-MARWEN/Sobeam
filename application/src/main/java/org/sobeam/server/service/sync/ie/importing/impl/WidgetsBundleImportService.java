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
import org.sobeam.common.util.JacksonUtil;
import org.sobeam.server.common.data.EntityType;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.id.WidgetsBundleId;
import org.sobeam.server.common.data.sync.ie.WidgetsBundleExportData;
import org.sobeam.server.common.data.util.CollectionsUtil;
import org.sobeam.server.common.data.widget.WidgetTypeDetails;
import org.sobeam.server.common.data.widget.WidgetsBundle;
import org.sobeam.server.dao.widget.WidgetTypeService;
import org.sobeam.server.dao.widget.WidgetsBundleService;
import org.sobeam.server.queue.util.TbCoreComponent;
import org.sobeam.server.service.sync.vc.data.EntitiesImportCtx;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class WidgetsBundleImportService extends BaseEntityImportService<WidgetsBundleId, WidgetsBundle, WidgetsBundleExportData> {

    private final WidgetsBundleService widgetsBundleService;
    private final WidgetTypeService widgetTypeService;

    @Override
    protected void setOwner(TenantId tenantId, WidgetsBundle widgetsBundle, IdProvider idProvider) {
        widgetsBundle.setTenantId(tenantId);
    }

    @Override
    protected WidgetsBundle prepare(EntitiesImportCtx ctx, WidgetsBundle widgetsBundle, WidgetsBundle old, WidgetsBundleExportData exportData, IdProvider idProvider) {
        return widgetsBundle;
    }

    @Override
    protected WidgetsBundle saveOrUpdate(EntitiesImportCtx ctx, WidgetsBundle widgetsBundle, WidgetsBundleExportData exportData, IdProvider idProvider) {
        if (CollectionsUtil.isNotEmpty(exportData.getWidgets())) {
            exportData.getWidgets().forEach(widgetTypeNode -> {
                String bundleAlias = widgetTypeNode.remove("bundleAlias").asText();
                String alias = widgetTypeNode.remove("alias").asText();
                String fqn = String.format("%s.%s", bundleAlias, alias);
                exportData.addFqn(fqn);
                WidgetTypeDetails widgetType = JacksonUtil.treeToValue(widgetTypeNode, WidgetTypeDetails.class);
                widgetType.setTenantId(ctx.getTenantId());
                widgetType.setFqn(fqn);
                var existingWidgetType = widgetTypeService.findWidgetTypeByTenantIdAndFqn(ctx.getTenantId(), fqn);
                if (existingWidgetType == null) {
                    widgetType.setId(null);
                } else {
                    widgetType.setId(existingWidgetType.getId());
                    widgetType.setCreatedTime(existingWidgetType.getCreatedTime());
                }
                widgetTypeService.saveWidgetType(widgetType);
            });
        }
        WidgetsBundle savedWidgetsBundle = widgetsBundleService.saveWidgetsBundle(widgetsBundle);
        widgetTypeService.updateWidgetsBundleWidgetFqns(ctx.getTenantId(), savedWidgetsBundle.getId(), exportData.getFqns());
        return savedWidgetsBundle;
    }

    @Override
    protected boolean compare(EntitiesImportCtx ctx, WidgetsBundleExportData exportData, WidgetsBundle prepared, WidgetsBundle existing) {
        return true;
    }

    @Override
    protected WidgetsBundle deepCopy(WidgetsBundle widgetsBundle) {
        return new WidgetsBundle(widgetsBundle);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.WIDGETS_BUNDLE;
    }

}
