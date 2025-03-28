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
package org.sobeam.server.actors;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.sobeam.common.util.JacksonUtil;
import org.sobeam.rule.engine.api.MailService;
import org.sobeam.rule.engine.api.NotificationCenter;
import org.sobeam.rule.engine.api.RuleEngineDeviceStateManager;
import org.sobeam.rule.engine.api.SmsService;
import org.sobeam.rule.engine.api.notification.SlackService;
import org.sobeam.rule.engine.api.sms.SmsSenderFactory;
import org.sobeam.script.api.js.JsInvokeService;
import org.sobeam.script.api.tbel.TbelInvokeService;
import org.sobeam.server.actors.service.ActorService;
import org.sobeam.server.actors.tenant.DebugTbRateLimits;
import org.sobeam.server.cluster.TbClusterService;
import org.sobeam.server.common.data.event.ErrorEvent;
import org.sobeam.server.common.data.event.LifecycleEvent;
import org.sobeam.server.common.data.event.RuleChainDebugEvent;
import org.sobeam.server.common.data.event.RuleNodeDebugEvent;
import org.sobeam.server.common.data.id.EntityId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.plugin.ComponentLifecycleEvent;
import org.sobeam.server.common.msg.TbActorMsg;
import org.sobeam.server.common.msg.TbMsg;
import org.sobeam.server.common.msg.notification.NotificationRuleProcessor;
import org.sobeam.server.common.msg.queue.ServiceType;
import org.sobeam.server.common.msg.queue.TopicPartitionInfo;
import org.sobeam.server.common.msg.tools.TbRateLimits;
import org.sobeam.server.common.stats.TbApiUsageReportClient;
import org.sobeam.server.dao.alarm.AlarmCommentService;
import org.sobeam.server.dao.asset.AssetProfileService;
import org.sobeam.server.dao.asset.AssetService;
import org.sobeam.server.dao.attributes.AttributesService;
import org.sobeam.server.dao.audit.AuditLogService;
import org.sobeam.server.dao.cassandra.CassandraCluster;
import org.sobeam.server.dao.customer.CustomerService;
import org.sobeam.server.dao.dashboard.DashboardService;
import org.sobeam.server.dao.device.ClaimDevicesService;
import org.sobeam.server.dao.device.DeviceCredentialsService;
import org.sobeam.server.dao.device.DeviceProfileService;
import org.sobeam.server.dao.device.DeviceService;
import org.sobeam.server.dao.edge.EdgeEventService;
import org.sobeam.server.dao.edge.EdgeService;
import org.sobeam.server.dao.entity.EntityService;
import org.sobeam.server.dao.entityview.EntityViewService;
import org.sobeam.server.dao.event.EventService;
import org.sobeam.server.dao.nosql.CassandraBufferedRateReadExecutor;
import org.sobeam.server.dao.nosql.CassandraBufferedRateWriteExecutor;
import org.sobeam.server.dao.notification.NotificationRequestService;
import org.sobeam.server.dao.notification.NotificationRuleService;
import org.sobeam.server.dao.notification.NotificationTargetService;
import org.sobeam.server.dao.notification.NotificationTemplateService;
import org.sobeam.server.dao.ota.OtaPackageService;
import org.sobeam.server.dao.queue.QueueService;
import org.sobeam.server.dao.queue.QueueStatsService;
import org.sobeam.server.dao.relation.RelationService;
import org.sobeam.server.dao.resource.ResourceService;
import org.sobeam.server.dao.rule.RuleChainService;
import org.sobeam.server.dao.rule.RuleNodeStateService;
import org.sobeam.server.dao.tenant.TbTenantProfileCache;
import org.sobeam.server.dao.tenant.TenantProfileService;
import org.sobeam.server.dao.tenant.TenantService;
import org.sobeam.server.dao.timeseries.TimeseriesService;
import org.sobeam.server.dao.user.UserService;
import org.sobeam.server.dao.widget.WidgetTypeService;
import org.sobeam.server.dao.widget.WidgetsBundleService;
import org.sobeam.server.queue.discovery.DiscoveryService;
import org.sobeam.server.queue.discovery.PartitionService;
import org.sobeam.server.queue.discovery.TbServiceInfoProvider;
import org.sobeam.server.service.apiusage.TbApiUsageStateService;
import org.sobeam.server.service.component.ComponentDiscoveryService;
import org.sobeam.server.service.edge.rpc.EdgeRpcService;
import org.sobeam.server.service.entitiy.entityview.TbEntityViewService;
import org.sobeam.server.service.executors.DbCallbackExecutorService;
import org.sobeam.server.service.executors.ExternalCallExecutorService;
import org.sobeam.server.service.executors.NotificationExecutorService;
import org.sobeam.server.service.executors.PubSubRuleNodeExecutorProvider;
import org.sobeam.server.service.executors.SharedEventLoopGroupService;
import org.sobeam.server.service.mail.MailExecutorService;
import org.sobeam.server.service.profile.TbAssetProfileCache;
import org.sobeam.server.service.profile.TbDeviceProfileCache;
import org.sobeam.server.service.rpc.TbCoreDeviceRpcService;
import org.sobeam.server.service.rpc.TbRpcService;
import org.sobeam.server.service.rpc.TbRuleEngineDeviceRpcService;
import org.sobeam.server.service.session.DeviceSessionCacheService;
import org.sobeam.server.service.sms.SmsExecutorService;
import org.sobeam.server.service.state.DeviceStateService;
import org.sobeam.server.service.telemetry.AlarmSubscriptionService;
import org.sobeam.server.service.telemetry.TelemetrySubscriptionService;
import org.sobeam.server.service.transport.TbCoreToTransportService;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ActorSystemContext {

    private static final FutureCallback<Void> RULE_CHAIN_DEBUG_EVENT_ERROR_CALLBACK = new FutureCallback<>() {
        @Override
        public void onSuccess(@Nullable Void event) {

        }

        @Override
        public void onFailure(Throwable th) {
            log.error("Could not save debug Event for Rule Chain", th);
        }
    };
    private static final FutureCallback<Void> RULE_NODE_DEBUG_EVENT_ERROR_CALLBACK = new FutureCallback<>() {
        @Override
        public void onSuccess(@Nullable Void event) {

        }

        @Override
        public void onFailure(Throwable th) {
            log.error("Could not save debug Event for Node", th);
        }
    };

    private final ConcurrentMap<TenantId, DebugTbRateLimits> debugPerTenantLimits = new ConcurrentHashMap<>();

    public ConcurrentMap<TenantId, DebugTbRateLimits> getDebugPerTenantLimits() {
        return debugPerTenantLimits;
    }

    @Autowired
    @Getter
    private TbApiUsageStateService apiUsageStateService;

    @Autowired
    @Getter
    private TbApiUsageReportClient apiUsageClient;

    @Autowired
    @Getter
    @Setter
    private TbServiceInfoProvider serviceInfoProvider;

    @Getter
    @Setter
    private ActorService actorService;

    @Autowired
    @Getter
    @Setter
    private ComponentDiscoveryService componentService;

    @Autowired
    @Getter
    private DiscoveryService discoveryService;

    @Autowired
    @Getter
    private DeviceService deviceService;

    @Autowired
    @Getter
    private DeviceProfileService deviceProfileService;

    @Autowired
    @Getter
    private AssetProfileService assetProfileService;

    @Autowired
    @Getter
    private DeviceCredentialsService deviceCredentialsService;

    @Autowired(required = false)
    @Getter
    private RuleEngineDeviceStateManager deviceStateManager;

    @Autowired
    @Getter
    private TbTenantProfileCache tenantProfileCache;

    @Autowired
    @Getter
    private TbDeviceProfileCache deviceProfileCache;

    @Autowired
    @Getter
    private TbAssetProfileCache assetProfileCache;

    @Autowired
    @Getter
    private AssetService assetService;

    @Autowired
    @Getter
    private DashboardService dashboardService;

    @Autowired
    @Getter
    private TenantService tenantService;

    @Autowired
    @Getter
    private TenantProfileService tenantProfileService;

    @Autowired
    @Getter
    private CustomerService customerService;

    @Autowired
    @Getter
    private UserService userService;

    @Autowired
    @Getter
    private RuleChainService ruleChainService;

    @Autowired
    @Getter
    private RuleNodeStateService ruleNodeStateService;

    @Autowired
    @Getter
    private PartitionService partitionService;

    @Autowired
    @Getter
    private TbClusterService clusterService;

    @Autowired
    @Getter
    private TimeseriesService tsService;

    @Autowired
    @Getter
    private AttributesService attributesService;

    @Autowired
    @Getter
    private EventService eventService;

    @Autowired
    @Getter
    private RelationService relationService;

    @Autowired
    @Getter
    private AuditLogService auditLogService;

    @Autowired
    @Getter
    private EntityViewService entityViewService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private TbEntityViewService tbEntityViewService;

    @Autowired
    @Getter
    private TelemetrySubscriptionService tsSubService;

    @Autowired
    @Getter
    private AlarmSubscriptionService alarmService;

    @Autowired
    @Getter
    private AlarmCommentService alarmCommentService;

    @Autowired
    @Getter
    private JsInvokeService jsInvokeService;

    @Autowired(required = false)
    @Getter
    private TbelInvokeService tbelInvokeService;

    @Autowired
    @Getter
    private MailExecutorService mailExecutor;

    @Autowired
    @Getter
    private SmsExecutorService smsExecutor;

    @Autowired
    @Getter
    private DbCallbackExecutorService dbCallbackExecutor;

    @Autowired
    @Getter
    private ExternalCallExecutorService externalCallExecutorService;

    @Autowired
    @Getter
    private NotificationExecutorService notificationExecutor;

    @Lazy
    @Autowired
    @Getter
    private PubSubRuleNodeExecutorProvider pubSubRuleNodeExecutorProvider;

    @Autowired
    @Getter
    private SharedEventLoopGroupService sharedEventLoopGroupService;

    @Autowired
    @Getter
    private MailService mailService;

    @Autowired
    @Getter
    private SmsService smsService;

    @Autowired
    @Getter
    private SmsSenderFactory smsSenderFactory;

    @Autowired
    @Getter
    private NotificationCenter notificationCenter;

    @Autowired
    @Getter
    private NotificationRuleProcessor notificationRuleProcessor;

    @Autowired
    @Getter
    private NotificationTargetService notificationTargetService;

    @Autowired
    @Getter
    private NotificationTemplateService notificationTemplateService;

    @Autowired
    @Getter
    private NotificationRequestService notificationRequestService;

    @Autowired
    @Getter
    private NotificationRuleService notificationRuleService;

    @Autowired
    @Getter
    private SlackService slackService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private ClaimDevicesService claimDevicesService;

    @Autowired
    @Getter
    private JsInvokeStats jsInvokeStats;

    //TODO: separate context for TbCore and TbRuleEngine
    @Autowired(required = false)
    @Getter
    private DeviceStateService deviceStateService;

    @Autowired(required = false)
    @Getter
    private DeviceSessionCacheService deviceSessionCacheService;

    @Autowired(required = false)
    @Getter
    private TbCoreToTransportService tbCoreToTransportService;

    /**
     * The following Service will be null if we operate in tb-core mode
     */
    @Lazy
    @Autowired(required = false)
    @Getter
    private TbRuleEngineDeviceRpcService tbRuleEngineDeviceRpcService;

    /**
     * The following Service will be null if we operate in tb-rule-engine mode
     */
    @Lazy
    @Autowired(required = false)
    @Getter
    private TbCoreDeviceRpcService tbCoreDeviceRpcService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private EdgeService edgeService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private EdgeEventService edgeEventService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private EdgeRpcService edgeRpcService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private ResourceService resourceService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private OtaPackageService otaPackageService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private TbRpcService tbRpcService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private QueueService queueService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private QueueStatsService queueStatsService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private WidgetsBundleService widgetsBundleService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private WidgetTypeService widgetTypeService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private EntityService entityService;

    @Value("${actors.session.max_concurrent_sessions_per_device:1}")
    @Getter
    private long maxConcurrentSessionsPerDevice;

    @Value("${actors.session.sync.timeout:10000}")
    @Getter
    private long syncSessionTimeout;

    @Value("${actors.rule.chain.error_persist_frequency:3000}")
    @Getter
    private long ruleChainErrorPersistFrequency;

    @Value("${actors.rule.node.error_persist_frequency:3000}")
    @Getter
    private long ruleNodeErrorPersistFrequency;

    @Value("${actors.statistics.enabled:true}")
    @Getter
    private boolean statisticsEnabled;

    @Value("${actors.statistics.persist_frequency:3600000}")
    @Getter
    private long statisticsPersistFrequency;

    @Value("${edges.enabled:true}")
    @Getter
    private boolean edgesEnabled;

    @Value("${cache.type:caffeine}")
    @Getter
    private String cacheType;

    @Getter
    private boolean localCacheType;

    @PostConstruct
    public void init() {
        this.localCacheType = "caffeine".equals(cacheType);
    }

    @Scheduled(fixedDelayString = "${actors.statistics.js_print_interval_ms}")
    public void printStats() {
        if (statisticsEnabled) {
            if (jsInvokeStats.getRequests() > 0 || jsInvokeStats.getResponses() > 0 || jsInvokeStats.getFailures() > 0) {
                log.info("Rule Engine JS Invoke Stats: requests [{}] responses [{}] failures [{}]",
                        jsInvokeStats.getRequests(), jsInvokeStats.getResponses(), jsInvokeStats.getFailures());
                jsInvokeStats.reset();
            }
        }
    }

    @Value("${actors.tenant.create_components_on_init:true}")
    @Getter
    private boolean tenantComponentsInitEnabled;

    @Value("${actors.rule.allow_system_mail_service:true}")
    @Getter
    private boolean allowSystemMailService;

    @Value("${actors.rule.allow_system_sms_service:true}")
    @Getter
    private boolean allowSystemSmsService;

    @Value("${transport.sessions.inactivity_timeout:300000}")
    @Getter
    private long sessionInactivityTimeout;

    @Value("${transport.sessions.report_timeout:3000}")
    @Getter
    private long sessionReportTimeout;

    @Value("${actors.rule.chain.debug_mode_rate_limits_per_tenant.enabled:true}")
    @Getter
    private boolean debugPerTenantEnabled;

    @Value("${actors.rule.chain.debug_mode_rate_limits_per_tenant.configuration:50000:3600}")
    @Getter
    private String debugPerTenantLimitsConfiguration;

    @Value("${actors.rpc.submit_strategy:BURST}")
    @Getter
    private String rpcSubmitStrategy;

    @Value("${actors.rpc.response_timeout_ms:30000}")
    @Getter
    private long rpcResponseTimeout;

    @Value("${actors.rpc.max_retries:5}")
    @Getter
    private int maxRpcRetries;

    @Value("${actors.rule.external.force_ack:false}")
    @Getter
    private boolean externalNodeForceAck;

    @Value("${state.rule.node.deviceState.rateLimit:1:1,30:60,60:3600}")
    @Getter
    private String deviceStateNodeRateLimitConfig;

    @Getter
    @Setter
    private TbActorSystem actorSystem;

    @Setter
    private TbActorRef appActor;

    @Getter
    @Setter
    private TbActorRef statsActor;

    @Autowired(required = false)
    @Getter
    private CassandraCluster cassandraCluster;

    @Autowired(required = false)
    @Getter
    private CassandraBufferedRateReadExecutor cassandraBufferedRateReadExecutor;

    @Autowired(required = false)
    @Getter
    private CassandraBufferedRateWriteExecutor cassandraBufferedRateWriteExecutor;

    @Autowired(required = false)
    @Getter
    private RedisTemplate<String, Object> redisTemplate;

    public ScheduledExecutorService getScheduler() {
        return actorSystem.getScheduler();
    }

    public void persistError(TenantId tenantId, EntityId entityId, String method, Exception e) {
        eventService.saveAsync(ErrorEvent.builder()
                .tenantId(tenantId)
                .entityId(entityId.getId())
                .serviceId(getServiceId())
                .method(method)
                .error(toString(e)).build());
    }

    public void persistLifecycleEvent(TenantId tenantId, EntityId entityId, ComponentLifecycleEvent lcEvent, Exception e) {
        LifecycleEvent.LifecycleEventBuilder event = LifecycleEvent.builder()
                .tenantId(tenantId)
                .entityId(entityId.getId())
                .serviceId(getServiceId())
                .lcEventType(lcEvent.name());

        if (e != null) {
            event.success(false).error(toString(e));
        } else {
            event.success(true);
        }

        eventService.saveAsync(event.build());
    }

    private String toString(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public TopicPartitionInfo resolve(ServiceType serviceType, TenantId tenantId, EntityId entityId) {
        return partitionService.resolve(serviceType, tenantId, entityId);
    }

    public TopicPartitionInfo resolve(ServiceType serviceType, String queueName, TenantId tenantId, EntityId entityId) {
        return partitionService.resolve(serviceType, queueName, tenantId, entityId);
    }

    public TopicPartitionInfo resolve(TenantId tenantId, EntityId entityId, TbMsg msg) {
        return partitionService.resolve(ServiceType.TB_RULE_ENGINE, msg.getQueueName(), tenantId, entityId, msg.getPartition());
    }

    public String getServiceId() {
        return serviceInfoProvider.getServiceId();
    }

    public void persistDebugInput(TenantId tenantId, EntityId entityId, TbMsg tbMsg, String relationType) {
        persistDebugAsync(tenantId, entityId, "IN", tbMsg, relationType, null, null);
    }

    public void persistDebugInput(TenantId tenantId, EntityId entityId, TbMsg tbMsg, String relationType, Throwable error) {
        persistDebugAsync(tenantId, entityId, "IN", tbMsg, relationType, error, null);
    }

    public void persistDebugOutput(TenantId tenantId, EntityId entityId, TbMsg tbMsg, String relationType, Throwable error, String failureMessage) {
        persistDebugAsync(tenantId, entityId, "OUT", tbMsg, relationType, error, failureMessage);
    }

    public void persistDebugOutput(TenantId tenantId, EntityId entityId, TbMsg tbMsg, String relationType, Throwable error) {
        persistDebugAsync(tenantId, entityId, "OUT", tbMsg, relationType, error, null);
    }

    public void persistDebugOutput(TenantId tenantId, EntityId entityId, TbMsg tbMsg, String relationType) {
        persistDebugAsync(tenantId, entityId, "OUT", tbMsg, relationType, null, null);
    }

    private void persistDebugAsync(TenantId tenantId, EntityId entityId, String type, TbMsg tbMsg, String relationType, Throwable error, String failureMessage) {
        if (checkLimits(tenantId, tbMsg, error)) {
            try {
                RuleNodeDebugEvent.RuleNodeDebugEventBuilder event = RuleNodeDebugEvent.builder()
                        .tenantId(tenantId)
                        .entityId(entityId.getId())
                        .serviceId(getServiceId())
                        .eventType(type)
                        .eventEntity(tbMsg.getOriginator())
                        .msgId(tbMsg.getId())
                        .msgType(tbMsg.getType())
                        .dataType(tbMsg.getDataType().name())
                        .relationType(relationType)
                        .data(tbMsg.getData())
                        .metadata(JacksonUtil.toString(tbMsg.getMetaData().getData()));

                if (error != null) {
                    event.error(toString(error));
                } else if (failureMessage != null) {
                    event.error(failureMessage);
                }

                ListenableFuture<Void> future = eventService.saveAsync(event.build());
                Futures.addCallback(future, RULE_NODE_DEBUG_EVENT_ERROR_CALLBACK, MoreExecutors.directExecutor());
            } catch (IllegalArgumentException ex) {
                log.warn("Failed to persist rule node debug message", ex);
            }
        }
    }

    private boolean checkLimits(TenantId tenantId, TbMsg tbMsg, Throwable error) {
        if (debugPerTenantEnabled) {
            DebugTbRateLimits debugTbRateLimits = debugPerTenantLimits.computeIfAbsent(tenantId, id ->
                    new DebugTbRateLimits(new TbRateLimits(debugPerTenantLimitsConfiguration), false));

            if (!debugTbRateLimits.getTbRateLimits().tryConsume()) {
                if (!debugTbRateLimits.isRuleChainEventSaved()) {
                    persistRuleChainDebugModeEvent(tenantId, tbMsg.getRuleChainId(), error);
                    debugTbRateLimits.setRuleChainEventSaved(true);
                }
                if (log.isTraceEnabled()) {
                    log.trace("[{}] Tenant level debug mode rate limit detected: {}", tenantId, tbMsg);
                }
                return false;
            }
        }
        return true;
    }

    private void persistRuleChainDebugModeEvent(TenantId tenantId, EntityId entityId, Throwable error) {
        RuleChainDebugEvent.RuleChainDebugEventBuilder event = RuleChainDebugEvent.builder()
                .tenantId(tenantId)
                .entityId(entityId.getId())
                .serviceId(getServiceId())
                .message("Reached debug mode rate limit!");
        if (error != null) {
            event.error(toString(error));
        }

        ListenableFuture<Void> future = eventService.saveAsync(event.build());
        Futures.addCallback(future, RULE_CHAIN_DEBUG_EVENT_ERROR_CALLBACK, MoreExecutors.directExecutor());
    }

    public static Exception toException(Throwable error) {
        return Exception.class.isInstance(error) ? (Exception) error : new Exception(error);
    }

    public void tell(TbActorMsg tbActorMsg) {
        appActor.tell(tbActorMsg);
    }

    public void tellWithHighPriority(TbActorMsg tbActorMsg) {
        appActor.tellWithHighPriority(tbActorMsg);
    }

    public void schedulePeriodicMsgWithDelay(TbActorRef ctx, TbActorMsg msg, long delayInMs, long periodInMs) {
        log.debug("Scheduling periodic msg {} every {} ms with delay {} ms", msg, periodInMs, delayInMs);
        getScheduler().scheduleWithFixedDelay(() -> ctx.tell(msg), delayInMs, periodInMs, TimeUnit.MILLISECONDS);
    }

    public void scheduleMsgWithDelay(TbActorRef ctx, TbActorMsg msg, long delayInMs) {
        log.debug("Scheduling msg {} with delay {} ms", msg, delayInMs);
        if (delayInMs > 0) {
            getScheduler().schedule(() -> ctx.tell(msg), delayInMs, TimeUnit.MILLISECONDS);
        } else {
            ctx.tell(msg);
        }
    }

}
