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
package org.sobeam.server.service.rpc;

import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import org.sobeam.server.common.msg.rpc.ToDeviceRpcRequest;
import org.sobeam.server.service.security.model.SecurityUser;

/**
 * Created by ashvayka on 16.04.18.
 */
@Data
public class LocalRequestMetaData {
    private final ToDeviceRpcRequest request;
    private final SecurityUser user;
    private final DeferredResult<ResponseEntity> responseWriter;
}