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
package org.sobeam.server.service.housekeeper.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.sobeam.server.dao.attributes.AttributesService;
import org.sobeam.server.common.data.housekeeper.HousekeeperTask;
import org.sobeam.server.common.data.housekeeper.HousekeeperTaskType;

@Component
@RequiredArgsConstructor
@Slf4j
public class AttributesDeletionTaskProcessor extends HousekeeperTaskProcessor<HousekeeperTask> {

    private final AttributesService attributesService;

    @Override
    public void process(HousekeeperTask task) throws Exception {
        int deletedCount = attributesService.removeAllByEntityId(task.getTenantId(), task.getEntityId());
        log.debug("[{}][{}][{}] Deleted {} attributes", task.getTenantId(), task.getEntityId().getEntityType(), task.getEntityId(), deletedCount);
    }

    @Override
    public HousekeeperTaskType getTaskType() {
        return HousekeeperTaskType.DELETE_ATTRIBUTES;
    }

}
