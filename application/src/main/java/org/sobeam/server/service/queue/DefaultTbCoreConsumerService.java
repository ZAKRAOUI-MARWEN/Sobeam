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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.sobeam.common.util.DonAsynchron;
import org.sobeam.common.util.JacksonUtil;
import org.sobeam.common.util.SoBeamThreadFactory;
import org.sobeam.server.actors.ActorSystemContext;
import org.sobeam.server.common.data.JavaSerDesUtil;
import org.sobeam.server.common.data.alarm.AlarmInfo;
import org.sobeam.server.common.data.event.ErrorEvent;
import org.sobeam.server.common.data.event.Event;
import org.sobeam.server.common.data.event.LifecycleEvent;
import org.sobeam.server.common.data.id.DeviceId;
import org.sobeam.server.common.data.id.NotificationRequestId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.id.UserId;
import org.sobeam.server.common.data.notification.rule.trigger.NotificationRuleTrigger;
import org.sobeam.server.common.data.queue.QueueConfig;
import org.sobeam.server.common.data.rpc.RpcError;
import org.sobeam.server.common.msg.MsgType;
import org.sobeam.server.common.msg.TbActorMsg;
import org.sobeam.server.common.msg.notification.NotificationRuleProcessor;
import org.sobeam.server.common.msg.queue.ServiceType;
import org.sobeam.server.common.msg.queue.TbCallback;
import org.sobeam.server.common.msg.rpc.FromDeviceRpcResponse;
import org.sobeam.server.common.msg.rpc.ToDeviceRpcRequestActorMsg;
import org.sobeam.server.common.stats.StatsFactory;
import org.sobeam.server.common.util.KvProtoUtil;
import org.sobeam.server.common.util.ProtoUtils;
import org.sobeam.server.dao.resource.ImageCacheKey;
import org.sobeam.server.dao.tenant.TbTenantProfileCache;
import org.sobeam.server.gen.transport.TransportProtos;
import org.sobeam.server.gen.transport.TransportProtos.DeviceStateServiceMsgProto;
import org.sobeam.server.gen.transport.TransportProtos.EdgeNotificationMsgProto;
import org.sobeam.server.gen.transport.TransportProtos.ErrorEventProto;
import org.sobeam.server.gen.transport.TransportProtos.FromDeviceRPCResponseProto;
import org.sobeam.server.gen.transport.TransportProtos.LifecycleEventProto;
import org.sobeam.server.gen.transport.TransportProtos.LocalSubscriptionServiceMsgProto;
import org.sobeam.server.gen.transport.TransportProtos.SubscriptionMgrMsgProto;
import org.sobeam.server.gen.transport.TransportProtos.TbAlarmDeleteProto;
import org.sobeam.server.gen.transport.TransportProtos.TbAlarmUpdateProto;
import org.sobeam.server.gen.transport.TransportProtos.TbAttributeDeleteProto;
import org.sobeam.server.gen.transport.TransportProtos.TbAttributeUpdateProto;
import org.sobeam.server.gen.transport.TransportProtos.TbEntitySubEventProto;
import org.sobeam.server.gen.transport.TransportProtos.TbTimeSeriesDeleteProto;
import org.sobeam.server.gen.transport.TransportProtos.TbTimeSeriesUpdateProto;
import org.sobeam.server.gen.transport.TransportProtos.ToCoreMsg;
import org.sobeam.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.sobeam.server.gen.transport.TransportProtos.ToOtaPackageStateServiceMsg;
import org.sobeam.server.gen.transport.TransportProtos.ToUsageStatsServiceMsg;
import org.sobeam.server.gen.transport.TransportProtos.TransportToDeviceActorMsg;
import org.sobeam.server.queue.TbQueueConsumer;
import org.sobeam.server.queue.common.TbProtoQueueMsg;
import org.sobeam.server.queue.common.consumer.QueueConsumerManager;
import org.sobeam.server.queue.discovery.PartitionService;
import org.sobeam.server.queue.discovery.QueueKey;
import org.sobeam.server.queue.discovery.event.PartitionChangeEvent;
import org.sobeam.server.queue.provider.TbCoreQueueFactory;
import org.sobeam.server.queue.util.TbCoreComponent;
import org.sobeam.server.service.apiusage.TbApiUsageStateService;
import org.sobeam.server.service.edge.EdgeNotificationService;
import org.sobeam.server.service.notification.NotificationSchedulerService;
import org.sobeam.server.service.ota.OtaPackageStateService;
import org.sobeam.server.service.profile.TbAssetProfileCache;
import org.sobeam.server.service.profile.TbDeviceProfileCache;
import org.sobeam.server.service.queue.consumer.MainQueueConsumerManager;
import org.sobeam.server.service.queue.processing.AbstractConsumerService;
import org.sobeam.server.service.queue.processing.IdMsgPair;
import org.sobeam.server.service.resource.TbImageService;
import org.sobeam.server.service.rpc.TbCoreDeviceRpcService;
import org.sobeam.server.service.security.auth.jwt.settings.JwtSettingsService;
import org.sobeam.server.service.state.DeviceStateService;
import org.sobeam.server.service.subscription.SubscriptionManagerService;
import org.sobeam.server.service.subscription.TbLocalSubscriptionService;
import org.sobeam.server.service.subscription.TbSubscriptionUtils;
import org.sobeam.server.service.sync.vc.GitVersionControlQueueService;
import org.sobeam.server.service.transport.msg.TransportToDeviceActorMsgWrapper;
import org.sobeam.server.service.ws.notification.sub.NotificationRequestUpdate;
import org.sobeam.server.service.ws.notification.sub.NotificationUpdate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
@Slf4j
public class DefaultTbCoreConsumerService extends AbstractConsumerService<ToCoreNotificationMsg> implements TbCoreConsumerService {

