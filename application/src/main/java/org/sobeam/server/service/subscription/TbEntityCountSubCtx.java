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

import lombok.extern.slf4j.Slf4j;
import org.sobeam.server.common.data.query.EntityCountQuery;
import org.sobeam.server.dao.attributes.AttributesService;
import org.sobeam.server.dao.entity.EntityService;
import org.sobeam.server.service.ws.WebSocketService;
import org.sobeam.server.service.ws.WebSocketSessionRef;
import org.sobeam.server.service.ws.telemetry.cmd.v2.EntityCountUpdate;

@Slf4j
public class TbEntityCountSubCtx extends TbAbstractSubCtx<EntityCountQuery> {

    private volatile int result;

    public TbEntityCountSubCtx(String serviceId, WebSocketService wsService, EntityService entityService,
                               TbLocalSubscriptionService localSubscriptionService, AttributesService attributesService,
                               SubscriptionServiceStatistics stats, WebSocketSessionRef sessionRef, int cmdId) {
        super(serviceId, wsService, entityService, localSubscriptionService, attributesService, stats, sessionRef, cmdId);
    }

    @Override
    public void fetchData() {
        result = (int) entityService.countEntitiesByQuery(getTenantId(), getCustomerId(), query);
        sendWsMsg(new EntityCountUpdate(cmdId, result));
    }

    @Override
    protected void update() {
        int newCount = (int) entityService.countEntitiesByQuery(getTenantId(), getCustomerId(), query);
        if (newCount != result) {
            result = newCount;
            sendWsMsg(new EntityCountUpdate(cmdId, result));
        }
    }

    @Override
    public boolean isDynamic() {
        return true;
    }
}
