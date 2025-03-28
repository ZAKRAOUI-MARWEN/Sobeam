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
package org.sobeam.rule.engine.api;

import io.netty.channel.EventLoopGroup;
import org.sobeam.common.util.ExecutorProvider;
import org.sobeam.common.util.ListeningExecutor;
import org.sobeam.rule.engine.api.notification.SlackService;
import org.sobeam.rule.engine.api.sms.SmsSenderFactory;
import org.sobeam.server.cluster.TbClusterService;
import org.sobeam.server.common.data.Customer;
import org.sobeam.server.common.data.Device;
import org.sobeam.server.common.data.DeviceProfile;
import org.sobeam.server.common.data.TenantProfile;
import org.sobeam.server.common.data.alarm.Alarm;
import org.sobeam.server.common.data.asset.Asset;
import org.sobeam.server.common.data.asset.AssetProfile;
import org.sobeam.server.common.data.id.AssetId;
import org.sobeam.server.common.data.id.CustomerId;
import org.sobeam.server.common.data.id.DeviceId;
import org.sobeam.server.common.data.id.EntityId;
import org.sobeam.server.common.data.id.RuleChainId;
import org.sobeam.server.common.data.id.RuleNodeId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.kv.AttributeKvEntry;
import org.sobeam.server.common.data.msg.TbMsgType;
import org.sobeam.server.common.data.page.PageData;
import org.sobeam.server.common.data.page.PageLink;
import org.sobeam.server.common.data.rule.RuleNode;
import org.sobeam.server.common.data.rule.RuleNodeState;
import org.sobeam.server.common.data.script.ScriptLanguage;
import org.sobeam.server.common.msg.TbMsg;
import org.sobeam.server.common.msg.TbMsgMetaData;
import org.sobeam.server.dao.alarm.AlarmCommentService;
import org.sobeam.server.dao.asset.AssetProfileService;
import org.sobeam.server.dao.asset.AssetService;
import org.sobeam.server.dao.attributes.AttributesService;
import org.sobeam.server.dao.audit.AuditLogService;
import org.sobeam.server.dao.cassandra.CassandraCluster;
import org.sobeam.server.dao.customer.CustomerService;
import org.sobeam.server.dao.dashboard.DashboardService;
import org.sobeam.server.dao.device.DeviceCredentialsService;
import org.sobeam.server.dao.device.DeviceProfileService;
import org.sobeam.server.dao.device.DeviceService;
import org.sobeam.server.dao.edge.EdgeEventService;
import org.sobeam.server.dao.edge.EdgeService;
import org.sobeam.server.dao.entity.EntityService;
import org.sobeam.server.dao.entityview.EntityViewService;
import org.sobeam.server.dao.event.EventService;
import org.sobeam.server.dao.nosql.CassandraStatementTask;
import org.sobeam.server.dao.nosql.TbResultSetFuture;
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
import org.sobeam.server.dao.tenant.TenantService;
import org.sobeam.server.dao.timeseries.TimeseriesService;
import org.sobeam.server.dao.user.UserService;
import org.sobeam.server.dao.widget.WidgetTypeService;
import org.sobeam.server.dao.widget.WidgetsBundleService;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by ashvayka on 13.01.18.
 */
public interface TbContext {

    /*
     *
     *  METHODS TO CONTROL THE MESSAGE FLOW
     *
     */

    /**
     * Indicates that message was successfully processed by the rule node.
     * Sends message to all Rule Nodes in the Rule Chain
     * that are connected to the current Rule Node using "Success" relationType.
     *
     * @param msg
     */
    void tellSuccess(TbMsg msg);

    /**
     * Sends message to all Rule Nodes in the Rule Chain
     * that are connected to the current Rule Node using specified relationType.
     *
     * @param msg
     * @param relationType
     */
    void tellNext(TbMsg msg, String relationType);

    /**
     * Sends message to all Rule Nodes in the Rule Chain
     * that are connected to the current Rule Node using one of specified relationTypes.
     *
     * @param msg
     * @param relationTypes
     */
    void tellNext(TbMsg msg, Set<String> relationTypes);

    /**
     * Sends message to the current Rule Node with specified delay in milliseconds.
     * Note: this message is not queued and may be lost in case of a server restart.
     *
     * @param msg
     */
    void tellSelf(TbMsg msg, long delayMs);

    /**
     * Notifies Rule Engine about failure to process current message.
     *
     * @param msg - message
     * @param th  - exception
     */
    void tellFailure(TbMsg msg, Throwable th);

