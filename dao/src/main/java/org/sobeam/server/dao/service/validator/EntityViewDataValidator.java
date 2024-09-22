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
package org.sobeam.server.dao.service.validator;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.sobeam.server.common.data.Customer;
import org.sobeam.server.common.data.EntityView;
import org.sobeam.server.common.data.id.CustomerId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.dao.customer.CustomerDao;
import org.sobeam.server.dao.entityview.EntityViewDao;
import org.sobeam.server.dao.exception.DataValidationException;
import org.sobeam.server.dao.service.DataValidator;
import org.sobeam.server.dao.tenant.TenantService;

import static org.sobeam.server.dao.model.ModelConstants.NULL_UUID;

@Component
@AllArgsConstructor
public class EntityViewDataValidator extends DataValidator<EntityView> {

    private final EntityViewDao entityViewDao;
    private final TenantService tenantService;
    private final CustomerDao customerDao;

    @Override
    protected void validateCreate(TenantId tenantId, EntityView entityView) {
        entityViewDao.findEntityViewByTenantIdAndName(entityView.getTenantId().getId(), entityView.getName())
                .ifPresent(e -> {
                    throw new DataValidationException("Entity view with such name already exists!");
                });
    }

    @Override
    protected EntityView validateUpdate(TenantId tenantId, EntityView entityView) {
        var opt = entityViewDao.findEntityViewByTenantIdAndName(entityView.getTenantId().getId(), entityView.getName());
        opt.ifPresent(e -> {
            if (!e.getUuidId().equals(entityView.getUuidId())) {
                throw new DataValidationException("Entity view with such name already exists!");
            }
        });
        return opt.orElse(null);
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, EntityView entityView) {
        validateString("Entity view name", entityView.getName());
        validateString("Entity view type", entityView.getType());
        if (entityView.getTenantId() == null) {
            throw new DataValidationException("Entity view should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(entityView.getTenantId())) {
                throw new DataValidationException("Entity view is referencing to non-existent tenant!");
            }
        }
        if (entityView.getCustomerId() == null) {
            entityView.setCustomerId(new CustomerId(NULL_UUID));
        } else if (!entityView.getCustomerId().getId().equals(NULL_UUID)) {
            Customer customer = customerDao.findById(tenantId, entityView.getCustomerId().getId());
            if (customer == null) {
                throw new DataValidationException("Can't assign entity view to non-existent customer!");
            }
            if (!customer.getTenantId().getId().equals(entityView.getTenantId().getId())) {
                throw new DataValidationException("Can't assign entity view to customer from different tenant!");
            }
        }
    }
}
