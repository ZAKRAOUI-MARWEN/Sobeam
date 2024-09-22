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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.sobeam.server.common.data.Customer;
import org.sobeam.server.common.data.EntityType;
import org.sobeam.server.common.data.asset.Asset;
import org.sobeam.server.common.data.id.CustomerId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.dao.asset.AssetDao;
import org.sobeam.server.dao.asset.BaseAssetService;
import org.sobeam.server.dao.customer.CustomerDao;
import org.sobeam.server.dao.exception.DataValidationException;
import org.sobeam.server.dao.service.DataValidator;
import org.sobeam.server.dao.tenant.TenantService;

import static org.sobeam.server.dao.model.ModelConstants.NULL_UUID;

@Component
public class AssetDataValidator extends DataValidator<Asset> {

    @Autowired
    private AssetDao assetDao;

    @Autowired
    @Lazy
    private TenantService tenantService;

    @Autowired
    private CustomerDao customerDao;

    @Override
    protected void validateCreate(TenantId tenantId, Asset asset) {
        validateNumberOfEntitiesPerTenant(tenantId, EntityType.ASSET);
    }

    @Override
    protected Asset validateUpdate(TenantId tenantId, Asset asset) {
        Asset old = assetDao.findById(asset.getTenantId(), asset.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing asset!");
        }
        return old;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, Asset asset) {
        validateString("Asset name", asset.getName());
        if (asset.getTenantId() == null) {
            throw new DataValidationException("Asset should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(asset.getTenantId())) {
                throw new DataValidationException("Asset is referencing to non-existent tenant!");
            }
        }
        if (asset.getCustomerId() == null) {
            asset.setCustomerId(new CustomerId(NULL_UUID));
        } else if (!asset.getCustomerId().getId().equals(NULL_UUID)) {
            Customer customer = customerDao.findById(tenantId, asset.getCustomerId().getId());
            if (customer == null) {
                throw new DataValidationException("Can't assign asset to non-existent customer!");
            }
            if (!customer.getTenantId().equals(asset.getTenantId())) {
                throw new DataValidationException("Can't assign asset to customer from different tenant!");
            }
        }
    }
}
