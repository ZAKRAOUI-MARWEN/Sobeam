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
package org.sobeam.server.service.security.system;

import org.springframework.security.core.AuthenticationException;
import org.sobeam.server.common.data.User;
import org.sobeam.server.common.data.audit.ActionType;
import org.sobeam.server.common.data.id.CustomerId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.security.UserCredentials;
import org.sobeam.server.common.data.security.model.SecuritySettings;
import org.sobeam.server.common.data.security.model.UserPasswordPolicy;
import org.sobeam.server.common.data.security.model.mfa.PlatformTwoFaSettings;
import org.sobeam.server.dao.exception.DataValidationException;
import org.sobeam.server.service.security.model.SecurityUser;

import jakarta.servlet.http.HttpServletRequest;

public interface SystemSecurityService {

    SecuritySettings getSecuritySettings();

    SecuritySettings saveSecuritySettings(SecuritySettings securitySettings);

    void validatePasswordByPolicy(String password, UserPasswordPolicy passwordPolicy);

    void validateUserCredentials(TenantId tenantId, UserCredentials userCredentials, String username, String password) throws AuthenticationException;

    void validateTwoFaVerification(SecurityUser securityUser, boolean verificationSuccess, PlatformTwoFaSettings twoFaSettings);

    void validatePassword(String password, UserCredentials userCredentials) throws DataValidationException;

    String getBaseUrl(TenantId tenantId, CustomerId customerId, HttpServletRequest httpServletRequest);

    void logLoginAction(User user, Object authenticationDetails, ActionType actionType, Exception e);

    void logLoginAction(User user, Object authenticationDetails, ActionType actionType, String provider, Exception e);
}
