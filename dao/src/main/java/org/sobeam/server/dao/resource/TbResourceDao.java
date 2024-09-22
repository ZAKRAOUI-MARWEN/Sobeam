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
package org.sobeam.server.dao.resource;

import org.sobeam.server.common.data.ResourceType;
import org.sobeam.server.common.data.TbResource;
import org.sobeam.server.common.data.id.TbResourceId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.page.PageData;
import org.sobeam.server.common.data.page.PageLink;
import org.sobeam.server.dao.Dao;
import org.sobeam.server.dao.ExportableEntityDao;
import org.sobeam.server.dao.TenantEntityWithDataDao;

import java.util.List;

public interface TbResourceDao extends Dao<TbResource>, TenantEntityWithDataDao, ExportableEntityDao<TbResourceId, TbResource> {

    TbResource findResourceByTenantIdAndKey(TenantId tenantId, ResourceType resourceType, String resourceId);

    PageData<TbResource> findAllByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<TbResource> findResourcesByTenantIdAndResourceType(TenantId tenantId,
                                                                ResourceType resourceType,
                                                                PageLink pageLink);

    List<TbResource> findResourcesByTenantIdAndResourceType(TenantId tenantId,
                                                            ResourceType resourceType,
                                                            String[] objectIds,
                                                            String searchText);

    byte[] getResourceData(TenantId tenantId, TbResourceId resourceId);

    byte[] getResourcePreview(TenantId tenantId, TbResourceId resourceId);

    long getResourceSize(TenantId tenantId, TbResourceId resourceId);

}
