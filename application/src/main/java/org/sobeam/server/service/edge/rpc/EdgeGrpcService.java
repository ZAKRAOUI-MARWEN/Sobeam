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
package org.sobeam.server.service.edge.rpc;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.sobeam.common.util.JacksonUtil;
import org.sobeam.common.util.SoBeamThreadFactory;
import org.sobeam.server.cluster.TbClusterService;
import org.sobeam.server.common.data.AttributeScope;
import org.sobeam.server.common.data.DataConstants;
import org.sobeam.server.common.data.ResourceUtils;
import org.sobeam.server.common.data.edge.Edge;
import org.sobeam.server.common.data.id.EdgeId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.kv.BasicTsKvEntry;
import org.sobeam.server.common.data.kv.BooleanDataEntry;
import org.sobeam.server.common.data.kv.LongDataEntry;
import org.sobeam.server.common.data.msg.TbMsgType;
import org.sobeam.server.common.data.notification.rule.trigger.EdgeConnectionTrigger;
import org.sobeam.server.common.msg.TbMsg;
import org.sobeam.server.common.msg.TbMsgDataType;
import org.sobeam.server.common.msg.TbMsgMetaData;
import org.sobeam.server.common.msg.edge.EdgeEventUpdateMsg;
import org.sobeam.server.common.msg.edge.EdgeSessionMsg;
import org.sobeam.server.common.msg.edge.FromEdgeSyncResponse;
import org.sobeam.server.common.msg.edge.ToEdgeSyncRequest;
import org.sobeam.server.gen.edge.v1.EdgeRpcServiceGrpc;
import org.sobeam.server.gen.edge.v1.RequestMsg;
import org.sobeam.server.gen.edge.v1.ResponseMsg;
import org.sobeam.server.queue.util.TbCoreComponent;
import org.sobeam.server.service.edge.EdgeContextComponent;
import org.sobeam.server.service.state.DefaultDeviceStateService;
import org.sobeam.server.service.telemetry.TelemetrySubscriptionService;

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "edges", value = "enabled", havingValue = "true")
@TbCoreComponent
public class EdgeGrpcService extends EdgeRpcServiceGrpc.EdgeRpcServiceImplBase implements EdgeRpcService {

    private final ConcurrentMap<EdgeId, EdgeGrpcSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<EdgeId, Lock> sessionNewEventsLocks = new ConcurrentHashMap<>();
    private final Map<EdgeId, Boolean> sessionNewEvents = new HashMap<>();
    private final ConcurrentMap<EdgeId, ScheduledFuture<?>> sessionEdgeEventChecks = new ConcurrentHashMap<>();

    private final ConcurrentMap<UUID, Consumer<FromEdgeSyncResponse>> localSyncEdgeRequests = new ConcurrentHashMap<>();

    @Value("${edges.rpc.port}")
    private int rpcPort;
    @Value("${edges.rpc.ssl.enabled}")
    private boolean sslEnabled;
    @Value("${edges.rpc.ssl.cert}")
    private String certFileResource;
    @Value("${edges.rpc.ssl.private_key}")
    private String privateKeyResource;
    @Value("${edges.state.persistToTelemetry:false}")
    private boolean persistToTelemetry;
    @Value("${edges.rpc.client_max_keep_alive_time_sec:1}")
    private int clientMaxKeepAliveTimeSec;
    @Value("${edges.rpc.max_inbound_message_size:4194304}")
    private int maxInboundMessageSize;
    @Value("${edges.rpc.keep_alive_time_sec:10}")
    private int keepAliveTimeSec;
    @Value("${edges.rpc.keep_alive_timeout_sec:5}")
    private int keepAliveTimeoutSec;
    @Value("${edges.scheduler_pool_size}")
    private int schedulerPoolSize;

    @Value("${edges.send_scheduler_pool_size}")
    private int sendSchedulerPoolSize;

    @Autowired
    private EdgeContextComponent ctx;

    @Autowired
    private TelemetrySubscriptionService tsSubService;

    @Autowired
    private TbClusterService clusterService;

    private Server server;

    private ScheduledExecutorService edgeEventProcessingExecutorService;

    private ScheduledExecutorService sendDownlinkExecutorService;

    private ScheduledExecutorService executorService;