    @Value("${queue.core.poll-interval}")
    private long pollInterval;
    @Value("${queue.core.pack-processing-timeout}")
    private long packProcessingTimeout;
    @Value("${queue.core.consumer-per-partition:true}")
    private boolean consumerPerPartition;
    @Value("${queue.core.stats.enabled:false}")
    private boolean statsEnabled;

    @Value("${queue.core.ota.pack-interval-ms:60000}")
    private long firmwarePackInterval;
    @Value("${queue.core.ota.pack-size:100}")
    private int firmwarePackSize;

    private final DeviceStateService stateService;
    private final TbApiUsageStateService statsService;
    private final TbLocalSubscriptionService localSubscriptionService;
    private final SubscriptionManagerService subscriptionManagerService;
    private final TbCoreDeviceRpcService tbCoreDeviceRpcService;
    private final EdgeNotificationService edgeNotificationService;
    private final OtaPackageStateService firmwareStateService;
    private final GitVersionControlQueueService vcQueueService;
    private final NotificationSchedulerService notificationSchedulerService;
    private final NotificationRuleProcessor notificationRuleProcessor;
    private final TbCoreQueueFactory queueFactory;
    private final TbImageService imageService;
    private final TbCoreConsumerStats stats;

    private MainQueueConsumerManager<TbProtoQueueMsg<ToCoreMsg>, CoreQueueConfig> mainConsumer;
    private QueueConsumerManager<TbProtoQueueMsg<ToUsageStatsServiceMsg>> usageStatsConsumer;
    private QueueConsumerManager<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>> firmwareStatesConsumer;

    private volatile ListeningExecutorService deviceActivityEventsExecutor;

    public DefaultTbCoreConsumerService(TbCoreQueueFactory tbCoreQueueFactory,
                                        ActorSystemContext actorContext,
                                        DeviceStateService stateService,
                                        TbLocalSubscriptionService localSubscriptionService,
                                        SubscriptionManagerService subscriptionManagerService,
                                        TbCoreDeviceRpcService tbCoreDeviceRpcService,
                                        StatsFactory statsFactory,
                                        TbDeviceProfileCache deviceProfileCache,
                                        TbAssetProfileCache assetProfileCache,
                                        TbApiUsageStateService statsService,
                                        TbTenantProfileCache tenantProfileCache,
                                        TbApiUsageStateService apiUsageStateService,
                                        EdgeNotificationService edgeNotificationService,
                                        OtaPackageStateService firmwareStateService,
                                        GitVersionControlQueueService vcQueueService,
                                        PartitionService partitionService,
                                        ApplicationEventPublisher eventPublisher,
                                        JwtSettingsService jwtSettingsService,
                                        NotificationSchedulerService notificationSchedulerService,
                                        NotificationRuleProcessor notificationRuleProcessor,
                                        TbImageService imageService) {
        super(actorContext, tenantProfileCache, deviceProfileCache, assetProfileCache, apiUsageStateService, partitionService,
                eventPublisher, jwtSettingsService);
        this.stateService = stateService;
        this.localSubscriptionService = localSubscriptionService;
        this.subscriptionManagerService = subscriptionManagerService;
        this.tbCoreDeviceRpcService = tbCoreDeviceRpcService;
        this.edgeNotificationService = edgeNotificationService;
        this.stats = new TbCoreConsumerStats(statsFactory);
        this.statsService = statsService;
        this.firmwareStateService = firmwareStateService;
        this.vcQueueService = vcQueueService;
        this.notificationSchedulerService = notificationSchedulerService;
        this.notificationRuleProcessor = notificationRuleProcessor;
        this.imageService = imageService;
        this.queueFactory = tbCoreQueueFactory;
    }

    @PostConstruct
    public void init() {
        super.init("tb-core");
        this.deviceActivityEventsExecutor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor(SoBeamThreadFactory.forName("tb-core-device-activity-events-executor")));

