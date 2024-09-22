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

import org.sobeam.server.common.data.Customer;
import org.sobeam.server.common.data.edge.Edge;
import org.sobeam.server.common.data.id.EntityId;
import org.sobeam.server.service.edge.EdgeContextComponent;
import org.sobeam.server.service.edge.rpc.fetch.AdminSettingsEdgeEventFetcher;
import org.sobeam.server.service.edge.rpc.fetch.AssetProfilesEdgeEventFetcher;
import org.sobeam.server.service.edge.rpc.fetch.AssetsEdgeEventFetcher;
import org.sobeam.server.service.edge.rpc.fetch.CustomerEdgeEventFetcher;
import org.sobeam.server.service.edge.rpc.fetch.CustomerUsersEdgeEventFetcher;
import org.sobeam.server.service.edge.rpc.fetch.DashboardsEdgeEventFetcher;
import org.sobeam.server.service.edge.rpc.fetch.DefaultProfilesEdgeEventFetcher;
import org.sobeam.server.service.edge.rpc.fetch.DeviceProfilesEdgeEventFetcher;
import org.sobeam.server.service.edge.rpc.fetch.DevicesEdgeEventFetcher;
import org.sobeam.server.service.edge.rpc.fetch.EdgeEventFetcher;
import org.sobeam.server.service.edge.rpc.fetch.EntityViewsEdgeEventFetcher;
import org.sobeam.server.service.edge.rpc.fetch.NotificationRuleEdgeEventFetcher;
import org.sobeam.server.service.edge.rpc.fetch.NotificationTargetEdgeEventFetcher;
import org.sobeam.server.service.edge.rpc.fetch.NotificationTemplateEdgeEventFetcher;
import org.sobeam.server.service.edge.rpc.fetch.OAuth2EdgeEventFetcher;
import org.sobeam.server.service.edge.rpc.fetch.OtaPackagesEdgeEventFetcher;
import org.sobeam.server.service.edge.rpc.fetch.QueuesEdgeEventFetcher;
import org.sobeam.server.service.edge.rpc.fetch.RuleChainsEdgeEventFetcher;
import org.sobeam.server.service.edge.rpc.fetch.SystemWidgetTypesEdgeEventFetcher;
import org.sobeam.server.service.edge.rpc.fetch.SystemWidgetsBundlesEdgeEventFetcher;
import org.sobeam.server.service.edge.rpc.fetch.TenantAdminUsersEdgeEventFetcher;
import org.sobeam.server.service.edge.rpc.fetch.TenantEdgeEventFetcher;
import org.sobeam.server.service.edge.rpc.fetch.TenantResourcesEdgeEventFetcher;
import org.sobeam.server.service.edge.rpc.fetch.TenantWidgetTypesEdgeEventFetcher;
import org.sobeam.server.service.edge.rpc.fetch.TenantWidgetsBundlesEdgeEventFetcher;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

public class EdgeSyncCursor {

    private final List<EdgeEventFetcher> fetchers = new LinkedList<>();

    private int currentIdx = 0;

    public EdgeSyncCursor(EdgeContextComponent ctx, Edge edge, boolean fullSync) {
        if (fullSync) {
            fetchers.add(new TenantEdgeEventFetcher(ctx.getTenantService()));
            fetchers.add(new QueuesEdgeEventFetcher(ctx.getQueueService()));
            fetchers.add(new RuleChainsEdgeEventFetcher(ctx.getRuleChainService()));
            fetchers.add(new AdminSettingsEdgeEventFetcher(ctx.getAdminSettingsService()));
            fetchers.add(new TenantAdminUsersEdgeEventFetcher(ctx.getUserService()));
            Customer publicCustomer = ctx.getCustomerService().findOrCreatePublicCustomer(edge.getTenantId());
            fetchers.add(new CustomerEdgeEventFetcher(publicCustomer.getId()));
            if (edge.getCustomerId() != null && !EntityId.NULL_UUID.equals(edge.getCustomerId().getId())) {
                fetchers.add(new CustomerEdgeEventFetcher(edge.getCustomerId()));
                fetchers.add(new CustomerUsersEdgeEventFetcher(ctx.getUserService(), edge.getCustomerId()));
            }
        }
        fetchers.add(new DashboardsEdgeEventFetcher(ctx.getDashboardService()));
        fetchers.add(new DefaultProfilesEdgeEventFetcher(ctx.getDeviceProfileService(), ctx.getAssetProfileService()));
        fetchers.add(new DeviceProfilesEdgeEventFetcher(ctx.getDeviceProfileService()));
        fetchers.add(new AssetProfilesEdgeEventFetcher(ctx.getAssetProfileService()));
        fetchers.add(new DevicesEdgeEventFetcher(ctx.getDeviceService()));
        fetchers.add(new AssetsEdgeEventFetcher(ctx.getAssetService()));
        fetchers.add(new EntityViewsEdgeEventFetcher(ctx.getEntityViewService()));
        if (fullSync) {
            fetchers.add(new NotificationTemplateEdgeEventFetcher(ctx.getNotificationTemplateService()));
            fetchers.add(new NotificationTargetEdgeEventFetcher(ctx.getNotificationTargetService()));
            fetchers.add(new NotificationRuleEdgeEventFetcher(ctx.getNotificationRuleService()));
            fetchers.add(new SystemWidgetTypesEdgeEventFetcher(ctx.getWidgetTypeService()));
            fetchers.add(new TenantWidgetTypesEdgeEventFetcher(ctx.getWidgetTypeService()));
            fetchers.add(new SystemWidgetsBundlesEdgeEventFetcher(ctx.getWidgetsBundleService()));
            fetchers.add(new TenantWidgetsBundlesEdgeEventFetcher(ctx.getWidgetsBundleService()));
            fetchers.add(new OtaPackagesEdgeEventFetcher(ctx.getOtaPackageService()));
            fetchers.add(new TenantResourcesEdgeEventFetcher(ctx.getResourceService()));
            fetchers.add(new OAuth2EdgeEventFetcher(ctx.getOAuth2Service()));
        }
    }

    public boolean hasNext() {
        return fetchers.size() > currentIdx;
    }

    public EdgeEventFetcher getNext() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        EdgeEventFetcher edgeEventFetcher = fetchers.get(currentIdx);
        currentIdx++;
        return edgeEventFetcher;
    }

    public int getCurrentIdx() {
        return currentIdx;
    }

}
