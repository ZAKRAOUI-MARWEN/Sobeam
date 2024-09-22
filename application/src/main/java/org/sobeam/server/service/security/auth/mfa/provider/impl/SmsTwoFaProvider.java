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
package org.sobeam.server.service.security.auth.mfa.provider.impl;

import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.sobeam.rule.engine.api.SmsService;
import org.sobeam.rule.engine.api.util.TbNodeUtils;
import org.sobeam.server.common.data.User;
import org.sobeam.server.common.data.audit.ActionType;
import org.sobeam.server.common.data.exception.SobeamErrorCode;
import org.sobeam.server.common.data.exception.SobeamException;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.security.model.mfa.account.SmsTwoFaAccountConfig;
import org.sobeam.server.common.data.security.model.mfa.provider.SmsTwoFaProviderConfig;
import org.sobeam.server.common.data.security.model.mfa.provider.TwoFaProviderType;
import org.sobeam.server.dao.audit.AuditLogService;
import org.sobeam.server.queue.util.TbCoreComponent;
import org.sobeam.server.service.security.model.SecurityUser;

import java.util.Map;

@Service
@TbCoreComponent
public class SmsTwoFaProvider extends OtpBasedTwoFaProvider<SmsTwoFaProviderConfig, SmsTwoFaAccountConfig> {

    private final SmsService smsService;
    private final AuditLogService auditLogService;

    public SmsTwoFaProvider(CacheManager cacheManager, SmsService smsService, AuditLogService auditLogService) {
        super(cacheManager);
        this.smsService = smsService;
        this.auditLogService = auditLogService;
    }


    @Override
    public SmsTwoFaAccountConfig generateNewAccountConfig(User user, SmsTwoFaProviderConfig providerConfig) {
        return new SmsTwoFaAccountConfig();
    }

    @Override
    protected void sendVerificationCode(SecurityUser user, String verificationCode, SmsTwoFaProviderConfig providerConfig, SmsTwoFaAccountConfig accountConfig) throws SobeamException {
        Map<String, String> messageData = Map.of(
                "code", verificationCode,
                "userEmail", user.getEmail()
        );
        String message = TbNodeUtils.processTemplate(providerConfig.getSmsVerificationMessageTemplate(), messageData);
        String phoneNumber = accountConfig.getPhoneNumber();
        try {
            smsService.sendSms(user.getTenantId(), user.getCustomerId(), new String[]{phoneNumber}, message);
            auditLogService.logEntityAction(user.getTenantId(), user.getCustomerId(), user.getId(), user.getName(), user.getId(), user, ActionType.SMS_SENT, null, phoneNumber);
        } catch (SobeamException e) {
            auditLogService.logEntityAction(user.getTenantId(), user.getCustomerId(), user.getId(), user.getName(), user.getId(), user, ActionType.SMS_SENT, e, phoneNumber);
            throw e;
        }
    }

    @Override
    public void check(TenantId tenantId) throws SobeamException {
        if (!smsService.isConfigured(tenantId)) {
            throw new SobeamException("SMS service is not configured", SobeamErrorCode.BAD_REQUEST_PARAMS);
        }
    }


    @Override
    public TwoFaProviderType getType() {
        return TwoFaProviderType.SMS;
    }

}
