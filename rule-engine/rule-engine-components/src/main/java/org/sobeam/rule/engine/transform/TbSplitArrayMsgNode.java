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
package org.sobeam.rule.engine.transform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;
import org.sobeam.common.util.JacksonUtil;
import org.sobeam.rule.engine.api.EmptyNodeConfiguration;
import org.sobeam.rule.engine.api.RuleNode;
import org.sobeam.rule.engine.api.TbContext;
import org.sobeam.rule.engine.api.TbNode;
import org.sobeam.rule.engine.api.TbNodeConfiguration;
import org.sobeam.rule.engine.api.TbNodeException;
import org.sobeam.rule.engine.api.util.TbNodeUtils;
import org.sobeam.server.common.data.msg.TbNodeConnectionType;
import org.sobeam.server.common.data.plugin.ComponentType;
import org.sobeam.server.common.msg.TbMsg;
import org.sobeam.server.common.msg.queue.RuleEngineException;
import org.sobeam.server.common.msg.queue.TbMsgCallback;

import java.util.concurrent.ExecutionException;

@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "split array msg",
        configClazz = EmptyNodeConfiguration.class,
        nodeDescription = "Split array message into several messages",
        nodeDetails = "Splits an array message into individual elements, with each element sent as a separate message. " +
                "All outbound messages will have the same type and metadata as the original array message.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        icon = "content_copy",
        configDirective = "tbNodeEmptyConfig"
)
public class TbSplitArrayMsgNode implements TbNode {

    private EmptyNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, EmptyNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        JsonNode jsonNode = JacksonUtil.toJsonNode(msg.getData());
        if (jsonNode.isArray()) {
            ArrayNode data = (ArrayNode) jsonNode;
            if (data.isEmpty()) {
                ctx.ack(msg);
            } else if (data.size() == 1) {
                ctx.tellSuccess(TbMsg.transformMsgData(msg, JacksonUtil.toString(data.get(0))));
            } else {
                TbMsgCallbackWrapper wrapper = new MultipleTbMsgsCallbackWrapper(data.size(), new TbMsgCallback() {
                    @Override
                    public void onSuccess() {
                        ctx.ack(msg);
                    }

                    @Override
                    public void onFailure(RuleEngineException e) {
                        ctx.tellFailure(msg, e);
                    }
                });
                data.forEach(msgNode -> {
                    TbMsg outMsg = TbMsg.transformMsgData(msg, JacksonUtil.toString(msgNode));
                    ctx.enqueueForTellNext(outMsg, TbNodeConnectionType.SUCCESS, wrapper::onSuccess, wrapper::onFailure);
                });
            }
        } else {
            ctx.tellFailure(msg, new RuntimeException("Msg data is not a JSON Array!"));
        }
    }
}
