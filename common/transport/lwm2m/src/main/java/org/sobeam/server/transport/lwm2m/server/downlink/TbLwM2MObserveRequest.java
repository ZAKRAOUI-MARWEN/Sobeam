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
package org.sobeam.server.transport.lwm2m.server.downlink;

import lombok.Builder;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.sobeam.server.transport.lwm2m.server.LwM2MOperationType;

import java.util.Optional;

public class TbLwM2MObserveRequest extends AbstractTbLwM2MTargetedDownlinkRequest<ObserveResponse> implements HasContentFormat {

    private final Optional<ContentFormat> requestContentFormat;

    @Builder
    private TbLwM2MObserveRequest(String versionedId, long timeout, ContentFormat requestContentFormat) {
        super(versionedId, timeout);
        this.requestContentFormat = Optional.ofNullable(requestContentFormat);
    }

    @Override
    public LwM2MOperationType getType() {
        return LwM2MOperationType.OBSERVE;
    }


    @Override
    public Optional<ContentFormat> getRequestContentFormat() {
        return this.requestContentFormat;
    }
}