    @PostConstruct
    public void init() {
        log.info("Initializing Edge RPC service!");
        NettyServerBuilder builder = NettyServerBuilder.forPort(rpcPort)
                .permitKeepAliveTime(clientMaxKeepAliveTimeSec, TimeUnit.SECONDS)
                .keepAliveTime(keepAliveTimeSec, TimeUnit.SECONDS)
                .keepAliveTimeout(keepAliveTimeoutSec, TimeUnit.SECONDS)
                .permitKeepAliveWithoutCalls(true)
                .maxInboundMessageSize(maxInboundMessageSize)
                .addService(this);
        if (sslEnabled) {
            try {
                InputStream certFileIs = ResourceUtils.getInputStream(this, certFileResource);
                InputStream privateKeyFileIs = ResourceUtils.getInputStream(this, privateKeyResource);
                builder.useTransportSecurity(certFileIs, privateKeyFileIs);
            } catch (Exception e) {
                log.error("Unable to set up SSL context. Reason: " + e.getMessage(), e);
                throw new RuntimeException("Unable to set up SSL context!", e);
            }
        }
        server = builder.build();
        log.info("Going to start Edge RPC server using port: {}", rpcPort);
        try {
            server.start();
        } catch (IOException e) {
            log.error("Failed to start Edge RPC server!", e);
            throw new RuntimeException("Failed to start Edge RPC server!");
        }
        this.edgeEventProcessingExecutorService = Executors.newScheduledThreadPool(schedulerPoolSize, SoBeamThreadFactory.forName("edge-event-check-scheduler"));
        this.sendDownlinkExecutorService = Executors.newScheduledThreadPool(sendSchedulerPoolSize, SoBeamThreadFactory.forName("edge-send-scheduler"));
        this.executorService = Executors.newSingleThreadScheduledExecutor(SoBeamThreadFactory.forName("edge-service"));
        log.info("Edge RPC service initialized!");
    }

