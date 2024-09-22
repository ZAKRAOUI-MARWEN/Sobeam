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
package org.sobeam.server.service.security.auth.mfa;

import org.sobeam.server.common.data.User;
import org.sobeam.server.common.data.exception.SobeamException;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.id.UserId;
import org.sobeam.server.common.data.security.model.mfa.account.TwoFaAccountConfig;
import org.sobeam.server.common.data.security.model.mfa.provider.TwoFaProviderType;
import org.sobeam.server.service.security.model.SecurityUser;

public interface TwoFactorAuthService {

    boolean isTwoFaEnabled(TenantId tenantId, UserId userId);

    void checkProvider(TenantId tenantId, TwoFaProviderType providerType) throws SobeamException;


    void prepareVerificationCode(SecurityUser user, TwoFaProviderType providerType, boolean checkLimits) throws Exception;

    void prepareVerificationCode(SecurityUser user, TwoFaAccountConfig accountConfig, boolean checkLimits) throws SobeamException;


    boolean checkVerificationCode(SecurityUser user, TwoFaProviderType providerType, String verificationCode, boolean checkLimits) throws SobeamException;

    boolean checkVerificationCode(SecurityUser user, String verificationCode, TwoFaAccountConfig accountConfig, boolean checkLimits) throws SobeamException;


    TwoFaAccountConfig generateNewAccountConfig(User user, TwoFaProviderType providerType) throws SobeamException;

}
