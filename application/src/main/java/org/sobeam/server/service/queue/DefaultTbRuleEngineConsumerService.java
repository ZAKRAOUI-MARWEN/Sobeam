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
package org.sobeam.server.service.queue;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.sobeam.server.actors.ActorSystemContext;
import org.sobeam.server.common.data.EntityType;
import org.sobeam.server.common.data.id.QueueId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.plugin.ComponentLifecycleEvent;
import org.sobeam.server.common.data.queue.Queue;
import org.sobeam.server.common.data.rpc.RpcError;
import org.sobeam.server.common.msg.plugin.ComponentLifecycleMsg;
import org.sobeam.server.common.msg.queue.ServiceType;
import org.sobeam.server.common.msg.queue.TbCallback;
import org.sobeam.server.common.msg.rpc.FromDeviceRpcResponse;
import org.sobeam.server.common.util.ProtoUtils;
import org.sobeam.server.dao.queue.QueueService;
import org.sobeam.server.dao.tenant.TbTenantProfileCache;
import org.sobeam.server.gen.transport.TransportProtos;
import org.sobeam.server.gen.transport.TransportProtos.QueueDeleteMsg;
import org.sobeam.server.gen.transport.TransportProtos.QueueUpdateMsg;
import org.sobeam.server.gen.transport.TransportProtos.ToRuleEngineNotificationMsg;
import org.sobeam.server.queue.TbQueueConsumer;
import org.sobeam.server.queue.common.TbProtoQueueMsg;
import org.sobeam.server.queue.discovery.PartitionService;
import org.sobeam.server.queue.discovery.QueueKey;
import org.sobeam.server.queue.discovery.event.PartitionChangeEvent;
import org.sobeam.server.queue.util.TbRuleEngineComponent;
import org.sobeam.server.service.apiusage.TbApiUsageStateService;
import org.sobeam.server.service.profile.TbAssetProfileCache;
import org.sobeam.server.service.profile.TbDeviceProfileCache;
import org.sobeam.server.service.queue.processing.AbstractConsumerService;
import org.sobeam.server.service.queue.ruleengine.TbRuleEngineConsumerContext;
import org.sobeam.server.service.queue.ruleengine.TbRuleEngineQueueConsumerManager;
import org.sobeam.server.service.rpc.TbRuleEngineDeviceRpcService;
import org.sobeam.server.service.security.auth.jwt.settings.JwtSettingsService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
@TbRuleEngineComponent
@Slf4j
public class DefaultTbRuleEngineConsumerService extends AbstractConsumerService<ToRuleEngineNotificationMsg> implements TbRuleEngineConsumerService {

    private final TbRuleEngineConsumerContext ctx;
    private final QueueService queueService;
    private final TbRuleEngineDeviceRpcService tbDeviceRpcService;

    private final ConcurrentMap<QueueKey, TbRuleEngineQueueConsumerManager> consumers = new ConcurrentHashMap<>();

    public DefaultTbRuleEngineConsumerService(TbRuleEngineConsumerContext ctx,
                                              ActorSystemContext actorContext,
                                              TbRuleEngineDeviceRpcService tbDeviceRpcService,
                                              QueueService queueService,
                                              TbDeviceProfileCache deviceProfileCache,
                                              TbAssetProfileCache assetProfileCache,
                                              TbTenantProfileCache tenantProfileCache,
                                              TbApiUsageStateService apiUsageStateService,
                                              PartitionService partitionService,
                                              ApplicationEventPublisher eventPublisher,
                                              JwtSettingsService jwtSettingsService) {
        super(actorContext, tenantProfileCache, deviceProfileCache, assetProfileCache, apiUsageStateService, partitionService, eventPublisher, jwtSettingsService);
        this.ctx = ctx;
        this.tbDeviceRpcService = tbDeviceRpcService;
        this.queueService = queueService;
    }

