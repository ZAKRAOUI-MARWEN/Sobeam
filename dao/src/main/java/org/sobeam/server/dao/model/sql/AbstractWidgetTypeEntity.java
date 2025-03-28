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
package org.sobeam.server.dao.model.sql;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.id.WidgetTypeId;
import org.sobeam.server.common.data.widget.BaseWidgetType;
import org.sobeam.server.dao.model.BaseEntity;
import org.sobeam.server.dao.model.BaseSqlEntity;
import org.sobeam.server.dao.model.ModelConstants;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class AbstractWidgetTypeEntity<T extends BaseWidgetType> extends BaseSqlEntity<T> {

    @Column(name = ModelConstants.WIDGET_TYPE_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.WIDGET_TYPE_FQN_PROPERTY)
    private String fqn;

    @Column(name = ModelConstants.WIDGET_TYPE_NAME_PROPERTY)
    private String name;

    @Column(name = ModelConstants.WIDGET_TYPE_DEPRECATED_PROPERTY)
    private boolean deprecated;

    public AbstractWidgetTypeEntity() {
        super();
    }

    public AbstractWidgetTypeEntity(BaseWidgetType widgetType) {
        if (widgetType.getId() != null) {
            this.setUuid(widgetType.getId().getId());
        }
        this.setCreatedTime(widgetType.getCreatedTime());
        if (widgetType.getTenantId() != null) {
            this.tenantId = widgetType.getTenantId().getId();
        }
        this.fqn = widgetType.getFqn();
        this.name = widgetType.getName();
        this.deprecated = widgetType.isDeprecated();
    }

    public AbstractWidgetTypeEntity(AbstractWidgetTypeEntity widgetTypeEntity) {
        this.setId(widgetTypeEntity.getId());
        this.setCreatedTime(widgetTypeEntity.getCreatedTime());
        this.tenantId = widgetTypeEntity.getTenantId();
        this.fqn = widgetTypeEntity.getFqn();
        this.name = widgetTypeEntity.getName();
        this.deprecated = widgetTypeEntity.isDeprecated();
    }

    protected BaseWidgetType toBaseWidgetType() {
        BaseWidgetType widgetType = new BaseWidgetType(new WidgetTypeId(getUuid()));
        widgetType.setCreatedTime(createdTime);
        if (tenantId != null) {
            widgetType.setTenantId(TenantId.fromUUID(tenantId));
        }
        widgetType.setFqn(fqn);
        widgetType.setName(name);
        widgetType.setDeprecated(deprecated);
        return widgetType;
    }

}