    /**
     * Puts new message to queue for processing by the Root Rule Chain
     *
     * @param msg - message
     */
    void enqueue(TbMsg msg, Runnable onSuccess, Consumer<Throwable> onFailure);

    /**
     * Sends message to the nested rule chain.
     * Fails processing of the message if the nested rule chain is not found.
     *
     * @param msg - the message
     * @param ruleChainId - the id of a nested rule chain
     */
    void input(TbMsg msg, RuleChainId ruleChainId);

    /**
     * Sends message to the caller rule chain.
     * Acknowledge the message if no caller rule chain is present in processing stack
     *
     * @param msg - the message
     * @param relationType - the relation type that will be used to route messages in the caller rule chain
     */
    void output(TbMsg msg, String relationType);

    /**
     * Puts new message to custom queue for processing
     *
     * @param msg - message
     */
    void enqueue(TbMsg msg, String queueName, Runnable onSuccess, Consumer<Throwable> onFailure);

    void enqueueForTellFailure(TbMsg msg, String failureMessage);

    void enqueueForTellFailure(TbMsg tbMsg, Throwable t);

    void enqueueForTellNext(TbMsg msg, String relationType);

    void enqueueForTellNext(TbMsg msg, Set<String> relationTypes);

    void enqueueForTellNext(TbMsg msg, String relationType, Runnable onSuccess, Consumer<Throwable> onFailure);

    void enqueueForTellNext(TbMsg msg, Set<String> relationTypes, Runnable onSuccess, Consumer<Throwable> onFailure);

    void enqueueForTellNext(TbMsg msg, String queueName, String relationType, Runnable onSuccess, Consumer<Throwable> onFailure);

    void enqueueForTellNext(TbMsg msg, String queueName, Set<String> relationTypes, Runnable onSuccess, Consumer<Throwable> onFailure);

    void ack(TbMsg tbMsg);

    @Deprecated(since = "3.6.0", forRemoval = true)
    TbMsg newMsg(String queueName, String type, EntityId originator, TbMsgMetaData metaData, String data);

    /**
     * Creates a new TbMsg instance with the specified parameters.
     *
     * <p><strong>Deprecated:</strong> This method is deprecated since version 3.6.0 and should only be used when you need to
     * specify a custom message type that doesn't exist in the {@link TbMsgType} enum. For standard message types,
     * it is recommended to use the {@link #newMsg(String, TbMsgType, EntityId, CustomerId, TbMsgMetaData, String)}
     * method instead.</p>
     *
     * @param queueName   the name of the queue where the message will be sent
     * @param type        the type of the message
     * @param originator  the originator of the message
     * @param customerId  the ID of the customer associated with the message
     * @param metaData    the metadata of the message
     * @param data        the data of the message
     * @return new TbMsg instance
     */
    @Deprecated(since = "3.6.0")
    TbMsg newMsg(String queueName, String type, EntityId originator, CustomerId customerId, TbMsgMetaData metaData, String data);

    @Deprecated(since = "3.6.0", forRemoval = true)
    TbMsg transformMsg(TbMsg origMsg, String type, EntityId originator, TbMsgMetaData metaData, String data);

    TbMsg newMsg(String queueName, TbMsgType type, EntityId originator, TbMsgMetaData metaData, String data);

    TbMsg newMsg(String queueName, TbMsgType type, EntityId originator, CustomerId customerId, TbMsgMetaData metaData, String data);

    TbMsg transformMsg(TbMsg origMsg, TbMsgType type, EntityId originator, TbMsgMetaData metaData, String data);

    TbMsg transformMsg(TbMsg origMsg, TbMsgMetaData metaData, String data);

    TbMsg transformMsgOriginator(TbMsg origMsg, EntityId originator);

    TbMsg customerCreatedMsg(Customer customer, RuleNodeId ruleNodeId);

    TbMsg deviceCreatedMsg(Device device, RuleNodeId ruleNodeId);

    TbMsg assetCreatedMsg(Asset asset, RuleNodeId ruleNodeId);

    @Deprecated(since = "3.6.0", forRemoval = true)
    TbMsg alarmActionMsg(Alarm alarm, RuleNodeId ruleNodeId, String action);

    TbMsg alarmActionMsg(Alarm alarm, RuleNodeId ruleNodeId, TbMsgType actionMsgType);

