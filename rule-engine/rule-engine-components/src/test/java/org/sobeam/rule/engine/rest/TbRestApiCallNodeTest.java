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
package org.sobeam.rule.engine.rest;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sobeam.common.util.JacksonUtil;
import org.sobeam.rule.engine.api.TbContext;
import org.sobeam.rule.engine.api.TbNodeConfiguration;
import org.sobeam.rule.engine.api.TbNodeException;
import org.sobeam.server.common.data.id.DeviceId;
import org.sobeam.server.common.data.id.EntityId;
import org.sobeam.server.common.data.id.RuleChainId;
import org.sobeam.server.common.data.id.RuleNodeId;
import org.sobeam.server.common.data.msg.TbMsgType;
import org.sobeam.server.common.data.util.TbPair;
import org.sobeam.server.common.msg.TbMsg;
import org.sobeam.server.common.msg.TbMsgDataType;
import org.sobeam.server.common.msg.TbMsgMetaData;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TbRestApiCallNodeTest {

    private TbRestApiCallNode restNode;

    @Mock
    private TbContext ctx;

    private EntityId originator = new DeviceId(Uuids.timeBased());
    private TbMsgMetaData metaData = new TbMsgMetaData();

    private RuleChainId ruleChainId = new RuleChainId(Uuids.timeBased());
    private RuleNodeId ruleNodeId = new RuleNodeId(Uuids.timeBased());

    private HttpServer server;

    public void setupServer(String pattern, HttpRequestHandler handler) throws IOException {
        SocketConfig config = SocketConfig.custom().setSoReuseAddress(true).setTcpNoDelay(true).build();
        server = ServerBootstrap.bootstrap()
                .setSocketConfig(config)
                .registerHandler(pattern, handler)
                .create();
        server.start();
    }

    private void initWithConfig(TbRestApiCallNodeConfiguration config) {
        try {
            TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
            restNode = new TbRestApiCallNode();
            restNode.init(ctx, nodeConfiguration);
        } catch (TbNodeException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @AfterEach
    public void teardown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void deleteRequestWithoutBody() throws IOException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final String path = "/path/to/delete";
        setupServer("*", new HttpRequestHandler() {

            @Override
            public void handle(HttpRequest request, HttpResponse response, HttpContext context)
                    throws HttpException, IOException {
                try {
                    assertEquals(request.getRequestLine().getUri(), path, "Request path matches");
                    assertTrue(request.containsHeader("Foo"), "Custom header included");
                    assertEquals("Bar", request.getFirstHeader("Foo").getValue(), "Custom header value");
                    response.setStatusCode(200);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(1000L);
                            } catch (InterruptedException e) {
                                // ignore
                            } finally {
                                latch.countDown();
                            }
                        }
                    }).start();
                } catch (Exception e) {
                    System.out.println("Exception handling request: " + e.toString());
                    e.printStackTrace();
                    latch.countDown();
                }
            }
        });

        TbRestApiCallNodeConfiguration config = new TbRestApiCallNodeConfiguration().defaultConfiguration();
        config.setRequestMethod("DELETE");
        config.setHeaders(Collections.singletonMap("Foo", "Bar"));
        config.setIgnoreRequestBody(true);
        config.setRestEndpointUrlPattern(String.format("http://localhost:%d%s", server.getLocalPort(), path));
        initWithConfig(config);

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, metaData, TbMsgDataType.JSON, TbMsg.EMPTY_JSON_OBJECT, ruleChainId, ruleNodeId);
        restNode.onMsg(ctx, msg);

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Server handled request");

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertNotSame(metaData, metadataCaptor.getValue());
        assertEquals(TbMsg.EMPTY_JSON_OBJECT, dataCaptor.getValue());
    }

    @Test
    public void deleteRequestWithBody() throws IOException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final String path = "/path/to/delete";
        setupServer("*", new HttpRequestHandler() {

            @Override
            public void handle(HttpRequest request, HttpResponse response, HttpContext context)
                    throws HttpException, IOException {
                try {
                    assertEquals(path, request.getRequestLine().getUri(), "Request path matches");
                    assertTrue(request.containsHeader("Content-Type"), "Content-Type included");
                    assertEquals("text/plain;charset=UTF-8",
                            request.getFirstHeader("Content-Type").getValue(), "Content-Type value");
                    assertTrue(request.containsHeader("Content-Length"), "Content-Length included");
                    assertEquals("2",
                            request.getFirstHeader("Content-Length").getValue(), "Content-Length value");
                    assertTrue(request.containsHeader("Foo"), "Custom header included");
                    assertEquals("Bar", request.getFirstHeader("Foo").getValue(), "Custom header value");
                    response.setStatusCode(200);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(1000L);
                            } catch (InterruptedException e) {
                                // ignore
                            } finally {
                                latch.countDown();
                            }
                        }
                    }).start();
                } catch (Exception e) {
                    System.out.println("Exception handling request: " + e.toString());
                    e.printStackTrace();
                    latch.countDown();
                }
            }
        });

        TbRestApiCallNodeConfiguration config = new TbRestApiCallNodeConfiguration().defaultConfiguration();
        config.setRequestMethod("DELETE");
        config.setHeaders(Collections.singletonMap("Foo", "Bar"));
        config.setIgnoreRequestBody(false);
        config.setRestEndpointUrlPattern(String.format("http://localhost:%d%s", server.getLocalPort(), path));
        initWithConfig(config);

        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, metaData, TbMsgDataType.JSON, TbMsg.EMPTY_JSON_OBJECT, ruleChainId, ruleNodeId);
        restNode.onMsg(ctx, msg);

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Server handled request");

        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertNotSame(metaData, metadataCaptor.getValue());
        assertEquals(TbMsg.EMPTY_JSON_OBJECT, dataCaptor.getValue());
    }

    @Test
    public void givenOldConfig_whenUpgrade_thenShouldReturnTrueResultWithNewConfig() throws Exception {
        var defaultConfig = new TbRestApiCallNodeConfiguration().defaultConfiguration();
        var node = new TbRestApiCallNode();
        String oldConfig = "{\"restEndpointUrlPattern\":\"http://localhost/api\",\"requestMethod\":\"POST\"," +
                "\"useSimpleClientHttpFactory\":false,\"ignoreRequestBody\":false,\"enableProxy\":false," +
                "\"useSystemProxyProperties\":false,\"proxyScheme\":null,\"proxyHost\":null,\"proxyPort\":0," +
                "\"proxyUser\":null,\"proxyPassword\":null,\"readTimeoutMs\":0,\"maxParallelRequestsCount\":0," +
                "\"headers\":{\"Content-Type\":\"application/json\"},\"useRedisQueueForMsgPersistence\":false," +
                "\"trimQueue\":null,\"maxQueueSize\":null,\"credentials\":{\"type\":\"anonymous\"},\"trimDoubleQuotes\":true}";
        JsonNode configJson = JacksonUtil.toJsonNode(oldConfig);
        TbPair<Boolean, JsonNode> upgrade = node.upgrade(0, configJson);
        Assertions.assertTrue(upgrade.getFirst());
        Assertions.assertTrue(JacksonUtil.treeToValue(upgrade.getSecond(), defaultConfig.getClass()).isParseToPlainText());
    }

}
