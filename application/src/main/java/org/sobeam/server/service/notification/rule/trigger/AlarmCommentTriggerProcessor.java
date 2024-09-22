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
package org.sobeam.server.service.notification.rule.trigger;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.sobeam.server.common.data.alarm.Alarm;
import org.sobeam.server.common.data.alarm.AlarmCommentType;
import org.sobeam.server.common.data.alarm.AlarmInfo;
import org.sobeam.server.common.data.alarm.AlarmStatusFilter;
import org.sobeam.server.common.data.audit.ActionType;
import org.sobeam.server.common.data.notification.info.AlarmCommentNotificationInfo;
import org.sobeam.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.sobeam.server.common.data.notification.rule.trigger.config.AlarmCommentNotificationRuleTriggerConfig;
import org.sobeam.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;
import org.sobeam.server.common.data.notification.rule.trigger.AlarmCommentTrigger;
import org.sobeam.server.dao.entity.EntityService;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.sobeam.server.common.data.util.CollectionsUtil.emptyOrContains;

@Service
@RequiredArgsConstructor
public class AlarmCommentTriggerProcessor implements NotificationRuleTriggerProcessor<AlarmCommentTrigger, AlarmCommentNotificationRuleTriggerConfig> {

    private final EntityService entityService;

    @Override
    public boolean matchesFilter(AlarmCommentTrigger trigger, AlarmCommentNotificationRuleTriggerConfig triggerConfig) {
        if (trigger.getActionType() == ActionType.UPDATED_COMMENT && !triggerConfig.isNotifyOnCommentUpdate()) {
            return false;
        }
        if (triggerConfig.isOnlyUserComments()) {
            if (trigger.getComment().getType() == AlarmCommentType.SYSTEM) {
                return false;
            }
        }
        Alarm alarm = trigger.getAlarm();
        return emptyOrContains(triggerConfig.getAlarmTypes(), alarm.getType()) &&
                emptyOrContains(triggerConfig.getAlarmSeverities(), alarm.getSeverity()) &&
                (isEmpty(triggerConfig.getAlarmStatuses()) || AlarmStatusFilter.from(triggerConfig.getAlarmStatuses()).matches(alarm));
    }

    @Override
    public RuleOriginatedNotificationInfo constructNotificationInfo(AlarmCommentTrigger trigger) {
        Alarm alarm = trigger.getAlarm();
        String originatorName;
        if (alarm instanceof AlarmInfo) {
            originatorName = ((AlarmInfo) alarm).getOriginatorName();
        } else {
            originatorName = entityService.fetchEntityName(trigger.getTenantId(), alarm.getOriginator()).orElse("");
        }
        return AlarmCommentNotificationInfo.builder()
                .comment(trigger.getComment().getComment().get("text").asText())
                .action(trigger.getActionType() == ActionType.ADDED_COMMENT ? "added" : "updated")
                .userEmail(trigger.getUser().getEmail())
                .userFirstName(trigger.getUser().getFirstName())
                .userLastName(trigger.getUser().getLastName())
                .alarmId(alarm.getUuidId())
                .alarmType(alarm.getType())
                .alarmOriginator(alarm.getOriginator())
                .alarmOriginatorName(originatorName)
                .alarmSeverity(alarm.getSeverity())
                .alarmStatus(alarm.getStatus())
                .alarmCustomerId(alarm.getCustomerId())
                .dashboardId(alarm.getDashboardId())
                .build();
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.ALARM_COMMENT;
    }

}
