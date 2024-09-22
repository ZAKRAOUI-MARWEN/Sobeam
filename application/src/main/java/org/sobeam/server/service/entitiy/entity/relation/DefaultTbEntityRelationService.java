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
package org.sobeam.server.service.entitiy.entity.relation;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.sobeam.server.common.data.User;
import org.sobeam.server.common.data.audit.ActionType;
import org.sobeam.server.common.data.exception.SobeamErrorCode;
import org.sobeam.server.common.data.exception.SobeamException;
import org.sobeam.server.common.data.id.CustomerId;
import org.sobeam.server.common.data.id.EntityId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.relation.EntityRelation;
import org.sobeam.server.dao.relation.RelationService;
import org.sobeam.server.queue.util.TbCoreComponent;
import org.sobeam.server.service.entitiy.AbstractTbEntityService;

@Service
@TbCoreComponent
@AllArgsConstructor
@Slf4j
public class DefaultTbEntityRelationService extends AbstractTbEntityService implements TbEntityRelationService {

    private final RelationService relationService;

    @Override
    public void save(TenantId tenantId, CustomerId customerId, EntityRelation relation, User user) throws SobeamException {
        ActionType actionType = ActionType.RELATION_ADD_OR_UPDATE;
        try {
            relationService.saveRelation(tenantId, relation);
            logEntityActionService.logEntityRelationAction(tenantId, customerId,
                    relation, user, actionType, null, relation);
        } catch (Exception e) {
            logEntityActionService.logEntityRelationAction(tenantId, customerId,
                    relation, user, actionType, e, relation);
            throw e;
        }
    }

    @Override
    public void delete(TenantId tenantId, CustomerId customerId, EntityRelation relation, User user) throws SobeamException {
        ActionType actionType = ActionType.RELATION_DELETED;
        try {
            boolean found = relationService.deleteRelation(tenantId, relation.getFrom(), relation.getTo(), relation.getType(), relation.getTypeGroup());
            if (!found) {
                throw new SobeamException("Requested item wasn't found!", SobeamErrorCode.ITEM_NOT_FOUND);
            }
            logEntityActionService.logEntityRelationAction(tenantId, customerId, relation, user, actionType, null, relation);
        } catch (Exception e) {
            logEntityActionService.logEntityRelationAction(tenantId, customerId,
                    relation, user, actionType, e, relation);
            throw e;
        }
    }

    @Override
    public void deleteCommonRelations(TenantId tenantId, CustomerId customerId, EntityId entityId, User user) throws SobeamException {
        try {
            relationService.deleteEntityCommonRelations(tenantId, entityId);
            logEntityActionService.logEntityAction(tenantId, entityId, null, customerId, ActionType.RELATIONS_DELETED, user);
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, entityId, null, customerId,
                    ActionType.RELATIONS_DELETED, user, e);
            throw e;
        }
    }
}
