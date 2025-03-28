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
package org.sobeam.rule.engine.rpc;

import com.google.common.util.concurrent.SettableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sobeam.common.util.JacksonUtil;
import org.sobeam.common.util.ListeningExecutor;
import org.sobeam.rule.engine.api.RuleEngineRpcService;
import org.sobeam.rule.engine.api.TbContext;
import org.sobeam.rule.engine.api.TbNodeConfiguration;
import org.sobeam.rule.engine.api.TbNodeException;
import org.sobeam.server.common.data.DataConstants;
import org.sobeam.server.common.data.id.DeviceId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.msg.TbMsgType;
import org.sobeam.server.common.msg.TbMsg;
import org.sobeam.server.common.msg.TbMsgDataType;
import org.sobeam.server.common.msg.TbMsgMetaData;
import org.sobeam.server.dao.edge.EdgeEventService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TbSendRPCReplyNodeTest {

    private static final String DUMMY_SERVICE_ID = "testServiceId";
    private static final int DUMMY_REQUEST_ID = 0;
    private static final UUID DUMMY_SESSION_ID = UUID.randomUUID();
    private static final String DUMMY_DATA = "{\"key\":\"value\"}";

    TbSendRPCReplyNode node;

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    private final DeviceId deviceId = new DeviceId(UUID.randomUUID());

    @Mock
    private TbContext ctx;

    @Mock
    private RuleEngineRpcService rpcService;

    @Mock
    private EdgeEventService edgeEventService;

    @Mock
    private ListeningExecutor listeningExecutor;

    @BeforeEach
    public void setUp() throws TbNodeException {
        node = new TbSendRPCReplyNode();
        TbSendRpcReplyNodeConfiguration config = new TbSendRpcReplyNodeConfiguration().defaultConfiguration();
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
    }

    @Test
    public void sendReplyToTransport() {
        Mockito.when(ctx.getRpcService()).thenReturn(rpcService);


        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, deviceId, getDefaultMetadata(),
                TbMsgDataType.JSON, DUMMY_DATA, null, null);

        node.onMsg(ctx, msg);

        verify(rpcService).sendRpcReplyToDevice(DUMMY_SERVICE_ID, DUMMY_SESSION_ID, DUMMY_REQUEST_ID, DUMMY_DATA);
        verify(edgeEventService, never()).saveAsync(any());
    }

    @Test
    public void sendReplyToEdgeQueue() {
        Mockito.when(ctx.getTenantId()).thenReturn(tenantId);
        Mockito.when(ctx.getEdgeEventService()).thenReturn(edgeEventService);
        Mockito.when(edgeEventService.saveAsync(any())).thenReturn(SettableFuture.create());
        Mockito.when(ctx.getDbCallbackExecutor()).thenReturn(listeningExecutor);

        TbMsgMetaData defaultMetadata = getDefaultMetadata();
        defaultMetadata.putValue(DataConstants.EDGE_ID, UUID.randomUUID().toString());
        defaultMetadata.putValue(DataConstants.DEVICE_ID, UUID.randomUUID().toString());
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, deviceId, defaultMetadata,
                TbMsgDataType.JSON, DUMMY_DATA, null, null);

        node.onMsg(ctx, msg);

        verify(edgeEventService).saveAsync(any());
        verify(rpcService, never()).sendRpcReplyToDevice(DUMMY_SERVICE_ID, DUMMY_SESSION_ID, DUMMY_REQUEST_ID, DUMMY_DATA);
    }

    private TbMsgMetaData getDefaultMetadata() {
        TbSendRpcReplyNodeConfiguration config = new TbSendRpcReplyNodeConfiguration().defaultConfiguration();
        TbMsgMetaData metadata = new TbMsgMetaData();
        metadata.putValue(config.getServiceIdMetaDataAttribute(), DUMMY_SERVICE_ID);
        metadata.putValue(config.getSessionIdMetaDataAttribute(), DUMMY_SESSION_ID.toString());
        metadata.putValue(config.getRequestIdMetaDataAttribute(), Integer.toString(DUMMY_REQUEST_ID));
        return metadata;
    }
}
