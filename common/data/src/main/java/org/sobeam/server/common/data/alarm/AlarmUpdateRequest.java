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
package org.sobeam.server.common.data.alarm;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import org.sobeam.server.common.data.id.AlarmId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.id.UserId;
import org.sobeam.server.common.data.validation.NoXss;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
public class AlarmUpdateRequest implements AlarmModificationRequest {

    @NotNull
    @Schema(description = "JSON object with Tenant Id", accessMode = Schema.AccessMode.READ_ONLY)
    private TenantId tenantId;
    @NotNull
    @Schema(description = "JSON object with the alarm Id. " +
            "Specify this field to update the alarm. " +
            "Referencing non-existing alarm Id will cause error. " +
            "Omit this field to create new alarm.")
    private AlarmId alarmId;
    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Alarm severity", example = "CRITICAL")
    private AlarmSeverity severity;
    @Schema(description = "Timestamp of the alarm start time, in milliseconds", example = "1634058704565")
    private long startTs;
    @Schema(description = "Timestamp of the alarm end time(last time update), in milliseconds", example = "1634111163522")
    private long endTs;
    @NoXss
    @Schema(description = "JSON object with alarm details")
    private JsonNode details;
    @Valid
    @Schema(description = "JSON object with propagation details")
    private AlarmPropagationInfo propagation;

    private UserId userId;

    public static AlarmUpdateRequest fromAlarm(Alarm a) {
        return fromAlarm(a, null);
    }

    public static AlarmUpdateRequest fromAlarm(Alarm a, UserId userId) {
        return AlarmUpdateRequest.builder()
                .tenantId(a.getTenantId())
                .alarmId(a.getId())
                .severity((a.getSeverity()))
                .startTs(a.getStartTs())
                .endTs(a.getEndTs())
                .details(a.getDetails())
                .propagation(AlarmPropagationInfo.builder()
                        .propagate(a.isPropagate())
                        .propagateToOwner(a.isPropagateToOwner())
                        .propagateToTenant(a.isPropagateToTenant())
                        .propagateRelationTypes(a.getPropagateRelationTypes()).build())
                .userId(userId)
                .build();
    }
}