    @PostConstruct
    public void init() {
        super.init("tb-rule-engine");
        List<Queue> queues = queueService.findAllQueues();
        for (Queue configuration : queues) {
            if (partitionService.isManagedByCurrentService(configuration.getTenantId())) {
                QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, configuration);
                createConsumer(queueKey, configuration);
            }
        }
    }

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent event) {
        event.getPartitionsMap().forEach((queueKey, partitions) -> {
            if (partitionService.isManagedByCurrentService(queueKey.getTenantId())) {
                var consumer = getConsumer(queueKey).orElseGet(() -> {
                    Queue config = queueService.findQueueByTenantIdAndName(queueKey.getTenantId(), queueKey.getQueueName());
                    if (config == null) {
                        if (!partitions.isEmpty()) {
                            log.error("[{}] Queue configuration is missing", queueKey, new RuntimeException("stacktrace"));
                        }
                        return null;
                    }
                    return createConsumer(queueKey, config);
                });
                if (consumer != null) {
                    consumer.update(partitions);
                }
            }
        });
        consumers.keySet().stream()
                .collect(Collectors.groupingBy(QueueKey::getTenantId))
                .forEach((tenantId, queueKeys) -> {
                    if (!partitionService.isManagedByCurrentService(tenantId)) {
                        queueKeys.forEach(queueKey -> {
                            removeConsumer(queueKey).ifPresent(TbRuleEngineQueueConsumerManager::stop);
                        });
                    }
                });
    }

    @Override
    protected void stopConsumers() {
        super.stopConsumers();
        consumers.values().forEach(TbRuleEngineQueueConsumerManager::stop);
        consumers.values().forEach(TbRuleEngineQueueConsumerManager::awaitStop);
    }

    @Override
    protected ServiceType getServiceType() {
        return ServiceType.TB_RULE_ENGINE;
    }

    @Override
    protected long getNotificationPollDuration() {
        return ctx.getPollDuration();
    }

    @Override
    protected long getNotificationPackProcessingTimeout() {
        return ctx.getPackProcessingTimeout();
    }

    @Override
    protected int getMgmtThreadPoolSize() {
        return ctx.getMgmtThreadPoolSize();
    }

    @Override
    protected TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> createNotificationsConsumer() {
        return ctx.getQueueFactory().createToRuleEngineNotificationsMsgConsumer();
    }

    @Override
    protected void handleNotification(UUID id, TbProtoQueueMsg<ToRuleEngineNotificationMsg> msg, TbCallback callback) throws Exception {
        ToRuleEngineNotificationMsg nfMsg = msg.getValue();
        if (nfMsg.hasComponentLifecycle()) {
            handleComponentLifecycleMsg(id, ProtoUtils.fromProto(nfMsg.getComponentLifecycle()));
            callback.onSuccess();
        } else if (nfMsg.hasFromDeviceRpcResponse()) {
            TransportProtos.FromDeviceRPCResponseProto proto = nfMsg.getFromDeviceRpcResponse();
            RpcError error = proto.getError() > 0 ? RpcError.values()[proto.getError()] : null;
            FromDeviceRpcResponse response = new FromDeviceRpcResponse(new UUID(proto.getRequestIdMSB(), proto.getRequestIdLSB())
                    , proto.getResponse(), error);
            tbDeviceRpcService.processRpcResponseFromDevice(response);
            callback.onSuccess();
        } else if (nfMsg.getQueueUpdateMsgsCount() > 0) {
            updateQueues(nfMsg.getQueueUpdateMsgsList());
            callback.onSuccess();
        } else if (nfMsg.getQueueDeleteMsgsCount() > 0) {
            deleteQueues(nfMsg.getQueueDeleteMsgsList());
            callback.onSuccess();
        } else {
            log.trace("Received notification with missing handler");
            callback.onSuccess();
        }
    }

    private void updateQueues(List<QueueUpdateMsg> queueUpdateMsgs) {
        for (QueueUpdateMsg queueUpdateMsg : queueUpdateMsgs) {
            log.info("Received queue update msg: [{}]", queueUpdateMsg);
            TenantId tenantId = new TenantId(new UUID(queueUpdateMsg.getTenantIdMSB(), queueUpdateMsg.getTenantIdLSB()));
            if (partitionService.isManagedByCurrentService(tenantId)) {
                QueueId queueId = new QueueId(new UUID(queueUpdateMsg.getQueueIdMSB(), queueUpdateMsg.getQueueIdLSB()));
                String queueName = queueUpdateMsg.getQueueName();
                QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, queueName, tenantId);
                Queue queue = queueService.findQueueById(tenantId, queueId);

                getConsumer(queueKey).ifPresentOrElse(consumer -> consumer.update(queue),
                        () -> createConsumer(queueKey, queue));
            }
        }

        partitionService.updateQueues(queueUpdateMsgs);
        partitionService.recalculatePartitions(ctx.getServiceInfoProvider().getServiceInfo(),
                new ArrayList<>(partitionService.getOtherServices(ServiceType.TB_RULE_ENGINE)));
    }

    private void deleteQueues(List<QueueDeleteMsg> queueDeleteMsgs) {
        for (QueueDeleteMsg queueDeleteMsg : queueDeleteMsgs) {
            log.info("Received queue delete msg: [{}]", queueDeleteMsg);
            TenantId tenantId = new TenantId(new UUID(queueDeleteMsg.getTenantIdMSB(), queueDeleteMsg.getTenantIdLSB()));
            QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, queueDeleteMsg.getQueueName(), tenantId);
            removeConsumer(queueKey).ifPresent(consumer -> consumer.delete(true));
        }

        partitionService.removeQueues(queueDeleteMsgs);
        partitionService.recalculatePartitions(ctx.getServiceInfoProvider().getServiceInfo(), new ArrayList<>(partitionService.getOtherServices(ServiceType.TB_RULE_ENGINE)));
    }

    @EventListener
    public void handleComponentLifecycleEvent(ComponentLifecycleMsg event) {
        if (event.getEntityId().getEntityType() == EntityType.TENANT) {
            if (event.getEvent() == ComponentLifecycleEvent.DELETED) {
                List<QueueKey> toRemove = consumers.keySet().stream()
                        .filter(queueKey -> queueKey.getTenantId().equals(event.getTenantId()))
                        .collect(Collectors.toList());
                toRemove.forEach(queueKey -> {
                    removeConsumer(queueKey).ifPresent(consumer -> consumer.delete(false));
                });
            }
        }
    }

    private Optional<TbRuleEngineQueueConsumerManager> getConsumer(QueueKey queueKey) {
        return Optional.ofNullable(consumers.get(queueKey));
    }

    private TbRuleEngineQueueConsumerManager createConsumer(QueueKey queueKey, Queue queue) {
        var consumer = TbRuleEngineQueueConsumerManager.create()
                .ctx(ctx)
                .queueKey(queueKey)
                .consumerExecutor(consumersExecutor)
                .scheduler(scheduler)
                .taskExecutor(mgmtExecutor)
                .build();
        consumers.put(queueKey, consumer);
        consumer.init(queue);
        return consumer;
    }

    private Optional<TbRuleEngineQueueConsumerManager> removeConsumer(QueueKey queueKey) {
        return Optional.ofNullable(consumers.remove(queueKey));
    }

    @Scheduled(fixedDelayString = "${queue.rule-engine.stats.print-interval-ms}")
    public void printStats() {
        if (ctx.isStatsEnabled()) {
            long ts = System.currentTimeMillis();
            consumers.values().forEach(manager -> manager.printStats(ts));
        }
    }

}
