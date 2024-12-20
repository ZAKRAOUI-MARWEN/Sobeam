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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.sobeam.server.common.data.EntityType;
import org.sobeam.server.common.data.User;
import org.sobeam.server.common.data.audit.ActionType;
import org.sobeam.server.common.data.id.RuleChainId;
import org.sobeam.server.common.data.id.RuleNodeId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.plugin.ComponentLifecycleEvent;
import org.sobeam.server.common.data.rule.RuleChain;
import org.sobeam.server.common.data.rule.RuleChainMetaData;
import org.sobeam.server.common.data.rule.RuleChainType;
import org.sobeam.server.common.data.rule.RuleNode;
import org.sobeam.server.common.data.sync.ie.RuleChainExportData;
import org.sobeam.server.dao.rule.RuleChainService;
import org.sobeam.server.dao.rule.RuleNodeDao;
import org.sobeam.server.queue.util.TbCoreComponent;
import org.sobeam.server.service.rule.TbRuleChainService;
import org.sobeam.server.service.sync.vc.data.EntitiesImportCtx;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@TbCoreComponent
@RequiredArgsConstructor
public class RuleChainImportService extends BaseEntityImportService<RuleChainId, RuleChain, RuleChainExportData> {

    private static final LinkedHashSet<EntityType> HINTS = new LinkedHashSet<>(Arrays.asList(EntityType.RULE_CHAIN, EntityType.DEVICE, EntityType.ASSET));
    public static final Pattern PROCESSED_CONFIG_FIELDS_PATTERN = Pattern.compile(".*[iI]d.*");

    private final TbRuleChainService tbRuleChainService;
    private final RuleChainService ruleChainService;
    private final RuleNodeDao ruleNodeDao;

    @Override
    protected void setOwner(TenantId tenantId, RuleChain ruleChain, IdProvider idProvider) {
        ruleChain.setTenantId(tenantId);
    }

    @Override
    protected RuleChain findExistingEntity(EntitiesImportCtx ctx, RuleChain ruleChain, IdProvider idProvider) {
        RuleChain existingRuleChain = super.findExistingEntity(ctx, ruleChain, idProvider);
        if (existingRuleChain == null && ctx.isFindExistingByName()) {
            existingRuleChain = ruleChainService.findTenantRuleChainsByTypeAndName(ctx.getTenantId(), ruleChain.getType(), ruleChain.getName()).stream().findFirst().orElse(null);
        }
        return existingRuleChain;
    }

    @Override
    protected RuleChain prepare(EntitiesImportCtx ctx, RuleChain ruleChain, RuleChain old, RuleChainExportData exportData, IdProvider idProvider) {
        RuleChainMetaData metaData = exportData.getMetaData();
        List<RuleNode> ruleNodes = Optional.ofNullable(metaData.getNodes()).orElse(Collections.emptyList());
        if (old != null) {
            List<RuleNodeId> nodeIds = ruleNodes.stream().map(RuleNode::getId).collect(Collectors.toList());
            List<RuleNode> existing = ruleNodeDao.findByExternalIds(old.getId(), nodeIds);
            existing.forEach(node -> ctx.putInternalId(node.getExternalId(), node.getId()));
            ruleNodes.forEach(node -> {
                node.setRuleChainId(old.getId());
                node.setExternalId(node.getId());
                node.setId((RuleNodeId) ctx.getInternalId(node.getId()));
            });
        } else {
            ruleNodes.forEach(node -> {
                node.setRuleChainId(null);
                node.setExternalId(node.getId());
                node.setId(null);
            });
        }

        ruleNodes.forEach(ruleNode -> replaceIdsRecursively(ctx, idProvider, ruleNode.getConfiguration(),
                Collections.emptySet(), PROCESSED_CONFIG_FIELDS_PATTERN, HINTS));
        Optional.ofNullable(metaData.getRuleChainConnections()).orElse(Collections.emptyList())
                .forEach(ruleChainConnectionInfo -> {
                    ruleChainConnectionInfo.setTargetRuleChainId(idProvider.getInternalId(ruleChainConnectionInfo.getTargetRuleChainId(), false));
                });
        if (ruleChain.getFirstRuleNodeId() != null) {
            ruleChain.setFirstRuleNodeId((RuleNodeId) ctx.getInternalId(ruleChain.getFirstRuleNodeId()));
        }
        return ruleChain;
    }

    @Override
    protected RuleChain saveOrUpdate(EntitiesImportCtx ctx, RuleChain ruleChain, RuleChainExportData exportData, IdProvider idProvider) {
        ruleChain = ruleChainService.saveRuleChain(ruleChain);
        if (ctx.isFinalImportAttempt() || ctx.getCurrentImportResult().isUpdatedAllExternalIds()) {
            exportData.getMetaData().setRuleChainId(ruleChain.getId());
            ruleChainService.saveRuleChainMetaData(ctx.getTenantId(), exportData.getMetaData(), tbRuleChainService::updateRuleNodeConfiguration);
            return ruleChainService.findRuleChainById(ctx.getTenantId(), ruleChain.getId());
        } else {
            return ruleChain;
        }
    }

    @Override
    protected boolean compare(EntitiesImportCtx ctx, RuleChainExportData exportData, RuleChain prepared, RuleChain existing) {
        boolean different = super.compare(ctx, exportData, prepared, existing);
        if (!different) {
            RuleChainMetaData newMD = exportData.getMetaData();
            RuleChainMetaData existingMD = ruleChainService.loadRuleChainMetaData(ctx.getTenantId(), prepared.getId());
            existingMD.setRuleChainId(null);
            different = !newMD.equals(existingMD);
        }
        return different;
    }

    @Override
    protected void onEntitySaved(User user, RuleChain savedRuleChain, RuleChain oldRuleChain) {
        entityActionService.logEntityAction(user, savedRuleChain.getId(), savedRuleChain, null,
                oldRuleChain == null ? ActionType.ADDED : ActionType.UPDATED, null);
    }

    @Override
    protected RuleChain deepCopy(RuleChain ruleChain) {
        return new RuleChain(ruleChain);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.RULE_CHAIN;
    }

}
