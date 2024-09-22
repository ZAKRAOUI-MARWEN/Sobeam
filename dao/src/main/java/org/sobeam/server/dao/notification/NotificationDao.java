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

import org.sobeam.server.common.data.id.NotificationId;
import org.sobeam.server.common.data.id.NotificationRequestId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.id.UserId;
import org.sobeam.server.common.data.notification.Notification;
import org.sobeam.server.common.data.notification.NotificationDeliveryMethod;
import org.sobeam.server.common.data.notification.NotificationStatus;
import org.sobeam.server.common.data.page.PageData;
import org.sobeam.server.common.data.page.PageLink;
import org.sobeam.server.dao.Dao;

public interface NotificationDao extends Dao<Notification> {

    PageData<Notification> findUnreadByDeliveryMethodAndRecipientIdAndPageLink(TenantId tenantId, NotificationDeliveryMethod deliveryMethod, UserId recipientId, PageLink pageLink);

    PageData<Notification> findByDeliveryMethodAndRecipientIdAndPageLink(TenantId tenantId, NotificationDeliveryMethod deliveryMethod, UserId recipientId, PageLink pageLink);

    boolean updateStatusByIdAndRecipientId(TenantId tenantId, UserId recipientId, NotificationId notificationId, NotificationStatus status);

    int countUnreadByDeliveryMethodAndRecipientId(TenantId tenantId, NotificationDeliveryMethod deliveryMethod, UserId recipientId);

    boolean deleteByIdAndRecipientId(TenantId tenantId, UserId recipientId, NotificationId notificationId);

    int updateStatusByDeliveryMethodAndRecipientId(TenantId tenantId, NotificationDeliveryMethod deliveryMethod, UserId recipientId, NotificationStatus status);

    void deleteByRequestId(TenantId tenantId, NotificationRequestId requestId);

    void deleteByRecipientId(TenantId tenantId, UserId recipientId);

}
