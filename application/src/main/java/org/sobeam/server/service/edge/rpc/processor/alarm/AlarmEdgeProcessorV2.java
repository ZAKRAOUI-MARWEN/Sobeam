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
package org.sobeam.server.service.edge.rpc.processor.alarm;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.sobeam.common.util.JacksonUtil;
import org.sobeam.server.common.data.alarm.Alarm;
import org.sobeam.server.common.data.id.AlarmId;
import org.sobeam.server.common.data.id.EntityId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.gen.edge.v1.AlarmUpdateMsg;
import org.sobeam.server.queue.util.TbCoreComponent;

@Primary
@Component
@TbCoreComponent
public class AlarmEdgeProcessorV2 extends AlarmEdgeProcessor {

    @Override
    protected EntityId getAlarmOriginatorFromMsg(TenantId tenantId, AlarmUpdateMsg alarmUpdateMsg) {
        Alarm alarm = JacksonUtil.fromString(alarmUpdateMsg.getEntity(), Alarm.class, true);
        return alarm != null ? alarm.getOriginator() : null;
    }

    @Override
    protected Alarm constructAlarmFromUpdateMsg(TenantId tenantId, AlarmId alarmId, EntityId originatorId, AlarmUpdateMsg alarmUpdateMsg) {
        return JacksonUtil.fromString(alarmUpdateMsg.getEntity(), Alarm.class, true);
    }

}