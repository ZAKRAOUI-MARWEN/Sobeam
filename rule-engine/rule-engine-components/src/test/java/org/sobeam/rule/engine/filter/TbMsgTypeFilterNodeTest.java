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
package org.sobeam.rule.engine.filter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sobeam.common.util.JacksonUtil;
import org.sobeam.rule.engine.api.TbContext;
import org.sobeam.rule.engine.api.TbNodeConfiguration;
import org.sobeam.rule.engine.api.TbNodeException;
import org.sobeam.server.common.data.id.DeviceId;
import org.sobeam.server.common.data.id.EntityId;
import org.sobeam.server.common.data.msg.TbMsgType;
import org.sobeam.server.common.data.msg.TbNodeConnectionType;
import org.sobeam.server.common.msg.TbMsg;
import org.sobeam.server.common.msg.TbMsgMetaData;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.sobeam.server.common.data.msg.TbMsgType.ATTRIBUTES_UPDATED;
import static org.sobeam.server.common.data.msg.TbMsgType.POST_ATTRIBUTES_REQUEST;

class TbMsgTypeFilterNodeTest {

    private DeviceId deviceId;
    private TbContext ctx;
    private TbMsgTypeFilterNode node;

    @BeforeEach
    void setUp() throws TbNodeException {
        ctx = mock(TbContext.class);
        var config = new TbMsgTypeFilterNodeConfiguration().defaultConfiguration();
        deviceId = new DeviceId(UUID.randomUUID());
        node = new TbMsgTypeFilterNode();
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
    }

    @AfterEach
    void tearDown() {
        node.destroy();
    }

    @Test
    void givenPostAttributes_whenOnMsg_then_True() {
        // GIVEN
        TbMsg msg = getTbMsg(deviceId, POST_ATTRIBUTES_REQUEST);

        // WHEN
        node.onMsg(ctx, msg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.TRUE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(msg);
    }

    @Test
    void givenAttributesUpdated_whenOnMsg_then_False() {
        // GIVEN
        TbMsg msg = getTbMsg(deviceId, ATTRIBUTES_UPDATED);

        // WHEN
        node.onMsg(ctx, msg);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.FALSE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(msg);
    }

    private TbMsg getTbMsg(EntityId entityId, TbMsgType msgType) {
        return TbMsg.newMsg(msgType, entityId, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_OBJECT);
    }

}
