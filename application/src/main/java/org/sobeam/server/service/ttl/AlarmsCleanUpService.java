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
package org.sobeam.server.service.ttl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.sobeam.server.common.data.alarm.Alarm;
import org.sobeam.server.common.data.audit.ActionType;
import org.sobeam.server.common.data.id.AlarmId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.page.PageData;
import org.sobeam.server.common.data.page.PageDataIterable;
import org.sobeam.server.common.data.page.PageLink;
import org.sobeam.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.sobeam.server.common.msg.queue.ServiceType;
import org.sobeam.server.dao.alarm.AlarmDao;
import org.sobeam.server.dao.alarm.AlarmService;
import org.sobeam.server.dao.relation.RelationService;
import org.sobeam.server.dao.tenant.TbTenantProfileCache;
import org.sobeam.server.dao.tenant.TenantService;
import org.sobeam.server.queue.discovery.PartitionService;
import org.sobeam.server.queue.util.TbCoreComponent;
import org.sobeam.server.service.action.EntityActionService;

import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@TbCoreComponent
@Service
@Slf4j
@RequiredArgsConstructor
public class AlarmsCleanUpService {

    @Value("${sql.ttl.alarms.removal_batch_size}")
    private Integer removalBatchSize;

    private final TenantService tenantService;
    private final AlarmDao alarmDao;
    private final AlarmService alarmService;
    private final EntityActionService entityActionService;
    private final PartitionService partitionService;
    private final TbTenantProfileCache tenantProfileCache;

    @Scheduled(initialDelayString = "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${sql.ttl.alarms.checking_interval})}", fixedDelayString = "${sql.ttl.alarms.checking_interval}")
    public void cleanUp() {
        PageDataIterable<TenantId> tenants = new PageDataIterable<>(tenantService::findTenantsIds, 10_000);
        for (TenantId tenantId : tenants) {
            try {
                cleanUp(tenantId);
            } catch (Exception e) {
                log.warn("Failed to clean up alarms by ttl for tenant {}", tenantId, e);
            }
        }
    }

    private void cleanUp(TenantId tenantId) {
        if (!partitionService.resolve(ServiceType.TB_CORE, tenantId, tenantId).isMyPartition()) {
            return;
        }

        Optional<DefaultTenantProfileConfiguration> tenantProfileConfiguration = tenantProfileCache.get(tenantId).getProfileConfiguration();
        if (tenantProfileConfiguration.isEmpty() || tenantProfileConfiguration.get().getAlarmsTtlDays() == 0) {
            return;
        }

        long ttl = TimeUnit.DAYS.toMillis(tenantProfileConfiguration.get().getAlarmsTtlDays());
        long expirationTime = System.currentTimeMillis() - ttl;

        PageLink removalBatchRequest = new PageLink(removalBatchSize, 0);
        long totalRemoved = 0;
        Set<String> typesToRemove = new HashSet<>();
        while (true) {
            PageData<AlarmId> toRemove = alarmDao.findAlarmsIdsByEndTsBeforeAndTenantId(expirationTime, tenantId, removalBatchRequest);
            for (AlarmId alarmId : toRemove.getData()) {
                Alarm alarm = alarmService.delAlarm(tenantId, alarmId, false).getAlarm();
                if (alarm != null) {
                    entityActionService.pushEntityActionToRuleEngine(alarm.getOriginator(), alarm, tenantId, null, ActionType.ALARM_DELETE, null);
                    totalRemoved++;
                    typesToRemove.add(alarm.getType());
                }
            }
            if (!toRemove.hasNext()) {
                break;
            }
        }

        alarmService.delAlarmTypes(tenantId, typesToRemove);

        if (totalRemoved > 0) {
            log.info("Removed {} outdated alarm(s) for tenant {} older than {}", totalRemoved, tenantId, new Date(expirationTime));
        }
    }

}
