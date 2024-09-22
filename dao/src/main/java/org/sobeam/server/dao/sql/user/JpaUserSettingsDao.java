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
package org.sobeam.server.dao.sql.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.id.UserId;
import org.sobeam.server.common.data.settings.UserSettings;
import org.sobeam.server.common.data.settings.UserSettingsCompositeKey;
import org.sobeam.server.common.data.settings.UserSettingsType;
import org.sobeam.server.dao.DaoUtil;
import org.sobeam.server.dao.model.sql.UserSettingsEntity;
import org.sobeam.server.dao.sql.JpaAbstractDaoListeningExecutorService;
import org.sobeam.server.dao.user.UserSettingsDao;
import org.sobeam.server.dao.util.SqlDao;

import java.util.List;

@Slf4j
@Component
@SqlDao
public class JpaUserSettingsDao extends JpaAbstractDaoListeningExecutorService implements UserSettingsDao {

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Override
    public UserSettings save(TenantId tenantId, UserSettings userSettings) {
        log.trace("save [{}][{}]", tenantId, userSettings);
        return DaoUtil.getData(userSettingsRepository.save(new UserSettingsEntity(userSettings)));
    }

    @Override
    public UserSettings findById(TenantId tenantId, UserSettingsCompositeKey id) {
        return DaoUtil.getData(userSettingsRepository.findById(id));
    }

    @Override
    public void removeById(TenantId tenantId, UserSettingsCompositeKey id) {
        userSettingsRepository.deleteById(id);
    }

    @Override
    public void removeByUserId(TenantId tenantId, UserId userId) {
        userSettingsRepository.deleteByUserId(userId.getId());
    }

    @Override
    public List<UserSettings> findByTypeAndPath(TenantId tenantId, UserSettingsType type, String... path) {
        log.trace("findByTypeAndPath [{}][{}][{}]", tenantId, type, path);
        return DaoUtil.convertDataList(userSettingsRepository.findByTypeAndPathExisting(type.name(), path));
    }

}
