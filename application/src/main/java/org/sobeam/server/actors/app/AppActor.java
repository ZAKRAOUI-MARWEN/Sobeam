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
package org.sobeam.server.actors.app;

import lombok.extern.slf4j.Slf4j;
import org.sobeam.server.actors.ActorSystemContext;
import org.sobeam.server.actors.ProcessFailureStrategy;
import org.sobeam.server.actors.TbActor;
import org.sobeam.server.actors.TbActorCtx;
import org.sobeam.server.actors.TbActorException;
import org.sobeam.server.actors.TbActorId;
import org.sobeam.server.actors.TbActorRef;
import org.sobeam.server.actors.TbEntityActorId;
import org.sobeam.server.actors.device.SessionTimeoutCheckMsg;
import org.sobeam.server.actors.service.ContextAwareActor;
import org.sobeam.server.actors.service.ContextBasedCreator;
import org.sobeam.server.actors.service.DefaultActorService;
import org.sobeam.server.actors.tenant.TenantActor;
import org.sobeam.server.common.data.EntityType;
import org.sobeam.server.common.data.Tenant;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.page.PageDataIterable;
import org.sobeam.server.common.data.plugin.ComponentLifecycleEvent;
import org.sobeam.server.common.msg.MsgType;
import org.sobeam.server.common.msg.TbActorMsg;
import org.sobeam.server.common.msg.aware.TenantAwareMsg;
import org.sobeam.server.common.msg.edge.EdgeSessionMsg;
import org.sobeam.server.common.msg.plugin.ComponentLifecycleMsg;
import org.sobeam.server.common.msg.queue.QueueToRuleEngineMsg;
import org.sobeam.server.common.msg.queue.RuleEngineException;
import org.sobeam.server.common.msg.queue.ServiceType;
import org.sobeam.server.dao.model.ModelConstants;
import org.sobeam.server.dao.tenant.TenantService;
import org.sobeam.server.service.transport.msg.TransportToDeviceActorMsgWrapper;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class AppActor extends ContextAwareActor {

    private final TenantService tenantService;
    private final Set<TenantId> deletedTenants;
    private volatile boolean ruleChainsInitialized;

    private AppActor(ActorSystemContext systemContext) {
        super(systemContext);
        this.tenantService = systemContext.getTenantService();
        this.deletedTenants = new HashSet<>();
    }

    @Override
    public void init(TbActorCtx ctx) throws TbActorException {
        super.init(ctx);
        if (systemContext.getServiceInfoProvider().isService(ServiceType.TB_CORE)) {
            systemContext.schedulePeriodicMsgWithDelay(ctx, SessionTimeoutCheckMsg.instance(),
                    systemContext.getSessionReportTimeout(), systemContext.getSessionReportTimeout());
        }
    }

    @Override
    protected boolean doProcess(TbActorMsg msg) {
        if (!ruleChainsInitialized) {
            if (MsgType.APP_INIT_MSG.equals(msg.getMsgType())) {
                initTenantActors();
                ruleChainsInitialized = true;
            } else {
                if (!msg.getMsgType().isIgnoreOnStart()) {
                    log.warn("Attempt to initialize Rule Chains by unexpected message: {}", msg);
                }
                return true;
            }
        }
        switch (msg.getMsgType()) {
            case APP_INIT_MSG:
                break;
            case PARTITION_CHANGE_MSG:
                ctx.broadcastToChildren(msg, true);
                break;
            case COMPONENT_LIFE_CYCLE_MSG:
                onComponentLifecycleMsg((ComponentLifecycleMsg) msg);
                break;
            case QUEUE_TO_RULE_ENGINE_MSG:
                onQueueToRuleEngineMsg((QueueToRuleEngineMsg) msg);
                break;
            case TRANSPORT_TO_DEVICE_ACTOR_MSG:
                onToDeviceActorMsg((TenantAwareMsg) msg, false);
                break;
            case DEVICE_ATTRIBUTES_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_CREDENTIALS_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_NAME_OR_TYPE_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_EDGE_UPDATE_TO_DEVICE_ACTOR_MSG:
            case DEVICE_RPC_REQUEST_TO_DEVICE_ACTOR_MSG:
            case DEVICE_RPC_RESPONSE_TO_DEVICE_ACTOR_MSG:
            case SERVER_RPC_RESPONSE_TO_DEVICE_ACTOR_MSG:
            case REMOVE_RPC_TO_DEVICE_ACTOR_MSG:
                onToDeviceActorMsg((TenantAwareMsg) msg, true);
                break;
            case EDGE_EVENT_UPDATE_TO_EDGE_SESSION_MSG:
            case EDGE_SYNC_REQUEST_TO_EDGE_SESSION_MSG:
            case EDGE_SYNC_RESPONSE_FROM_EDGE_SESSION_MSG:
                onToEdgeSessionMsg((EdgeSessionMsg) msg);
                break;
            case SESSION_TIMEOUT_MSG:
                ctx.broadcastToChildrenByType(msg, EntityType.TENANT);
                break;
            default:
                return false;
        }
        return true;
    }

    private void initTenantActors() {
        log.info("Starting main system actor.");
        try {
            if (systemContext.isTenantComponentsInitEnabled()) {
                PageDataIterable<Tenant> tenantIterator = new PageDataIterable<>(tenantService::findTenants, ENTITY_PACK_LIMIT);
                for (Tenant tenant : tenantIterator) {
                    log.debug("[{}] Creating tenant actor", tenant.getId());
                    getOrCreateTenantActor(tenant.getId()).ifPresentOrElse(tenantActor -> {
                        log.debug("[{}] Tenant actor created.", tenant.getId());
                    }, () -> {
                        log.debug("[{}] Skipped actor creation", tenant.getId());
                    });
                }
            }
            log.info("Main system actor started.");
        } catch (Exception e) {
            log.warn("Unknown failure", e);
        }
    }

    private void onQueueToRuleEngineMsg(QueueToRuleEngineMsg msg) {
        if (TenantId.SYS_TENANT_ID.equals(msg.getTenantId())) {
            msg.getMsg().getCallback().onFailure(new RuleEngineException("Message has system tenant id!"));
        } else {
            getOrCreateTenantActor(msg.getTenantId()).ifPresentOrElse(actor -> {
                actor.tell(msg);
            }, () -> msg.getMsg().getCallback().onSuccess());
        }
    }

    private void onComponentLifecycleMsg(ComponentLifecycleMsg msg) {
        TbActorRef target = null;
        if (TenantId.SYS_TENANT_ID.equals(msg.getTenantId())) {
            if (!EntityType.TENANT_PROFILE.equals(msg.getEntityId().getEntityType())) {
                log.warn("Message has system tenant id: {}", msg);
            }
        } else {
            if (EntityType.TENANT.equals(msg.getEntityId().getEntityType())) {
                TenantId tenantId = TenantId.fromUUID(msg.getEntityId().getId());
                if (msg.getEvent() == ComponentLifecycleEvent.DELETED) {
                    log.info("[{}] Handling tenant deleted notification: {}", msg.getTenantId(), msg);
                    deletedTenants.add(tenantId);
                    ctx.stop(new TbEntityActorId(tenantId));
                    return;
                }
            }
            target = getOrCreateTenantActor(msg.getTenantId()).orElseGet(() -> {
                log.debug("Ignoring component lifecycle msg for tenant {} because it is not managed by this service", msg.getTenantId());
                return null;
            });
        }
        if (target != null) {
            target.tellWithHighPriority(msg);
        } else {
            log.debug("[{}] Invalid component lifecycle msg: {}", msg.getTenantId(), msg);
        }
    }

    private void onToDeviceActorMsg(TenantAwareMsg msg, boolean priority) {
        getOrCreateTenantActor(msg.getTenantId()).ifPresentOrElse(tenantActor -> {
            if (priority) {
                tenantActor.tellWithHighPriority(msg);
            } else {
                tenantActor.tell(msg);
            }
        }, () -> {
            if (msg instanceof TransportToDeviceActorMsgWrapper) {
                ((TransportToDeviceActorMsgWrapper) msg).getCallback().onSuccess();
            }
        });
    }

    private Optional<TbActorRef> getOrCreateTenantActor(TenantId tenantId) {
        if (deletedTenants.contains(tenantId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(ctx.getOrCreateChildActor(new TbEntityActorId(tenantId),
                () -> DefaultActorService.TENANT_DISPATCHER_NAME,
                () -> new TenantActor.ActorCreator(systemContext, tenantId),
                () -> true));
    }

    private void onToEdgeSessionMsg(EdgeSessionMsg msg) {
        TbActorRef target = null;
        if (ModelConstants.SYSTEM_TENANT.equals(msg.getTenantId())) {
            log.warn("Message has system tenant id: {}", msg);
        } else {
            target = getOrCreateTenantActor(msg.getTenantId()).orElse(null);
        }
        if (target != null) {
            target.tellWithHighPriority(msg);
        } else {
            log.debug("[{}] Invalid edge session msg: {}", msg.getTenantId(), msg);
        }
    }

    @Override
    public ProcessFailureStrategy onProcessFailure(TbActorMsg msg, Throwable t) {
        log.error("Failed to process msg: {}", msg, t);
        return doProcessFailure(t);
    }

    public static class ActorCreator extends ContextBasedCreator {

        public ActorCreator(ActorSystemContext context) {
            super(context);
        }

        @Override
        public TbActorId createActorId() {
            return new TbEntityActorId(TenantId.SYS_TENANT_ID);
        }

        @Override
        public TbActor createActor() {
            return new AppActor(context);
        }
    }

}
