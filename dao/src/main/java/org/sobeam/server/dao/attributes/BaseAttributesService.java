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
package org.sobeam.server.dao.attributes;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.sobeam.server.common.data.AttributeScope;
import org.sobeam.server.common.data.EntityType;
import org.sobeam.server.common.data.StringUtils;
import org.sobeam.server.common.data.id.DeviceProfileId;
import org.sobeam.server.common.data.id.EntityId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.kv.AttributeKvEntry;
import org.sobeam.server.dao.service.Validator;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.sobeam.server.dao.attributes.AttributeUtils.validate;

/**
 * @author Andrew Shvayka
 */
@Service
@ConditionalOnProperty(prefix = "cache.attributes", value = "enabled", havingValue = "false", matchIfMissing = true)
@Primary
@Slf4j
public class BaseAttributesService implements AttributesService {
    private final AttributesDao attributesDao;

    @Value("${sql.attributes.value_no_xss_validation:false}")
    private boolean valueNoXssValidation;

    public BaseAttributesService(AttributesDao attributesDao) {
        this.attributesDao = attributesDao;
    }

    @Override
    public ListenableFuture<Optional<AttributeKvEntry>> find(TenantId tenantId, EntityId entityId, String scope, String attributeKey) {
        validate(entityId, scope);
        Validator.validateString(attributeKey, k -> "Incorrect attribute key " + k);
        return Futures.immediateFuture(attributesDao.find(tenantId, entityId, AttributeScope.valueOf(scope), attributeKey));
    }

    @Override
    public ListenableFuture<Optional<AttributeKvEntry>> find(TenantId tenantId, EntityId entityId, AttributeScope scope, String attributeKey) {
        validate(entityId, scope);
        Validator.validateString(attributeKey, k -> "Incorrect attribute key " + k);
        return Futures.immediateFuture(attributesDao.find(tenantId, entityId, scope, attributeKey));
    }

    @Override
    public ListenableFuture<List<AttributeKvEntry>> find(TenantId tenantId, EntityId entityId, String scope, Collection<String> attributeKeys) {
        validate(entityId, scope);
        attributeKeys.forEach(attributeKey -> Validator.validateString(attributeKey, k -> "Incorrect attribute key " + k));
        return Futures.immediateFuture(attributesDao.find(tenantId, entityId, AttributeScope.valueOf(scope), attributeKeys));
    }

    @Override
    public ListenableFuture<List<AttributeKvEntry>> find(TenantId tenantId, EntityId entityId, AttributeScope scope, Collection<String> attributeKeys) {
        validate(entityId, scope);
        attributeKeys.forEach(attributeKey -> Validator.validateString(attributeKey, k -> "Incorrect attribute key " + k));
        return Futures.immediateFuture(attributesDao.find(tenantId, entityId, scope, attributeKeys));
    }

    @Override
    public ListenableFuture<List<AttributeKvEntry>> findAll(TenantId tenantId, EntityId entityId, String scope) {
        validate(entityId, scope);
        return Futures.immediateFuture(attributesDao.findAll(tenantId, entityId, AttributeScope.valueOf(scope)));
    }

    @Override
    public ListenableFuture<List<AttributeKvEntry>> findAll(TenantId tenantId, EntityId entityId, AttributeScope scope) {
        validate(entityId, scope);
        return Futures.immediateFuture(attributesDao.findAll(tenantId, entityId, scope));
    }

    @Override
    public List<String> findAllKeysByDeviceProfileId(TenantId tenantId, DeviceProfileId deviceProfileId) {
        return attributesDao.findAllKeysByDeviceProfileId(tenantId, deviceProfileId);
    }

    @Override
    public List<String> findAllKeysByEntityIds(TenantId tenantId, EntityType entityType, List<EntityId> entityIds) {
        return attributesDao.findAllKeysByEntityIds(tenantId, entityIds);
    }

    @Override
    public List<String> findAllKeysByEntityIds(TenantId tenantId, List<EntityId> entityIds) {
        return attributesDao.findAllKeysByEntityIds(tenantId, entityIds);
    }

    @Override
    public List<String> findAllKeysByEntityIds(TenantId tenantId, List<EntityId> entityIds, String scope) {
        if (StringUtils.isEmpty(scope)) {
            return attributesDao.findAllKeysByEntityIds(tenantId, entityIds);
        } else {
            return attributesDao.findAllKeysByEntityIdsAndAttributeType(tenantId, entityIds, scope);
        }
    }

    @Override
    public ListenableFuture<String> save(TenantId tenantId, EntityId entityId, String scope, AttributeKvEntry attribute) {
        validate(entityId, scope);
        AttributeUtils.validate(attribute, valueNoXssValidation);
        return attributesDao.save(tenantId, entityId, AttributeScope.valueOf(scope), attribute);
    }

    @Override
    public ListenableFuture<String> save(TenantId tenantId, EntityId entityId, AttributeScope scope, AttributeKvEntry attribute) {
        validate(entityId, scope);
        AttributeUtils.validate(attribute, valueNoXssValidation);
        return attributesDao.save(tenantId, entityId, scope, attribute);
    }

    @Override
    public ListenableFuture<List<String>> save(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes) {
        validate(entityId, scope);
        AttributeUtils.validate(attributes, valueNoXssValidation);
        List<ListenableFuture<String>> saveFutures = attributes.stream().map(attribute -> attributesDao.save(tenantId, entityId, AttributeScope.valueOf(scope), attribute)).collect(Collectors.toList());
        return Futures.allAsList(saveFutures);
    }

    @Override
    public ListenableFuture<List<String>> save(TenantId tenantId, EntityId entityId, AttributeScope scope, List<AttributeKvEntry> attributes) {
        validate(entityId, scope);
        AttributeUtils.validate(attributes, valueNoXssValidation);
        List<ListenableFuture<String>> saveFutures = attributes.stream().map(attribute -> attributesDao.save(tenantId, entityId, scope, attribute)).collect(Collectors.toList());
        return Futures.allAsList(saveFutures);
    }

    @Override
    public ListenableFuture<List<String>> removeAll(TenantId tenantId, EntityId entityId, String scope, List<String> attributeKeys) {
        validate(entityId, scope);
        return Futures.allAsList(attributesDao.removeAll(tenantId, entityId, AttributeScope.valueOf(scope), attributeKeys));
    }

    @Override
    public ListenableFuture<List<String>> removeAll(TenantId tenantId, EntityId entityId, AttributeScope scope, List<String> attributeKeys) {
        validate(entityId, scope);
        return Futures.allAsList(attributesDao.removeAll(tenantId, entityId, scope, attributeKeys));
    }

    @Override
    public int removeAllByEntityId(TenantId tenantId, EntityId entityId) {
        List<Pair<AttributeScope, String>> deleted = attributesDao.removeAllByEntityId(tenantId, entityId);
        return deleted.size();
    }

}