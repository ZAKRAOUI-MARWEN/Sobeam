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
import org.sobeam.server.common.data.id.OAuth2DomainId;
import org.sobeam.server.common.data.id.OAuth2ParamsId;
import org.sobeam.server.common.data.oauth2.OAuth2Domain;
import org.sobeam.server.common.data.oauth2.SchemeType;
import org.sobeam.server.dao.model.BaseSqlEntity;
import org.sobeam.server.dao.model.ModelConstants;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.OAUTH2_DOMAIN_TABLE_NAME)
public class OAuth2DomainEntity extends BaseSqlEntity<OAuth2Domain> {

    @Column(name = ModelConstants.OAUTH2_PARAMS_ID_PROPERTY)
    private UUID oauth2ParamsId;

    @Column(name = ModelConstants.OAUTH2_DOMAIN_NAME_PROPERTY)
    private String domainName;

    @Enumerated(EnumType.STRING)
    @Column(name = ModelConstants.OAUTH2_DOMAIN_SCHEME_PROPERTY)
    private SchemeType domainScheme;

    public OAuth2DomainEntity() {
        super();
    }

    public OAuth2DomainEntity(OAuth2Domain domain) {
        if (domain.getId() != null) {
            this.setUuid(domain.getId().getId());
        }
        this.setCreatedTime(domain.getCreatedTime());
        if (domain.getOauth2ParamsId() != null) {
            this.oauth2ParamsId = domain.getOauth2ParamsId().getId();
        }
        this.domainName = domain.getDomainName();
        this.domainScheme = domain.getDomainScheme();
    }

    @Override
    public OAuth2Domain toData() {
        OAuth2Domain domain = new OAuth2Domain();
        domain.setId(new OAuth2DomainId(id));
        domain.setCreatedTime(createdTime);
        domain.setOauth2ParamsId(new OAuth2ParamsId(oauth2ParamsId));
        domain.setDomainName(domainName);
        domain.setDomainScheme(domainScheme);
        return domain;
    }
}