    @PreDestroy
    public void destroy() {
        if (server != null) {
            server.shutdownNow();
        }
        for (Map.Entry<EdgeId, ScheduledFuture<?>> entry : sessionEdgeEventChecks.entrySet()) {
            EdgeId edgeId = entry.getKey();
            ScheduledFuture<?> sessionEdgeEventCheck = entry.getValue();
            if (sessionEdgeEventCheck != null && !sessionEdgeEventCheck.isCancelled() && !sessionEdgeEventCheck.isDone()) {
                sessionEdgeEventCheck.cancel(true);
                sessionEdgeEventChecks.remove(edgeId);
            }
        }
        if (edgeEventProcessingExecutorService != null) {
            edgeEventProcessingExecutorService.shutdownNow();
        }
        if (sendDownlinkExecutorService != null) {
            sendDownlinkExecutorService.shutdownNow();
        }
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @Override
    public StreamObserver<RequestMsg> handleMsgs(StreamObserver<ResponseMsg> outputStream) {
        return new EdgeGrpcSession(ctx, outputStream, this::onEdgeConnect, this::onEdgeDisconnect, sendDownlinkExecutorService, this.maxInboundMessageSize).getInputStream();
    }

    @Override
    public void onToEdgeSessionMsg(TenantId tenantId, EdgeSessionMsg msg) {
        executorService.execute(() -> {
            switch (msg.getMsgType()) {
                case EDGE_EVENT_UPDATE_TO_EDGE_SESSION_MSG:
                    EdgeEventUpdateMsg edgeEventUpdateMsg = (EdgeEventUpdateMsg) msg;
                    log.trace("[{}] onToEdgeSessionMsg [{}]", tenantId, msg);
                    onEdgeEvent(tenantId, edgeEventUpdateMsg.getEdgeId());
                    break;
                case EDGE_SYNC_REQUEST_TO_EDGE_SESSION_MSG:
                    ToEdgeSyncRequest toEdgeSyncRequest = (ToEdgeSyncRequest) msg;
                    log.trace("[{}] toEdgeSyncRequest [{}]", tenantId, msg);
                    startSyncProcess(tenantId, toEdgeSyncRequest.getEdgeId(), toEdgeSyncRequest.getId());
                    break;
                case EDGE_SYNC_RESPONSE_FROM_EDGE_SESSION_MSG:
                    FromEdgeSyncResponse fromEdgeSyncResponse = (FromEdgeSyncResponse) msg;
                    log.trace("[{}] fromEdgeSyncResponse [{}]", tenantId, msg);
                    processSyncResponse(fromEdgeSyncResponse);
                    break;
            }
        });
    }

    @Override
    public void updateEdge(TenantId tenantId, Edge edge) {
        executorService.execute(() -> {
            EdgeGrpcSession session = sessions.get(edge.getId());
            if (session != null && session.isConnected()) {
                log.debug("[{}] Updating configuration for edge [{}] [{}]", tenantId, edge.getName(), edge.getId());
                session.onConfigurationUpdate(edge);
            } else {
                log.debug("[{}] Session doesn't exist for edge [{}] [{}]", tenantId, edge.getName(), edge.getId());
            }
        });
    }

    @Override
    public void deleteEdge(TenantId tenantId, EdgeId edgeId) {
        executorService.execute(() -> {
            EdgeGrpcSession session = sessions.get(edgeId);
            if (session != null && session.isConnected()) {
                log.info("[{}] Closing and removing session for edge [{}]", tenantId, edgeId);
                session.close();
                sessions.remove(edgeId);
                final Lock newEventLock = sessionNewEventsLocks.computeIfAbsent(edgeId, id -> new ReentrantLock());
                newEventLock.lock();
                try {
                    sessionNewEvents.remove(edgeId);
                } finally {
                    newEventLock.unlock();
                }
                cancelScheduleEdgeEventsCheck(edgeId);
            }
        });
    }

    private void onEdgeEvent(TenantId tenantId, EdgeId edgeId) {
        EdgeGrpcSession session = sessions.get(edgeId);
        if (session != null && session.isConnected()) {
            log.trace("[{}] onEdgeEvent [{}]", tenantId, edgeId.getId());
            final Lock newEventLock = sessionNewEventsLocks.computeIfAbsent(edgeId, id -> new ReentrantLock());
            newEventLock.lock();
            try {
                if (Boolean.FALSE.equals(sessionNewEvents.get(edgeId))) {
                    log.trace("[{}] set session new events flag to true [{}]", tenantId, edgeId.getId());
                    sessionNewEvents.put(edgeId, true);
                }
            } finally {
                newEventLock.unlock();
            }
        }
    }

    private void onEdgeConnect(EdgeId edgeId, EdgeGrpcSession edgeGrpcSession) {
        Edge edge = edgeGrpcSession.getEdge();
        TenantId tenantId = edge.getTenantId();
        log.info("[{}][{}] edge [{}] connected successfully.", tenantId, edgeGrpcSession.getSessionId(), edgeId);
        sessions.put(edgeId, edgeGrpcSession);
        final Lock newEventLock = sessionNewEventsLocks.computeIfAbsent(edgeId, id -> new ReentrantLock());
        newEventLock.lock();
        try {
            sessionNewEvents.put(edgeId, true);
        } finally {
            newEventLock.unlock();
        }
        save(tenantId, edgeId, DefaultDeviceStateService.ACTIVITY_STATE, true);
        long lastConnectTs = System.currentTimeMillis();
        save(tenantId, edgeId, DefaultDeviceStateService.LAST_CONNECT_TIME, lastConnectTs);
        pushRuleEngineMessage(tenantId, edge, lastConnectTs, TbMsgType.CONNECT_EVENT);
        cancelScheduleEdgeEventsCheck(edgeId);
        scheduleEdgeEventsCheck(edgeGrpcSession);
    }

    private void startSyncProcess(TenantId tenantId, EdgeId edgeId, UUID requestId) {
        EdgeGrpcSession session = sessions.get(edgeId);
        if (session != null) {
            boolean success = false;
            if (session.isConnected()) {
                session.startSyncProcess(true);
                success = true;
            }
            clusterService.pushEdgeSyncResponseToCore(new FromEdgeSyncResponse(requestId, tenantId, edgeId, success));
        }
    }

    @Override
    public void processSyncRequest(ToEdgeSyncRequest request, Consumer<FromEdgeSyncResponse> responseConsumer) {
        log.trace("[{}][{}] Processing sync edge request [{}]", request.getTenantId(), request.getId(), request.getEdgeId());
        UUID requestId = request.getId();
        localSyncEdgeRequests.put(requestId, responseConsumer);
        clusterService.pushEdgeSyncRequestToCore(request);
        scheduleSyncRequestTimeout(request, requestId);
    }

    private void scheduleSyncRequestTimeout(ToEdgeSyncRequest request, UUID requestId) {
        log.trace("[{}] scheduling sync edge request", requestId);
        executorService.schedule(() -> {
            log.trace("[{}] checking if sync edge request is not processed...", requestId);
            Consumer<FromEdgeSyncResponse> consumer = localSyncEdgeRequests.remove(requestId);
            if (consumer != null) {
                log.trace("[{}] timeout for processing sync edge request.", requestId);
                consumer.accept(new FromEdgeSyncResponse(requestId, request.getTenantId(), request.getEdgeId(), false));
            }
        }, 20, TimeUnit.SECONDS);
    }

    private void processSyncResponse(FromEdgeSyncResponse response) {
        log.trace("[{}] Received response from sync service: [{}]", response.getId(), response);
        UUID requestId = response.getId();
        Consumer<FromEdgeSyncResponse> consumer = localSyncEdgeRequests.remove(requestId);
        if (consumer != null) {
            consumer.accept(response);
        } else {
            log.trace("[{}] Unknown or stale sync response received [{}]", requestId, response);
        }
    }

    private void scheduleEdgeEventsCheck(EdgeGrpcSession session) {
        EdgeId edgeId = session.getEdge().getId();
        UUID tenantId = session.getEdge().getTenantId().getId();
        if (sessions.containsKey(edgeId)) {
            ScheduledFuture<?> edgeEventCheckTask = edgeEventProcessingExecutorService.schedule(() -> {
                try {
                    final Lock newEventLock = sessionNewEventsLocks.computeIfAbsent(edgeId, id -> new ReentrantLock());
                    newEventLock.lock();
                    try {
                        if (Boolean.TRUE.equals(sessionNewEvents.get(edgeId))) {
                            log.trace("[{}][{}] Set session new events flag to false", tenantId, edgeId.getId());
                            sessionNewEvents.put(edgeId, false);
                            Futures.addCallback(session.processEdgeEvents(), new FutureCallback<>() {
                                @Override
                                public void onSuccess(Boolean newEventsAdded) {
                                    if (Boolean.TRUE.equals(newEventsAdded)) {
                                        sessionNewEvents.put(edgeId, true);
                                    }
                                    scheduleEdgeEventsCheck(session);
                                }

                                @Override
                                public void onFailure(Throwable t) {
                                    log.warn("[{}] Failed to process edge events for edge [{}]!", tenantId, session.getEdge().getId().getId(), t);
                                    scheduleEdgeEventsCheck(session);
                                }
                            }, ctx.getGrpcCallbackExecutorService());
                        } else {
                            scheduleEdgeEventsCheck(session);
                        }
                    } finally {
                        newEventLock.unlock();
                    }
                } catch (Exception e) {
                    log.warn("[{}] Failed to process edge events for edge [{}]!", tenantId, session.getEdge().getId().getId(), e);
                }
            }, ctx.getEdgeEventStorageSettings().getNoRecordsSleepInterval(), TimeUnit.MILLISECONDS);
            sessionEdgeEventChecks.put(edgeId, edgeEventCheckTask);
            log.trace("[{}] Check edge event scheduled for edge [{}]", tenantId, edgeId.getId());
        } else {
            log.debug("[{}] Session was removed and edge event check schedule must not be started [{}]",
                    tenantId, edgeId.getId());
        }
    }

    private void cancelScheduleEdgeEventsCheck(EdgeId edgeId) {
        log.trace("[{}] cancelling edge event check for edge", edgeId);
        if (sessionEdgeEventChecks.containsKey(edgeId)) {
            ScheduledFuture<?> sessionEdgeEventCheck = sessionEdgeEventChecks.get(edgeId);
            if (sessionEdgeEventCheck != null && !sessionEdgeEventCheck.isCancelled() && !sessionEdgeEventCheck.isDone()) {
                sessionEdgeEventCheck.cancel(true);
                sessionEdgeEventChecks.remove(edgeId);
            }
        }
    }

    private void onEdgeDisconnect(Edge edge, UUID sessionId) {
        EdgeId edgeId = edge.getId();
        log.info("[{}][{}] edge disconnected!", edgeId, sessionId);
        EdgeGrpcSession toRemove = sessions.get(edgeId);
        if (toRemove.getSessionId().equals(sessionId)) {
            toRemove = sessions.remove(edgeId);
            final Lock newEventLock = sessionNewEventsLocks.computeIfAbsent(edgeId, id -> new ReentrantLock());
            newEventLock.lock();
            try {
                sessionNewEvents.remove(edgeId);
            } finally {
                newEventLock.unlock();
            }
            TenantId tenantId = toRemove.getEdge().getTenantId();
            save(tenantId, edgeId, DefaultDeviceStateService.ACTIVITY_STATE, false);
            long lastDisconnectTs = System.currentTimeMillis();
            save(tenantId, edgeId, DefaultDeviceStateService.LAST_DISCONNECT_TIME, lastDisconnectTs);
            pushRuleEngineMessage(toRemove.getEdge().getTenantId(), edge, lastDisconnectTs, TbMsgType.DISCONNECT_EVENT);
            cancelScheduleEdgeEventsCheck(edgeId);
        } else {
            log.debug("[{}] edge session [{}] is not available anymore, nothing to remove. most probably this session is already outdated!", edgeId, sessionId);
        }
    }

    private void save(TenantId tenantId, EdgeId edgeId, String key, long value) {
        log.debug("[{}][{}] Updating long edge telemetry [{}] [{}]", tenantId, edgeId, key, value);
        if (persistToTelemetry) {
            tsSubService.saveAndNotify(
                    tenantId, edgeId,
                    Collections.singletonList(new BasicTsKvEntry(System.currentTimeMillis(), new LongDataEntry(key, value))),
                    new AttributeSaveCallback(tenantId, edgeId, key, value));
        } else {
            tsSubService.saveAttrAndNotify(tenantId, edgeId, AttributeScope.SERVER_SCOPE, key, value, new AttributeSaveCallback(tenantId, edgeId, key, value));
        }
    }

    private void save(TenantId tenantId, EdgeId edgeId, String key, boolean value) {
        log.debug("[{}][{}] Updating boolean edge telemetry [{}] [{}]", tenantId, edgeId, key, value);
        if (persistToTelemetry) {
            tsSubService.saveAndNotify(
                    tenantId, edgeId,
                    Collections.singletonList(new BasicTsKvEntry(System.currentTimeMillis(), new BooleanDataEntry(key, value))),
                    new AttributeSaveCallback(tenantId, edgeId, key, value));
        } else {
            tsSubService.saveAttrAndNotify(tenantId, edgeId, AttributeScope.SERVER_SCOPE, key, value, new AttributeSaveCallback(tenantId, edgeId, key, value));
        }
    }

    private static class AttributeSaveCallback implements FutureCallback<Void> {
        private final TenantId tenantId;
        private final EdgeId edgeId;
        private final String key;
        private final Object value;

        AttributeSaveCallback(TenantId tenantId, EdgeId edgeId, String key, Object value) {
            this.tenantId = tenantId;
            this.edgeId = edgeId;
            this.key = key;
            this.value = value;
        }

        @Override
        public void onSuccess(@Nullable Void result) {
            log.trace("[{}][{}] Successfully updated attribute [{}] with value [{}]", tenantId, edgeId, key, value);
        }

        @Override
        public void onFailure(Throwable t) {
            log.warn("[{}][{}] Failed to update attribute [{}] with value [{}]", tenantId, edgeId, key, value, t);
        }
    }

    private void pushRuleEngineMessage(TenantId tenantId, Edge edge, long ts, TbMsgType msgType) {
        try {
            EdgeId edgeId = edge.getId();
            ObjectNode edgeState = JacksonUtil.newObjectNode();
            boolean isConnected = TbMsgType.CONNECT_EVENT.equals(msgType);
            if (isConnected) {
                edgeState.put(DefaultDeviceStateService.ACTIVITY_STATE, true);
                edgeState.put(DefaultDeviceStateService.LAST_CONNECT_TIME, ts);
            } else {
                edgeState.put(DefaultDeviceStateService.ACTIVITY_STATE, false);
                edgeState.put(DefaultDeviceStateService.LAST_DISCONNECT_TIME, ts);
            }
            ctx.getNotificationRuleProcessor().process(EdgeConnectionTrigger.builder()
                    .tenantId(tenantId)
                    .customerId(edge.getCustomerId())
                    .edgeId(edgeId)
                    .edgeName(edge.getName())
                    .connected(isConnected).build());
            String data = JacksonUtil.toString(edgeState);
            TbMsgMetaData md = new TbMsgMetaData();
            if (!persistToTelemetry) {
                md.putValue(DataConstants.SCOPE, DataConstants.SERVER_SCOPE);
                md.putValue("edgeName", edge.getName());
                md.putValue("edgeType", edge.getType());
            }
            TbMsg tbMsg = TbMsg.newMsg(msgType, edgeId, md, TbMsgDataType.JSON, data);
            clusterService.pushMsgToRuleEngine(tenantId, edgeId, tbMsg, null);
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push {}", tenantId, edge.getId(), msgType, e);
        }
    }

}
