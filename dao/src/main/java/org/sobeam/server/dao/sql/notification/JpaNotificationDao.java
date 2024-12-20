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
package org.sobeam.server.dao.sql.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.sobeam.server.common.data.EntityType;
import org.sobeam.server.common.data.id.NotificationId;
import org.sobeam.server.common.data.id.NotificationRequestId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.id.UserId;
import org.sobeam.server.common.data.notification.Notification;
import org.sobeam.server.common.data.notification.NotificationDeliveryMethod;
import org.sobeam.server.common.data.notification.NotificationStatus;
import org.sobeam.server.common.data.page.PageData;
import org.sobeam.server.common.data.page.PageLink;
import org.sobeam.server.dao.DaoUtil;
import org.sobeam.server.dao.model.ModelConstants;
import org.sobeam.server.dao.model.sql.NotificationEntity;
import org.sobeam.server.dao.notification.NotificationDao;
import org.sobeam.server.dao.sql.JpaPartitionedAbstractDao;
import org.sobeam.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.sobeam.server.dao.util.SqlDao;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@SqlDao
@RequiredArgsConstructor
public class JpaNotificationDao extends JpaPartitionedAbstractDao<NotificationEntity, Notification> implements NotificationDao {

    private final NotificationRepository notificationRepository;
    private final SqlPartitioningRepository partitioningRepository;

    @Value("${sql.notifications.partition_size:168}")
    private int partitionSizeInHours;

    @Override
    public PageData<Notification> findUnreadByDeliveryMethodAndRecipientIdAndPageLink(TenantId tenantId, NotificationDeliveryMethod deliveryMethod, UserId recipientId, PageLink pageLink) {
        return DaoUtil.toPageData(notificationRepository.findByDeliveryMethodAndRecipientIdAndStatusNot(deliveryMethod,
                recipientId.getId(), NotificationStatus.READ, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Notification> findByDeliveryMethodAndRecipientIdAndPageLink(TenantId tenantId, NotificationDeliveryMethod deliveryMethod, UserId recipientId, PageLink pageLink) {
        return DaoUtil.toPageData(notificationRepository.findByDeliveryMethodAndRecipientId(deliveryMethod, recipientId.getId(),
                pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public boolean updateStatusByIdAndRecipientId(TenantId tenantId, UserId recipientId, NotificationId notificationId, NotificationStatus status) {
        return notificationRepository.updateStatusByIdAndRecipientId(notificationId.getId(), recipientId.getId(), status) != 0;
    }

    /**
     * For this hot method, the partial index `idx_notification_recipient_id_unread` was introduced since 3.6.0
     * */
    @Override
    public int countUnreadByDeliveryMethodAndRecipientId(TenantId tenantId, NotificationDeliveryMethod deliveryMethod, UserId recipientId) {
        return notificationRepository.countByDeliveryMethodAndRecipientIdAndStatusNot(deliveryMethod, recipientId.getId(), NotificationStatus.READ);
    }

    @Override
    public boolean deleteByIdAndRecipientId(TenantId tenantId, UserId recipientId, NotificationId notificationId) {
        return notificationRepository.deleteByIdAndRecipientId(notificationId.getId(), recipientId.getId()) != 0;
    }

    @Override
    public int updateStatusByDeliveryMethodAndRecipientId(TenantId tenantId, NotificationDeliveryMethod deliveryMethod, UserId recipientId, NotificationStatus status) {
        return notificationRepository.updateStatusByDeliveryMethodAndRecipientIdAndStatusNot(deliveryMethod, recipientId.getId(), status);
    }

    @Override
    public void deleteByRequestId(TenantId tenantId, NotificationRequestId requestId) {
        notificationRepository.deleteByRequestId(requestId.getId());
    }

    @Override
    public void deleteByRecipientId(TenantId tenantId, UserId recipientId) {
        notificationRepository.deleteByRecipientId(recipientId.getId());
    }

    @Override
    public void createPartition(NotificationEntity entity) {
        partitioningRepository.createPartitionIfNotExists(ModelConstants.NOTIFICATION_TABLE_NAME,
                entity.getCreatedTime(), TimeUnit.HOURS.toMillis(partitionSizeInHours));
    }

    @Override
    protected Class<NotificationEntity> getEntityClass() {
        return NotificationEntity.class;
    }

    @Override
    protected JpaRepository<NotificationEntity, UUID> getRepository() {
        return notificationRepository;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.NOTIFICATION;
    }

}
