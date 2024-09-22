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

import org.sobeam.server.common.data.id.NotificationTemplateId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.notification.NotificationType;
import org.sobeam.server.common.data.notification.template.NotificationTemplate;
import org.sobeam.server.common.data.page.PageData;
import org.sobeam.server.common.data.page.PageLink;

import java.util.List;

public interface NotificationTemplateService {

    NotificationTemplate findNotificationTemplateById(TenantId tenantId, NotificationTemplateId id);

    NotificationTemplate saveNotificationTemplate(TenantId tenantId, NotificationTemplate notificationTemplate);

    PageData<NotificationTemplate> findNotificationTemplatesByTenantIdAndNotificationTypes(TenantId tenantId, List<NotificationType> notificationTypes, PageLink pageLink);

    int countNotificationTemplatesByTenantIdAndNotificationTypes(TenantId tenantId, List<NotificationType> notificationTypes);

    void deleteNotificationTemplateById(TenantId tenantId, NotificationTemplateId id);

    void deleteNotificationTemplatesByTenantId(TenantId tenantId);

}
