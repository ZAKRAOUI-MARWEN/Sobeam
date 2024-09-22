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
package org.sobeam.server.dao.notification;

import org.sobeam.server.common.data.id.NotificationTargetId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.notification.NotificationType;
import org.sobeam.server.common.data.notification.targets.NotificationTarget;
import org.sobeam.server.common.data.notification.targets.platform.UsersFilterType;
import org.sobeam.server.common.data.page.PageData;
import org.sobeam.server.common.data.page.PageLink;
import org.sobeam.server.dao.Dao;
import org.sobeam.server.dao.ExportableEntityDao;
import org.sobeam.server.dao.TenantEntityDao;

import java.util.List;

public interface NotificationTargetDao extends Dao<NotificationTarget>, TenantEntityDao, ExportableEntityDao<NotificationTargetId, NotificationTarget> {

    PageData<NotificationTarget> findByTenantIdAndPageLink(TenantId tenantId, PageLink pageLink);

    PageData<NotificationTarget> findByTenantIdAndSupportedNotificationTypeAndPageLink(TenantId tenantId, NotificationType notificationType, PageLink pageLink);

    List<NotificationTarget> findByTenantIdAndIds(TenantId tenantId, List<NotificationTargetId> ids);

    List<NotificationTarget> findByTenantIdAndUsersFilterType(TenantId tenantId, UsersFilterType filterType);

    void removeByTenantId(TenantId tenantId);

}
