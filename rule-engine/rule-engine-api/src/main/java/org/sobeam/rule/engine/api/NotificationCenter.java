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
package org.sobeam.rule.engine.api;

import com.google.common.util.concurrent.FutureCallback;
import org.sobeam.server.common.data.id.NotificationId;
import org.sobeam.server.common.data.id.NotificationRequestId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.id.UserId;
import org.sobeam.server.common.data.notification.NotificationDeliveryMethod;
import org.sobeam.server.common.data.notification.NotificationRequest;
import org.sobeam.server.common.data.notification.NotificationRequestStats;
import org.sobeam.server.common.data.notification.targets.platform.UsersFilter;
import org.sobeam.server.common.data.notification.template.NotificationTemplate;

import java.util.Set;

public interface NotificationCenter {

    NotificationRequest processNotificationRequest(TenantId tenantId, NotificationRequest notificationRequest, FutureCallback<NotificationRequestStats> callback);

    void sendGeneralWebNotification(TenantId tenantId, UsersFilter recipients, NotificationTemplate template);

    void deleteNotificationRequest(TenantId tenantId, NotificationRequestId notificationRequestId);

    void markNotificationAsRead(TenantId tenantId, UserId recipientId, NotificationId notificationId);

    void markAllNotificationsAsRead(TenantId tenantId, NotificationDeliveryMethod deliveryMethod, UserId recipientId);

    void deleteNotification(TenantId tenantId, UserId recipientId, NotificationId notificationId);

    Set<NotificationDeliveryMethod> getAvailableDeliveryMethods(TenantId tenantId);

}
