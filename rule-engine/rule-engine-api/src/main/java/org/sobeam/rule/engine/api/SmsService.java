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

import org.sobeam.server.common.data.exception.SobeamException;
import org.sobeam.server.common.data.id.CustomerId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.sms.config.TestSmsRequest;

public interface SmsService {

    void updateSmsConfiguration();

    void sendSms(TenantId tenantId, CustomerId customerId, String[] numbersTo, String message) throws SobeamException;;

    void sendTestSms(TestSmsRequest testSmsRequest) throws SobeamException;

    boolean isConfigured(TenantId tenantId);

}
