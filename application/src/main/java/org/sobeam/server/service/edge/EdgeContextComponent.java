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
package org.sobeam.server.service.edge;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.sobeam.server.cache.limits.RateLimitService;
import org.sobeam.server.cluster.TbClusterService;
import org.sobeam.server.common.msg.notification.NotificationRuleProcessor;
import org.sobeam.server.dao.asset.AssetProfileService;
import org.sobeam.server.dao.asset.AssetService;
import org.sobeam.server.dao.attributes.AttributesService;
import org.sobeam.server.dao.customer.CustomerService;
import org.sobeam.server.dao.dashboard.DashboardService;
import org.sobeam.server.dao.device.DeviceProfileService;
import org.sobeam.server.dao.device.DeviceService;
import org.sobeam.server.dao.edge.EdgeEventService;
import org.sobeam.server.dao.edge.EdgeService;
import org.sobeam.server.dao.entityview.EntityViewService;
import org.sobeam.server.dao.notification.NotificationRuleService;
import org.sobeam.server.dao.notification.NotificationTargetService;
import org.sobeam.server.dao.notification.NotificationTemplateService;
import org.sobeam.server.dao.oauth2.OAuth2Service;
import org.sobeam.server.dao.ota.OtaPackageService;
import org.sobeam.server.dao.queue.QueueService;
import org.sobeam.server.dao.resource.ResourceService;
import org.sobeam.server.dao.rule.RuleChainService;
import org.sobeam.server.dao.settings.AdminSettingsService;
import org.sobeam.server.dao.tenant.TenantProfileService;
import org.sobeam.server.dao.tenant.TenantService;
import org.sobeam.server.dao.user.UserService;
import org.sobeam.server.dao.widget.WidgetTypeService;
import org.sobeam.server.dao.widget.WidgetsBundleService;
import org.sobeam.server.queue.util.TbCoreComponent;
import org.sobeam.server.service.edge.rpc.EdgeEventStorageSettings;
import org.sobeam.server.service.edge.rpc.constructor.edge.EdgeMsgConstructor;
import org.sobeam.server.service.edge.rpc.processor.alarm.AlarmEdgeProcessor;
import org.sobeam.server.service.edge.rpc.processor.alarm.AlarmEdgeProcessorFactory;
import org.sobeam.server.service.edge.rpc.processor.asset.AssetEdgeProcessor;
import org.sobeam.server.service.edge.rpc.processor.asset.AssetEdgeProcessorFactory;
import org.sobeam.server.service.edge.rpc.processor.asset.profile.AssetProfileEdgeProcessor;
import org.sobeam.server.service.edge.rpc.processor.asset.profile.AssetProfileEdgeProcessorFactory;
import org.sobeam.server.service.edge.rpc.processor.customer.CustomerEdgeProcessor;
import org.sobeam.server.service.edge.rpc.processor.dashboard.DashboardEdgeProcessor;
import org.sobeam.server.service.edge.rpc.processor.dashboard.DashboardEdgeProcessorFactory;
import org.sobeam.server.service.edge.rpc.processor.device.DeviceEdgeProcessor;
import org.sobeam.server.service.edge.rpc.processor.device.DeviceEdgeProcessorFactory;
import org.sobeam.server.service.edge.rpc.processor.device.profile.DeviceProfileEdgeProcessor;
import org.sobeam.server.service.edge.rpc.processor.device.profile.DeviceProfileEdgeProcessorFactory;
import org.sobeam.server.service.edge.rpc.processor.edge.EdgeProcessor;
import org.sobeam.server.service.edge.rpc.processor.entityview.EntityViewEdgeProcessor;
import org.sobeam.server.service.edge.rpc.processor.entityview.EntityViewProcessorFactory;
import org.sobeam.server.service.edge.rpc.processor.notification.NotificationEdgeProcessor;
import org.sobeam.server.service.edge.rpc.processor.oauth2.OAuth2EdgeProcessor;
import org.sobeam.server.service.edge.rpc.processor.ota.OtaPackageEdgeProcessor;
import org.sobeam.server.service.edge.rpc.processor.queue.QueueEdgeProcessor;
import org.sobeam.server.service.edge.rpc.processor.relation.RelationEdgeProcessor;
import org.sobeam.server.service.edge.rpc.processor.relation.RelationEdgeProcessorFactory;
import org.sobeam.server.service.edge.rpc.processor.resource.ResourceEdgeProcessor;
import org.sobeam.server.service.edge.rpc.processor.resource.ResourceEdgeProcessorFactory;
import org.sobeam.server.service.edge.rpc.processor.rule.RuleChainEdgeProcessor;
import org.sobeam.server.service.edge.rpc.processor.settings.AdminSettingsEdgeProcessor;
import org.sobeam.server.service.edge.rpc.processor.telemetry.TelemetryEdgeProcessor;
import org.sobeam.server.service.edge.rpc.processor.tenant.TenantEdgeProcessor;
import org.sobeam.server.service.edge.rpc.processor.tenant.TenantProfileEdgeProcessor;
import org.sobeam.server.service.edge.rpc.processor.user.UserEdgeProcessor;
import org.sobeam.server.service.edge.rpc.processor.widget.WidgetBundleEdgeProcessor;
import org.sobeam.server.service.edge.rpc.processor.widget.WidgetTypeEdgeProcessor;
import org.sobeam.server.service.edge.rpc.sync.EdgeRequestsService;
import org.sobeam.server.service.executors.DbCallbackExecutorService;
import org.sobeam.server.service.executors.GrpcCallbackExecutorService;

