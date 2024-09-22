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
package org.sobeam.server.dao.asset;

import org.sobeam.server.common.data.EntityInfo;
import org.sobeam.server.common.data.asset.AssetProfile;
import org.sobeam.server.common.data.asset.AssetProfileInfo;
import org.sobeam.server.common.data.id.AssetProfileId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.page.PageData;
import org.sobeam.server.common.data.page.PageLink;
import org.sobeam.server.dao.Dao;
import org.sobeam.server.dao.ExportableEntityDao;
import org.sobeam.server.dao.ImageContainerDao;

import java.util.List;
import java.util.UUID;

public interface AssetProfileDao extends Dao<AssetProfile>, ExportableEntityDao<AssetProfileId, AssetProfile>, ImageContainerDao<AssetProfileInfo> {

    AssetProfileInfo findAssetProfileInfoById(TenantId tenantId, UUID assetProfileId);

    AssetProfile save(TenantId tenantId, AssetProfile assetProfile);

    AssetProfile saveAndFlush(TenantId tenantId, AssetProfile assetProfile);

    PageData<AssetProfile> findAssetProfiles(TenantId tenantId, PageLink pageLink);

    PageData<AssetProfileInfo> findAssetProfileInfos(TenantId tenantId, PageLink pageLink);

    AssetProfile findDefaultAssetProfile(TenantId tenantId);

    AssetProfileInfo findDefaultAssetProfileInfo(TenantId tenantId);

    AssetProfile findByName(TenantId tenantId, String profileName);

    PageData<AssetProfile> findAllWithImages(PageLink pageLink);

    List<EntityInfo> findTenantAssetProfileNames(UUID tenantId, boolean activeOnly);

}
