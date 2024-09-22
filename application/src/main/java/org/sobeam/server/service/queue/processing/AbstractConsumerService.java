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
package org.sobeam.server.service.queue.processing;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.sobeam.common.util.SoBeamExecutors;
import org.sobeam.common.util.SoBeamThreadFactory;
import org.sobeam.server.actors.ActorSystemContext;
import org.sobeam.server.common.data.EntityType;
import org.sobeam.server.common.data.id.AssetId;
import org.sobeam.server.common.data.id.AssetProfileId;
import org.sobeam.server.common.data.id.CustomerId;
import org.sobeam.server.common.data.id.DeviceId;
import org.sobeam.server.common.data.id.DeviceProfileId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.id.TenantProfileId;
import org.sobeam.server.common.data.plugin.ComponentLifecycleEvent;
import org.sobeam.server.common.msg.plugin.ComponentLifecycleMsg;
import org.sobeam.server.common.msg.queue.ServiceType;
import org.sobeam.server.common.msg.queue.TbCallback;
import org.sobeam.server.dao.tenant.TbTenantProfileCache;
import org.sobeam.server.queue.TbQueueConsumer;
import org.sobeam.server.queue.common.TbProtoQueueMsg;
import org.sobeam.server.queue.common.consumer.QueueConsumerManager;
import org.sobeam.server.queue.discovery.PartitionService;
import org.sobeam.server.queue.discovery.TbApplicationEventListener;
import org.sobeam.server.queue.discovery.event.PartitionChangeEvent;
import org.sobeam.server.queue.util.AfterStartUp;
import org.sobeam.server.service.apiusage.TbApiUsageStateService;
import org.sobeam.server.service.profile.TbAssetProfileCache;
import org.sobeam.server.service.profile.TbDeviceProfileCache;
import org.sobeam.server.service.queue.TbPackCallback;
import org.sobeam.server.service.queue.TbPackProcessingContext;
import org.sobeam.server.service.security.auth.jwt.settings.JwtSettingsService;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractConsumerService<N extends com.google.protobuf.GeneratedMessageV3> extends TbApplicationEventListener<PartitionChangeEvent> {

    protected final ActorSystemContext actorContext;
    protected final TbTenantProfileCache tenantProfileCache;
    protected final TbDeviceProfileCache deviceProfileCache;
    protected final TbAssetProfileCache assetProfileCache;
    protected final TbApiUsageStateService apiUsageStateService;
    protected final PartitionService partitionService;
    protected final ApplicationEventPublisher eventPublisher;
    protected final JwtSettingsService jwtSettingsService;

    protected QueueConsumerManager<TbProtoQueueMsg<N>> nfConsumer;

    protected ExecutorService consumersExecutor;
    protected ExecutorService mgmtExecutor;
    protected ScheduledExecutorService scheduler;

    public void init(String prefix) {
        this.consumersExecutor = Executors.newCachedThreadPool(SoBeamThreadFactory.forName(prefix + "-consumer"));
        this.mgmtExecutor = SoBeamExecutors.newWorkStealingPool(getMgmtThreadPoolSize(), prefix + "-mgmt");
        this.scheduler = Executors.newSingleThreadScheduledExecutor(SoBeamThreadFactory.forName(prefix + "-consumer-scheduler"));

        this.nfConsumer = QueueConsumerManager.<TbProtoQueueMsg<N>>builder()
                .name(getServiceType().getLabel() + " Notifications")
                .msgPackProcessor(this::processNotifications)
                .pollInterval(getNotificationPollDuration())
                .consumerCreator(this::createNotificationsConsumer)
                .consumerExecutor(consumersExecutor)
                .threadPrefix("notifications")
                .build();
    }

    @AfterStartUp(order = AfterStartUp.REGULAR_SERVICE)
    public void afterStartUp() {
        startConsumers();
    }

    protected void startConsumers() {
        nfConsumer.subscribe();
        nfConsumer.launch();
    }

    @Override
    protected boolean filterTbApplicationEvent(PartitionChangeEvent event) {
        return event.getServiceType() == getServiceType();
    }

    protected abstract ServiceType getServiceType();

    protected void stopConsumers() {
        nfConsumer.stop();
    }

    protected abstract long getNotificationPollDuration();

    protected abstract long getNotificationPackProcessingTimeout();

    protected abstract int getMgmtThreadPoolSize();

    protected abstract TbQueueConsumer<TbProtoQueueMsg<N>> createNotificationsConsumer();

    protected void processNotifications(List<TbProtoQueueMsg<N>> msgs, TbQueueConsumer<TbProtoQueueMsg<N>> consumer) throws Exception {
        List<IdMsgPair<N>> orderedMsgList = msgs.stream().map(msg -> new IdMsgPair<>(UUID.randomUUID(), msg)).collect(Collectors.toList());
        ConcurrentMap<UUID, TbProtoQueueMsg<N>> pendingMap = orderedMsgList.stream().collect(
                Collectors.toConcurrentMap(IdMsgPair::getUuid, IdMsgPair::getMsg));
        CountDownLatch processingTimeoutLatch = new CountDownLatch(1);
        TbPackProcessingContext<TbProtoQueueMsg<N>> ctx = new TbPackProcessingContext<>(
                processingTimeoutLatch, pendingMap, new ConcurrentHashMap<>());
        orderedMsgList.forEach(element -> {
            UUID id = element.getUuid();
            TbProtoQueueMsg<N> msg = element.getMsg();
            log.trace("[{}] Creating notification callback for message: {}", id, msg.getValue());
            TbCallback callback = new TbPackCallback<>(id, ctx);
            try {
                handleNotification(id, msg, callback);
            } catch (Throwable e) {
                log.warn("[{}] Failed to process notification: {}", id, msg, e);
                callback.onFailure(e);
            }
        });
        if (!processingTimeoutLatch.await(getNotificationPackProcessingTimeout(), TimeUnit.MILLISECONDS)) {
            ctx.getAckMap().forEach((id, msg) -> log.warn("[{}] Timeout to process notification: {}", id, msg.getValue()));
            ctx.getFailedMap().forEach((id, msg) -> log.warn("[{}] Failed to process notification: {}", id, msg.getValue()));
        }
        consumer.commit();
    }

    protected final void handleComponentLifecycleMsg(UUID id, ComponentLifecycleMsg componentLifecycleMsg) {
        TenantId tenantId = componentLifecycleMsg.getTenantId();
        log.debug("[{}][{}][{}] Received Lifecycle event: {}", tenantId, componentLifecycleMsg.getEntityId().getEntityType(),
                componentLifecycleMsg.getEntityId(), componentLifecycleMsg.getEvent());
        if (EntityType.TENANT_PROFILE.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            TenantProfileId tenantProfileId = new TenantProfileId(componentLifecycleMsg.getEntityId().getId());
            tenantProfileCache.evict(tenantProfileId);
            if (componentLifecycleMsg.getEvent().equals(ComponentLifecycleEvent.UPDATED)) {
                apiUsageStateService.onTenantProfileUpdate(tenantProfileId);
            }
        } else if (EntityType.TENANT.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            if (TenantId.SYS_TENANT_ID.equals(tenantId)) {
                jwtSettingsService.reloadJwtSettings();
                return;
            } else {
                tenantProfileCache.evict(tenantId);
                partitionService.evictTenantInfo(tenantId);
                if (componentLifecycleMsg.getEvent().equals(ComponentLifecycleEvent.UPDATED)) {
                    apiUsageStateService.onTenantUpdate(tenantId);
                } else if (componentLifecycleMsg.getEvent().equals(ComponentLifecycleEvent.DELETED)) {
                    apiUsageStateService.onTenantDelete(tenantId);
                    partitionService.removeTenant(tenantId);
                }
            }
        } else if (EntityType.DEVICE_PROFILE.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            deviceProfileCache.evict(tenantId, new DeviceProfileId(componentLifecycleMsg.getEntityId().getId()));
        } else if (EntityType.DEVICE.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            deviceProfileCache.evict(tenantId, new DeviceId(componentLifecycleMsg.getEntityId().getId()));
        } else if (EntityType.ASSET_PROFILE.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            assetProfileCache.evict(tenantId, new AssetProfileId(componentLifecycleMsg.getEntityId().getId()));
        } else if (EntityType.ASSET.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            assetProfileCache.evict(tenantId, new AssetId(componentLifecycleMsg.getEntityId().getId()));
        } else if (EntityType.ENTITY_VIEW.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            actorContext.getTbEntityViewService().onComponentLifecycleMsg(componentLifecycleMsg);
        } else if (EntityType.API_USAGE_STATE.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            apiUsageStateService.onApiUsageStateUpdate(tenantId);
        } else if (EntityType.CUSTOMER.equals(componentLifecycleMsg.getEntityId().getEntityType())) {
            if (componentLifecycleMsg.getEvent() == ComponentLifecycleEvent.DELETED) {
                apiUsageStateService.onCustomerDelete((CustomerId) componentLifecycleMsg.getEntityId());
            }
        }

        eventPublisher.publishEvent(componentLifecycleMsg);
        log.trace("[{}] Forwarding component lifecycle message to App Actor {}", id, componentLifecycleMsg);
        actorContext.tellWithHighPriority(componentLifecycleMsg);
    }

    protected abstract void handleNotification(UUID id, TbProtoQueueMsg<N> msg, TbCallback callback) throws Exception;

    @PreDestroy
    public void destroy() {
        stopConsumers();
        if (consumersExecutor != null) {
            consumersExecutor.shutdownNow();
        }
        if (mgmtExecutor != null) {
            mgmtExecutor.shutdownNow();
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }
}
