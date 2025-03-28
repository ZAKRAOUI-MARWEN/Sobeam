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
package org.sobeam.server.service.query;

import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.page.PageData;
import org.sobeam.server.common.data.query.AlarmCountQuery;
import org.sobeam.server.common.data.query.AlarmData;
import org.sobeam.server.common.data.query.AlarmDataQuery;
import org.sobeam.server.common.data.query.EntityCountQuery;
import org.sobeam.server.common.data.query.EntityData;
import org.sobeam.server.common.data.query.EntityDataQuery;
import org.sobeam.server.service.security.model.SecurityUser;

public interface EntityQueryService {

    long countEntitiesByQuery(SecurityUser securityUser, EntityCountQuery query);

    PageData<EntityData> findEntityDataByQuery(SecurityUser securityUser, EntityDataQuery query);

    PageData<AlarmData> findAlarmDataByQuery(SecurityUser securityUser, AlarmDataQuery query);

    long countAlarmsByQuery(SecurityUser securityUser, AlarmCountQuery query);

    DeferredResult<ResponseEntity> getKeysByQuery(SecurityUser securityUser, TenantId tenantId, EntityDataQuery query,
                                                  boolean isTimeseries, boolean isAttributes, String attributesScope);

}
