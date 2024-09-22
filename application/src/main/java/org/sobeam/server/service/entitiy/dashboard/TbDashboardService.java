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
package org.sobeam.server.service.entitiy.dashboard;

import org.sobeam.server.common.data.Customer;
import org.sobeam.server.common.data.Dashboard;
import org.sobeam.server.common.data.User;
import org.sobeam.server.common.data.edge.Edge;
import org.sobeam.server.common.data.exception.SobeamException;
import org.sobeam.server.common.data.id.CustomerId;
import org.sobeam.server.common.data.id.DashboardId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.service.entitiy.SimpleTbEntityService;

import java.util.Set;

public interface TbDashboardService extends SimpleTbEntityService<Dashboard> {

    Dashboard assignDashboardToCustomer(Dashboard dashboard, Customer customer, User user) throws SobeamException;

    Dashboard assignDashboardToPublicCustomer(Dashboard dashboard, User user) throws SobeamException;

    Dashboard unassignDashboardFromPublicCustomer(Dashboard dashboard, User user) throws SobeamException;

    Dashboard updateDashboardCustomers(Dashboard dashboard, Set<CustomerId> customerIds, User user) throws SobeamException;

    Dashboard addDashboardCustomers(Dashboard dashboard, Set<CustomerId> customerIds, User user) throws SobeamException;

    Dashboard removeDashboardCustomers(Dashboard dashboard, Set<CustomerId> customerIds, User user) throws SobeamException;

    Dashboard asignDashboardToEdge(TenantId tenantId, DashboardId dashboardId, Edge edge, User user) throws SobeamException;

    Dashboard unassignDashboardFromEdge(Dashboard dashboard, Edge edge, User user) throws SobeamException;

    Dashboard unassignDashboardFromCustomer(Dashboard dashboard, Customer customer, User user) throws SobeamException;

}
