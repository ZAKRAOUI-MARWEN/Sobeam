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

import org.sobeam.server.common.data.asset.Asset;
import org.sobeam.server.common.data.asset.AssetProfile;
import org.sobeam.server.common.data.id.AssetId;
import org.sobeam.server.common.data.id.AssetProfileId;
import org.sobeam.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.sobeam.server.gen.edge.v1.AssetUpdateMsg;
import org.sobeam.server.gen.edge.v1.UpdateMsgType;
import org.sobeam.server.service.edge.rpc.constructor.MsgConstructor;

public interface AssetMsgConstructor extends MsgConstructor {

    AssetUpdateMsg constructAssetUpdatedMsg(UpdateMsgType msgType, Asset asset);

    AssetUpdateMsg constructAssetDeleteMsg(AssetId assetId);

    AssetProfileUpdateMsg constructAssetProfileUpdatedMsg(UpdateMsgType msgType, AssetProfile assetProfile);

    AssetProfileUpdateMsg constructAssetProfileDeleteMsg(AssetProfileId assetProfileId);
}
