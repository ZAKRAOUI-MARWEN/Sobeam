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
package org.sobeam.server.common.msg.edge;

import lombok.Data;
import org.sobeam.server.common.data.id.EdgeId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.msg.MsgType;

import java.util.UUID;

@Data
public class FromEdgeSyncResponse implements EdgeSessionMsg {

    private static final long serialVersionUID = -6360890886315347486L;

    private final UUID id;
    private final TenantId tenantId;
    private final EdgeId edgeId;
    private final boolean success;

    @Override
    public MsgType getMsgType() {
        return MsgType.EDGE_SYNC_RESPONSE_FROM_EDGE_SESSION_MSG;
    }
}
