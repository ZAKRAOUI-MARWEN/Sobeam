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
package org.sobeam.server.service.entitiy.device.profile;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.sobeam.server.common.data.DeviceProfile;
import org.sobeam.server.common.data.EntityType;
import org.sobeam.server.common.data.User;
import org.sobeam.server.common.data.audit.ActionType;
import org.sobeam.server.common.data.exception.SobeamException;
import org.sobeam.server.common.data.id.DeviceProfileId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.dao.device.DeviceProfileService;
import org.sobeam.server.queue.util.TbCoreComponent;
import org.sobeam.server.service.entitiy.AbstractTbEntityService;

@Service
@TbCoreComponent
@AllArgsConstructor
@Slf4j
public class DefaultTbDeviceProfileService extends AbstractTbEntityService implements TbDeviceProfileService {

    private final DeviceProfileService deviceProfileService;

    @Override
    public DeviceProfile save(DeviceProfile deviceProfile, User user) throws Exception {
        ActionType actionType = deviceProfile.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        TenantId tenantId = deviceProfile.getTenantId();
        try {
            DeviceProfile savedDeviceProfile = checkNotNull(deviceProfileService.saveDeviceProfile(deviceProfile));
            autoCommit(user, savedDeviceProfile.getId());

            logEntityActionService.logEntityAction(tenantId, savedDeviceProfile.getId(), savedDeviceProfile,
                    null, actionType, user);
            return savedDeviceProfile;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.DEVICE_PROFILE), deviceProfile, actionType, user, e);
            throw e;
        }
    }

    @Override
    public void delete(DeviceProfile deviceProfile, User user) {
        ActionType actionType = ActionType.DELETED;
        DeviceProfileId deviceProfileId = deviceProfile.getId();
        TenantId tenantId = deviceProfile.getTenantId();
        try {
            deviceProfileService.deleteDeviceProfile(tenantId, deviceProfileId);

            logEntityActionService.logEntityAction(tenantId, deviceProfileId, deviceProfile, null,
                    actionType, user, deviceProfileId.toString());
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.DEVICE_PROFILE), actionType,
                    user, e, deviceProfileId.toString());
            throw e;
        }
    }

    @Override
    public DeviceProfile setDefaultDeviceProfile(DeviceProfile deviceProfile, DeviceProfile previousDefaultDeviceProfile, User user) throws SobeamException {
        TenantId tenantId = deviceProfile.getTenantId();
        DeviceProfileId deviceProfileId = deviceProfile.getId();
        try {
            if (deviceProfileService.setDefaultDeviceProfile(tenantId, deviceProfileId)) {
                if (previousDefaultDeviceProfile != null) {
                    previousDefaultDeviceProfile = deviceProfileService.findDeviceProfileById(tenantId, previousDefaultDeviceProfile.getId());
                    logEntityActionService.logEntityAction(tenantId, previousDefaultDeviceProfile.getId(), previousDefaultDeviceProfile,
                            ActionType.UPDATED, user);
                }
                deviceProfile = deviceProfileService.findDeviceProfileById(tenantId, deviceProfileId);

                logEntityActionService.logEntityAction(tenantId, deviceProfileId, deviceProfile, ActionType.UPDATED, user);
            }
            return deviceProfile;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(tenantId, emptyId(EntityType.DEVICE_PROFILE), ActionType.UPDATED,
                    user, e, deviceProfileId.toString());
            throw e;
        }
    }
}
