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
import lombok.NoArgsConstructor;
import org.sobeam.server.common.data.id.OAuth2ParamsId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.oauth2.OAuth2Params;
import org.sobeam.server.dao.model.BaseSqlEntity;
import org.sobeam.server.dao.model.ModelConstants;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.OAUTH2_PARAMS_TABLE_NAME)
@NoArgsConstructor
public class OAuth2ParamsEntity extends BaseSqlEntity<OAuth2Params> {

    @Column(name = ModelConstants.OAUTH2_PARAMS_ENABLED_PROPERTY)
    private Boolean enabled;

    @Column(name = ModelConstants.OAUTH2_PARAMS_EDGE_ENABLED_PROPERTY)
    private Boolean edgeEnabled;

    @Column(name = ModelConstants.OAUTH2_PARAMS_TENANT_ID_PROPERTY)
    private UUID tenantId;

    public OAuth2ParamsEntity(OAuth2Params oauth2Params) {
        if (oauth2Params.getId() != null) {
            this.setUuid(oauth2Params.getUuidId());
        }
        this.setCreatedTime(oauth2Params.getCreatedTime());
        this.enabled = oauth2Params.isEnabled();
        this.edgeEnabled = oauth2Params.isEdgeEnabled();
        if (oauth2Params.getTenantId() != null) {
            this.tenantId = oauth2Params.getTenantId().getId();
        }
    }

    @Override
    public OAuth2Params toData() {
        OAuth2Params oauth2Params = new OAuth2Params();
        oauth2Params.setId(new OAuth2ParamsId(id));
        oauth2Params.setCreatedTime(createdTime);
        oauth2Params.setTenantId(TenantId.fromUUID(tenantId));
        oauth2Params.setEnabled(enabled);
        oauth2Params.setEdgeEnabled(edgeEnabled);
        return oauth2Params;
    }
}
