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

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sobeam.common.util.JacksonUtil;
import org.sobeam.rule.engine.api.ScriptEngine;
import org.sobeam.rule.engine.api.TbContext;
import org.sobeam.rule.engine.api.TbNodeConfiguration;
import org.sobeam.rule.engine.api.TbNodeException;
import org.sobeam.server.common.data.id.RuleChainId;
import org.sobeam.server.common.data.id.RuleNodeId;
import org.sobeam.server.common.data.msg.TbMsgType;
import org.sobeam.server.common.data.msg.TbNodeConnectionType;
import org.sobeam.server.common.data.script.ScriptLanguage;
import org.sobeam.server.common.msg.TbMsg;
import org.sobeam.server.common.msg.TbMsgDataType;
import org.sobeam.server.common.msg.TbMsgMetaData;

import javax.script.ScriptException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TbJsFilterNodeTest {

    private TbJsFilterNode node;

    @Mock
    private TbContext ctx;
    @Mock
    private ScriptEngine scriptEngine;

    private final RuleChainId ruleChainId = new RuleChainId(Uuids.timeBased());
    private final RuleNodeId ruleNodeId = new RuleNodeId(Uuids.timeBased());

    @Test
    public void falseEvaluationDoNotSendMsg() throws TbNodeException {
        initWithScript();
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, null, TbMsgMetaData.EMPTY, TbMsgDataType.JSON, TbMsg.EMPTY_JSON_OBJECT, ruleChainId, ruleNodeId);
        when(scriptEngine.executeFilterAsync(msg)).thenReturn(Futures.immediateFuture(false));

        node.onMsg(ctx, msg);
        verify(ctx).getDbCallbackExecutor();
        verify(ctx).tellNext(msg, TbNodeConnectionType.FALSE);
    }

    @Test
    public void exceptionInJsThrowsException() throws TbNodeException {
        initWithScript();
        TbMsgMetaData metaData = new TbMsgMetaData();
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, null, metaData, TbMsgDataType.JSON, TbMsg.EMPTY_JSON_OBJECT, ruleChainId, ruleNodeId);
        when(scriptEngine.executeFilterAsync(msg)).thenReturn(Futures.immediateFailedFuture(new ScriptException("error")));


        node.onMsg(ctx, msg);
        verifyError(msg, "error", ScriptException.class);
    }

    @Test
    public void metadataConditionCanBeTrue() throws TbNodeException {
        initWithScript();
        TbMsgMetaData metaData = new TbMsgMetaData();
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, null, metaData, TbMsgDataType.JSON, TbMsg.EMPTY_JSON_OBJECT, ruleChainId, ruleNodeId);
        when(scriptEngine.executeFilterAsync(msg)).thenReturn(Futures.immediateFuture(true));

        node.onMsg(ctx, msg);
        verify(ctx).getDbCallbackExecutor();
        verify(ctx).tellNext(msg, TbNodeConnectionType.TRUE);
    }

    private void initWithScript() throws TbNodeException {
        TbJsFilterNodeConfiguration config = new TbJsFilterNodeConfiguration();
        config.setScriptLang(ScriptLanguage.JS);
        config.setJsScript("scr");
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        when(ctx.createScriptEngine(ScriptLanguage.JS, "scr")).thenReturn(scriptEngine);

        node = new TbJsFilterNode();
        node.init(ctx, nodeConfiguration);
    }

    private void verifyError(TbMsg msg, String message, Class expectedClass) {
        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctx).tellFailure(same(msg), captor.capture());

        Throwable value = captor.getValue();
        assertEquals(expectedClass, value.getClass());
        assertEquals(message, value.getMessage());
    }
}