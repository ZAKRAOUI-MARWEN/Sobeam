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
package org.sobeam.server.service.edge.rpc.processor.widget;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.sobeam.server.common.data.EdgeUtils;
import org.sobeam.server.common.data.edge.EdgeEvent;
import org.sobeam.server.common.data.id.WidgetTypeId;
import org.sobeam.server.common.data.widget.WidgetTypeDetails;
import org.sobeam.server.gen.edge.v1.DownlinkMsg;
import org.sobeam.server.gen.edge.v1.EdgeVersion;
import org.sobeam.server.gen.edge.v1.UpdateMsgType;
import org.sobeam.server.gen.edge.v1.WidgetTypeUpdateMsg;
import org.sobeam.server.queue.util.TbCoreComponent;
import org.sobeam.server.service.edge.rpc.constructor.widget.WidgetMsgConstructor;
import org.sobeam.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
@Component
@TbCoreComponent
public class WidgetTypeEdgeProcessor extends BaseEdgeProcessor {

    public DownlinkMsg convertWidgetTypeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        WidgetTypeId widgetTypeId = new WidgetTypeId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED -> {
                WidgetTypeDetails widgetTypeDetails = widgetTypeService.findWidgetTypeDetailsById(edgeEvent.getTenantId(), widgetTypeId);
                if (widgetTypeDetails != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    WidgetTypeUpdateMsg widgetTypeUpdateMsg =
                            ((WidgetMsgConstructor) widgetMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion)).constructWidgetTypeUpdateMsg(msgType, widgetTypeDetails, edgeVersion);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addWidgetTypeUpdateMsg(widgetTypeUpdateMsg)
                            .build();
                }
            }
            case DELETED -> {
                WidgetTypeUpdateMsg widgetTypeUpdateMsg =
                        ((WidgetMsgConstructor) widgetMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion)).constructWidgetTypeDeleteMsg(widgetTypeId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addWidgetTypeUpdateMsg(widgetTypeUpdateMsg)
                        .build();
            }
        }
        return downlinkMsg;
    }

}