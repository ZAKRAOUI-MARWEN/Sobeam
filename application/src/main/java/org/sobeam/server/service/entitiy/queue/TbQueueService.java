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
package org.sobeam.server.service.entitiy.queue;

import org.sobeam.server.common.data.TenantProfile;
import org.sobeam.server.common.data.id.QueueId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.queue.Queue;

import java.util.List;

public interface TbQueueService {

    Queue saveQueue(Queue queue);

    void deleteQueue(TenantId tenantId, QueueId queueId);

    void deleteQueueByQueueName(TenantId tenantId, String queueName);

    void updateQueuesByTenants(List<TenantId> tenantIds, TenantProfile newTenantProfile, TenantProfile oldTenantProfile);
}