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
package org.sobeam.server.service.edge.rpc.processor.telemetry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.sobeam.server.common.data.DataConstants;
import org.sobeam.server.common.data.EdgeUtils;
import org.sobeam.server.common.data.EntityType;
import org.sobeam.server.common.data.StringUtils;
import org.sobeam.server.common.data.edge.Edge;
import org.sobeam.server.common.data.edge.EdgeEvent;
import org.sobeam.server.common.data.notification.rule.trigger.EdgeCommunicationFailureTrigger;
import org.sobeam.server.common.msg.notification.NotificationRuleProcessor;
import org.sobeam.server.gen.edge.v1.DownlinkMsg;
import org.sobeam.server.gen.edge.v1.EntityDataProto;
import org.sobeam.server.queue.util.TbCoreComponent;

@Slf4j
@Component
@TbCoreComponent
public class TelemetryEdgeProcessor extends BaseTelemetryProcessor {

    @Value("${edges.rpc.max_telemetry_message_size:0}")
    private int maxTelemetryMessageSize;

    @Lazy
    @Autowired
    private NotificationRuleProcessor notificationRuleProcessor;

    @Override
    protected String getMsgSourceKey() {
        return DataConstants.EDGE_MSG_SOURCE;
    }

    public DownlinkMsg convertTelemetryEventToDownlink(Edge edge, EdgeEvent edgeEvent) {
        if (edgeEvent.getBody() != null) {
            String bodyStr = edgeEvent.getBody().toString();
            if (maxTelemetryMessageSize > 0 && bodyStr.length() > maxTelemetryMessageSize) {
                String error = "Conversion to a DownlinkMsg telemetry event failed due to a size limit violation.";
                String message = String.format("%s Current size is %s, but the limit is %s", error, bodyStr.length(), maxTelemetryMessageSize);
                log.debug("[{}][{}][{}] {}. {}", edgeEvent.getTenantId(), edgeEvent.getEdgeId(),
                        edgeEvent.getEntityId(), message, StringUtils.truncate(bodyStr, 100));
                notificationRuleProcessor.process(EdgeCommunicationFailureTrigger.builder().tenantId(edgeEvent.getTenantId())
                        .edgeId(edgeEvent.getEdgeId()).customerId(edge.getCustomerId()).edgeName(edge.getName()).failureMsg(message).error(error).build());
                return null;
            }
        }
        EntityType entityType = EntityType.valueOf(edgeEvent.getType().name());
        EntityDataProto entityDataProto = convertTelemetryEventToEntityDataProto(
                edgeEvent.getTenantId(), entityType, edgeEvent.getEntityId(),
                edgeEvent.getAction(), edgeEvent.getBody());
        return DownlinkMsg.newBuilder()
                .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                .addEntityData(entityDataProto)
                .build();
    }

}
