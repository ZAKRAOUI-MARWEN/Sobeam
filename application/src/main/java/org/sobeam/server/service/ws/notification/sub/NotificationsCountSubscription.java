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
package org.sobeam.server.service.ws.notification.sub;

import lombok.Builder;
import lombok.Getter;
import org.sobeam.server.common.data.id.EntityId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.service.subscription.TbSubscription;
import org.sobeam.server.service.subscription.TbSubscriptionType;
import org.sobeam.server.service.ws.notification.cmd.UnreadNotificationsCountUpdate;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

@Getter
public class NotificationsCountSubscription extends AbstractNotificationSubscription<NotificationsSubscriptionUpdate> {

    @Builder
    public NotificationsCountSubscription(String serviceId, String sessionId, int subscriptionId, TenantId tenantId, EntityId entityId,
                                          BiConsumer<TbSubscription<NotificationsSubscriptionUpdate>, NotificationsSubscriptionUpdate> updateProcessor) {
        super(serviceId, sessionId, subscriptionId, tenantId, entityId, TbSubscriptionType.NOTIFICATIONS_COUNT, updateProcessor);
    }

    public UnreadNotificationsCountUpdate createUpdate() {
        return UnreadNotificationsCountUpdate.builder()
                .cmdId(getSubscriptionId())
                .totalUnreadCount(totalUnreadCounter.get())
                .sequenceNumber(sequence.incrementAndGet())
                .build();
    }

}
