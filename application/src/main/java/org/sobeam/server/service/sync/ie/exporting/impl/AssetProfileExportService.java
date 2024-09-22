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

import org.springframework.stereotype.Service;
import org.sobeam.server.common.data.EntityType;
import org.sobeam.server.common.data.asset.AssetProfile;
import org.sobeam.server.common.data.id.AssetProfileId;
import org.sobeam.server.common.data.sync.ie.EntityExportData;
import org.sobeam.server.queue.util.TbCoreComponent;
import org.sobeam.server.service.sync.vc.data.EntitiesExportCtx;

import java.util.Set;

@Service
@TbCoreComponent
public class AssetProfileExportService extends BaseEntityExportService<AssetProfileId, AssetProfile, EntityExportData<AssetProfile>> {

    @Override
    protected void setRelatedEntities(EntitiesExportCtx<?> ctx, AssetProfile assetProfile, EntityExportData<AssetProfile> exportData) {
        assetProfile.setDefaultDashboardId(getExternalIdOrElseInternal(ctx, assetProfile.getDefaultDashboardId()));
        assetProfile.setDefaultRuleChainId(getExternalIdOrElseInternal(ctx, assetProfile.getDefaultRuleChainId()));
        assetProfile.setDefaultEdgeRuleChainId(getExternalIdOrElseInternal(ctx, assetProfile.getDefaultEdgeRuleChainId()));
        imageService.inlineImage(assetProfile);
    }

    @Override
    public Set<EntityType> getSupportedEntityTypes() {
        return Set.of(EntityType.ASSET_PROFILE);
    }

}
