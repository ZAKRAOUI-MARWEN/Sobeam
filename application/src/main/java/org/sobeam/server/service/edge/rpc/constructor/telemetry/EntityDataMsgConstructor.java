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
package org.sobeam.server.service.edge.rpc.constructor.telemetry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.sobeam.server.common.data.DataConstants;
import org.sobeam.server.common.data.StringUtils;
import org.sobeam.server.common.data.edge.EdgeEventActionType;
import org.sobeam.server.common.data.id.EntityId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.adaptor.JsonConverter;
import org.sobeam.server.gen.edge.v1.AttributeDeleteMsg;
import org.sobeam.server.gen.edge.v1.EntityDataProto;
import org.sobeam.server.gen.transport.TransportProtos;
import org.sobeam.server.queue.util.TbCoreComponent;

import java.util.List;

@Slf4j
@Component
@TbCoreComponent
public class EntityDataMsgConstructor {

    public EntityDataProto constructEntityDataMsg(TenantId tenantId, EntityId entityId, EdgeEventActionType actionType, JsonElement entityData) {
        EntityDataProto.Builder builder = EntityDataProto.newBuilder()
                .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                .setEntityIdLSB(entityId.getId().getLeastSignificantBits())
                .setEntityType(entityId.getEntityType().name());
        switch (actionType) {
            case TIMESERIES_UPDATED:
                try {
                    JsonObject data = entityData.getAsJsonObject();
                    long ts;
                    if (data.get("ts") != null && !data.get("ts").isJsonNull()) {
                        ts = data.getAsJsonPrimitive("ts").getAsLong();
                    } else {
                        ts = System.currentTimeMillis();
                    }
                    builder.setPostTelemetryMsg(JsonConverter.convertToTelemetryProto(data.getAsJsonObject("data"), ts));
                } catch (Exception e) {
                    log.warn("[{}][{}] Can't convert to telemetry proto, entityData [{}]", tenantId, entityId, entityData, e);
                }
                break;
            case ATTRIBUTES_UPDATED:
                try {
                    JsonObject data = entityData.getAsJsonObject();
                    TransportProtos.PostAttributeMsg attributesUpdatedMsg = JsonConverter.convertToAttributesProto(data.getAsJsonObject("kv"));
                    builder.setAttributesUpdatedMsg(attributesUpdatedMsg);
                    builder.setPostAttributeScope(getScopeOfDefault(data));
                } catch (Exception e) {
                    log.warn("[{}][{}] Can't convert to AttributesUpdatedMsg proto, entityData [{}]", tenantId, entityId, entityData, e);
                }
                break;
            case POST_ATTRIBUTES:
                try {
                    JsonObject data = entityData.getAsJsonObject();
                    TransportProtos.PostAttributeMsg postAttributesMsg = JsonConverter.convertToAttributesProto(data.getAsJsonObject("kv"));
                    builder.setPostAttributesMsg(postAttributesMsg);
                    builder.setPostAttributeScope(getScopeOfDefault(data));
                } catch (Exception e) {
                    log.warn("[{}][{}] Can't convert to PostAttributesMsg, entityData [{}]", tenantId, entityId, entityData, e);
                }
                break;
            case ATTRIBUTES_DELETED:
                try {
                    AttributeDeleteMsg.Builder attributeDeleteMsg = AttributeDeleteMsg.newBuilder();
                    attributeDeleteMsg.setScope(entityData.getAsJsonObject().getAsJsonPrimitive("scope").getAsString());
                    JsonArray jsonArray = entityData.getAsJsonObject().getAsJsonArray("keys");
                    List<String> keys = new Gson().fromJson(jsonArray.toString(), new TypeToken<>(){}.getType());
                    attributeDeleteMsg.addAllAttributeNames(keys);
                    attributeDeleteMsg.build();
                    builder.setAttributeDeleteMsg(attributeDeleteMsg);
                } catch (Exception e) {
                    log.warn("[{}][{}] Can't convert to AttributeDeleteMsg proto, entityData [{}]", tenantId, entityId, entityData, e);
                }
                break;
        }
        return builder.build();
    }

    private String getScopeOfDefault(JsonObject data) {
        JsonPrimitive scope = data.getAsJsonPrimitive("scope");
        String result = DataConstants.SERVER_SCOPE;
        if (scope != null && StringUtils.isNotBlank(scope.getAsString())) {
            result = scope.getAsString();
        }
        return result;
    }
}
