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

import org.sobeam.server.common.data.id.QueueStatsId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.page.PageData;
import org.sobeam.server.common.data.page.PageLink;
import org.sobeam.server.common.data.queue.QueueStats;
import org.sobeam.server.dao.entity.EntityDaoService;

import java.util.List;

public interface QueueStatsService extends EntityDaoService {

    QueueStats save(TenantId tenantId, QueueStats queueStats);

    QueueStats findQueueStatsById(TenantId tenantId, QueueStatsId queueStatsId);

    List<QueueStats> findQueueStatsByIds(TenantId tenantId, List<QueueStatsId> queueStatsId);

    QueueStats findByTenantIdAndNameAndServiceId(TenantId tenantId, String queueName, String serviceId);

    PageData<QueueStats> findByTenantId(TenantId tenantId, PageLink pageLink);

}
