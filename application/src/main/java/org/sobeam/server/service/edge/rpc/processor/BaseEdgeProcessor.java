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
package org.sobeam.server.service.edge.rpc.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.sobeam.common.util.JacksonUtil;
import org.sobeam.server.cluster.TbClusterService;
import org.sobeam.server.common.data.AttributeScope;
import org.sobeam.server.common.data.Dashboard;
import org.sobeam.server.common.data.Device;
import org.sobeam.server.common.data.DeviceProfile;
import org.sobeam.server.common.data.EdgeUtils;
import org.sobeam.server.common.data.EntityType;
import org.sobeam.server.common.data.EntityView;
import org.sobeam.server.common.data.TbResource;
import org.sobeam.server.common.data.asset.Asset;
import org.sobeam.server.common.data.asset.AssetProfile;
import org.sobeam.server.common.data.edge.Edge;
import org.sobeam.server.common.data.edge.EdgeEvent;
import org.sobeam.server.common.data.edge.EdgeEventActionType;
import org.sobeam.server.common.data.edge.EdgeEventType;
import org.sobeam.server.common.data.id.AssetId;
import org.sobeam.server.common.data.id.CustomerId;
import org.sobeam.server.common.data.id.DashboardId;
import org.sobeam.server.common.data.id.DeviceId;
import org.sobeam.server.common.data.id.EdgeId;
import org.sobeam.server.common.data.id.EntityId;
import org.sobeam.server.common.data.id.EntityIdFactory;
import org.sobeam.server.common.data.id.EntityViewId;
import org.sobeam.server.common.data.id.RuleChainId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.id.UserId;
import org.sobeam.server.common.data.kv.AttributeKvEntry;
import org.sobeam.server.common.data.msg.TbMsgType;
import org.sobeam.server.common.data.page.PageDataIterable;
import org.sobeam.server.common.data.page.PageDataIterableByTenantIdEntityId;
import org.sobeam.server.common.data.relation.EntityRelation;
import org.sobeam.server.common.data.relation.RelationTypeGroup;
import org.sobeam.server.common.data.rule.RuleChain;
import org.sobeam.server.common.data.rule.RuleChainConnectionInfo;
import org.sobeam.server.common.msg.TbMsg;
import org.sobeam.server.common.msg.TbMsgDataType;
import org.sobeam.server.common.msg.TbMsgMetaData;
import org.sobeam.server.dao.alarm.AlarmCommentService;
import org.sobeam.server.dao.alarm.AlarmService;
import org.sobeam.server.dao.asset.AssetProfileService;
import org.sobeam.server.dao.asset.AssetService;
import org.sobeam.server.dao.attributes.AttributesService;
import org.sobeam.server.dao.customer.CustomerService;
import org.sobeam.server.dao.dashboard.DashboardService;
import org.sobeam.server.dao.device.DeviceCredentialsService;
import org.sobeam.server.dao.device.DeviceProfileService;
import org.sobeam.server.dao.device.DeviceService;
import org.sobeam.server.dao.edge.EdgeEventService;
import org.sobeam.server.dao.edge.EdgeService;
import org.sobeam.server.dao.edge.EdgeSynchronizationManager;
import org.sobeam.server.dao.entityview.EntityViewService;
import org.sobeam.server.dao.notification.NotificationRuleService;
import org.sobeam.server.dao.notification.NotificationTargetService;
import org.sobeam.server.dao.notification.NotificationTemplateService;
import org.sobeam.server.dao.oauth2.OAuth2Service;
import org.sobeam.server.dao.ota.OtaPackageService;
import org.sobeam.server.dao.queue.QueueService;
import org.sobeam.server.dao.relation.RelationService;
import org.sobeam.server.dao.resource.ResourceService;
import org.sobeam.server.dao.rule.RuleChainService;
import org.sobeam.server.dao.service.DataValidator;
import org.sobeam.server.dao.tenant.TenantProfileService;
import org.sobeam.server.dao.tenant.TenantService;
import org.sobeam.server.dao.user.UserService;
import org.sobeam.server.dao.widget.WidgetTypeService;
import org.sobeam.server.dao.widget.WidgetsBundleService;
import org.sobeam.server.gen.edge.v1.EdgeVersion;
import org.sobeam.server.gen.edge.v1.UpdateMsgType;
import org.sobeam.server.gen.transport.TransportProtos;
import org.sobeam.server.queue.TbQueueCallback;
import org.sobeam.server.queue.TbQueueMsgMetadata;
import org.sobeam.server.queue.discovery.PartitionService;
import org.sobeam.server.queue.provider.TbQueueProducerProvider;
import org.sobeam.server.service.edge.rpc.constructor.alarm.AlarmMsgConstructorFactory;
import org.sobeam.server.service.edge.rpc.constructor.asset.AssetMsgConstructorFactory;
import org.sobeam.server.service.edge.rpc.constructor.customer.CustomerMsgConstructorFactory;
import org.sobeam.server.service.edge.rpc.constructor.dashboard.DashboardMsgConstructorFactory;
import org.sobeam.server.service.edge.rpc.constructor.device.DeviceMsgConstructorFactory;
import org.sobeam.server.service.edge.rpc.constructor.edge.EdgeMsgConstructor;
import org.sobeam.server.service.edge.rpc.constructor.entityview.EntityViewMsgConstructorFactory;
import org.sobeam.server.service.edge.rpc.constructor.notification.NotificationMsgConstructor;
import org.sobeam.server.service.edge.rpc.constructor.oauth2.OAuth2MsgConstructor;
import org.sobeam.server.service.edge.rpc.constructor.ota.OtaPackageMsgConstructorFactory;
import org.sobeam.server.service.edge.rpc.constructor.queue.QueueMsgConstructorFactory;
import org.sobeam.server.service.edge.rpc.constructor.relation.RelationMsgConstructorFactory;
import org.sobeam.server.service.edge.rpc.constructor.resource.ResourceMsgConstructorFactory;
import org.sobeam.server.service.edge.rpc.constructor.rule.RuleChainMsgConstructorFactory;
import org.sobeam.server.service.edge.rpc.constructor.settings.AdminSettingsMsgConstructorFactory;
import org.sobeam.server.service.edge.rpc.constructor.telemetry.EntityDataMsgConstructor;
import org.sobeam.server.service.edge.rpc.constructor.tenant.TenantMsgConstructorFactory;
import org.sobeam.server.service.edge.rpc.constructor.user.UserMsgConstructorFactory;
import org.sobeam.server.service.edge.rpc.constructor.widget.WidgetMsgConstructorFactory;
import org.sobeam.server.service.edge.rpc.processor.alarm.AlarmEdgeProcessorFactory;
import org.sobeam.server.service.edge.rpc.processor.asset.AssetEdgeProcessorFactory;
import org.sobeam.server.service.edge.rpc.processor.entityview.EntityViewProcessorFactory;
import org.sobeam.server.service.entitiy.TbLogEntityActionService;
import org.sobeam.server.service.executors.DbCallbackExecutorService;
import org.sobeam.server.service.profile.TbAssetProfileCache;
import org.sobeam.server.service.profile.TbDeviceProfileCache;
import org.sobeam.server.service.state.DefaultDeviceStateService;
import org.sobeam.server.service.state.DeviceStateService;
import org.sobeam.server.service.telemetry.TelemetrySubscriptionService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public abstract class BaseEdgeProcessor {

    protected static final Lock deviceCreationLock = new ReentrantLock();
    protected static final Lock assetCreationLock = new ReentrantLock();

    protected static final int DEFAULT_PAGE_SIZE = 100;

    @Autowired
    protected TelemetrySubscriptionService tsSubService;

    @Autowired
    protected TbLogEntityActionService logEntityActionService;

    @Autowired
    protected RuleChainService ruleChainService;

    @Autowired
    protected AlarmService alarmService;

    @Autowired
    protected AlarmCommentService alarmCommentService;

    @Autowired
    protected DeviceService deviceService;

    @Autowired
    protected TbDeviceProfileCache deviceProfileCache;

    @Autowired
    protected TbAssetProfileCache assetProfileCache;

    @Autowired
    protected DashboardService dashboardService;

    @Autowired
    protected AssetService assetService;

    @Autowired
    protected EntityViewService entityViewService;

    @Autowired
    protected TenantService tenantService;

    @Autowired
    protected TenantProfileService tenantProfileService;

    @Autowired
    protected EdgeService edgeService;

    @Autowired
    protected CustomerService customerService;

    @Autowired
    protected UserService userService;

    @Autowired
    protected DeviceProfileService deviceProfileService;

    @Autowired
    protected AssetProfileService assetProfileService;

    @Autowired
    protected RelationService relationService;

    @Autowired
    protected DeviceCredentialsService deviceCredentialsService;

    @Autowired
    protected AttributesService attributesService;

    @Autowired
    protected TbClusterService tbClusterService;

    @Autowired
    protected DeviceStateService deviceStateService;

    @Autowired
    protected EdgeEventService edgeEventService;

    @Autowired
    protected WidgetsBundleService widgetsBundleService;

    @Autowired
    protected WidgetTypeService widgetTypeService;

    @Autowired
    protected OtaPackageService otaPackageService;

    @Autowired
    protected QueueService queueService;

    @Autowired
    protected PartitionService partitionService;

    @Autowired
    protected ResourceService resourceService;

    @Autowired
    protected NotificationRuleService notificationRuleService;

    @Autowired
    protected NotificationTargetService notificationTargetService;

    @Autowired
    protected NotificationTemplateService notificationTemplateService;

    @Autowired
    protected OAuth2Service oAuth2Service;

    @Autowired
    @Lazy
    protected TbQueueProducerProvider producerProvider;

    @Autowired
    protected DataValidator<Device> deviceValidator;

    @Autowired
    protected DataValidator<DeviceProfile> deviceProfileValidator;

    @Autowired
    protected DataValidator<Asset> assetValidator;

    @Autowired
    protected DataValidator<AssetProfile> assetProfileValidator;

    @Autowired
    protected DataValidator<Dashboard> dashboardValidator;

    @Autowired
    protected DataValidator<EntityView> entityViewValidator;

    @Autowired
    protected DataValidator<TbResource> resourceValidator;

    @Autowired
    protected EdgeMsgConstructor edgeMsgConstructor;

    @Autowired
    protected EntityDataMsgConstructor entityDataMsgConstructor;

    @Autowired
    protected NotificationMsgConstructor notificationMsgConstructor;

    @Autowired
    protected OAuth2MsgConstructor oAuth2MsgConstructor;


    @Autowired
    protected RuleChainMsgConstructorFactory ruleChainMsgConstructorFactory;

    @Autowired
    protected AlarmMsgConstructorFactory alarmMsgConstructorFactory;

    @Autowired
    protected DeviceMsgConstructorFactory deviceMsgConstructorFactory;

    @Autowired
    protected AssetMsgConstructorFactory assetMsgConstructorFactory;

    @Autowired
    protected EntityViewMsgConstructorFactory entityViewMsgConstructorFactory;

    @Autowired
    protected DashboardMsgConstructorFactory dashboardMsgConstructorFactory;

    @Autowired
    protected RelationMsgConstructorFactory relationMsgConstructorFactory;

    @Autowired
    protected UserMsgConstructorFactory userMsgConstructorFactory;

    @Autowired
    protected CustomerMsgConstructorFactory customerMsgConstructorFactory;

    @Autowired
    protected TenantMsgConstructorFactory tenantMsgConstructorFactory;

    @Autowired
    protected WidgetMsgConstructorFactory widgetMsgConstructorFactory;

    @Autowired
    protected AdminSettingsMsgConstructorFactory adminSettingsMsgConstructorFactory;

    @Autowired
    protected OtaPackageMsgConstructorFactory otaPackageMsgConstructorFactory;

    @Autowired
    protected QueueMsgConstructorFactory queueMsgConstructorFactory;

    @Autowired
    protected ResourceMsgConstructorFactory resourceMsgConstructorFactory;

    @Autowired
    protected AlarmEdgeProcessorFactory alarmEdgeProcessorFactory;

    @Autowired
    protected AssetEdgeProcessorFactory assetEdgeProcessorFactory;

    @Autowired
    protected EntityViewProcessorFactory entityViewProcessorFactory;

    @Autowired
    protected EdgeSynchronizationManager edgeSynchronizationManager;

    @Autowired
    protected DbCallbackExecutorService dbCallbackExecutorService;

    protected ListenableFuture<Void> saveEdgeEvent(TenantId tenantId,
                                                   EdgeId edgeId,
                                                   EdgeEventType type,
                                                   EdgeEventActionType action,
                                                   EntityId entityId,
                                                   JsonNode body) {
        ListenableFuture<Optional<AttributeKvEntry>> future =
                attributesService.find(tenantId, edgeId, AttributeScope.SERVER_SCOPE, DefaultDeviceStateService.ACTIVITY_STATE);
        return Futures.transformAsync(future, activeOpt -> {
            if (activeOpt.isEmpty()) {
                log.trace("Edge is not activated. Skipping event. tenantId [{}], edgeId [{}], type[{}], " +
                                "action [{}], entityId [{}], body [{}]",
                        tenantId, edgeId, type, action, entityId, body);
                return Futures.immediateFuture(null);
            }
            if (activeOpt.get().getBooleanValue().isPresent() && activeOpt.get().getBooleanValue().get()) {
                return doSaveEdgeEvent(tenantId, edgeId, type, action, entityId, body);
            } else {
                if (doSaveIfEdgeIsOffline(type, action)) {
                    return doSaveEdgeEvent(tenantId, edgeId, type, action, entityId, body);
                } else {
                    log.trace("Edge is not active at the moment. Skipping event. tenantId [{}], edgeId [{}], type[{}], " +
                                    "action [{}], entityId [{}], body [{}]",
                            tenantId, edgeId, type, action, entityId, body);
                    return Futures.immediateFuture(null);
                }
            }
        }, dbCallbackExecutorService);
    }

    private boolean doSaveIfEdgeIsOffline(EdgeEventType type,
                                          EdgeEventActionType action) {
        return switch (action) {
            case TIMESERIES_UPDATED, ALARM_ACK, ALARM_CLEAR, ALARM_ASSIGNED, ALARM_UNASSIGNED, CREDENTIALS_REQUEST, ADDED_COMMENT, UPDATED_COMMENT ->
                    true;
            default -> switch (type) {
                case ALARM, ALARM_COMMENT, RULE_CHAIN, RULE_CHAIN_METADATA, USER, CUSTOMER, TENANT, TENANT_PROFILE, WIDGETS_BUNDLE, WIDGET_TYPE,
                        ADMIN_SETTINGS, OTA_PACKAGE, QUEUE, RELATION, NOTIFICATION_TEMPLATE, NOTIFICATION_TARGET, NOTIFICATION_RULE ->
                        true;
                default -> false;
            };
        };
    }

    private ListenableFuture<Void> doSaveEdgeEvent(TenantId tenantId, EdgeId edgeId, EdgeEventType type, EdgeEventActionType action, EntityId entityId, JsonNode body) {
        log.debug("Pushing event to edge queue. tenantId [{}], edgeId [{}], type[{}], " +
                        "action [{}], entityId [{}], body [{}]",
                tenantId, edgeId, type, action, entityId, body);

        EdgeEvent edgeEvent = EdgeUtils.constructEdgeEvent(tenantId, edgeId, type, action, entityId, body);

        return edgeEventService.saveAsync(edgeEvent);
    }

    protected ListenableFuture<Void> processActionForAllEdges(TenantId tenantId, EdgeEventType type,
                                                              EdgeEventActionType actionType, EntityId entityId,
                                                              JsonNode body, EdgeId sourceEdgeId) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        if (TenantId.SYS_TENANT_ID.equals(tenantId)) {
            PageDataIterable<TenantId> tenantIds = new PageDataIterable<>(link -> tenantService.findTenantsIds(link), 1024);
            for (TenantId tenantId1 : tenantIds) {
                futures.addAll(processActionForAllEdgesByTenantId(tenantId1, type, actionType, entityId, body, sourceEdgeId));
            }
        } else {
            futures = processActionForAllEdgesByTenantId(tenantId, type, actionType, entityId, null, sourceEdgeId);
        }
        return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
    }

    private List<ListenableFuture<Void>> processActionForAllEdgesByTenantId(TenantId tenantId,
                                                                            EdgeEventType type,
                                                                            EdgeEventActionType actionType,
                                                                            EntityId entityId,
                                                                            JsonNode body,
                                                                            EdgeId sourceEdgeId) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        PageDataIterable<Edge> edges = new PageDataIterable<>(link -> edgeService.findEdgesByTenantId(tenantId, link), 1024);
        for (Edge edge : edges) {
            if (!edge.getId().equals(sourceEdgeId)) {
                futures.add(saveEdgeEvent(tenantId, edge.getId(), type, actionType, entityId, body));
            }
        }
        return futures;
    }

    protected ListenableFuture<Void> handleUnsupportedMsgType(UpdateMsgType msgType) {
        String errMsg = String.format("Unsupported msg type %s", msgType);
        log.error(errMsg);
        return Futures.immediateFailedFuture(new RuntimeException(errMsg));
    }

    protected UpdateMsgType getUpdateMsgType(EdgeEventActionType actionType) {
        return switch (actionType) {
            case UPDATED, CREDENTIALS_UPDATED, ASSIGNED_TO_CUSTOMER, UNASSIGNED_FROM_CUSTOMER, UPDATED_COMMENT ->
                    UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE;
            case ADDED, ASSIGNED_TO_EDGE, RELATION_ADD_OR_UPDATE, ADDED_COMMENT -> UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE;
            case DELETED, UNASSIGNED_FROM_EDGE, RELATION_DELETED, DELETED_COMMENT, ALARM_DELETE -> UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE;
            case ALARM_ACK -> UpdateMsgType.ALARM_ACK_RPC_MESSAGE;
            case ALARM_CLEAR -> UpdateMsgType.ALARM_CLEAR_RPC_MESSAGE;
            default -> throw new RuntimeException("Unsupported actionType [" + actionType + "]");
        };
    }

    public ListenableFuture<Void> processEntityNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
        EntityId entityId = EntityIdFactory.getByEdgeEventTypeAndUuid(type, new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        EdgeId originatorEdgeId = safeGetEdgeId(edgeNotificationMsg.getOriginatorEdgeIdMSB(), edgeNotificationMsg.getOriginatorEdgeIdLSB());
        if (type.isAllEdgesRelated()) {
            return processEntityNotificationForAllEdges(tenantId, type, actionType, entityId, originatorEdgeId);
        } else {
            JsonNode body = JacksonUtil.toJsonNode(edgeNotificationMsg.getBody());
            EdgeId edgeId = safeGetEdgeId(edgeNotificationMsg.getEdgeIdMSB(), edgeNotificationMsg.getEdgeIdLSB());
            switch (actionType) {
                case UPDATED:
                case CREDENTIALS_UPDATED:
                case ASSIGNED_TO_CUSTOMER:
                case UNASSIGNED_FROM_CUSTOMER:
                    if (edgeId != null) {
                        return saveEdgeEvent(tenantId, edgeId, type, actionType, entityId, body);
                    } else {
                        return processNotificationToRelatedEdges(tenantId, entityId, type, actionType, originatorEdgeId);
                    }
                case DELETED:
                    EdgeEventActionType deleted = EdgeEventActionType.DELETED;
                    if (edgeId != null) {
                        return saveEdgeEvent(tenantId, edgeId, type, deleted, entityId, body);
                    } else {
                        return Futures.transform(Futures.allAsList(processActionForAllEdgesByTenantId(tenantId, type, deleted, entityId, body, originatorEdgeId)),
                                voids -> null, dbCallbackExecutorService);
                    }
                case ASSIGNED_TO_EDGE:
                case UNASSIGNED_FROM_EDGE:
                    if (originatorEdgeId == null) {
                        ListenableFuture<Void> future = saveEdgeEvent(tenantId, edgeId, type, actionType, entityId, body);
                        return Futures.transformAsync(future, unused -> {
                            if (type.equals(EdgeEventType.RULE_CHAIN)) {
                                return updateDependentRuleChains(tenantId, new RuleChainId(entityId.getId()), edgeId);
                            } else {
                                return Futures.immediateFuture(null);
                            }
                        }, dbCallbackExecutorService);
                    } else {
                        return Futures.immediateFuture(null);
                    }
                default:
                    return Futures.immediateFuture(null);
            }
        }
    }

    protected EdgeId safeGetEdgeId(long edgeIdMSB, long edgeIdLSB) {
        if (edgeIdMSB != 0 && edgeIdLSB != 0) {
            return new EdgeId(new UUID(edgeIdMSB, edgeIdLSB));
        } else {
            return null;
        }
    }

    private ListenableFuture<Void> processNotificationToRelatedEdges(TenantId tenantId, EntityId entityId, EdgeEventType type,
                                                                     EdgeEventActionType actionType, EdgeId sourceEdgeId) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        PageDataIterableByTenantIdEntityId<EdgeId> edgeIds =
                new PageDataIterableByTenantIdEntityId<>(edgeService::findRelatedEdgeIdsByEntityId, tenantId, entityId, DEFAULT_PAGE_SIZE);
        for (EdgeId relatedEdgeId : edgeIds) {
            if (!relatedEdgeId.equals(sourceEdgeId)) {
                futures.add(saveEdgeEvent(tenantId, relatedEdgeId, type, actionType, entityId, null));
            }
        }
        return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
    }

    private ListenableFuture<Void> updateDependentRuleChains(TenantId tenantId, RuleChainId processingRuleChainId, EdgeId edgeId) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        PageDataIterable<RuleChain> ruleChains = new PageDataIterable<>(link -> ruleChainService.findRuleChainsByTenantIdAndEdgeId(tenantId, edgeId, link), 1024);
        for (RuleChain ruleChain : ruleChains) {
            List<RuleChainConnectionInfo> connectionInfos =
                    ruleChainService.loadRuleChainMetaData(ruleChain.getTenantId(), ruleChain.getId()).getRuleChainConnections();
            if (connectionInfos != null && !connectionInfos.isEmpty()) {
                for (RuleChainConnectionInfo connectionInfo : connectionInfos) {
                    if (connectionInfo.getTargetRuleChainId().equals(processingRuleChainId)) {
                        futures.add(saveEdgeEvent(tenantId,
                                edgeId,
                                EdgeEventType.RULE_CHAIN_METADATA,
                                EdgeEventActionType.UPDATED,
                                ruleChain.getId(),
                                null));
                    }
                }
            }
        }
        return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
    }

    private ListenableFuture<Void> processEntityNotificationForAllEdges(TenantId tenantId, EdgeEventType type, EdgeEventActionType actionType, EntityId entityId, EdgeId sourceEdgeId) {
        return switch (actionType) {
            case ADDED, UPDATED, DELETED, CREDENTIALS_UPDATED -> // used by USER entity
                    processActionForAllEdges(tenantId, type, actionType, entityId, null, sourceEdgeId);
            default -> Futures.immediateFuture(null);
        };
    }

    protected EntityId constructEntityId(String entityTypeStr, long entityIdMSB, long entityIdLSB) {
        EntityType entityType = EntityType.valueOf(entityTypeStr);
        return switch (entityType) {
            case DEVICE -> new DeviceId(new UUID(entityIdMSB, entityIdLSB));
            case ASSET -> new AssetId(new UUID(entityIdMSB, entityIdLSB));
            case ENTITY_VIEW -> new EntityViewId(new UUID(entityIdMSB, entityIdLSB));
            case DASHBOARD -> new DashboardId(new UUID(entityIdMSB, entityIdLSB));
            case TENANT -> TenantId.fromUUID(new UUID(entityIdMSB, entityIdLSB));
            case CUSTOMER -> new CustomerId(new UUID(entityIdMSB, entityIdLSB));
            case USER -> new UserId(new UUID(entityIdMSB, entityIdLSB));
            case EDGE -> new EdgeId(new UUID(entityIdMSB, entityIdLSB));
            default -> {
                log.warn("Unsupported entity type [{}] during construct of entity id. entityIdMSB [{}], entityIdLSB [{}]",
                        entityTypeStr, entityIdMSB, entityIdLSB);
                yield null;
            }
        };
    }

    protected UUID safeGetUUID(long mSB, long lSB) {
        return mSB != 0 && lSB != 0 ? new UUID(mSB, lSB) : null;
    }

    protected CustomerId safeGetCustomerId(long mSB, long lSB) {
        CustomerId customerId = null;
        UUID customerUUID = safeGetUUID(mSB, lSB);
        if (customerUUID != null) {
            customerId = new CustomerId(customerUUID);
        }
        return customerId;
    }

    protected boolean isEntityExists(TenantId tenantId, EntityId entityId) {
        return switch (entityId.getEntityType()) {
            case TENANT -> tenantService.findTenantById(tenantId) != null;
            case DEVICE -> deviceService.findDeviceById(tenantId, new DeviceId(entityId.getId())) != null;
            case ASSET -> assetService.findAssetById(tenantId, new AssetId(entityId.getId())) != null;
            case ENTITY_VIEW -> entityViewService.findEntityViewById(tenantId, new EntityViewId(entityId.getId())) != null;
            case CUSTOMER -> customerService.findCustomerById(tenantId, new CustomerId(entityId.getId())) != null;
            case USER -> userService.findUserById(tenantId, new UserId(entityId.getId())) != null;
            case DASHBOARD -> dashboardService.findDashboardById(tenantId, new DashboardId(entityId.getId())) != null;
            case EDGE -> edgeService.findEdgeById(tenantId, new EdgeId(entityId.getId())) != null;
            default -> false;
        };
    }

    protected void createRelationFromEdge(TenantId tenantId, EdgeId edgeId, EntityId entityId) {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(edgeId);
        relation.setTo(entityId);
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        relation.setType(EntityRelation.EDGE_TYPE);
        relationService.saveRelation(tenantId, relation);
    }

    protected TbMsgMetaData getEdgeActionTbMsgMetaData(Edge edge, CustomerId customerId) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("edgeId", edge.getId().toString());
        metaData.putValue("edgeName", edge.getName());
        if (customerId != null && !customerId.isNullUid()) {
            metaData.putValue("customerId", customerId.toString());
        }
        return metaData;
    }

    protected void pushEntityEventToRuleEngine(TenantId tenantId, EntityId entityId, CustomerId customerId,
                                               TbMsgType msgType, String msgData, TbMsgMetaData metaData) {
        TbMsg tbMsg = TbMsg.newMsg(msgType, entityId, customerId, metaData, TbMsgDataType.JSON, msgData);
        tbClusterService.pushMsgToRuleEngine(tenantId, entityId, tbMsg, new TbQueueCallback() {
            @Override
            public void onSuccess(TbQueueMsgMetadata metadata) {
                log.debug("[{}] Successfully send ENTITY_CREATED EVENT to rule engine [{}]", tenantId, msgData);
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("[{}] Failed to send ENTITY_CREATED EVENT to rule engine [{}]", tenantId, msgData, t);
            }
        });
    }

    protected AssetProfile checkIfAssetProfileDefaultFieldsAssignedToEdge(TenantId tenantId, EdgeId edgeId, AssetProfile assetProfile, EdgeVersion edgeVersion) {
        if (EdgeVersion.V_3_3_0.equals(edgeVersion) || EdgeVersion.V_3_3_3.equals(edgeVersion) || EdgeVersion.V_3_4_0.equals(edgeVersion)) {
            if (assetProfile.getDefaultDashboardId() != null && isEntityNotAssignedToEdge(tenantId, assetProfile.getDefaultDashboardId(), edgeId)) {
                assetProfile.setDefaultDashboardId(null);
            }
            if (assetProfile.getDefaultEdgeRuleChainId() != null && isEntityNotAssignedToEdge(tenantId, assetProfile.getDefaultEdgeRuleChainId(), edgeId)) {
                assetProfile.setDefaultEdgeRuleChainId(null);
            }
        }
        return assetProfile;
    }

    protected DeviceProfile checkIfDeviceProfileDefaultFieldsAssignedToEdge(TenantId tenantId, EdgeId edgeId, DeviceProfile deviceProfile, EdgeVersion edgeVersion) {
        if (EdgeVersion.V_3_3_0.equals(edgeVersion) || EdgeVersion.V_3_3_3.equals(edgeVersion) || EdgeVersion.V_3_4_0.equals(edgeVersion)) {
            if (deviceProfile.getDefaultDashboardId() != null && isEntityNotAssignedToEdge(tenantId, deviceProfile.getDefaultDashboardId(), edgeId)) {
                deviceProfile.setDefaultDashboardId(null);
            }
            if (deviceProfile.getDefaultEdgeRuleChainId() != null && isEntityNotAssignedToEdge(tenantId, deviceProfile.getDefaultEdgeRuleChainId(), edgeId)) {
                deviceProfile.setDefaultEdgeRuleChainId(null);
            }
        }
        return deviceProfile;
    }

    private boolean isEntityNotAssignedToEdge(TenantId tenantId, EntityId entityId, EdgeId edgeId) {
        PageDataIterableByTenantIdEntityId<EdgeId> edgeIds =
                new PageDataIterableByTenantIdEntityId<>(edgeService::findRelatedEdgeIdsByEntityId, tenantId, entityId, DEFAULT_PAGE_SIZE);
        for (EdgeId edgeId1 : edgeIds) {
            if (edgeId1.equals(edgeId)) {
                return false;
            }
        }
        return true;
    }

}