        this.mainConsumer = MainQueueConsumerManager.<TbProtoQueueMsg<ToCoreMsg>, CoreQueueConfig>builder()
                .queueKey(new QueueKey(ServiceType.TB_CORE))
                .config(CoreQueueConfig.of(consumerPerPartition, (int) pollInterval))
                .msgPackProcessor(this::processMsgs)
                .consumerCreator((config, partitionId) -> queueFactory.createToCoreMsgConsumer())
                .consumerExecutor(consumersExecutor)
                .scheduler(scheduler)
                .taskExecutor(mgmtExecutor)
                .build();
        this.usageStatsConsumer = QueueConsumerManager.<TbProtoQueueMsg<ToUsageStatsServiceMsg>>builder()
                .name("TB Usage Stats")
                .msgPackProcessor(this::processUsageStatsMsg)
                .pollInterval(pollInterval)
                .consumerCreator(queueFactory::createToUsageStatsServiceMsgConsumer)
                .consumerExecutor(consumersExecutor)
                .threadPrefix("usage-stats")
                .build();
        this.firmwareStatesConsumer = QueueConsumerManager.<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>>builder()
                .name("TB Ota Package States")
                .msgPackProcessor(this::processFirmwareMsgs)
                .pollInterval(pollInterval)
                .consumerCreator(queueFactory::createToOtaPackageStateServiceMsgConsumer)
                .consumerExecutor(consumersExecutor)
                .threadPrefix("firmware")
                .build();
    }

    @PreDestroy
    public void destroy() {
        super.destroy();
        if (deviceActivityEventsExecutor != null) {
            deviceActivityEventsExecutor.shutdownNow();
        }
    }

    @Override
    protected void startConsumers() {
        super.startConsumers();
        firmwareStatesConsumer.subscribe();
        firmwareStatesConsumer.launch();
        usageStatsConsumer.launch();
    }

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent event) {
        log.info("Subscribing to partitions: {}", event.getPartitions());
        mainConsumer.update(event.getPartitions());
        usageStatsConsumer.subscribe(event.getPartitions()
                .stream()
                .map(tpi -> tpi.newByTopic(usageStatsConsumer.getConsumer().getTopic()))
                .collect(Collectors.toSet()));
    }

    private void processMsgs(List<TbProtoQueueMsg<ToCoreMsg>> msgs, TbQueueConsumer<TbProtoQueueMsg<ToCoreMsg>> consumer, CoreQueueConfig config) throws Exception {
        List<IdMsgPair<ToCoreMsg>> orderedMsgList = msgs.stream().map(msg -> new IdMsgPair<>(UUID.randomUUID(), msg)).collect(Collectors.toList());
        ConcurrentMap<UUID, TbProtoQueueMsg<ToCoreMsg>> pendingMap = orderedMsgList.stream().collect(
                Collectors.toConcurrentMap(IdMsgPair::getUuid, IdMsgPair::getMsg));
        CountDownLatch processingTimeoutLatch = new CountDownLatch(1);
        TbPackProcessingContext<TbProtoQueueMsg<ToCoreMsg>> ctx = new TbPackProcessingContext<>(
                processingTimeoutLatch, pendingMap, new ConcurrentHashMap<>());
        PendingMsgHolder pendingMsgHolder = new PendingMsgHolder();
        Future<?> packSubmitFuture = consumersExecutor.submit(() -> {
            orderedMsgList.forEach((element) -> {
                UUID id = element.getUuid();
                TbProtoQueueMsg<ToCoreMsg> msg = element.getMsg();
                log.trace("[{}] Creating main callback for message: {}", id, msg.getValue());
                TbCallback callback = new TbPackCallback<>(id, ctx);
                try {
                    ToCoreMsg toCoreMsg = msg.getValue();
                    pendingMsgHolder.setToCoreMsg(toCoreMsg);
                    if (toCoreMsg.hasToSubscriptionMgrMsg()) {
                        log.trace("[{}] Forwarding message to subscription manager service {}", id, toCoreMsg.getToSubscriptionMgrMsg());
                        forwardToSubMgrService(toCoreMsg.getToSubscriptionMgrMsg(), callback);
                    } else if (toCoreMsg.hasToDeviceActorMsg()) {
                        log.trace("[{}] Forwarding message to device actor {}", id, toCoreMsg.getToDeviceActorMsg());
                        forwardToDeviceActor(toCoreMsg.getToDeviceActorMsg(), callback);
                    } else if (toCoreMsg.hasDeviceStateServiceMsg()) {
                        log.trace("[{}] Forwarding message to device state service {}", id, toCoreMsg.getDeviceStateServiceMsg());
                        forwardToStateService(toCoreMsg.getDeviceStateServiceMsg(), callback);
                    } else if (toCoreMsg.hasEdgeNotificationMsg()) {
                        log.trace("[{}] Forwarding message to edge service {}", id, toCoreMsg.getEdgeNotificationMsg());
                        forwardToEdgeNotificationService(toCoreMsg.getEdgeNotificationMsg(), callback);
                    } else if (toCoreMsg.hasDeviceConnectMsg()) {
                        log.trace("[{}] Forwarding message to device state service {}", id, toCoreMsg.getDeviceConnectMsg());
                        forwardToStateService(toCoreMsg.getDeviceConnectMsg(), callback);
                    } else if (toCoreMsg.hasDeviceActivityMsg()) {
                        log.trace("[{}] Forwarding message to device state service {}", id, toCoreMsg.getDeviceActivityMsg());
                        forwardToStateService(toCoreMsg.getDeviceActivityMsg(), callback);
                    } else if (toCoreMsg.hasDeviceDisconnectMsg()) {
                        log.trace("[{}] Forwarding message to device state service {}", id, toCoreMsg.getDeviceDisconnectMsg());
                        forwardToStateService(toCoreMsg.getDeviceDisconnectMsg(), callback);
                    } else if (toCoreMsg.hasDeviceInactivityMsg()) {
                        log.trace("[{}] Forwarding message to device state service {}", id, toCoreMsg.getDeviceInactivityMsg());
                        forwardToStateService(toCoreMsg.getDeviceInactivityMsg(), callback);
                    } else if (toCoreMsg.hasToDeviceActorNotification()) {
                        TbActorMsg actorMsg = ProtoUtils.fromProto(toCoreMsg.getToDeviceActorNotification());
                        if (actorMsg != null) {
                            if (actorMsg.getMsgType().equals(MsgType.DEVICE_RPC_REQUEST_TO_DEVICE_ACTOR_MSG)) {
                                tbCoreDeviceRpcService.forwardRpcRequestToDeviceActor((ToDeviceRpcRequestActorMsg) actorMsg);
                            } else {
                                log.trace("[{}] Forwarding message to App Actor {}", id, actorMsg);
                                actorContext.tell(actorMsg);
                            }
                        }
                        callback.onSuccess();
                    } else if (toCoreMsg.hasNotificationSchedulerServiceMsg()) {
                        TransportProtos.NotificationSchedulerServiceMsg notificationSchedulerServiceMsg = toCoreMsg.getNotificationSchedulerServiceMsg();
                        log.trace("[{}] Forwarding message to notification scheduler service {}", id, toCoreMsg.getNotificationSchedulerServiceMsg());
                        forwardToNotificationSchedulerService(notificationSchedulerServiceMsg, callback);
                    } else if (toCoreMsg.hasErrorEventMsg()) {
                        forwardToEventService(toCoreMsg.getErrorEventMsg(), callback);
                    } else if (toCoreMsg.hasLifecycleEventMsg()) {
                        forwardToEventService(toCoreMsg.getLifecycleEventMsg(), callback);
                    }
                } catch (Throwable e) {
                    log.warn("[{}] Failed to process message: {}", id, msg, e);
                    callback.onFailure(e);
                }
            });
        });
        if (!processingTimeoutLatch.await(packProcessingTimeout, TimeUnit.MILLISECONDS)) {
            if (!packSubmitFuture.isDone()) {
                packSubmitFuture.cancel(true);
                ToCoreMsg lastSubmitMsg = pendingMsgHolder.getToCoreMsg();
                log.info("Timeout to process message: {}", lastSubmitMsg);
            }
            if (log.isDebugEnabled()) {
                ctx.getAckMap().forEach((id, msg) -> log.debug("[{}] Timeout to process message: {}", id, msg.getValue()));
            }
            ctx.getFailedMap().forEach((id, msg) -> log.warn("[{}] Failed to process message: {}", id, msg.getValue()));
        }
        consumer.commit();
    }

    private static class PendingMsgHolder {
        @Getter
        @Setter
        private volatile ToCoreMsg toCoreMsg;
    }

    @Override
    protected ServiceType getServiceType() {
        return ServiceType.TB_CORE;
    }

    @Override
    protected long getNotificationPollDuration() {
        return pollInterval;
    }

    @Override
    protected long getNotificationPackProcessingTimeout() {
        return packProcessingTimeout;
    }

    @Override
    protected int getMgmtThreadPoolSize() {
        return Math.max(Runtime.getRuntime().availableProcessors(), 4);
    }

    @Override
    protected TbQueueConsumer<TbProtoQueueMsg<ToCoreNotificationMsg>> createNotificationsConsumer() {
        return queueFactory.createToCoreNotificationsMsgConsumer();
    }

    @Override
    protected void handleNotification(UUID id, TbProtoQueueMsg<ToCoreNotificationMsg> msg, TbCallback callback) {
        ToCoreNotificationMsg toCoreNotification = msg.getValue();
        if (toCoreNotification.hasToLocalSubscriptionServiceMsg()) {
            log.trace("[{}] Forwarding message to local subscription service {}", id, toCoreNotification.getToLocalSubscriptionServiceMsg());
            forwardToLocalSubMgrService(toCoreNotification.getToLocalSubscriptionServiceMsg(), callback);
        } else if (toCoreNotification.hasCoreStartupMsg()) {
            log.trace("[{}] Forwarding message to local subscription service {}", id, toCoreNotification.getCoreStartupMsg());
            forwardCoreStartupMsg(toCoreNotification.getCoreStartupMsg(), callback);
        } else if (toCoreNotification.hasFromDeviceRpcResponse()) {
            log.trace("[{}] Forwarding message to RPC service {}", id, toCoreNotification.getFromDeviceRpcResponse());
            forwardToCoreRpcService(toCoreNotification.getFromDeviceRpcResponse(), callback);
        } else if (toCoreNotification.hasComponentLifecycle()) {
            handleComponentLifecycleMsg(id, ProtoUtils.fromProto(toCoreNotification.getComponentLifecycle()));
            callback.onSuccess();
        } else if (toCoreNotification.hasEdgeEventUpdate()) {
            forwardToAppActor(id, ProtoUtils.fromProto(toCoreNotification.getEdgeEventUpdate()));
            callback.onSuccess();
        } else if (toCoreNotification.hasToEdgeSyncRequest()) {
            forwardToAppActor(id, ProtoUtils.fromProto(toCoreNotification.getToEdgeSyncRequest()));
            callback.onSuccess();
        } else if (toCoreNotification.hasFromEdgeSyncResponse()) {
            forwardToAppActor(id, ProtoUtils.fromProto(toCoreNotification.getFromEdgeSyncResponse()));
            callback.onSuccess();
        } else if (toCoreNotification.getQueueUpdateMsgsCount() > 0) {
            partitionService.updateQueues(toCoreNotification.getQueueUpdateMsgsList());
            callback.onSuccess();
        } else if (toCoreNotification.getQueueDeleteMsgsCount() > 0) {
            partitionService.removeQueues(toCoreNotification.getQueueDeleteMsgsList());
            callback.onSuccess();
        } else if (toCoreNotification.hasVcResponseMsg()) {
            vcQueueService.processResponse(toCoreNotification.getVcResponseMsg());
            callback.onSuccess();
        } else if (toCoreNotification.hasToSubscriptionMgrMsg()) {
            forwardToSubMgrService(toCoreNotification.getToSubscriptionMgrMsg(), callback);
        } else if (toCoreNotification.hasNotificationRuleProcessorMsg()) {
            NotificationRuleTrigger notificationRuleTrigger =
                    JavaSerDesUtil.decode(toCoreNotification.getNotificationRuleProcessorMsg().getTrigger().toByteArray());
            notificationRuleProcessor.process(notificationRuleTrigger);
            callback.onSuccess();
        } else if (toCoreNotification.hasResourceCacheInvalidateMsg()) {
            forwardToResourceService(toCoreNotification.getResourceCacheInvalidateMsg(), callback);
        }
        if (statsEnabled) {
            stats.log(toCoreNotification);
        }
    }

    private void processUsageStatsMsg(List<TbProtoQueueMsg<ToUsageStatsServiceMsg>> msgs, TbQueueConsumer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> consumer) throws Exception {
        ConcurrentMap<UUID, TbProtoQueueMsg<ToUsageStatsServiceMsg>> pendingMap = msgs.stream().collect(
                Collectors.toConcurrentMap(s -> UUID.randomUUID(), Function.identity()));
        CountDownLatch processingTimeoutLatch = new CountDownLatch(1);
        TbPackProcessingContext<TbProtoQueueMsg<ToUsageStatsServiceMsg>> ctx = new TbPackProcessingContext<>(
                processingTimeoutLatch, pendingMap, new ConcurrentHashMap<>());
        pendingMap.forEach((id, msg) -> {
            log.trace("[{}] Creating usage stats callback for message: {}", id, msg.getValue());
            TbCallback callback = new TbPackCallback<>(id, ctx);
            try {
                handleUsageStats(msg, callback);
            } catch (Throwable e) {
                log.warn("[{}] Failed to process usage stats: {}", id, msg, e);
                callback.onFailure(e);
            }
        });
        if (!processingTimeoutLatch.await(getNotificationPackProcessingTimeout(), TimeUnit.MILLISECONDS)) {
            ctx.getAckMap().forEach((id, msg) -> log.warn("[{}] Timeout to process usage stats: {}", id, msg.getValue()));
            ctx.getFailedMap().forEach((id, msg) -> log.warn("[{}] Failed to process usage stats: {}", id, msg.getValue()));
        }
        consumer.commit();

    }

    private void processFirmwareMsgs(List<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>> msgs, TbQueueConsumer<TbProtoQueueMsg<ToOtaPackageStateServiceMsg>> consumer) {
        long maxProcessingTimeoutPerRecord = firmwarePackInterval / firmwarePackSize;
        long timeToSleep = maxProcessingTimeoutPerRecord;
        for (TbProtoQueueMsg<ToOtaPackageStateServiceMsg> msg : msgs) {
            try {
                long startTime = System.currentTimeMillis();
                boolean isSuccessUpdate = handleOtaPackageUpdates(msg);
                long endTime = System.currentTimeMillis();
                long spentTime = endTime - startTime;
                timeToSleep = timeToSleep - spentTime;
                if (isSuccessUpdate) {
                    if (timeToSleep > 0) {
                        log.debug("Spent time per record is: [{}]!", spentTime);
                        Thread.sleep(timeToSleep);
                        timeToSleep = 0;
                    }
                    timeToSleep += maxProcessingTimeoutPerRecord;
                }
            } catch (InterruptedException e) {
                return;
            } catch (Throwable e) {
                log.warn("Failed to process firmware update msg: {}", msg, e);
            }
        }
        consumer.commit();
    }

    private void handleUsageStats(TbProtoQueueMsg<ToUsageStatsServiceMsg> msg, TbCallback callback) {
        statsService.process(msg, callback);
    }

    private boolean handleOtaPackageUpdates(TbProtoQueueMsg<ToOtaPackageStateServiceMsg> msg) {
        return firmwareStateService.process(msg.getValue());
    }

    private void forwardToCoreRpcService(FromDeviceRPCResponseProto proto, TbCallback callback) {
        RpcError error = proto.getError() > 0 ? RpcError.values()[proto.getError()] : null;
        FromDeviceRpcResponse response = new FromDeviceRpcResponse(new UUID(proto.getRequestIdMSB(), proto.getRequestIdLSB())
                , proto.getResponse(), error);
        tbCoreDeviceRpcService.processRpcResponseFromRuleEngine(response);
        callback.onSuccess();
    }

    @Scheduled(fixedDelayString = "${queue.core.stats.print-interval-ms}")
    public void printStats() {
        if (statsEnabled) {
            stats.printStats();
            stats.reset();
        }
    }

    private void forwardToLocalSubMgrService(LocalSubscriptionServiceMsgProto msg, TbCallback callback) {
        if (msg.hasSubEventCallback()) {
            localSubscriptionService.onSubEventCallback(msg.getSubEventCallback(), callback);
        } else if (msg.hasTsUpdate()) {
            localSubscriptionService.onTimeSeriesUpdate(msg.getTsUpdate(), callback);
        } else if (msg.hasAttrUpdate()) {
            localSubscriptionService.onAttributesUpdate(msg.getAttrUpdate(), callback);
        } else if (msg.hasAlarmUpdate()) {
            localSubscriptionService.onAlarmUpdate(msg.getAlarmUpdate(), callback);
        } else if (msg.hasNotificationsUpdate()) {
            localSubscriptionService.onNotificationUpdate(msg.getNotificationsUpdate(), callback);
        } else if (msg.hasSubUpdate() || msg.hasAlarmSubUpdate() || msg.hasNotificationsSubUpdate()) {
            //OLD CODE -> Do NOTHING.
            callback.onSuccess();
        } else {
            throwNotHandled(msg, callback);
        }
    }

    private void forwardCoreStartupMsg(TransportProtos.CoreStartupMsg coreStartupMsg, TbCallback callback) {
        log.info("[{}] Processing core startup with partitions: {}", coreStartupMsg.getServiceId(), coreStartupMsg.getPartitionsList());
        localSubscriptionService.onCoreStartupMsg(coreStartupMsg);
        callback.onSuccess();
    }

    private void forwardToResourceService(TransportProtos.ResourceCacheInvalidateMsg msg, TbCallback callback) {
        var tenantId = new TenantId(new UUID(msg.getTenantIdMSB(), msg.getTenantIdLSB()));
        msg.getKeysList().stream().map(cacheKeyProto -> {
            if (cacheKeyProto.hasResourceKey()) {
                return ImageCacheKey.forImage(tenantId, cacheKeyProto.getResourceKey());
            } else {
                return ImageCacheKey.forPublicImage(cacheKeyProto.getPublicResourceKey());
            }
        }).forEach(imageService::evictETags);
        callback.onSuccess();
    }

    private void forwardToSubMgrService(SubscriptionMgrMsgProto msg, TbCallback callback) {
        if (msg.hasSubEvent()) {
            TbEntitySubEventProto subEvent = msg.getSubEvent();
            subscriptionManagerService.onSubEvent(subEvent.getServiceId(), TbSubscriptionUtils.fromProto(subEvent), callback);
        } else if (msg.hasTelemetrySub()) {
            callback.onSuccess();
            // Deprecated, for removal; Left intentionally to avoid throwNotHandled
        } else if (msg.hasAlarmSub()) {
            callback.onSuccess();
            // Deprecated, for removal; Left intentionally to avoid throwNotHandled
        } else if (msg.hasNotificationsSub()) {
            callback.onSuccess();
            // Deprecated, for removal; Left intentionally to avoid throwNotHandled
        } else if (msg.hasNotificationsCountSub()) {
            callback.onSuccess();
            // Deprecated, for removal; Left intentionally to avoid throwNotHandled
        } else if (msg.hasSubClose()) {
            callback.onSuccess();
            // Deprecated, for removal; Left intentionally to avoid throwNotHandled
        } else if (msg.hasTsUpdate()) {
            TbTimeSeriesUpdateProto proto = msg.getTsUpdate();
            long tenantIdMSB = proto.getTenantIdMSB();
            long tenantIdLSB = proto.getTenantIdLSB();
            subscriptionManagerService.onTimeSeriesUpdate(
                    toTenantId(tenantIdMSB, tenantIdLSB),
                    TbSubscriptionUtils.toEntityId(proto.getEntityType(), proto.getEntityIdMSB(), proto.getEntityIdLSB()),
                    KvProtoUtil.fromTsKvProtoList(proto.getDataList()), callback);
        } else if (msg.hasAttrUpdate()) {
            TbAttributeUpdateProto proto = msg.getAttrUpdate();
            subscriptionManagerService.onAttributesUpdate(
                    toTenantId(proto.getTenantIdMSB(), proto.getTenantIdLSB()),
                    TbSubscriptionUtils.toEntityId(proto.getEntityType(), proto.getEntityIdMSB(), proto.getEntityIdLSB()),
                    proto.getScope(), KvProtoUtil.toAttributeKvList(proto.getDataList()), callback);
        } else if (msg.hasAttrDelete()) {
            TbAttributeDeleteProto proto = msg.getAttrDelete();
            subscriptionManagerService.onAttributesDelete(
                    toTenantId(proto.getTenantIdMSB(), proto.getTenantIdLSB()),
                    TbSubscriptionUtils.toEntityId(proto.getEntityType(), proto.getEntityIdMSB(), proto.getEntityIdLSB()),
                    proto.getScope(), proto.getKeysList(), proto.getNotifyDevice(), callback);
        } else if (msg.hasTsDelete()) {
            TbTimeSeriesDeleteProto proto = msg.getTsDelete();
            subscriptionManagerService.onTimeSeriesDelete(
                    toTenantId(proto.getTenantIdMSB(), proto.getTenantIdLSB()),
                    TbSubscriptionUtils.toEntityId(proto.getEntityType(), proto.getEntityIdMSB(), proto.getEntityIdLSB()),
                    proto.getKeysList(), callback);
        } else if (msg.hasAlarmUpdate()) {
            TbAlarmUpdateProto proto = msg.getAlarmUpdate();
            subscriptionManagerService.onAlarmUpdate(
                    toTenantId(proto.getTenantIdMSB(), proto.getTenantIdLSB()),
                    TbSubscriptionUtils.toEntityId(proto.getEntityType(), proto.getEntityIdMSB(), proto.getEntityIdLSB()),
                    JacksonUtil.fromString(proto.getAlarm(), AlarmInfo.class),
                    callback);
        } else if (msg.hasAlarmDelete()) {
            TbAlarmDeleteProto proto = msg.getAlarmDelete();
            subscriptionManagerService.onAlarmDeleted(
                    toTenantId(proto.getTenantIdMSB(), proto.getTenantIdLSB()),
                    TbSubscriptionUtils.toEntityId(proto.getEntityType(), proto.getEntityIdMSB(), proto.getEntityIdLSB()),
                    JacksonUtil.fromString(proto.getAlarm(), AlarmInfo.class), callback);
        } else if (msg.hasNotificationUpdate()) {
            TransportProtos.NotificationUpdateProto updateProto = msg.getNotificationUpdate();
            TenantId tenantId = toTenantId(updateProto.getTenantIdMSB(), updateProto.getTenantIdLSB());
            UserId recipientId = new UserId(new UUID(updateProto.getRecipientIdMSB(), updateProto.getRecipientIdLSB()));
            NotificationUpdate update = JacksonUtil.fromString(updateProto.getUpdate(), NotificationUpdate.class);
            subscriptionManagerService.onNotificationUpdate(tenantId, recipientId, update, callback);
        } else if (msg.hasNotificationRequestUpdate()) {
            TransportProtos.NotificationRequestUpdateProto updateProto = msg.getNotificationRequestUpdate();
            TenantId tenantId = toTenantId(updateProto.getTenantIdMSB(), updateProto.getTenantIdLSB());
            NotificationRequestUpdate update = JacksonUtil.fromString(updateProto.getUpdate(), NotificationRequestUpdate.class);
            localSubscriptionService.onNotificationRequestUpdate(tenantId, update, callback);
        } else {
            throwNotHandled(msg, callback);
        }
        if (statsEnabled) {
            stats.log(msg);
        }
    }

    void forwardToStateService(DeviceStateServiceMsgProto deviceStateServiceMsg, TbCallback callback) {
        if (statsEnabled) {
            stats.log(deviceStateServiceMsg);
        }
        stateService.onQueueMsg(deviceStateServiceMsg, callback);
    }

    void forwardToStateService(TransportProtos.DeviceConnectProto deviceConnectMsg, TbCallback callback) {
        if (statsEnabled) {
            stats.log(deviceConnectMsg);
        }
        var tenantId = toTenantId(deviceConnectMsg.getTenantIdMSB(), deviceConnectMsg.getTenantIdLSB());
        var deviceId = new DeviceId(new UUID(deviceConnectMsg.getDeviceIdMSB(), deviceConnectMsg.getDeviceIdLSB()));
        ListenableFuture<?> future = deviceActivityEventsExecutor.submit(() -> stateService.onDeviceConnect(tenantId, deviceId, deviceConnectMsg.getLastConnectTime()));
        DonAsynchron.withCallback(future,
                __ -> callback.onSuccess(),
                t -> {
                    log.warn("[{}] Failed to process device connect message for device [{}]", tenantId.getId(), deviceId.getId(), t);
                    callback.onFailure(t);
                });
    }

    void forwardToStateService(TransportProtos.DeviceActivityProto deviceActivityMsg, TbCallback callback) {
        if (statsEnabled) {
            stats.log(deviceActivityMsg);
        }
        var tenantId = toTenantId(deviceActivityMsg.getTenantIdMSB(), deviceActivityMsg.getTenantIdLSB());
        var deviceId = new DeviceId(new UUID(deviceActivityMsg.getDeviceIdMSB(), deviceActivityMsg.getDeviceIdLSB()));
        ListenableFuture<?> future = deviceActivityEventsExecutor.submit(() -> stateService.onDeviceActivity(tenantId, deviceId, deviceActivityMsg.getLastActivityTime()));
        DonAsynchron.withCallback(future,
                __ -> callback.onSuccess(),
                t -> {
                    log.warn("[{}] Failed to process device activity message for device [{}]", tenantId.getId(), deviceId.getId(), t);
                    callback.onFailure(new RuntimeException("Failed to update device activity for device [" + deviceId.getId() + "]!", t));
                });
    }

    void forwardToStateService(TransportProtos.DeviceDisconnectProto deviceDisconnectMsg, TbCallback callback) {
        if (statsEnabled) {
            stats.log(deviceDisconnectMsg);
        }
        var tenantId = toTenantId(deviceDisconnectMsg.getTenantIdMSB(), deviceDisconnectMsg.getTenantIdLSB());
        var deviceId = new DeviceId(new UUID(deviceDisconnectMsg.getDeviceIdMSB(), deviceDisconnectMsg.getDeviceIdLSB()));
        ListenableFuture<?> future = deviceActivityEventsExecutor.submit(() -> stateService.onDeviceDisconnect(tenantId, deviceId, deviceDisconnectMsg.getLastDisconnectTime()));
        DonAsynchron.withCallback(future,
                __ -> callback.onSuccess(),
                t -> {
                    log.warn("[{}] Failed to process device disconnect message for device [{}]", tenantId.getId(), deviceId.getId(), t);
                    callback.onFailure(t);
                });
    }

    void forwardToStateService(TransportProtos.DeviceInactivityProto deviceInactivityMsg, TbCallback callback) {
        if (statsEnabled) {
            stats.log(deviceInactivityMsg);
        }
        var tenantId = toTenantId(deviceInactivityMsg.getTenantIdMSB(), deviceInactivityMsg.getTenantIdLSB());
        var deviceId = new DeviceId(new UUID(deviceInactivityMsg.getDeviceIdMSB(), deviceInactivityMsg.getDeviceIdLSB()));
        ListenableFuture<?> future = deviceActivityEventsExecutor.submit(() -> stateService.onDeviceInactivity(tenantId, deviceId, deviceInactivityMsg.getLastInactivityTime()));
        DonAsynchron.withCallback(future,
                __ -> callback.onSuccess(),
                t -> {
                    log.warn("[{}] Failed to process device inactivity message for device [{}]", tenantId.getId(), deviceId.getId(), t);
                    callback.onFailure(t);
                });
    }

    private void forwardToNotificationSchedulerService(TransportProtos.NotificationSchedulerServiceMsg msg, TbCallback callback) {
        TenantId tenantId = toTenantId(msg.getTenantIdMSB(), msg.getTenantIdLSB());
        NotificationRequestId notificationRequestId = new NotificationRequestId(new UUID(msg.getRequestIdMSB(), msg.getRequestIdLSB()));
        try {
            notificationSchedulerService.scheduleNotificationRequest(tenantId, notificationRequestId, msg.getTs());
            callback.onSuccess();
        } catch (Exception e) {
            callback.onFailure(new RuntimeException("Failed to schedule notification request", e));
        }
    }

    private void forwardToEdgeNotificationService(EdgeNotificationMsgProto edgeNotificationMsg, TbCallback callback) {
        if (statsEnabled) {
            stats.log(edgeNotificationMsg);
        }
        edgeNotificationService.pushNotificationToEdge(edgeNotificationMsg, callback);
    }

    private void forwardToDeviceActor(TransportToDeviceActorMsg toDeviceActorMsg, TbCallback callback) {
        if (statsEnabled) {
            stats.log(toDeviceActorMsg);
        }
        actorContext.tell(new TransportToDeviceActorMsgWrapper(toDeviceActorMsg, callback));
    }

    private void forwardToAppActor(UUID id, Optional<TbActorMsg> actorMsg, TbCallback callback) {
        if (actorMsg.isPresent()) {
            forwardToAppActor(id, actorMsg.get());
        }
        callback.onSuccess();
    }

    private void forwardToAppActor(UUID id, TbActorMsg actorMsg) {
        log.trace("[{}] Forwarding message to App Actor {}", id, actorMsg);
        actorContext.tell(actorMsg);
    }

    private void forwardToEventService(ErrorEventProto eventProto, TbCallback callback) {
        Event event = ErrorEvent.builder()
                .tenantId(toTenantId(eventProto.getTenantIdMSB(), eventProto.getTenantIdLSB()))
                .entityId(new UUID(eventProto.getEntityIdMSB(), eventProto.getEntityIdLSB()))
                .serviceId(eventProto.getServiceId())
                .ts(System.currentTimeMillis())
                .method(eventProto.getMethod())
                .error(eventProto.getError())
                .build();
        forwardToEventService(event, callback);
    }

    private void forwardToEventService(LifecycleEventProto eventProto, TbCallback callback) {
        Event event = LifecycleEvent.builder()
                .tenantId(toTenantId(eventProto.getTenantIdMSB(), eventProto.getTenantIdLSB()))
                .entityId(new UUID(eventProto.getEntityIdMSB(), eventProto.getEntityIdLSB()))
                .serviceId(eventProto.getServiceId())
                .ts(System.currentTimeMillis())
                .lcEventType(eventProto.getLcEventType())
                .success(eventProto.getSuccess())
                .error(StringUtils.isNotEmpty(eventProto.getError()) ? eventProto.getError() : null)
                .build();
        forwardToEventService(event, callback);
    }

    private void forwardToEventService(Event event, TbCallback callback) {
        DonAsynchron.withCallback(actorContext.getEventService().saveAsync(event),
                result -> callback.onSuccess(),
                callback::onFailure,
                actorContext.getDbCallbackExecutor());
    }

    private void throwNotHandled(Object msg, TbCallback callback) {
        log.warn("Message not handled: {}", msg);
        callback.onFailure(new RuntimeException("Message not handled!"));
    }

    private TenantId toTenantId(long tenantIdMSB, long tenantIdLSB) {
        return TenantId.fromUUID(new UUID(tenantIdMSB, tenantIdLSB));
    }

    @Override
    protected void stopConsumers() {
        super.stopConsumers();
        mainConsumer.stop();
        mainConsumer.awaitStop();
        usageStatsConsumer.stop();
        firmwareStatesConsumer.stop();
    }

    @Data(staticConstructor = "of")
    public static class CoreQueueConfig implements QueueConfig {
        private final boolean consumerPerPartition;
        private final int pollInterval;
    }

}
