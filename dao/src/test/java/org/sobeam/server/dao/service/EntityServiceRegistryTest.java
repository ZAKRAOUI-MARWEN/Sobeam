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
package org.sobeam.server.dao.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.sobeam.server.common.data.EntityType;
import org.sobeam.server.dao.entity.EntityDaoService;
import org.sobeam.server.dao.entity.EntityServiceRegistry;
import org.sobeam.server.dao.rule.RuleChainService;

@Slf4j
@DaoSqlTest
public class EntityServiceRegistryTest extends AbstractServiceTest {

    @Autowired
    EntityServiceRegistry entityServiceRegistry;

    @Test
    public void givenAllEntityTypes_whenGetServiceByEntityTypeCalled_thenAllBeansExists() {
        for (EntityType entityType : EntityType.values()) {
            EntityDaoService entityDaoService = entityServiceRegistry.getServiceByEntityType(entityType);
            Assert.assertNotNull("entityDaoService bean is missed for type: " + entityType.name(), entityDaoService);
        }
    }

    @Test
    public void givenRuleNodeEntityType_whenGetServiceByEntityTypeCalled_thenReturnedRuleChainService() {
        Assert.assertTrue(entityServiceRegistry.getServiceByEntityType(EntityType.RULE_NODE) instanceof RuleChainService);
    }

}
