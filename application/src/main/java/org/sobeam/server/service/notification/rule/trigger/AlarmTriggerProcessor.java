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

import org.springframework.stereotype.Service;
import org.sobeam.server.common.data.alarm.Alarm;
import org.sobeam.server.common.data.alarm.AlarmApiCallResult;
import org.sobeam.server.common.data.alarm.AlarmInfo;
import org.sobeam.server.common.data.alarm.AlarmStatusFilter;
import org.sobeam.server.common.data.notification.info.AlarmNotificationInfo;
import org.sobeam.server.common.data.notification.info.RuleOriginatedNotificationInfo;
import org.sobeam.server.common.data.notification.rule.trigger.AlarmTrigger;
import org.sobeam.server.common.data.notification.rule.trigger.config.AlarmNotificationRuleTriggerConfig;
import org.sobeam.server.common.data.notification.rule.trigger.config.AlarmNotificationRuleTriggerConfig.AlarmAction;
import org.sobeam.server.common.data.notification.rule.trigger.config.AlarmNotificationRuleTriggerConfig.ClearRule;
import org.sobeam.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.sobeam.server.common.data.util.CollectionsUtil.emptyOrContains;

@Service
public class AlarmTriggerProcessor implements NotificationRuleTriggerProcessor<AlarmTrigger, AlarmNotificationRuleTriggerConfig> {

    @Override
    public boolean matchesFilter(AlarmTrigger trigger, AlarmNotificationRuleTriggerConfig triggerConfig) {
        AlarmApiCallResult alarmUpdate = trigger.getAlarmUpdate();
        Alarm alarm = alarmUpdate.getAlarm();
        if (!typeMatches(alarm, triggerConfig)) {
            return false;
        }

        if (alarmUpdate.isCreated()) {
            if (triggerConfig.getNotifyOn().contains(AlarmAction.CREATED)) {
                return severityMatches(alarm, triggerConfig);
            }
        }  else if (alarmUpdate.isSeverityChanged()) {
            if (triggerConfig.getNotifyOn().contains(AlarmAction.SEVERITY_CHANGED)) {
                return severityMatches(alarmUpdate.getOld(), triggerConfig) || severityMatches(alarm, triggerConfig);
            }  else {
                // if we haven't yet sent notification about the alarm
                return !severityMatches(alarmUpdate.getOld(), triggerConfig) && severityMatches(alarm, triggerConfig);
            }
        } else if (alarmUpdate.isAcknowledged()) {
            if (triggerConfig.getNotifyOn().contains(AlarmAction.ACKNOWLEDGED)) {
                return severityMatches(alarm, triggerConfig);
            }
        } else if (alarmUpdate.isCleared()) {
            if (triggerConfig.getNotifyOn().contains(AlarmAction.CLEARED)) {
                return severityMatches(alarm, triggerConfig);
            }
        }
        return false;
    }

    @Override
    public boolean matchesClearRule(AlarmTrigger trigger, AlarmNotificationRuleTriggerConfig triggerConfig) {
        AlarmApiCallResult alarmUpdate = trigger.getAlarmUpdate();
        Alarm alarm = alarmUpdate.getAlarm();
        if (!typeMatches(alarm, triggerConfig)) {
            return false;
        }
        if (alarmUpdate.isDeleted()) {
            return true;
        }
        ClearRule clearRule = triggerConfig.getClearRule();
        if (clearRule != null) {
            if (isNotEmpty(clearRule.getAlarmStatuses())) {
                return AlarmStatusFilter.from(clearRule.getAlarmStatuses()).matches(alarm);
            }
        }
        return false;
    }

    private boolean severityMatches(Alarm alarm, AlarmNotificationRuleTriggerConfig triggerConfig) {
        return emptyOrContains(triggerConfig.getAlarmSeverities(), alarm.getSeverity());
    }

    private boolean typeMatches(Alarm alarm, AlarmNotificationRuleTriggerConfig triggerConfig) {
        return emptyOrContains(triggerConfig.getAlarmTypes(), alarm.getType());
    }

    @Override
    public RuleOriginatedNotificationInfo constructNotificationInfo(AlarmTrigger trigger) {
        AlarmApiCallResult alarmUpdate = trigger.getAlarmUpdate();
        AlarmInfo alarmInfo = alarmUpdate.getAlarm();
        return AlarmNotificationInfo.builder()
                .alarmId(alarmInfo.getUuidId())
                .alarmType(alarmInfo.getType())
                .action(alarmUpdate.isCreated() ? "created" :
                        alarmUpdate.isSeverityChanged() ? "severity changed" :
                        alarmUpdate.isAcknowledged() ? "acknowledged" :
                        alarmUpdate.isCleared() ? "cleared" :
                        alarmUpdate.isDeleted() ? "deleted" : null)
                .alarmOriginator(alarmInfo.getOriginator())
                .alarmOriginatorName(alarmInfo.getOriginatorName())
                .alarmSeverity(alarmInfo.getSeverity())
                .alarmStatus(alarmInfo.getStatus())
                .acknowledged(alarmInfo.isAcknowledged())
                .cleared(alarmInfo.isCleared())
                .alarmCustomerId(alarmInfo.getCustomerId())
                .dashboardId(alarmInfo.getDashboardId())
                .build();
    }

    @Override
    public NotificationRuleTriggerType getTriggerType() {
        return NotificationRuleTriggerType.ALARM;
    }

}
