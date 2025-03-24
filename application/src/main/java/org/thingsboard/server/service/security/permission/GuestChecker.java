/**
 * Copyright © 2016-2024 The Sobeam Authors
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
package org.thingsboard.server.service.security.permission;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.role.RoleService;

import javax.swing.text.html.parser.Entity;
import java.util.*;

@Service
@Slf4j
public class GuestChecker {

    private final RoleService roleService;
    private Map<String, Map<String ,Set<String>>> permissionsMap = new HashMap<>();

    public GuestChecker(RoleService roleService) {
        this.roleService = roleService;
    }

    public boolean doCheck(User user, Resource resource, Operation operation , EntityId entityId) throws ThingsboardException {
        if (user.getAuthority().equals(Authority.SYS_ADMIN) || (resource.name().equals(Resource.CUSTOMER.name())) || resource.name().equals(Resource.USER.name()) ) {
            return true;
        }

        permissionsMap = roleService.findRolesByUserId(user);
        if(permissionsMap.isEmpty()) {
           return  true;
        }
        if (!hasResourceAccess(resource)) {
            permissionDenied();
        }

        if (!hasOperationAccess(resource, operation)) {
            permissionDenied();
        }

        if(entityId != null && !permissionsMap.isEmpty()){
            if (resource.name().equals("DASHBOARD")|| resource.name().equals("DEVICE")  || resource.name().equals("ASSET")){
                if (!hasEntityAccess(resource, entityId)) {
                  permissionDenied();
                }
            }
        }

        return true;
    }

    private boolean hasResourceAccess(Resource resource) {
        return permissionsMap.get("menuPermission").containsKey(resource.name());
    }

    private boolean hasOperationAccess(Resource resource, Operation operation) {
        Map<String, Set<String>> menuPermissions = permissionsMap.get("menuPermission");
        Set<String> allowedOperations = menuPermissions.get(resource.name());
        return allowedOperations != null && (allowedOperations.contains("all") || allowedOperations.contains(operation.name()));
    }

    private boolean hasEntityAccess(Resource resource, EntityId entityId) {

        String typePermission, typeTenant;
        switch (resource.name()) {
            case "DASHBOARD":
                typePermission = "dashboardPermission";
                typeTenant = "dashboardTenant";
                break;
            case "DEVICE":
                typePermission = "devicePermission";
                typeTenant = "deviceTenant";
                break;
            case "ASSET":
                typePermission = "assetPermission";
                typeTenant = "assetTenant";
                break;
            default:
                typePermission = "defaultPermission";
                typeTenant = "defaultTenant";
        }
        // Initialisation sé curisée des maps
        Map<String, Set<String>> listPermission =  permissionsMap.getOrDefault(typePermission, Collections.emptyMap());
        Map<String, Set<String>> listTenant = permissionsMap.getOrDefault(typeTenant, new HashMap<>());

        // Merge permissions without modifying the original map
        listPermission.forEach((key, value) ->
                listTenant.merge(key, value, (v1, v2) -> {
                    Set<String> merged = new HashSet<>(v1);
                    merged.addAll(v2);
                    return merged;
                })
        );

        // Correct access check
        return listTenant.containsKey(entityId.getId().toString());
    }

    private void permissionDenied() throws ThingsboardException {
        throw new ThingsboardException(ThingsboardErrorCode.PERMISSION_DENIED);
    }
}