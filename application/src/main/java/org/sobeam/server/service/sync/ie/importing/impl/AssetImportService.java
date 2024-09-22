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
import org.sobeam.server.common.data.asset.Asset;
import org.sobeam.server.common.data.id.AssetId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.sync.ie.EntityExportData;
import org.sobeam.server.dao.asset.AssetService;
import org.sobeam.server.queue.util.TbCoreComponent;
import org.sobeam.server.service.sync.vc.data.EntitiesImportCtx;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class AssetImportService extends BaseEntityImportService<AssetId, Asset, EntityExportData<Asset>> {

    private final AssetService assetService;

    @Override
    protected void setOwner(TenantId tenantId, Asset asset, IdProvider idProvider) {
        asset.setTenantId(tenantId);
        asset.setCustomerId(idProvider.getInternalId(asset.getCustomerId()));
    }

    @Override
    protected Asset prepare(EntitiesImportCtx ctx, Asset asset, Asset old, EntityExportData<Asset> exportData, IdProvider idProvider) {
        asset.setAssetProfileId(idProvider.getInternalId(asset.getAssetProfileId()));
        return asset;
    }

    @Override
    protected Asset saveOrUpdate(EntitiesImportCtx ctx, Asset asset, EntityExportData<Asset> exportData, IdProvider idProvider) {
        return assetService.saveAsset(asset);
    }

    @Override
    protected Asset deepCopy(Asset asset) {
        return new Asset(asset);
    }

    @Override
    protected void cleanupForComparison(Asset e) {
        super.cleanupForComparison(e);
        if (e.getCustomerId() != null && e.getCustomerId().isNullUid()) {
            e.setCustomerId(null);
        }
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ASSET;
    }

}