    TbMsg attributesUpdatedActionMsg(EntityId originator, RuleNodeId ruleNodeId, String scope, List<AttributeKvEntry> attributes);

    TbMsg attributesDeletedActionMsg(EntityId originator, RuleNodeId ruleNodeId, String scope, List<String> keys);

    /*
     *
     *  METHODS TO PROCESS THE MESSAGES
     *
     */

    void schedule(Runnable runnable, long delay, TimeUnit timeUnit);

    void checkTenantEntity(EntityId entityId) throws TbNodeException;

    boolean isLocalEntity(EntityId entityId);

    RuleNodeId getSelfId();

    RuleNode getSelf();

    String getRuleChainName();

    String getQueueName();

    TenantId getTenantId();

    AttributesService getAttributesService();

    CustomerService getCustomerService();

    TenantService getTenantService();

    UserService getUserService();

    AssetService getAssetService();

    DeviceService getDeviceService();

    DeviceProfileService getDeviceProfileService();

    AssetProfileService getAssetProfileService();

    DeviceCredentialsService getDeviceCredentialsService();

    RuleEngineDeviceStateManager getDeviceStateManager();

    String getDeviceStateNodeRateLimitConfig();

    TbClusterService getClusterService();

    DashboardService getDashboardService();

    RuleEngineAlarmService getAlarmService();

    AlarmCommentService getAlarmCommentService();

    RuleChainService getRuleChainService();

    RuleEngineRpcService getRpcService();

    RuleEngineTelemetryService getTelemetryService();

    TimeseriesService getTimeseriesService();

    RelationService getRelationService();

    EntityViewService getEntityViewService();

    ResourceService getResourceService();

    OtaPackageService getOtaPackageService();

    RuleEngineDeviceProfileCache getDeviceProfileCache();

    RuleEngineAssetProfileCache getAssetProfileCache();

    EdgeService getEdgeService();

    EdgeEventService getEdgeEventService();

    QueueService getQueueService();

    QueueStatsService getQueueStatsService();

    ListeningExecutor getMailExecutor();

    ListeningExecutor getSmsExecutor();

    ListeningExecutor getDbCallbackExecutor();

    ListeningExecutor getExternalCallExecutor();

    ListeningExecutor getNotificationExecutor();

    ExecutorProvider getPubSubRuleNodeExecutorProvider();

    MailService getMailService(boolean isSystem);

    SmsService getSmsService();

    SmsSenderFactory getSmsSenderFactory();

    NotificationCenter getNotificationCenter();

    NotificationTargetService getNotificationTargetService();

    NotificationTemplateService getNotificationTemplateService();

    NotificationRequestService getNotificationRequestService();

    NotificationRuleService getNotificationRuleService();

    SlackService getSlackService();

    boolean isExternalNodeForceAck();

    /**
     * Creates JS Script Engine
     * @deprecated
     * <p> Use {@link #createScriptEngine} instead.
     *
     */
    @Deprecated
    ScriptEngine createJsScriptEngine(String script, String... argNames);

    ScriptEngine createScriptEngine(ScriptLanguage scriptLang, String script, String... argNames);

    void logJsEvalRequest();

    void logJsEvalResponse();

    void logJsEvalFailure();

    String getServiceId();

    EventLoopGroup getSharedEventLoop();

    CassandraCluster getCassandraCluster();

    TbResultSetFuture submitCassandraReadTask(CassandraStatementTask task);

    TbResultSetFuture submitCassandraWriteTask(CassandraStatementTask task);

    PageData<RuleNodeState> findRuleNodeStates(PageLink pageLink);

    RuleNodeState findRuleNodeStateForEntity(EntityId entityId);

    void removeRuleNodeStateForEntity(EntityId entityId);

    RuleNodeState saveRuleNodeState(RuleNodeState state);

    void clearRuleNodeStates();

    void addTenantProfileListener(Consumer<TenantProfile> listener);

    void addDeviceProfileListeners(Consumer<DeviceProfile> listener, BiConsumer<DeviceId, DeviceProfile> deviceListener);

    void addAssetProfileListeners(Consumer<AssetProfile> listener, BiConsumer<AssetId, AssetProfile> assetListener);

    void removeListeners();

    TenantProfile getTenantProfile();

    WidgetsBundleService getWidgetBundleService();

    WidgetTypeService getWidgetTypeService();

    RuleEngineApiUsageStateService getRuleEngineApiUsageStateService();

    EntityService getEntityService();

    EventService getEventService();

    AuditLogService getAuditLogService();
}
