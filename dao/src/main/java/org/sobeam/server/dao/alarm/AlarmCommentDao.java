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
package org.sobeam.server.dao.alarm;

import com.google.common.util.concurrent.ListenableFuture;
import org.sobeam.server.common.data.alarm.AlarmComment;
import org.sobeam.server.common.data.alarm.AlarmCommentInfo;
import org.sobeam.server.common.data.id.AlarmId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.page.PageData;
import org.sobeam.server.common.data.page.PageLink;
import org.sobeam.server.dao.Dao;

import java.util.UUID;

public interface AlarmCommentDao extends Dao<AlarmComment> {

    AlarmComment findAlarmCommentById(TenantId tenantId, UUID key);

    PageData<AlarmCommentInfo> findAlarmComments(TenantId tenantId, AlarmId id, PageLink pageLink);

    ListenableFuture<AlarmComment> findAlarmCommentByIdAsync(TenantId tenantId, UUID key);

}