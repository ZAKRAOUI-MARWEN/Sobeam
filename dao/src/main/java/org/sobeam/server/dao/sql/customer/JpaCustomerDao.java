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
package org.sobeam.server.dao.sql.customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.sobeam.server.common.data.Customer;
import org.sobeam.server.common.data.EntityType;
import org.sobeam.server.common.data.id.CustomerId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.page.PageData;
import org.sobeam.server.common.data.page.PageLink;
import org.sobeam.server.dao.DaoUtil;
import org.sobeam.server.dao.customer.CustomerDao;
import org.sobeam.server.dao.model.sql.CustomerEntity;
import org.sobeam.server.dao.sql.JpaAbstractDao;
import org.sobeam.server.dao.util.SqlDao;

import java.util.Optional;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
@Component
@SqlDao
public class JpaCustomerDao extends JpaAbstractDao<CustomerEntity, Customer> implements CustomerDao {

    @Autowired
    private CustomerRepository customerRepository;

    @Override
    protected Class<CustomerEntity> getEntityClass() {
        return CustomerEntity.class;
    }

    @Override
    protected JpaRepository<CustomerEntity, UUID> getRepository() {
        return customerRepository;
    }

    @Override
    public PageData<Customer> findCustomersByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(customerRepository.findByTenantId(
                tenantId,
                pageLink.getTextSearch(),
                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public Optional<Customer> findCustomerByTenantIdAndTitle(UUID tenantId, String title) {
        return Optional.ofNullable(DaoUtil.getData(customerRepository.findByTenantIdAndTitle(tenantId, title)));
    }

    @Override
    public Optional<Customer> findPublicCustomerByTenantId(UUID tenantId) {
        return Optional.ofNullable(DaoUtil.getData(customerRepository.findPublicCustomerByTenantId(tenantId)));
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return customerRepository.countByTenantId(tenantId.getId());
    }

    @Override
    public Customer findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(customerRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public Customer findByTenantIdAndName(UUID tenantId, String name) {
        return findCustomerByTenantIdAndTitle(tenantId, name).orElse(null);
    }

    @Override
    public PageData<Customer> findByTenantId(UUID tenantId, PageLink pageLink) {
        return findCustomersByTenantId(tenantId, pageLink);
    }

    @Override
    public CustomerId getExternalIdByInternal(CustomerId internalId) {
        return Optional.ofNullable(customerRepository.getExternalIdById(internalId.getId()))
                .map(CustomerId::new).orElse(null);
    }

    @Override
    public PageData<Customer> findCustomersWithTheSameTitle(PageLink pageLink) {
        return DaoUtil.toPageData(
                customerRepository.findCustomersWithTheSameTitle(DaoUtil.toPageable(pageLink))
        );
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.CUSTOMER;
    }

}
