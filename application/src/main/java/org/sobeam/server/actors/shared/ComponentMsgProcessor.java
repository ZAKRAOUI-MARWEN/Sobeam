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
package org.sobeam.server.actors.shared;

import lombok.extern.slf4j.Slf4j;
import org.sobeam.server.actors.ActorSystemContext;
import org.sobeam.server.actors.TbActorCtx;
import org.sobeam.server.actors.stats.StatsPersistTick;
import org.sobeam.server.common.data.id.EntityId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.plugin.ComponentLifecycleState;
import org.sobeam.server.common.data.tenant.profile.TenantProfileConfiguration;
import org.sobeam.server.common.msg.TbMsg;
import org.sobeam.server.common.msg.queue.PartitionChangeMsg;
import org.sobeam.server.common.msg.queue.RuleNodeException;

@Slf4j
public abstract class ComponentMsgProcessor<T extends EntityId> extends AbstractContextAwareMsgProcessor {

    protected final TenantId tenantId;
    protected final T entityId;
    protected ComponentLifecycleState state;

    protected ComponentMsgProcessor(ActorSystemContext systemContext, TenantId tenantId, T id) {
        super(systemContext);
        this.tenantId = tenantId;
        this.entityId = id;
    }

    protected TenantProfileConfiguration getTenantProfileConfiguration() {
        return systemContext.getTenantProfileCache().get(tenantId).getProfileData().getConfiguration();
    }

    public abstract String getComponentName();

    public abstract void start(TbActorCtx context) throws Exception;

    public abstract void stop(TbActorCtx context) throws Exception;

    public abstract void onPartitionChangeMsg(PartitionChangeMsg msg) throws Exception;

    public void onCreated(TbActorCtx context) throws Exception {
        start(context);
    }

    public void onUpdate(TbActorCtx context) throws Exception {
        restart(context);
    }

    public void onActivate(TbActorCtx context) throws Exception {
        restart(context);
    }

    public void onSuspend(TbActorCtx context) throws Exception {
        stop(context);
    }

    public void onStop(TbActorCtx context) throws Exception {
        stop(context);
    }

    private void restart(TbActorCtx context) throws Exception {
        stop(context);
        start(context);
    }

    public void scheduleStatsPersistTick(TbActorCtx context, long statsPersistFrequency) {
        schedulePeriodicMsgWithDelay(context, new StatsPersistTick(), statsPersistFrequency, statsPersistFrequency);
    }

    protected boolean checkMsgValid(TbMsg tbMsg) {
        var valid = tbMsg.isValid();
        if (!valid) {
            if (log.isTraceEnabled()) {
                log.trace("Skip processing of message: {} because it is no longer valid!", tbMsg);
            }
        }
        return valid;
    }

    protected void checkComponentStateActive(TbMsg tbMsg) throws RuleNodeException {
        if (state != ComponentLifecycleState.ACTIVE) {
            log.debug("Component is not active. Current state [{}] for processor [{}][{}] tenant [{}]", state, entityId.getEntityType(), entityId, tenantId);
            RuleNodeException ruleNodeException = getInactiveException();
            if (tbMsg != null) {
                tbMsg.getCallback().onFailure(ruleNodeException);
            }
            throw ruleNodeException;
        }
    }

    abstract protected RuleNodeException getInactiveException();

}
