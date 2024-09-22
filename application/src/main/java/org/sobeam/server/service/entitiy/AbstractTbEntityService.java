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
package org.sobeam.server.service.entitiy;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.sobeam.server.cluster.TbClusterService;
import org.sobeam.server.common.data.EntityType;
import org.sobeam.server.common.data.User;
import org.sobeam.server.common.data.exception.SobeamErrorCode;
import org.sobeam.server.common.data.exception.SobeamException;
import org.sobeam.server.common.data.id.EntityId;
import org.sobeam.server.common.data.id.EntityIdFactory;
import org.sobeam.server.dao.alarm.AlarmService;
import org.sobeam.server.dao.customer.CustomerService;
import org.sobeam.server.dao.edge.EdgeService;
import org.sobeam.server.dao.model.ModelConstants;
import org.sobeam.server.service.executors.DbCallbackExecutorService;
import org.sobeam.server.service.sync.vc.EntitiesVersionControlService;
import org.sobeam.server.service.telemetry.AlarmSubscriptionService;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
public abstract class AbstractTbEntityService {

    @Autowired
    private Environment env;

    @Value("${server.log_controller_error_stack_trace}")
    @Getter
    private boolean logControllerErrorStackTrace;

    @Autowired
    protected DbCallbackExecutorService dbExecutor;
    @Autowired(required = false)
    protected TbLogEntityActionService logEntityActionService;
    @Autowired(required = false)
    protected EdgeService edgeService;
    @Autowired
    protected AlarmService alarmService;
    @Autowired
    @Lazy
    protected AlarmSubscriptionService alarmSubscriptionService;
    @Autowired
    protected CustomerService customerService;
    @Autowired
    protected TbClusterService tbClusterService;
    @Autowired(required = false)
    @Lazy
    private EntitiesVersionControlService vcService;

    protected boolean isTestProfile() {
        return Set.of(this.env.getActiveProfiles()).contains("test");
    }

    protected <T> T checkNotNull(T reference) throws SobeamException {
        return checkNotNull(reference, "Requested item wasn't found!");
    }

    protected <T> T checkNotNull(T reference, String notFoundMessage) throws SobeamException {
        if (reference == null) {
            throw new SobeamException(notFoundMessage, SobeamErrorCode.ITEM_NOT_FOUND);
        }
        return reference;
    }

    protected <T> T checkNotNull(Optional<T> reference) throws SobeamException {
        return checkNotNull(reference, "Requested item wasn't found!");
    }

    protected <T> T checkNotNull(Optional<T> reference, String notFoundMessage) throws SobeamException {
        if (reference.isPresent()) {
            return reference.get();
        } else {
            throw new SobeamException(notFoundMessage, SobeamErrorCode.ITEM_NOT_FOUND);
        }
    }

    protected <I extends EntityId> I emptyId(EntityType entityType) {
        return (I) EntityIdFactory.getByTypeAndUuid(entityType, ModelConstants.NULL_UUID);
    }

    protected ListenableFuture<UUID> autoCommit(User user, EntityId entityId) throws Exception {
        if (vcService != null) {
            return vcService.autoCommit(user, entityId);
        } else {
            // We do not support auto-commit for rule engine
            return Futures.immediateFailedFuture(new RuntimeException("Operation not supported!"));
        }
    }

    protected ListenableFuture<UUID> autoCommit(User user, EntityType entityType, List<UUID> entityIds) throws Exception {
        if (vcService != null) {
            return vcService.autoCommit(user, entityType, entityIds);
        } else {
            // We do not support auto-commit for rule engine
            return Futures.immediateFailedFuture(new RuntimeException("Operation not supported!"));
        }
    }
}
