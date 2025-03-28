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
package org.sobeam.server.dao.service.validator;

import org.springframework.stereotype.Component;
import org.sobeam.server.common.data.StringUtils;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.queue.QueueStats;
import org.sobeam.server.dao.exception.DataValidationException;
import org.sobeam.server.dao.service.DataValidator;

@Component
public class QueueStatsDataValidator extends DataValidator<QueueStats> {

    @Override
    protected void validateDataImpl(TenantId tenantId, QueueStats queueStats) {
        if (queueStats.getTenantId() == null) {
            throw new DataValidationException("Tenant id should be specified!.");
        }
        if (queueStats.getQueueName() == null) {
            throw new DataValidationException("Queue name should be specified!.");
        }
        if (StringUtils.isEmpty(queueStats.getServiceId())) {
            throw new DataValidationException("Service id should be specified!.");
        }
    }
}
