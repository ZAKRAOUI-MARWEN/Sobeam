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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.id.UserId;
import org.sobeam.server.common.data.security.UserCredentials;
import org.sobeam.server.dao.DaoUtil;
import org.sobeam.server.dao.model.sql.UserCredentialsEntity;
import org.sobeam.server.dao.sql.JpaAbstractDao;
import org.sobeam.server.dao.user.UserCredentialsDao;
import org.sobeam.server.dao.util.SqlDao;

import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 4/22/2017.
 */
@Component
@SqlDao
public class JpaUserCredentialsDao extends JpaAbstractDao<UserCredentialsEntity, UserCredentials> implements UserCredentialsDao {

    @Autowired
    private UserCredentialsRepository userCredentialsRepository;

    @Override
    protected Class<UserCredentialsEntity> getEntityClass() {
        return UserCredentialsEntity.class;
    }

    @Override
    protected JpaRepository<UserCredentialsEntity, UUID> getRepository() {
        return userCredentialsRepository;
    }

    @Override
    public UserCredentials findByUserId(TenantId tenantId, UUID userId) {
        return DaoUtil.getData(userCredentialsRepository.findByUserId(userId));
    }

    @Override
    public UserCredentials findByActivateToken(TenantId tenantId, String activateToken) {
        return DaoUtil.getData(userCredentialsRepository.findByActivateToken(activateToken));
    }

    @Override
    public UserCredentials findByResetToken(TenantId tenantId, String resetToken) {
        return DaoUtil.getData(userCredentialsRepository.findByResetToken(resetToken));
    }

    @Override
    public void removeByUserId(TenantId tenantId, UserId userId) {
        userCredentialsRepository.removeByUserId(userId.getId());
    }

}
