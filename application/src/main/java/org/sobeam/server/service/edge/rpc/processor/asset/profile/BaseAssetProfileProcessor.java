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
package org.sobeam.server.service.edge.rpc.processor.asset.profile;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.sobeam.server.common.data.StringUtils;
import org.sobeam.server.common.data.asset.AssetProfile;
import org.sobeam.server.common.data.id.AssetProfileId;
import org.sobeam.server.common.data.id.DashboardId;
import org.sobeam.server.common.data.id.RuleChainId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.sobeam.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
public abstract class BaseAssetProfileProcessor extends BaseEdgeProcessor {

    protected Pair<Boolean, Boolean> saveOrUpdateAssetProfile(TenantId tenantId, AssetProfileId assetProfileId, AssetProfileUpdateMsg assetProfileUpdateMsg) {
        boolean created = false;
        boolean assetProfileNameUpdated = false;
        assetCreationLock.lock();
        try {
            AssetProfile assetProfile = constructAssetProfileFromUpdateMsg(tenantId, assetProfileId, assetProfileUpdateMsg);
            if (assetProfile == null) {
                throw new RuntimeException("[{" + tenantId + "}] assetProfileUpdateMsg {" + assetProfileUpdateMsg + "} cannot be converted to asset profile");
            }
            AssetProfile assetProfileById = assetProfileService.findAssetProfileById(tenantId, assetProfileId);
            if (assetProfileById == null) {
                created = true;
                assetProfile.setId(null);
                assetProfile.setDefault(false);
            } else {
                assetProfile.setId(assetProfileId);
                assetProfile.setDefault(assetProfileById.isDefault());
            }
            String assetProfileName = assetProfile.getName();
            AssetProfile assetProfileByName = assetProfileService.findAssetProfileByName(tenantId, assetProfileName);
            if (assetProfileByName != null && !assetProfileByName.getId().equals(assetProfileId)) {
                assetProfileName = assetProfileName + "_" + StringUtils.randomAlphabetic(15);
                log.warn("[{}] Asset profile with name {} already exists. Renaming asset profile name to {}",
                        tenantId, assetProfile.getName(), assetProfileName);
                assetProfileNameUpdated = true;
            }
            assetProfile.setName(assetProfileName);

            RuleChainId ruleChainId = assetProfile.getDefaultRuleChainId();
            setDefaultRuleChainId(tenantId, assetProfile, created ? null : assetProfileById.getDefaultRuleChainId());
            setDefaultEdgeRuleChainId(assetProfile, ruleChainId, assetProfileUpdateMsg);
            setDefaultDashboardId(tenantId, created ? null : assetProfileById.getDefaultDashboardId(), assetProfile, assetProfileUpdateMsg);

            assetProfileValidator.validate(assetProfile, AssetProfile::getTenantId);
            if (created) {
                assetProfile.setId(assetProfileId);
            }
            assetProfileService.saveAssetProfile(assetProfile, false, true);
        } catch (Exception e) {
            log.error("[{}] Failed to process asset profile update msg [{}]", tenantId, assetProfileUpdateMsg, e);
            throw e;
        } finally {
            assetCreationLock.unlock();
        }
        return Pair.of(created, assetProfileNameUpdated);
    }

    protected abstract AssetProfile constructAssetProfileFromUpdateMsg(TenantId tenantId, AssetProfileId assetProfileId, AssetProfileUpdateMsg assetProfileUpdateMsg);

    protected abstract void setDefaultRuleChainId(TenantId tenantId, AssetProfile assetProfile, RuleChainId ruleChainId);

    protected abstract void setDefaultEdgeRuleChainId(AssetProfile assetProfile, RuleChainId ruleChainId, AssetProfileUpdateMsg assetProfileUpdateMsg);

    protected abstract void setDefaultDashboardId(TenantId tenantId, DashboardId dashboardId, AssetProfile assetProfile, AssetProfileUpdateMsg assetProfileUpdateMsg);
}
