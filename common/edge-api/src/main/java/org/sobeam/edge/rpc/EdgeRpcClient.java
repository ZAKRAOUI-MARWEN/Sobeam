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
package org.sobeam.edge.rpc;

import org.sobeam.server.gen.edge.v1.DownlinkMsg;
import org.sobeam.server.gen.edge.v1.DownlinkResponseMsg;
import org.sobeam.server.gen.edge.v1.EdgeConfiguration;
import org.sobeam.server.gen.edge.v1.UplinkMsg;
import org.sobeam.server.gen.edge.v1.UplinkResponseMsg;

import java.util.function.Consumer;

public interface EdgeRpcClient {

    void connect(String integrationKey,
                 String integrationSecret,
                 Consumer<UplinkResponseMsg> onUplinkResponse,
                 Consumer<EdgeConfiguration> onEdgeUpdate,
                 Consumer<DownlinkMsg> onDownlink,
                 Consumer<Exception> onError);

    void disconnect(boolean onError) throws InterruptedException;

    void sendSyncRequestMsg(boolean fullSyncRequired);

    void sendUplinkMsg(UplinkMsg uplinkMsg);

    void sendDownlinkResponseMsg(DownlinkResponseMsg downlinkResponseMsg);

    int getServerMaxInboundMessageSize();
}
