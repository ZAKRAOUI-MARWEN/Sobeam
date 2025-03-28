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
package org.sobeam.server.common.data.notification.rule.trigger;

import lombok.Builder;
import lombok.Data;
import org.sobeam.server.common.data.HasName;
import org.sobeam.server.common.data.User;
import org.sobeam.server.common.data.audit.ActionType;
import org.sobeam.server.common.data.id.EntityId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.notification.rule.trigger.config.NotificationRuleTriggerType;

@Data
@Builder
public class EntityActionTrigger implements NotificationRuleTrigger {

    private final TenantId tenantId;
    private final EntityId entityId;
    private final HasName entity;
    private final ActionType actionType;
    private final User user;

    @Override
    public NotificationRuleTriggerType getType() {
        return NotificationRuleTriggerType.ENTITY_ACTION;
    }

    @Override
    public EntityId getOriginatorEntityId() {
        return entityId;
    }

}
