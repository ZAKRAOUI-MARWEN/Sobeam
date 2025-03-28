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

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.sobeam.server.common.data.edge.Edge;
import org.sobeam.server.common.data.id.CustomerId;
import org.sobeam.server.common.data.id.EdgeId;
import org.sobeam.server.common.data.id.RuleChainId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.dao.model.BaseSqlEntity;
import org.sobeam.server.dao.model.ModelConstants;
import org.sobeam.server.dao.util.mapping.JsonConverter;

import java.util.UUID;

import static org.sobeam.server.dao.model.ModelConstants.EDGE_CUSTOMER_ID_PROPERTY;
import static org.sobeam.server.dao.model.ModelConstants.EDGE_LABEL_PROPERTY;
import static org.sobeam.server.dao.model.ModelConstants.EDGE_NAME_PROPERTY;
import static org.sobeam.server.dao.model.ModelConstants.EDGE_ROOT_RULE_CHAIN_ID_PROPERTY;
import static org.sobeam.server.dao.model.ModelConstants.EDGE_ROUTING_KEY_PROPERTY;
import static org.sobeam.server.dao.model.ModelConstants.EDGE_SECRET_PROPERTY;
import static org.sobeam.server.dao.model.ModelConstants.EDGE_TENANT_ID_PROPERTY;
import static org.sobeam.server.dao.model.ModelConstants.EDGE_TYPE_PROPERTY;

@Data
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class AbstractEdgeEntity<T extends Edge> extends BaseSqlEntity<T> {

    @Column(name = EDGE_TENANT_ID_PROPERTY, columnDefinition = "uuid")
    private UUID tenantId;

    @Column(name = EDGE_CUSTOMER_ID_PROPERTY, columnDefinition = "uuid")
    private UUID customerId;

    @Column(name = EDGE_ROOT_RULE_CHAIN_ID_PROPERTY, columnDefinition = "uuid")
    private UUID rootRuleChainId;

    @Column(name = EDGE_TYPE_PROPERTY)
    private String type;

    @Column(name = EDGE_NAME_PROPERTY)
    private String name;

    @Column(name = EDGE_LABEL_PROPERTY)
    private String label;

    @Column(name = EDGE_ROUTING_KEY_PROPERTY)
    private String routingKey;

    @Column(name = EDGE_SECRET_PROPERTY)
    private String secret;

    @Convert(converter = JsonConverter.class)
    @Column(name = ModelConstants.EDGE_ADDITIONAL_INFO_PROPERTY)
    private JsonNode additionalInfo;

    public AbstractEdgeEntity() {
        super();
    }

    public AbstractEdgeEntity(Edge edge) {
        if (edge.getId() != null) {
            this.setUuid(edge.getId().getId());
        }
        this.setCreatedTime(edge.getCreatedTime());
        if (edge.getTenantId() != null) {
            this.tenantId = edge.getTenantId().getId();
        }
        if (edge.getCustomerId() != null) {
            this.customerId = edge.getCustomerId().getId();
        }
        if (edge.getRootRuleChainId() != null) {
            this.rootRuleChainId = edge.getRootRuleChainId().getId();
        }
        this.type = edge.getType();
        this.name = edge.getName();
        this.label = edge.getLabel();
        this.routingKey = edge.getRoutingKey();
        this.secret = edge.getSecret();
        this.additionalInfo = edge.getAdditionalInfo();
    }

    public AbstractEdgeEntity(EdgeEntity edgeEntity) {
        this.setId(edgeEntity.getId());
        this.setCreatedTime(edgeEntity.getCreatedTime());
        this.tenantId = edgeEntity.getTenantId();
        this.customerId = edgeEntity.getCustomerId();
        this.rootRuleChainId = edgeEntity.getRootRuleChainId();
        this.type = edgeEntity.getType();
        this.name = edgeEntity.getName();
        this.label = edgeEntity.getLabel();
        this.routingKey = edgeEntity.getRoutingKey();
        this.secret = edgeEntity.getSecret();
        this.additionalInfo = edgeEntity.getAdditionalInfo();
    }

    protected Edge toEdge() {
        Edge edge = new Edge(new EdgeId(getUuid()));
        edge.setCreatedTime(createdTime);
        if (tenantId != null) {
            edge.setTenantId(TenantId.fromUUID(tenantId));
        }
        if (customerId != null) {
            edge.setCustomerId(new CustomerId(customerId));
        }
        if (rootRuleChainId != null) {
            edge.setRootRuleChainId(new RuleChainId(rootRuleChainId));
        }
        edge.setType(type);
        edge.setName(name);
        edge.setLabel(label);
        edge.setRoutingKey(routingKey);
        edge.setSecret(secret);
        edge.setAdditionalInfo(additionalInfo);
        return edge;
    }
}
