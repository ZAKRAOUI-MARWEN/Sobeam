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
package org.sobeam.server.service.script;

import com.google.protobuf.util.JsonFormat;
import org.sobeam.server.gen.js.JsInvokeProtos;
import org.sobeam.server.queue.TbQueueMsg;
import org.sobeam.server.queue.common.TbProtoQueueMsg;
import org.sobeam.server.queue.kafka.TbKafkaDecoder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Created by ashvayka on 25.09.18.
 */
public class RemoteJsResponseDecoder implements TbKafkaDecoder<TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> {

    @Override
    public TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse> decode(TbQueueMsg msg) throws IOException {
        JsInvokeProtos.RemoteJsResponse.Builder builder = JsInvokeProtos.RemoteJsResponse.newBuilder();
        JsonFormat.parser().ignoringUnknownFields().merge(new String(msg.getData(), StandardCharsets.UTF_8), builder);
        return new TbProtoQueueMsg<>(msg.getKey(), builder.build(), msg.getHeaders());
    }
}
