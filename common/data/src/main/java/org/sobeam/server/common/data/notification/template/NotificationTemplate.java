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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.sobeam.server.common.data.BaseData;
import org.sobeam.server.common.data.ExportableEntity;
import org.sobeam.server.common.data.HasName;
import org.sobeam.server.common.data.HasTenantId;
import org.sobeam.server.common.data.id.NotificationTemplateId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.notification.NotificationType;
import org.sobeam.server.common.data.validation.Length;
import org.sobeam.server.common.data.validation.NoXss;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationTemplate extends BaseData<NotificationTemplateId> implements HasTenantId, HasName, ExportableEntity<NotificationTemplateId> {

    private TenantId tenantId;
    @NoXss
    @NotEmpty
    @Length(max = 255, message = "cannot be longer than 255 chars")
    private String name;
    @NoXss
    @NotNull
    private NotificationType notificationType;
    @Valid
    @NotNull
    private NotificationTemplateConfig configuration;

    private NotificationTemplateId externalId;

    public NotificationTemplate() {
    }

    public NotificationTemplate(NotificationTemplate other) {
        super(other);
        this.tenantId = other.tenantId;
        this.name = other.name;
        this.notificationType = other.notificationType;
        this.configuration = other.configuration != null ? other.configuration.copy() : null;
        this.externalId = other.externalId;
    }

}
