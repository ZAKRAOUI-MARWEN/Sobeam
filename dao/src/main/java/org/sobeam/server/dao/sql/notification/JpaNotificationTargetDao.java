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

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.sobeam.server.common.data.EntityType;
import org.sobeam.server.common.data.id.NotificationTargetId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.notification.NotificationType;
import org.sobeam.server.common.data.notification.targets.NotificationTarget;
import org.sobeam.server.common.data.notification.targets.platform.UsersFilterType;
import org.sobeam.server.common.data.page.PageData;
import org.sobeam.server.common.data.page.PageLink;
import org.sobeam.server.dao.DaoUtil;
import org.sobeam.server.dao.model.sql.NotificationTargetEntity;
import org.sobeam.server.dao.notification.NotificationTargetDao;
import org.sobeam.server.dao.sql.JpaAbstractDao;
import org.sobeam.server.dao.util.SqlDao;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@SqlDao
@RequiredArgsConstructor
public class JpaNotificationTargetDao extends JpaAbstractDao<NotificationTargetEntity, NotificationTarget> implements NotificationTargetDao {

    private final NotificationTargetRepository notificationTargetRepository;

    @Override
    public PageData<NotificationTarget> findByTenantIdAndPageLink(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(notificationTargetRepository.findByTenantIdAndSearchText(tenantId.getId(),
                pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<NotificationTarget> findByTenantIdAndSupportedNotificationTypeAndPageLink(TenantId tenantId, NotificationType notificationType, PageLink pageLink) {
        return DaoUtil.toPageData(notificationTargetRepository.findByTenantIdAndSearchTextAndUsersFilterTypeIfPresent(tenantId.getId(),
                pageLink.getTextSearch(),
                Arrays.stream(UsersFilterType.values())
                        .filter(type -> notificationType != NotificationType.GENERAL || !type.isForRules())
                        .map(Enum::name).collect(Collectors.toList()),
                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public List<NotificationTarget> findByTenantIdAndIds(TenantId tenantId, List<NotificationTargetId> ids) {
        return DaoUtil.convertDataList(notificationTargetRepository.findByTenantIdAndIdIn(tenantId.getId(), DaoUtil.toUUIDs(ids)));
    }

    @Override
    public List<NotificationTarget> findByTenantIdAndUsersFilterType(TenantId tenantId, UsersFilterType filterType) {
        return DaoUtil.convertDataList(notificationTargetRepository.findByTenantIdAndSearchTextAndUsersFilterTypeIfPresent(tenantId.getId(), null,
                List.of(filterType.name()), DaoUtil.toPageable(new PageLink(Integer.MAX_VALUE))).getContent());
    }

    @Override
    public void removeByTenantId(TenantId tenantId) {
        notificationTargetRepository.deleteByTenantId(tenantId.getId());
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return notificationTargetRepository.countByTenantId(tenantId.getId());
    }

    @Override
    public NotificationTarget findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(notificationTargetRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public NotificationTarget findByTenantIdAndName(UUID tenantId, String name) {
        return DaoUtil.getData(notificationTargetRepository.findByTenantIdAndName(tenantId, name));
    }

    @Override
    public PageData<NotificationTarget> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(notificationTargetRepository.findByTenantId(tenantId, DaoUtil.toPageable(pageLink)));
    }

    @Override
    public NotificationTargetId getExternalIdByInternal(NotificationTargetId internalId) {
        return DaoUtil.toEntityId(notificationTargetRepository.getExternalIdByInternal(internalId.getId()), NotificationTargetId::new);
    }

    @Override
    protected Class<NotificationTargetEntity> getEntityClass() {
        return NotificationTargetEntity.class;
    }

    @Override
    protected JpaRepository<NotificationTargetEntity, UUID> getRepository() {
        return notificationTargetRepository;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.NOTIFICATION_TARGET;
    }

}
