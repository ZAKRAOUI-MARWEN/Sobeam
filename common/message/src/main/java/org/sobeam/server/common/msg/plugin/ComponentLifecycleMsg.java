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
package org.sobeam.server.common.msg.plugin;

import lombok.Data;
import org.sobeam.server.common.data.EntityType;
import org.sobeam.server.common.data.id.EntityId;
import org.sobeam.server.common.data.id.RuleChainId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.plugin.ComponentLifecycleEvent;
import org.sobeam.server.common.msg.MsgType;
import org.sobeam.server.common.msg.aware.TenantAwareMsg;
import org.sobeam.server.common.msg.cluster.ToAllNodesMsg;

import java.util.Optional;

/**
 * @author Andrew Shvayka
 */
@Data
public class ComponentLifecycleMsg implements TenantAwareMsg, ToAllNodesMsg {
    private static final long serialVersionUID = -5303421482781273062L;

    private final TenantId tenantId;
    private final EntityId entityId;
    private final ComponentLifecycleEvent event;

    public Optional<RuleChainId> getRuleChainId() {
        return entityId.getEntityType() == EntityType.RULE_CHAIN ? Optional.of((RuleChainId) entityId) : Optional.empty();
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.COMPONENT_LIFE_CYCLE_MSG;
    }
}
