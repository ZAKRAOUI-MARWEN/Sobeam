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
package org.sobeam.server.dao.user;

import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.id.UserId;
import org.sobeam.server.common.data.settings.UserSettings;
import org.sobeam.server.common.data.settings.UserSettingsCompositeKey;
import org.sobeam.server.common.data.settings.UserSettingsType;

import java.util.List;

public interface UserSettingsDao {

    UserSettings save(TenantId tenantId, UserSettings userSettings);

    UserSettings findById(TenantId tenantId, UserSettingsCompositeKey key);

    void removeById(TenantId tenantId, UserSettingsCompositeKey key);

    void removeByUserId(TenantId tenantId, UserId userId);

    List<UserSettings> findByTypeAndPath(TenantId tenantId, UserSettingsType type, String... path);

}
