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
package org.sobeam.server.service.entitiy.customer;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.sobeam.server.common.data.Customer;
import org.sobeam.server.common.data.EntityType;
import org.sobeam.server.common.data.User;
import org.sobeam.server.common.data.audit.ActionType;
import org.sobeam.server.common.data.id.CustomerId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.plugin.ComponentLifecycleEvent;
import org.sobeam.server.service.entitiy.AbstractTbEntityService;

import java.util.Optional;

@Service
@AllArgsConstructor
public class DefaultTbCustomerService extends AbstractTbEntityService implements TbCustomerService {

    @Override
    public Customer save(Customer customer, User user) throws Exception {
        ActionType actionType = customer.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = customer.getTenantId();
        try {
            Customer savedCustomer = checkNotNull(customerService.saveCustomer(customer));
            autoCommit(user, savedCustomer.getId());
            logEntityActionService.logEntityAction(tenantId, savedCustomer.getId(), savedCustomer, null, actionType, user);
            return savedCustomer;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.CUSTOMER), customer, actionType, user, e);
            throw e;
        }
    }

    @Override
    public void delete(Customer customer, User user) {
        ActionType actionType = ActionType.DELETED;
        TenantId tenantId = customer.getTenantId();
        CustomerId customerId = customer.getId();
        try {
            customerService.deleteCustomer(tenantId, customerId);
            logEntityActionService.logEntityAction(tenantId, customer.getId(), customer, customerId, actionType,
                    user, customerId.toString());
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.CUSTOMER), actionType, user,
                    e, customerId.toString());
            throw e;
        }
    }

}
