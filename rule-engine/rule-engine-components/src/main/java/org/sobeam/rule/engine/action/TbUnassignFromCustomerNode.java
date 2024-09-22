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
package org.sobeam.rule.engine.action;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.sobeam.rule.engine.api.RuleNode;
import org.sobeam.rule.engine.api.TbContext;
import org.sobeam.rule.engine.api.TbNodeConfiguration;
import org.sobeam.rule.engine.api.TbNodeException;
import org.sobeam.rule.engine.api.util.TbNodeUtils;
import org.sobeam.server.common.data.EntityType;
import org.sobeam.server.common.data.StringUtils;
import org.sobeam.server.common.data.id.AssetId;
import org.sobeam.server.common.data.id.DashboardId;
import org.sobeam.server.common.data.id.DeviceId;
import org.sobeam.server.common.data.id.EdgeId;
import org.sobeam.server.common.data.id.EntityViewId;
import org.sobeam.server.common.data.plugin.ComponentType;
import org.sobeam.server.common.msg.TbMsg;

@RuleNode(
        type = ComponentType.ACTION,
        name = "unassign from customer",
        configClazz = TbUnassignFromCustomerNodeConfiguration.class,
        nodeDescription = "Unassign message originator entity from customer",
        nodeDetails = "If the message originator is not assigned to any customer, rule node will do nothing. <br><br>" +
                "If the incoming message originator is a dashboard, will try to search for the customer by title specified in the configuration. " +
                "If customer doesn't exist, the exception will be thrown. Otherwise will unassign the dashboard from retrieved customer.<br><br>" +
                "Other entities can be assigned only to one customer, so specified customer title in the configuration will be ignored if the originator isn't a dashboard.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeUnAssignToCustomerConfig",
        icon = "remove_circle",
        version = 1
)
public class TbUnassignFromCustomerNode extends TbAbstractCustomerActionNode<TbUnassignFromCustomerNodeConfiguration> {

    @Override
    protected boolean createCustomerIfNotExists() {
        return false;
    }

    @Override
    protected TbUnassignFromCustomerNodeConfiguration loadCustomerNodeActionConfig(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbUnassignFromCustomerNodeConfiguration.class);
    }

    @Override
    protected ListenableFuture<Void> processCustomerAction(TbContext ctx, TbMsg msg) {
        var originator = msg.getOriginator();
        var originatorType = originator.getEntityType();
        var tenantId = ctx.getTenantId();
        if (EntityType.DASHBOARD.equals(originatorType)) {
            if (StringUtils.isEmpty(config.getCustomerNamePattern())) {
                throw new RuntimeException("Failed to unassign dashboard with id '" +
                        originator.getId() + "' from customer! Customer title should be specified!");
            }
            var customerIdFuture = getCustomerIdFuture(ctx, msg);
            return Futures.transform(customerIdFuture, customerId -> {
                ctx.getDashboardService().unassignDashboardFromCustomer(tenantId, new DashboardId(originator.getId()), customerId);
                return null;
            }, MoreExecutors.directExecutor());
        }
        return ctx.getDbCallbackExecutor().submit(() -> {
            switch (originatorType) {
                case ASSET ->
                        ctx.getAssetService().unassignAssetFromCustomer(tenantId, new AssetId(originator.getId()));
                case DEVICE ->
                        ctx.getDeviceService().unassignDeviceFromCustomer(tenantId, new DeviceId(originator.getId()));
                case ENTITY_VIEW ->
                        ctx.getEntityViewService().unassignEntityViewFromCustomer(tenantId, new EntityViewId(originator.getId()));
                case EDGE -> ctx.getEdgeService().unassignEdgeFromCustomer(tenantId, new EdgeId(originator.getId()));
            }
            return null;
        });
    }

}