@Component
@TbCoreComponent
@Data
@Lazy
public class EdgeContextComponent {

    @Autowired
    private TbClusterService clusterService;

    @Autowired
    private EdgeService edgeService;

    @Autowired
    private EdgeEventService edgeEventService;

    @Autowired
    private AdminSettingsService adminSettingsService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private EntityViewService entityViewService;

    @Autowired
    private DeviceProfileService deviceProfileService;

    @Autowired
    private AssetProfileService assetProfileService;

    @Autowired
    private AttributesService attributesService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private RuleChainService ruleChainService;

    @Autowired
    private UserService userService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private WidgetTypeService widgetTypeService;

    @Autowired
    private WidgetsBundleService widgetsBundleService;

    @Autowired
    private EdgeRequestsService edgeRequestsService;

    @Autowired
    private OtaPackageService otaPackageService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private TenantProfileService tenantProfileService;

    @Autowired
    private QueueService queueService;

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private NotificationRuleService notificationRuleService;

    @Autowired
    private NotificationTargetService notificationTargetService;

    @Autowired
    private NotificationTemplateService notificationTemplateService;

    @Autowired
    private OAuth2Service oAuth2Service;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private NotificationRuleProcessor notificationRuleProcessor;

    @Autowired
    private AlarmEdgeProcessor alarmProcessor;

    @Autowired
    private DeviceProfileEdgeProcessor deviceProfileProcessor;

    @Autowired
    private AssetProfileEdgeProcessor assetProfileProcessor;

    @Autowired
    private EdgeProcessor edgeProcessor;

    @Autowired
    private DeviceEdgeProcessor deviceProcessor;

    @Autowired
    private AssetEdgeProcessor assetProcessor;

    @Autowired
    private EntityViewEdgeProcessor entityViewProcessor;

    @Autowired
    private UserEdgeProcessor userProcessor;

    @Autowired
    private RelationEdgeProcessor relationProcessor;

    @Autowired
    private TelemetryEdgeProcessor telemetryProcessor;

    @Autowired
    private DashboardEdgeProcessor dashboardProcessor;

    @Autowired
    private RuleChainEdgeProcessor ruleChainProcessor;

    @Autowired
    private CustomerEdgeProcessor customerProcessor;

    @Autowired
    private WidgetBundleEdgeProcessor widgetBundleProcessor;

    @Autowired
    private WidgetTypeEdgeProcessor widgetTypeProcessor;

    @Autowired
    private AdminSettingsEdgeProcessor adminSettingsProcessor;

    @Autowired
    private OtaPackageEdgeProcessor otaPackageEdgeProcessor;

    @Autowired
    private QueueEdgeProcessor queueEdgeProcessor;

    @Autowired
    private TenantEdgeProcessor tenantEdgeProcessor;

    @Autowired
    private TenantProfileEdgeProcessor tenantProfileEdgeProcessor;

    @Autowired
    private ResourceEdgeProcessor resourceEdgeProcessor;

    @Autowired
    private NotificationEdgeProcessor notificationEdgeProcessor;

    @Autowired
    private OAuth2EdgeProcessor oAuth2EdgeProcessor;

    @Autowired
    private EdgeMsgConstructor edgeMsgConstructor;

    @Autowired
    private AlarmEdgeProcessorFactory alarmEdgeProcessorFactory;

    @Autowired
    private AssetEdgeProcessorFactory assetEdgeProcessorFactory;

    @Autowired
    private AssetProfileEdgeProcessorFactory assetProfileEdgeProcessorFactory;

    @Autowired
    private DashboardEdgeProcessorFactory dashboardEdgeProcessorFactory;

    @Autowired
    private DeviceEdgeProcessorFactory deviceEdgeProcessorFactory;

    @Autowired
    private DeviceProfileEdgeProcessorFactory deviceProfileEdgeProcessorFactory;

    @Autowired
    private EntityViewProcessorFactory entityViewProcessorFactory;

    @Autowired
    private RelationEdgeProcessorFactory relationEdgeProcessorFactory;

    @Autowired
    private ResourceEdgeProcessorFactory resourceEdgeProcessorFactory;

    @Autowired
    private EdgeEventStorageSettings edgeEventStorageSettings;

    @Autowired
    private DbCallbackExecutorService dbCallbackExecutor;

    @Autowired
    private GrpcCallbackExecutorService grpcCallbackExecutorService;
}
