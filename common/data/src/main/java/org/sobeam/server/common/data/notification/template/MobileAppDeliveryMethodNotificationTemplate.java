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
package org.sobeam.server.common.data.notification.template;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.sobeam.server.common.data.notification.NotificationDeliveryMethod;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MobileAppDeliveryMethodNotificationTemplate extends DeliveryMethodNotificationTemplate implements HasSubject {

    @NotEmpty
    private String subject;
    private JsonNode additionalConfig;

    private final List<TemplatableValue> templatableValues = List.of(
            TemplatableValue.of(this::getBody, this::setBody),
            TemplatableValue.of(this::getSubject, this::setSubject)
    );

    public MobileAppDeliveryMethodNotificationTemplate(MobileAppDeliveryMethodNotificationTemplate other) {
        super(other);
        this.subject = other.subject;
        this.additionalConfig = other.additionalConfig;
    }

    @Override
    public NotificationDeliveryMethod getMethod() {
        return NotificationDeliveryMethod.MOBILE_APP;
    }

    @Override
    public MobileAppDeliveryMethodNotificationTemplate copy() {
        return new MobileAppDeliveryMethodNotificationTemplate(this);
    }

    @Override
    public List<TemplatableValue> getTemplatableValues() {
        return templatableValues;
    }

}