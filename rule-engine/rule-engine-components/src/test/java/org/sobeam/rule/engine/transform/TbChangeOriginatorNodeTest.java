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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sobeam.common.util.JacksonUtil;
import org.sobeam.common.util.ListeningExecutor;
import org.sobeam.rule.engine.TestDbCallbackExecutor;
import org.sobeam.rule.engine.api.TbContext;
import org.sobeam.rule.engine.api.TbNodeConfiguration;
import org.sobeam.rule.engine.api.TbNodeException;
import org.sobeam.server.common.data.asset.Asset;
import org.sobeam.server.common.data.id.AssetId;
import org.sobeam.server.common.data.id.CustomerId;
import org.sobeam.server.common.data.id.EntityId;
import org.sobeam.server.common.data.id.RuleChainId;
import org.sobeam.server.common.data.id.RuleNodeId;
import org.sobeam.server.common.data.msg.TbMsgType;
import org.sobeam.server.common.msg.TbMsg;
import org.sobeam.server.common.msg.TbMsgDataType;
import org.sobeam.server.common.msg.TbMsgMetaData;
import org.sobeam.server.dao.asset.AssetService;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TbChangeOriginatorNodeTest {

    private static final String CUSTOMER_SOURCE = "CUSTOMER";

    private TbChangeOriginatorNode node;

    @Mock
    private TbContext ctx;
    @Mock
    private AssetService assetService;

    private ListeningExecutor dbExecutor;

    @BeforeEach
    public void before() throws TbNodeException {
        dbExecutor = new TestDbCallbackExecutor();
        init();
    }

    @Test
    public void originatorCanBeChangedToCustomerId() {
        AssetId assetId = new AssetId(Uuids.timeBased());
        CustomerId customerId = new CustomerId(Uuids.timeBased());
        Asset asset = new Asset();
        asset.setCustomerId(customerId);

        RuleChainId ruleChainId = new RuleChainId(Uuids.timeBased());
        RuleNodeId ruleNodeId = new RuleNodeId(Uuids.timeBased());

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, assetId, TbMsgMetaData.EMPTY, TbMsgDataType.JSON, TbMsg.EMPTY_JSON_OBJECT, ruleChainId, ruleNodeId);

        when(ctx.getAssetService()).thenReturn(assetService);
        when(assetService.findAssetByIdAsync(any(),eq( assetId))).thenReturn(Futures.immediateFuture(asset));

        node.onMsg(ctx, msg);

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        verify(ctx).transformMsgOriginator(msgCaptor.capture(), originatorCaptor.capture());

        assertEquals(customerId, originatorCaptor.getValue());
    }

    @Test
    public void newChainCanBeStarted() {
        AssetId assetId = new AssetId(Uuids.timeBased());
        CustomerId customerId = new CustomerId(Uuids.timeBased());
        Asset asset = new Asset();
        asset.setCustomerId(customerId);

        RuleChainId ruleChainId = new RuleChainId(Uuids.timeBased());
        RuleNodeId ruleNodeId = new RuleNodeId(Uuids.timeBased());

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, assetId, TbMsgMetaData.EMPTY, TbMsgDataType.JSON,TbMsg.EMPTY_JSON_OBJECT, ruleChainId, ruleNodeId);

        when(ctx.getAssetService()).thenReturn(assetService);
        when(assetService.findAssetByIdAsync(any(), eq(assetId))).thenReturn(Futures.immediateFuture(asset));

        node.onMsg(ctx, msg);
        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        verify(ctx).transformMsgOriginator(msgCaptor.capture(), originatorCaptor.capture());

        assertEquals(customerId, originatorCaptor.getValue());
    }

    @Test
    public void exceptionThrownIfCannotFindNewOriginator() {
        AssetId assetId = new AssetId(Uuids.timeBased());
        CustomerId customerId = new CustomerId(Uuids.timeBased());
        Asset asset = new Asset();
        asset.setCustomerId(customerId);

        RuleChainId ruleChainId = new RuleChainId(Uuids.timeBased());
        RuleNodeId ruleNodeId = new RuleNodeId(Uuids.timeBased());

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, assetId, TbMsgMetaData.EMPTY, TbMsgDataType.JSON,TbMsg.EMPTY_JSON_OBJECT, ruleChainId, ruleNodeId);

        when(ctx.getAssetService()).thenReturn(assetService);
        when(assetService.findAssetByIdAsync(any(), eq(assetId))).thenReturn(Futures.immediateFuture(null));

        ArgumentCaptor<NoSuchElementException> exceptionCaptor = ArgumentCaptor.forClass(NoSuchElementException.class);

        node.onMsg(ctx, msg);
        verify(ctx).tellFailure(same(msg), exceptionCaptor.capture());

        assertEquals("Failed to find new originator!", exceptionCaptor.getValue().getMessage());
    }

    public void init() throws TbNodeException {
        TbChangeOriginatorNodeConfiguration config = new TbChangeOriginatorNodeConfiguration();
        config.setOriginatorSource(CUSTOMER_SOURCE);
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        when(ctx.getDbCallbackExecutor()).thenReturn(dbExecutor);

        node = new TbChangeOriginatorNode();
        node.init(null, nodeConfiguration);
    }
}
