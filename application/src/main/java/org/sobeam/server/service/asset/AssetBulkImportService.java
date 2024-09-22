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
package org.sobeam.server.service.asset;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.sobeam.server.common.data.EntityType;
import org.sobeam.server.common.data.StringUtils;
import org.sobeam.server.common.data.asset.Asset;
import org.sobeam.server.common.data.asset.AssetProfile;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.sync.ie.importing.csv.BulkImportColumnType;
import org.sobeam.server.dao.asset.AssetProfileService;
import org.sobeam.server.dao.asset.AssetService;
import org.sobeam.server.queue.util.TbCoreComponent;
import org.sobeam.server.service.entitiy.asset.TbAssetService;
import org.sobeam.server.service.security.model.SecurityUser;
import org.sobeam.server.service.sync.ie.importing.csv.AbstractBulkImportService;

import java.util.Map;
import java.util.Optional;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class AssetBulkImportService extends AbstractBulkImportService<Asset> {
    private final AssetService assetService;
    private final TbAssetService tbAssetService;
    private final AssetProfileService assetProfileService;

    @Override
    protected void setEntityFields(Asset entity, Map<BulkImportColumnType, String> fields) {
        ObjectNode additionalInfo = getOrCreateAdditionalInfoObj(entity);
        fields.forEach((columnType, value) -> {
            switch (columnType) {
                case NAME:
                    entity.setName(value);
                    break;
                case TYPE:
                    entity.setType(value);
                    break;
                case LABEL:
                    entity.setLabel(value);
                    break;
                case DESCRIPTION:
                    additionalInfo.set("description", new TextNode(value));
                    break;
            }
        });
        entity.setAdditionalInfo(additionalInfo);
    }

    @Override
    @SneakyThrows
    protected Asset saveEntity(SecurityUser user, Asset entity, Map<BulkImportColumnType, String> fields) {
        AssetProfile assetProfile;
        if (StringUtils.isNotEmpty(entity.getType())) {
            assetProfile = assetProfileService.findOrCreateAssetProfile(entity.getTenantId(), entity.getType());
        } else {
            assetProfile = assetProfileService.findDefaultAssetProfile(entity.getTenantId());
        }
        entity.setAssetProfileId(assetProfile.getId());
        return tbAssetService.save(entity, user);
    }

    @Override
    protected Asset findOrCreateEntity(TenantId tenantId, String name) {
        return Optional.ofNullable(assetService.findAssetByTenantIdAndName(tenantId, name))
                .orElseGet(Asset::new);
    }

    @Override
    protected void setOwners(Asset entity, SecurityUser user) {
        entity.setTenantId(user.getTenantId());
        entity.setCustomerId(user.getCustomerId());
    }

    @Override
    protected EntityType getEntityType() {
        return EntityType.ASSET;
    }

}
