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
package org.sobeam.server.service.notification;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.sobeam.common.util.JacksonUtil;
import org.sobeam.server.common.data.notification.Notification;
import org.sobeam.server.controller.TbTestWebSocketClient;
import org.sobeam.server.service.ws.notification.cmd.MarkAllNotificationsAsReadCmd;
import org.sobeam.server.service.ws.notification.cmd.MarkNotificationsAsReadCmd;
import org.sobeam.server.service.ws.notification.cmd.NotificationsCountSubCmd;
import org.sobeam.server.service.ws.notification.cmd.NotificationsSubCmd;
import org.sobeam.server.service.ws.notification.cmd.UnreadNotificationsCountUpdate;
import org.sobeam.server.service.ws.notification.cmd.UnreadNotificationsUpdate;
import org.sobeam.server.service.ws.telemetry.cmd.v2.CmdUpdateType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Getter
public class NotificationApiWsClient extends TbTestWebSocketClient {

    private UnreadNotificationsUpdate lastDataUpdate;
    private UnreadNotificationsCountUpdate lastCountUpdate;

    private int limit;
    private int unreadCount;
    private List<Notification> notifications;

    public NotificationApiWsClient(String wsUrl) throws URISyntaxException {
        super(new URI(wsUrl + "/api/ws"));
    }

    public NotificationApiWsClient subscribeForUnreadNotifications(int limit) {
        send(new NotificationsSubCmd(1, limit));
        this.limit = limit;
        return this;
    }

    public NotificationApiWsClient subscribeForUnreadNotificationsCount() {
        send(new NotificationsCountSubCmd(2));
        return this;
    }

    public void markNotificationAsRead(UUID... notifications) {
        send(new MarkNotificationsAsReadCmd(newCmdId(), Arrays.asList(notifications)));
    }

    public void markAllNotificationsAsRead() {
        send(new MarkAllNotificationsAsReadCmd(newCmdId()));
    }

    @Override
    public void registerWaitForUpdate(int count) {
        lastDataUpdate = null;
        lastCountUpdate = null;
        super.registerWaitForUpdate(count);
    }

    @Override
    public void onMessage(String s) {
        JsonNode update = JacksonUtil.toJsonNode(s);
        CmdUpdateType updateType = CmdUpdateType.valueOf(update.get("cmdUpdateType").asText());
        if (updateType == CmdUpdateType.NOTIFICATIONS) {
            lastDataUpdate = JacksonUtil.treeToValue(update, UnreadNotificationsUpdate.class);
            unreadCount = lastDataUpdate.getTotalUnreadCount();
            if (lastDataUpdate.getNotifications() != null) {
                notifications = new ArrayList<>(lastDataUpdate.getNotifications());
            } else {
                Notification notificationUpdate = lastDataUpdate.getUpdate();
                boolean updated = false;
                for (int i = 0; i < notifications.size(); i++) {
                    Notification existing = notifications.get(i);
                    if (existing.getId().equals(notificationUpdate.getId())) {
                        notifications.set(i, notificationUpdate);
                        updated = true;
                        break;
                    }
                }
                if (!updated) {
                    notifications.add(0, notificationUpdate);
                    if (notifications.size() > limit) {
                        notifications = notifications.subList(0, limit);
                    }
                }
            }
        } else if (updateType == CmdUpdateType.NOTIFICATIONS_COUNT) {
            UnreadNotificationsCountUpdate countUpdate = JacksonUtil.treeToValue(update, UnreadNotificationsCountUpdate.class);
            if (lastCountUpdate == null || countUpdate.getSequenceNumber() > lastCountUpdate.getSequenceNumber()) {
                lastCountUpdate = countUpdate;
                unreadCount = lastCountUpdate.getTotalUnreadCount();
            }
        }
        super.onMessage(s);
    }

    private static int newCmdId() {
        return RandomUtils.nextInt(1, 1000);
    }

}
