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
package org.sobeam.server.common.data.device.profile.lwm2m.bootstrap;

import org.sobeam.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode;

public class PSKLwM2MBootstrapServerCredential extends AbstractLwM2MBootstrapServerCredential {

    private static final long serialVersionUID = -1639587501559199887L;

    @Override
    public LwM2MSecurityMode getSecurityMode() {
        return LwM2MSecurityMode.PSK;
    }
}
