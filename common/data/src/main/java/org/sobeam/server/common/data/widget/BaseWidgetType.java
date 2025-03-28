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
package org.sobeam.server.common.data.widget;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.sobeam.server.common.data.BaseData;
import org.sobeam.server.common.data.HasName;
import org.sobeam.server.common.data.HasTenantId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.id.WidgetTypeId;
import org.sobeam.server.common.data.validation.Length;
import org.sobeam.server.common.data.validation.NoXss;

@Data
public class BaseWidgetType extends BaseData<WidgetTypeId> implements HasName, HasTenantId {

    private static final long serialVersionUID = 8388684344603660756L;

    @Schema(description = "JSON object with Tenant Id.", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;
    @NoXss
    @Length(fieldName = "fqn")
    @Schema(description = "Unique FQN that is used in dashboards as a reference widget type", accessMode = Schema.AccessMode.READ_ONLY)
    private String fqn;
    @NoXss
    @Length(fieldName = "name")
    @Schema(description = "Widget name used in search and UI", accessMode = Schema.AccessMode.READ_ONLY)
    private String name;

    @Schema(description = "Whether widget type is deprecated.", example = "true")
    private boolean deprecated;

    public BaseWidgetType() {
        super();
    }

    public BaseWidgetType(WidgetTypeId id) {
        super(id);
    }

    public BaseWidgetType(BaseWidgetType widgetType) {
        super(widgetType);
        this.tenantId = widgetType.getTenantId();
        this.fqn = widgetType.getFqn();
        this.name = widgetType.getName();
        this.deprecated = widgetType.isDeprecated();
    }

    @Schema(description = "JSON object with the Widget Type Id. " +
            "Specify this field to update the Widget Type. " +
            "Referencing non-existing Widget Type Id will cause error. " +
            "Omit this field to create new Widget Type." )
    @Override
    public WidgetTypeId getId() {
        return super.getId();
    }

    @Schema(description = "Timestamp of the Widget Type creation, in milliseconds", example = "1609459200000", accessMode = Schema.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }
}
