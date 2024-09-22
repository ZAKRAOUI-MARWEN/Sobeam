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

import org.sobeam.server.common.data.EntityType;
import org.sobeam.server.common.data.id.EntityId;
import org.sobeam.server.common.data.id.NotificationRequestId;
import org.sobeam.server.common.data.id.NotificationRuleId;
import org.sobeam.server.common.data.id.NotificationTargetId;
import org.sobeam.server.common.data.id.NotificationTemplateId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.notification.NotificationRequest;
import org.sobeam.server.common.data.notification.NotificationRequestInfo;
import org.sobeam.server.common.data.notification.NotificationRequestStats;
import org.sobeam.server.common.data.notification.NotificationRequestStatus;
import org.sobeam.server.common.data.page.PageData;
import org.sobeam.server.common.data.page.PageLink;
import org.sobeam.server.dao.Dao;

import java.util.List;

public interface NotificationRequestDao extends Dao<NotificationRequest> {

    PageData<NotificationRequest> findByTenantIdAndOriginatorTypeAndPageLink(TenantId tenantId, EntityType originatorType, PageLink pageLink);

    PageData<NotificationRequestInfo> findInfosByTenantIdAndOriginatorTypeAndPageLink(TenantId tenantId, EntityType originatorType, PageLink pageLink);

    List<NotificationRequestId> findIdsByRuleId(TenantId tenantId, NotificationRequestStatus requestStatus, NotificationRuleId ruleId);

    List<NotificationRequest> findByRuleIdAndOriginatorEntityId(TenantId tenantId, NotificationRuleId ruleId, EntityId originatorEntityId);

    PageData<NotificationRequest> findAllByStatus(NotificationRequestStatus status, PageLink pageLink);

    void updateById(TenantId tenantId, NotificationRequestId requestId, NotificationRequestStatus requestStatus, NotificationRequestStats stats);

    boolean existsByTenantIdAndStatusAndTargetId(TenantId tenantId, NotificationRequestStatus status, NotificationTargetId targetId);

    boolean existsByTenantIdAndStatusAndTemplateId(TenantId tenantId, NotificationRequestStatus status, NotificationTemplateId templateId);

    int removeAllByCreatedTimeBefore(long ts);

    NotificationRequestInfo findInfoById(TenantId tenantId, NotificationRequestId id);

    void removeByTenantId(TenantId tenantId);

}
