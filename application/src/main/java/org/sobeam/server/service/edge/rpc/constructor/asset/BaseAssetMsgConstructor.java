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
package org.sobeam.server.service.edge.rpc.constructor.asset;

import org.sobeam.server.common.data.id.AssetId;
import org.sobeam.server.common.data.id.AssetProfileId;
import org.sobeam.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.sobeam.server.gen.edge.v1.AssetUpdateMsg;
import org.sobeam.server.gen.edge.v1.UpdateMsgType;

public abstract class BaseAssetMsgConstructor implements AssetMsgConstructor {

    @Override
    public AssetUpdateMsg constructAssetDeleteMsg(AssetId assetId) {
        return AssetUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(assetId.getId().getMostSignificantBits())
                .setIdLSB(assetId.getId().getLeastSignificantBits()).build();
    }

    @Override
    public AssetProfileUpdateMsg constructAssetProfileDeleteMsg(AssetProfileId assetProfileId) {
        return AssetProfileUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(assetProfileId.getId().getMostSignificantBits())
                .setIdLSB(assetProfileId.getId().getLeastSignificantBits()).build();
    }
}
