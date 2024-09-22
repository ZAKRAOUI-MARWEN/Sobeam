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
package org.sobeam.server.dao.rule;

import org.sobeam.server.common.data.id.EntityId;
import org.sobeam.server.common.data.id.RuleNodeId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.page.PageData;
import org.sobeam.server.common.data.page.PageLink;
import org.sobeam.server.common.data.rule.RuleNodeState;

public interface RuleNodeStateService {

    PageData<RuleNodeState> findByRuleNodeId(TenantId tenantId, RuleNodeId ruleNodeId, PageLink pageLink);

    RuleNodeState findByRuleNodeIdAndEntityId(TenantId tenantId, RuleNodeId ruleNodeId, EntityId entityId);

    RuleNodeState save(TenantId tenantId, RuleNodeState ruleNodeState);

    void removeByRuleNodeId(TenantId tenantId, RuleNodeId selfId);

    void removeByRuleNodeIdAndEntityId(TenantId tenantId, RuleNodeId selfId, EntityId entityId);
}
