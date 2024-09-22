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
package org.sobeam.rule.engine.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.mail.javamail.JavaMailSender;
import org.sobeam.server.common.data.ApiFeature;
import org.sobeam.server.common.data.ApiUsageRecordState;
import org.sobeam.server.common.data.ApiUsageStateValue;
import org.sobeam.server.common.data.exception.SobeamException;
import org.sobeam.server.common.data.id.CustomerId;
import org.sobeam.server.common.data.id.TenantId;

public interface MailService {

    void updateMailConfiguration();

    void sendEmail(TenantId tenantId, String email, String subject, String message) throws SobeamException;

    void sendTestMail(JsonNode config, String email) throws SobeamException;

    void sendActivationEmail(String activationLink, String email) throws SobeamException;

    void sendAccountActivatedEmail(String loginLink, String email) throws SobeamException;

    void sendResetPasswordEmail(String passwordResetLink, String email) throws SobeamException;

    void sendResetPasswordEmailAsync(String passwordResetLink, String email);

    void sendPasswordWasResetEmail(String loginLink, String email) throws SobeamException;

    void sendAccountLockoutEmail(String lockoutEmail, String email, Integer maxFailedLoginAttempts) throws SobeamException;

    void sendTwoFaVerificationEmail(String email, String verificationCode, int expirationTimeSeconds) throws SobeamException;

    void send(TenantId tenantId, CustomerId customerId, TbEmail tbEmail) throws SobeamException;

    void send(TenantId tenantId, CustomerId customerId, TbEmail tbEmail, JavaMailSender javaMailSender, long timeout) throws SobeamException;

    void sendApiFeatureStateEmail(ApiFeature apiFeature, ApiUsageStateValue stateValue, String email, ApiUsageRecordState recordState) throws SobeamException;

    void testConnection(TenantId tenantId) throws Exception;

    boolean isConfigured(TenantId tenantId);

}
