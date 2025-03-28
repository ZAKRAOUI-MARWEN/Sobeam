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
package org.sobeam.server.service.sync.ie.exporting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.sobeam.server.common.data.EntityType;
import org.sobeam.server.common.data.ExportableEntity;
import org.sobeam.server.common.data.HasTenantId;
import org.sobeam.server.common.data.id.EntityId;
import org.sobeam.server.common.data.id.HasId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.page.PageData;
import org.sobeam.server.common.data.page.PageLink;
import org.sobeam.server.dao.Dao;
import org.sobeam.server.dao.ExportableEntityDao;
import org.sobeam.server.dao.entity.EntityDaoService;
import org.sobeam.server.dao.entity.EntityServiceRegistry;
import org.sobeam.server.queue.util.TbCoreComponent;
import org.sobeam.server.service.security.permission.AccessControlService;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class DefaultExportableEntitiesService implements ExportableEntitiesService {

    private final Map<EntityType, Dao<?>> daos = new HashMap<>();

    private final EntityServiceRegistry entityServiceRegistry;
    private final AccessControlService accessControlService;

    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> E findEntityByTenantIdAndExternalId(TenantId tenantId, I externalId) {
        EntityType entityType = externalId.getEntityType();
        Dao<E> dao = getDao(entityType);

        E entity = null;

        if (dao instanceof ExportableEntityDao) {
            ExportableEntityDao<I, E> exportableEntityDao = (ExportableEntityDao<I, E>) dao;
            entity = exportableEntityDao.findByTenantIdAndExternalId(tenantId.getId(), externalId.getId());
        }
        if (entity == null || !belongsToTenant(entity, tenantId)) {
            return null;
        }

        return entity;
    }

    @Override
    public <E extends HasId<I>, I extends EntityId> E findEntityByTenantIdAndId(TenantId tenantId, I id) {
        E entity = findEntityById(id);

        if (entity == null || !belongsToTenant(entity, tenantId)) {
            return null;
        }
        return entity;
    }

    @Override
    public <E extends HasId<I>, I extends EntityId> E findEntityById(I id) {
        EntityType entityType = id.getEntityType();
        Dao<E> dao = getDao(entityType);
        if (dao == null) {
            throw new IllegalArgumentException("Unsupported entity type " + entityType);
        }

        return dao.findById(TenantId.SYS_TENANT_ID, id.getId());
    }

    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> E findEntityByTenantIdAndName(TenantId tenantId, EntityType entityType, String name) {
        Dao<E> dao = getDao(entityType);

        E entity = null;

        if (dao instanceof ExportableEntityDao) {
            ExportableEntityDao<I, E> exportableEntityDao = (ExportableEntityDao<I, E>) dao;
            try {
                entity = exportableEntityDao.findByTenantIdAndName(tenantId.getId(), name);
            } catch (UnsupportedOperationException ignored) {
            }
        }
        if (entity == null || !belongsToTenant(entity, tenantId)) {
            return null;
        }

        return entity;
    }

    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> E findDefaultEntityByTenantId(TenantId tenantId, EntityType entityType) {
        ExportableEntityDao<I, E> dao = getExportableEntityDao(entityType);
        if (dao == null) {
            return null;
        }
        return dao.findDefaultEntityByTenantId(tenantId.getId());
    }

    @Override
    public <E extends ExportableEntity<I>, I extends EntityId> PageData<E> findEntitiesByTenantId(TenantId tenantId, EntityType entityType, PageLink pageLink) {
        ExportableEntityDao<I, E> dao = getExportableEntityDao(entityType);
        if (dao == null) {
            return PageData.emptyPageData();
        }
        return dao.findByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public <I extends EntityId> PageData<I> findEntitiesIdsByTenantId(TenantId tenantId, EntityType entityType, PageLink pageLink) {
        ExportableEntityDao<I, ?> dao = getExportableEntityDao(entityType);
        if (dao == null) {
            return PageData.emptyPageData();
        }
        return dao.findIdsByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public <I extends EntityId> I getExternalIdByInternal(I internalId) {
        ExportableEntityDao<I, ?> dao = getExportableEntityDao(internalId.getEntityType());
        if (dao == null) {
            return null;
        }
        return dao.getExternalIdByInternal(internalId);
    }

    private boolean belongsToTenant(HasId<? extends EntityId> entity, TenantId tenantId) {
        return tenantId.equals(((HasTenantId) entity).getTenantId());
    }


    @Override
    public <I extends EntityId> void removeById(TenantId tenantId, I id) {
        EntityType entityType = id.getEntityType();
        EntityDaoService entityService = entityServiceRegistry.getServiceByEntityType(entityType);
        entityService.deleteEntity(tenantId, id, false);
    }

    private <I extends EntityId, E extends ExportableEntity<I>> ExportableEntityDao<I, E> getExportableEntityDao(EntityType entityType) {
        Dao<E> dao = getDao(entityType);
        if (dao instanceof ExportableEntityDao) {
            return (ExportableEntityDao<I, E>) dao;
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private <E> Dao<E> getDao(EntityType entityType) {
        return (Dao<E>) daos.get(entityType);
    }

    @Autowired
    private void setDaos(Collection<Dao<?>> daos) {
        daos.forEach(dao -> {
            if (dao.getEntityType() != null) {
                this.daos.put(dao.getEntityType(), dao);
            }
        });
    }

}
