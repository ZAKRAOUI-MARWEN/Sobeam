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
package org.sobeam.server.dao.sql.rule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.sobeam.server.common.data.page.PageData;
import org.sobeam.server.common.data.page.PageLink;
import org.sobeam.server.common.data.rule.RuleNodeState;
import org.sobeam.server.dao.DaoUtil;
import org.sobeam.server.dao.model.sql.RuleNodeStateEntity;
import org.sobeam.server.dao.rule.RuleNodeStateDao;
import org.sobeam.server.dao.sql.JpaAbstractDao;
import org.sobeam.server.dao.util.SqlDao;

import java.util.UUID;

@Slf4j
@Component
@SqlDao
public class JpaRuleNodeStateDao extends JpaAbstractDao<RuleNodeStateEntity, RuleNodeState> implements RuleNodeStateDao {

    @Autowired
    private RuleNodeStateRepository ruleNodeStateRepository;

    @Override
    protected Class<RuleNodeStateEntity> getEntityClass() {
        return RuleNodeStateEntity.class;
    }

    @Override
    protected JpaRepository<RuleNodeStateEntity, UUID> getRepository() {
        return ruleNodeStateRepository;
    }

    @Override
    public PageData<RuleNodeState> findByRuleNodeId(UUID ruleNodeId, PageLink pageLink) {
        return DaoUtil.toPageData(ruleNodeStateRepository.findByRuleNodeId(ruleNodeId, DaoUtil.toPageable(pageLink)));
    }

    @Override
    public RuleNodeState findByRuleNodeIdAndEntityId(UUID ruleNodeId, UUID entityId) {
        return DaoUtil.getData(ruleNodeStateRepository.findByRuleNodeIdAndEntityId(ruleNodeId, entityId));
    }

    @Transactional
    @Override
    public void removeByRuleNodeId(UUID ruleNodeId) {
        ruleNodeStateRepository.removeByRuleNodeId(ruleNodeId);
    }

    @Transactional
    @Override
    public void removeByRuleNodeIdAndEntityId(UUID ruleNodeId, UUID entityId) {
        ruleNodeStateRepository.removeByRuleNodeIdAndEntityId(ruleNodeId, entityId);
    }
}
