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
package org.sobeam.server.common.data.device;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.sobeam.server.common.data.EntityType;
import org.sobeam.server.common.data.relation.EntityRelation;
import org.sobeam.server.common.data.relation.EntityRelationsQuery;
import org.sobeam.server.common.data.relation.RelationEntityTypeFilter;
import org.sobeam.server.common.data.relation.RelationsSearchParameters;

import java.util.Collections;
import java.util.List;

@Schema
@Data
public class DeviceSearchQuery {

    @Schema(description = "Main search parameters.")
    private RelationsSearchParameters parameters;
    @Schema(description = "Type of the relation between root entity and device (e.g. 'Contains' or 'Manages').")
    private String relationType;
    @Schema(description = "Array of device types to filter the related entities (e.g. 'Temperature Sensor', 'Smoke Sensor').")
    private List<String> deviceTypes;

    public EntityRelationsQuery toEntitySearchQuery() {
        EntityRelationsQuery query = new EntityRelationsQuery();
        query.setParameters(parameters);
        query.setFilters(
                Collections.singletonList(new RelationEntityTypeFilter(relationType == null ? EntityRelation.CONTAINS_TYPE : relationType,
                        Collections.singletonList(EntityType.DEVICE))));
        return query;
    }
}
