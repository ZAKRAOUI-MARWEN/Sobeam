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
package org.sobeam.server.dao.widget;

import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.id.WidgetsBundleId;
import org.sobeam.server.common.data.page.PageData;
import org.sobeam.server.common.data.page.PageLink;
import org.sobeam.server.common.data.widget.WidgetsBundle;
import org.sobeam.server.dao.entity.EntityDaoService;

import java.util.List;

public interface WidgetsBundleService extends EntityDaoService {

    WidgetsBundle findWidgetsBundleById(TenantId tenantId, WidgetsBundleId widgetsBundleId);

    WidgetsBundle saveWidgetsBundle(WidgetsBundle widgetsBundle);

    void deleteWidgetsBundle(TenantId tenantId, WidgetsBundleId widgetsBundleId);

    WidgetsBundle findWidgetsBundleByTenantIdAndAlias(TenantId tenantId, String alias);

    PageData<WidgetsBundle> findSystemWidgetsBundlesByPageLink(TenantId tenantId, boolean fullSearch, PageLink pageLink);

    List<WidgetsBundle> findSystemWidgetsBundles(TenantId tenantId);

    PageData<WidgetsBundle> findTenantWidgetsBundlesByTenantId(TenantId tenantId, PageLink pageLink);

    PageData<WidgetsBundle> findAllTenantWidgetsBundlesByTenantIdAndPageLink(TenantId tenantId, boolean fullSearch, PageLink pageLink);

    PageData<WidgetsBundle> findTenantWidgetsBundlesByTenantIdAndPageLink(TenantId tenantId, boolean fullSearch, PageLink pageLink);

    List<WidgetsBundle> findAllTenantWidgetsBundlesByTenantId(TenantId tenantId);

    void deleteWidgetsBundlesByTenantId(TenantId tenantId);

}
