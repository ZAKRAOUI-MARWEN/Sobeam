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
package org.sobeam.server.transport.lwm2m.server.rpc.composite;

import org.eclipse.leshan.core.ResponseCode;
import org.sobeam.server.common.transport.TransportService;
import org.sobeam.server.gen.transport.TransportProtos;
import org.sobeam.server.transport.lwm2m.server.client.LwM2mClient;
import org.sobeam.server.transport.lwm2m.server.downlink.DownlinkRequestCallback;
import org.sobeam.server.transport.lwm2m.server.downlink.composite.TbLwM2MCancelObserveCompositeRequest;
import org.sobeam.server.transport.lwm2m.server.rpc.LwM2MRpcResponseBody;
import org.sobeam.server.transport.lwm2m.server.rpc.RpcDownlinkRequestCallbackProxy;

public class RpcCancelObserveCompositeCallback extends RpcDownlinkRequestCallbackProxy<TbLwM2MCancelObserveCompositeRequest, Integer> {

    public RpcCancelObserveCompositeCallback(TransportService transportService, LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, DownlinkRequestCallback<TbLwM2MCancelObserveCompositeRequest, Integer> callback) {
        super(transportService, client, requestMsg, callback);
    }

    @Override
    protected void sendRpcReplyOnSuccess(Integer response) {
        reply(LwM2MRpcResponseBody.builder().result(ResponseCode.CONTENT.getName()).value(response.toString()).build());
    }
}
