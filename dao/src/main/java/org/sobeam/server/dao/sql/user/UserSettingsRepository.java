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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.sobeam.server.common.data.settings.UserSettingsCompositeKey;
import org.sobeam.server.dao.model.sql.UserSettingsEntity;

import java.util.List;
import java.util.UUID;

public interface UserSettingsRepository extends JpaRepository<UserSettingsEntity, UserSettingsCompositeKey> {

    @Transactional
    @Modifying
    @Query("DELETE FROM UserSettingsEntity s WHERE s.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

    @Query(value = "SELECT * FROM user_settings WHERE type = :type AND (settings #> :path) IS NOT NULL", nativeQuery = true)
    List<UserSettingsEntity> findByTypeAndPathExisting(@Param("type") String type, @Param("path") String[] path);

}
