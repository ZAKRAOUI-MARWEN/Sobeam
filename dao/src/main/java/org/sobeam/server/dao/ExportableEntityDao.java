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
package org.sobeam.server.dao;

import org.sobeam.server.common.data.ExportableEntity;
import org.sobeam.server.common.data.id.EntityId;
import org.sobeam.server.common.data.page.PageData;
import org.sobeam.server.common.data.page.PageLink;

import java.util.UUID;

public interface ExportableEntityDao<I extends EntityId, T extends ExportableEntity<I>> extends Dao<T> {

    T findByTenantIdAndExternalId(UUID tenantId, UUID externalId);

    default T findByTenantIdAndName(UUID tenantId, String name) { throw new UnsupportedOperationException(); }

    PageData<T> findByTenantId(UUID tenantId, PageLink pageLink);

    default PageData<I> findIdsByTenantId(UUID tenantId, PageLink pageLink) {
        return findByTenantId(tenantId, pageLink).mapData(ExportableEntity::getId);
    }

    I getExternalIdByInternal(I internalId);

    default T findDefaultEntityByTenantId(UUID tenantId) { throw new UnsupportedOperationException(); }

}