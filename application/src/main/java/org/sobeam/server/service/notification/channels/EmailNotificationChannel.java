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
package org.sobeam.server.service.notification.channels;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.sobeam.rule.engine.api.MailService;
import org.sobeam.rule.engine.api.TbEmail;
import org.sobeam.server.common.data.User;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.notification.NotificationDeliveryMethod;
import org.sobeam.server.common.data.notification.template.EmailDeliveryMethodNotificationTemplate;
import org.sobeam.server.service.notification.NotificationProcessingContext;

@Component
@RequiredArgsConstructor
public class EmailNotificationChannel implements NotificationChannel<User, EmailDeliveryMethodNotificationTemplate> {

    private final MailService mailService;

    @Override
    public void sendNotification(User recipient, EmailDeliveryMethodNotificationTemplate processedTemplate, NotificationProcessingContext ctx) throws Exception {
        mailService.send(ctx.getTenantId(), null, TbEmail.builder()
                .to(recipient.getEmail())
                .subject(processedTemplate.getSubject())
                .body(processedTemplate.getBody())
                .html(true)
                .build());
    }

    @Override
    public void check(TenantId tenantId) throws Exception {
        if (!mailService.isConfigured(tenantId)) {
            throw new RuntimeException("Mail server is not configured");
        }
    }

    @Override
    public NotificationDeliveryMethod getDeliveryMethod() {
        return NotificationDeliveryMethod.EMAIL;
    }

}
