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
package org.sobeam.rule.engine.notification;

import com.google.common.util.concurrent.FutureCallback;
import org.sobeam.common.util.DonAsynchron;
import org.sobeam.common.util.JacksonUtil;
import org.sobeam.rule.engine.api.RuleNode;
import org.sobeam.rule.engine.api.TbContext;
import org.sobeam.rule.engine.api.TbNodeConfiguration;
import org.sobeam.rule.engine.api.TbNodeException;
import org.sobeam.rule.engine.api.util.TbNodeUtils;
import org.sobeam.rule.engine.external.TbAbstractExternalNode;
import org.sobeam.server.common.data.EntityType;
import org.sobeam.server.common.data.notification.NotificationRequest;
import org.sobeam.server.common.data.notification.NotificationRequestConfig;
import org.sobeam.server.common.data.notification.NotificationRequestStats;
import org.sobeam.server.common.data.notification.info.RuleEngineOriginatedNotificationInfo;
import org.sobeam.server.common.data.plugin.ComponentType;
import org.sobeam.server.common.msg.TbMsg;
import org.sobeam.server.common.msg.TbMsgMetaData;

import java.util.concurrent.ExecutionException;

@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "send notification",
        configClazz = TbNotificationNodeConfiguration.class,
        nodeDescription = "Sends notification to targets using the template",
        nodeDetails = "Will send notification to the specified targets using the template",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbExternalNodeNotificationConfig",
        icon = "notifications"
)
public class TbNotificationNode extends TbAbstractExternalNode {

    private TbNotificationNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        super.init(ctx);
        this.config = TbNodeUtils.convert(configuration, TbNotificationNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        RuleEngineOriginatedNotificationInfo notificationInfo = RuleEngineOriginatedNotificationInfo.builder()
                .msgOriginator(msg.getOriginator())
                .msgCustomerId(msg.getOriginator().getEntityType() == EntityType.CUSTOMER
                        && msg.getOriginator().equals(msg.getCustomerId()) ? null : msg.getCustomerId())
                .msgMetadata(msg.getMetaData().getData())
                .msgData(JacksonUtil.toFlatMap(JacksonUtil.toJsonNode(msg.getData())))
                .msgType(msg.getType())
                .build();

        NotificationRequest notificationRequest = NotificationRequest.builder()
                .tenantId(ctx.getTenantId())
                .targets(config.getTargets())
                .templateId(config.getTemplateId())
                .info(notificationInfo)
                .additionalConfig(new NotificationRequestConfig())
                .originatorEntityId(ctx.getSelf().getRuleChainId())
                .build();

        var tbMsg = ackIfNeeded(ctx, msg);

        var callback = new FutureCallback<NotificationRequestStats>() {
            @Override
            public void onSuccess(NotificationRequestStats stats) {
                TbMsgMetaData metaData = tbMsg.getMetaData().copy();
                metaData.putValue("notificationRequestResult", JacksonUtil.toString(stats));
                tellSuccess(ctx, TbMsg.transformMsgMetadata(tbMsg, metaData));
            }

            @Override
            public void onFailure(Throwable e) {
                tellFailure(ctx, tbMsg, e);
            }
        };

        var future = ctx.getNotificationExecutor().executeAsync(() ->
                ctx.getNotificationCenter().processNotificationRequest(ctx.getTenantId(), notificationRequest, callback));
        DonAsynchron.withCallback(future, r -> {}, callback::onFailure);
    }

}
