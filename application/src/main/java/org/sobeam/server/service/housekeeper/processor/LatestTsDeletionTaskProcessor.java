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
import org.sobeam.server.common.data.housekeeper.HousekeeperTaskType;
import org.sobeam.server.common.data.housekeeper.LatestTsDeletionHousekeeperTask;
import org.sobeam.server.dao.timeseries.TimeseriesService;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LatestTsDeletionTaskProcessor extends HousekeeperTaskProcessor<LatestTsDeletionHousekeeperTask> {

    private final TimeseriesService timeseriesService;

    @Override
    public void process(LatestTsDeletionHousekeeperTask task) throws Exception {
        timeseriesService.removeLatest(task.getTenantId(), task.getEntityId(), List.of(task.getKey())).get();
        log.debug("[{}][{}][{}] Deleted latest telemetry for key '{}'", task.getTenantId(), task.getEntityId().getEntityType(), task.getEntityId(), task.getKey());
    }

    @Override
    public HousekeeperTaskType getTaskType() {
        return HousekeeperTaskType.DELETE_LATEST_TS;
    }

}
