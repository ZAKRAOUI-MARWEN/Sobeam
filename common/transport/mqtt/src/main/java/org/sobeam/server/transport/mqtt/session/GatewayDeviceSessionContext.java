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
package org.sobeam.server.transport.mqtt.session;

import org.sobeam.server.common.data.DeviceProfile;
import org.sobeam.server.common.transport.TransportService;
import org.sobeam.server.common.transport.auth.TransportDeviceInfo;

import java.util.concurrent.ConcurrentMap;

/**
 * Created by nickAS21 on 26.12.22
 */
public class GatewayDeviceSessionContext extends AbstractGatewayDeviceSessionContext<GatewaySessionHandler> {

    public GatewayDeviceSessionContext(GatewaySessionHandler parent,
                                       TransportDeviceInfo deviceInfo,
                                       DeviceProfile deviceProfile,
                                       ConcurrentMap<MqttTopicMatcher, Integer> mqttQoSMap,
                                       TransportService transportService) {
        super(parent, deviceInfo, deviceProfile, mqttQoSMap, transportService);
    }

}
