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
package org.sobeam.server.common.data.notification;

import lombok.Data;

import jakarta.validation.constraints.Max;

@Data
public class NotificationRequestConfig {

    @Max(value = MAX_SENDING_DELAY, message = "cannot be longer than 1 week")
    private int sendingDelayInSec;

    public static final int MAX_SENDING_DELAY = 604800;

}