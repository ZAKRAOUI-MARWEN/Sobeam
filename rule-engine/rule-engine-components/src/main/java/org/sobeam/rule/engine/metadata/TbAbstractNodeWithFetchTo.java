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
package org.sobeam.rule.engine.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import lombok.extern.slf4j.Slf4j;
import org.sobeam.common.util.JacksonUtil;
import org.sobeam.rule.engine.api.TbContext;
import org.sobeam.rule.engine.api.TbNode;
import org.sobeam.rule.engine.api.TbNodeConfiguration;
import org.sobeam.rule.engine.api.TbNodeException;
import org.sobeam.rule.engine.util.TbMsgSource;
import org.sobeam.server.common.data.id.EntityId;
import org.sobeam.server.common.data.kv.KvEntry;
import org.sobeam.server.common.data.util.TbPair;
import org.sobeam.server.common.msg.TbMsg;
import org.sobeam.server.common.msg.TbMsgMetaData;

import java.util.Arrays;
import java.util.NoSuchElementException;

@Slf4j
public abstract class TbAbstractNodeWithFetchTo<C extends TbAbstractFetchToNodeConfiguration> implements TbNode {

    protected final static String FETCH_TO_PROPERTY_NAME = "fetchTo";

    protected C config;
    protected TbMsgSource fetchTo;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        config = loadNodeConfiguration(configuration);
        if (config.getFetchTo() == null) {
            throw new TbNodeException("FetchTo option can't be null! Allowed values: " + Arrays.toString(TbMsgSource.values()));
        }
        fetchTo = config.getFetchTo();
    }

    protected abstract C loadNodeConfiguration(TbNodeConfiguration configuration) throws TbNodeException;

    protected <I extends EntityId> AsyncFunction<I, I> checkIfEntityIsPresentOrThrow(String message) {
        return id -> {
            if (id == null || id.isNullUid()) {
                return Futures.immediateFailedFuture(new NoSuchElementException(message));
            }
            return Futures.immediateFuture(id);
        };
    }

    protected ObjectNode getMsgDataAsObjectNode(TbMsg msg) {
        var msgDataNode = JacksonUtil.toJsonNode(msg.getData());
        if (msgDataNode == null || !msgDataNode.isObject()) {
            throw new IllegalArgumentException("Message body is not an object!");
        }
        return (ObjectNode) msgDataNode;
    }

    protected void enrichMessage(ObjectNode msgData, TbMsgMetaData metaData, KvEntry kvEntry, String targetKey) {
        if (TbMsgSource.DATA.equals(fetchTo)) {
            JacksonUtil.addKvEntry(msgData, kvEntry, targetKey);
        } else if (TbMsgSource.METADATA.equals(fetchTo)) {
            metaData.putValue(targetKey, kvEntry.getValueAsString());
        }
    }

    protected TbMsg transformMessage(TbMsg msg, ObjectNode msgDataNode, TbMsgMetaData msgMetaData) {
        switch (fetchTo) {
            case DATA:
                return TbMsg.transformMsgData(msg, JacksonUtil.toString(msgDataNode));
            case METADATA:
                return TbMsg.transformMsgMetadata(msg, msgMetaData);
            default:
                log.debug("Unexpected FetchTo value: {}. Allowed values: {}", fetchTo, TbMsgSource.values());
                return msg;
        }
    }

    protected TbPair<Boolean, JsonNode> upgradeRuleNodesWithOldPropertyToUseFetchTo(
            JsonNode oldConfiguration,
            String oldProperty,
            String ifTrue,
            String ifFalse
    ) throws TbNodeException {
        var newConfig = (ObjectNode) oldConfiguration;
        if (!newConfig.has(oldProperty)) {
            throw new TbNodeException("property to update: '" + oldProperty + "' doesn't exists in configuration!");
        }
        return upgradeConfigurationToUseFetchTo(oldProperty, ifTrue, ifFalse, newConfig);
    }

    protected TbPair<Boolean, JsonNode> upgradeConfigurationToUseFetchTo(
            String oldProperty, String ifTrue,
            String ifFalse, ObjectNode newConfig
    ) throws TbNodeException {
        var value = newConfig.get(oldProperty).asText();
        if ("true".equals(value)) {
            newConfig.remove(oldProperty);
            newConfig.put(FETCH_TO_PROPERTY_NAME, ifTrue);
            return new TbPair<>(true, newConfig);
        } else if ("false".equals(value)) {
            newConfig.remove(oldProperty);
            newConfig.put(FETCH_TO_PROPERTY_NAME, ifFalse);
            return new TbPair<>(true, newConfig);
        } else {
            throw new TbNodeException("property to update: '" + oldProperty + "' has unexpected value: "
                    + value + ". Allowed values: true or false!");
        }
    }

}
