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
package org.sobeam.server.dao.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.sobeam.server.common.data.EntityType;
import org.sobeam.server.common.data.id.EntityId;
import org.sobeam.server.common.data.id.HasId;
import org.sobeam.server.common.data.id.QueueStatsId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.page.PageData;
import org.sobeam.server.common.data.page.PageLink;
import org.sobeam.server.common.data.queue.QueueStats;
import org.sobeam.server.dao.entity.AbstractEntityService;
import org.sobeam.server.dao.service.DataValidator;
import org.sobeam.server.dao.service.Validator;

import java.util.List;
import java.util.Optional;

import static org.sobeam.server.dao.service.Validator.validateId;
import static org.sobeam.server.dao.service.Validator.validateIds;

@Service("QueueStatsDaoService")
@Slf4j
@RequiredArgsConstructor
public class BaseQueueStatsService extends AbstractEntityService implements QueueStatsService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    private final QueueStatsDao queueStatsDao;

    private final DataValidator<QueueStats> queueStatsValidator;

    @Override
    public QueueStats save(TenantId tenantId, QueueStats queueStats) {
        log.trace("Executing save [{}]", queueStats);
        queueStatsValidator.validate(queueStats, QueueStats::getTenantId);
        return queueStatsDao.save(tenantId, queueStats);
    }

    @Override
    public QueueStats findQueueStatsById(TenantId tenantId, QueueStatsId queueStatsId) {
        log.trace("Executing findQueueStatsById [{}]", queueStatsId);
        validateId(queueStatsId, id -> "Incorrect queueStatsId " + id);
        return queueStatsDao.findById(tenantId, queueStatsId.getId());
    }

    @Override
    public List<QueueStats> findQueueStatsByIds(TenantId tenantId, List<QueueStatsId> queueStatsIds) {
        log.trace("Executing findQueueStatsByIds, tenantId [{}], queueStatsIds [{}]", tenantId, queueStatsIds);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateIds(queueStatsIds, ids -> "Incorrect queueStatsIds " + ids);
        return queueStatsDao.findByIds(tenantId, queueStatsIds);
    }

    @Override
    public QueueStats findByTenantIdAndNameAndServiceId(TenantId tenantId, String queueName, String serviceId) {
        log.trace("Executing findByTenantIdAndNameAndServiceId, tenantId: [{}], queueName: [{}], serviceId: [{}]", tenantId, queueName, serviceId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return queueStatsDao.findByTenantIdQueueNameAndServiceId(tenantId, queueName, serviceId);
    }

    @Override
    public PageData<QueueStats> findByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findByTenantId, tenantId: [{}]", tenantId);
        Validator.validatePageLink(pageLink);
        return queueStatsDao.findByTenantId(tenantId, pageLink);
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        log.trace("Executing deleteByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        queueStatsDao.deleteByTenantId(tenantId);
    }

    @Override
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        queueStatsDao.removeById(tenantId, id.getId());
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findQueueStatsById(tenantId, new QueueStatsId(entityId.getId())));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.QUEUE_STATS;
    }

}
