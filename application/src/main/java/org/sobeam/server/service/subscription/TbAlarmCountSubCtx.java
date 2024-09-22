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
package org.sobeam.server.service.subscription;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.sobeam.server.common.data.query.AlarmCountQuery;
import org.sobeam.server.dao.alarm.AlarmService;
import org.sobeam.server.dao.attributes.AttributesService;
import org.sobeam.server.dao.entity.EntityService;
import org.sobeam.server.service.ws.WebSocketService;
import org.sobeam.server.service.ws.WebSocketSessionRef;
import org.sobeam.server.service.ws.telemetry.cmd.v2.AlarmCountUpdate;

@Slf4j
@ToString(callSuper = true)
public class TbAlarmCountSubCtx extends TbAbstractSubCtx<AlarmCountQuery> {

    private final AlarmService alarmService;

    @Getter
    @Setter
    private volatile int result;

    public TbAlarmCountSubCtx(String serviceId, WebSocketService wsService,
                              EntityService entityService, TbLocalSubscriptionService localSubscriptionService,
                              AttributesService attributesService, SubscriptionServiceStatistics stats, AlarmService alarmService,
                              WebSocketSessionRef sessionRef, int cmdId) {
        super(serviceId, wsService, entityService, localSubscriptionService, attributesService, stats, sessionRef, cmdId);
        this.alarmService = alarmService;
    }

    @Override
    public void fetchData() {
        result = (int) alarmService.countAlarmsByQuery(getTenantId(), getCustomerId(), query);
        sendWsMsg(new AlarmCountUpdate(cmdId, result));
    }

    @Override
    protected void update() {
        int newCount = (int) alarmService.countAlarmsByQuery(getTenantId(), getCustomerId(), query);
        if (newCount != result) {
            result = newCount;
            sendWsMsg(new AlarmCountUpdate(cmdId, result));
        }
    }

    @Override
    public boolean isDynamic() {
        return true;
    }
}
