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
package org.sobeam.server.dao.sql.attributes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.sobeam.server.dao.model.sql.AttributeKvCompositeKey;
import org.sobeam.server.dao.model.sql.AttributeKvEntity;

import java.util.List;
import java.util.UUID;

public interface AttributeKvRepository extends JpaRepository<AttributeKvEntity, AttributeKvCompositeKey> {

    @Query("SELECT a FROM AttributeKvEntity a WHERE a.id.entityId = :entityId " +
            "AND a.id.attributeType = :attributeType")
    List<AttributeKvEntity> findAllEntityIdAndAttributeType(@Param("entityId") UUID entityId,
                                                            @Param("attributeType") int attributeType);

    @Transactional
    @Modifying
    @Query("DELETE FROM AttributeKvEntity a WHERE a.id.entityId = :entityId " +
            "AND a.id.attributeType = :attributeType " +
            "AND a.id.attributeKey = :attributeKey")
    void delete(@Param("entityId") UUID entityId,
                @Param("attributeType") int attributeType,
                @Param("attributeKey") int attributeKey);

    @Query(value = "SELECT DISTINCT attribute_key FROM attribute_kv WHERE " +
            "entity_id in (SELECT id FROM device WHERE tenant_id = :tenantId and device_profile_id = :deviceProfileId limit 100) ORDER BY attribute_key", nativeQuery = true)
    List<Integer> findAllKeysByDeviceProfileId(@Param("tenantId") UUID tenantId,
                                               @Param("deviceProfileId") UUID deviceProfileId);

    @Query(value = "SELECT DISTINCT attribute_key FROM attribute_kv WHERE " +
            "entity_id in (SELECT id FROM device WHERE tenant_id = :tenantId limit 100) ORDER BY attribute_key", nativeQuery = true)
    List<Integer> findAllKeysByTenantId(@Param("tenantId") UUID tenantId);

    @Query(value = "SELECT DISTINCT attribute_key FROM attribute_kv WHERE " +
            "entity_id in :entityIds ORDER BY attribute_key", nativeQuery = true)
    List<Integer> findAllKeysByEntityIds(@Param("entityIds") List<UUID> entityIds);

    @Query(value = "SELECT DISTINCT attribute_key FROM attribute_kv WHERE " +
            "entity_id in :entityIds AND attribute_type = :attributeType ORDER BY attribute_key", nativeQuery = true)
    List<Integer> findAllKeysByEntityIdsAndAttributeType(@Param("entityIds") List<UUID> entityIds,
                                                         @Param("attributeType") int attributeType);
}

