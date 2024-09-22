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

import lombok.extern.slf4j.Slf4j;
import org.sobeam.common.util.JacksonUtil;
import org.sobeam.server.common.data.DeviceProfile;
import org.sobeam.server.common.data.exception.SobeamErrorCode;
import org.sobeam.server.common.data.exception.SobeamException;
import org.sobeam.server.common.transport.TransportService;
import org.sobeam.server.common.transport.auth.TransportDeviceInfo;
import org.sobeam.server.gen.transport.TransportProtos;
import org.sobeam.server.gen.transport.mqtt.SparkplugBProto;
import org.sobeam.server.transport.mqtt.util.sparkplug.SparkplugMessageType;
import org.sobeam.server.transport.mqtt.util.sparkplug.SparkplugRpcRequestHeader;
import org.sobeam.server.transport.mqtt.util.sparkplug.SparkplugTopic;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static org.sobeam.server.transport.mqtt.util.sparkplug.SparkplugMetricUtil.getTsKvProto;

@Slf4j
public class SparkplugDeviceSessionContext extends AbstractGatewayDeviceSessionContext<SparkplugNodeSessionHandler> {

    private final Map<String, SparkplugBProto.Payload.Metric> deviceBirthMetrics = new ConcurrentHashMap<>();

    public SparkplugDeviceSessionContext(SparkplugNodeSessionHandler parent,
                                         TransportDeviceInfo deviceInfo,
                                         DeviceProfile deviceProfile,
                                         ConcurrentMap<MqttTopicMatcher,
                                                 Integer> mqttQoSMap,
                                         TransportService transportService) {
        super(parent, deviceInfo, deviceProfile, mqttQoSMap, transportService);
    }

    public  Map<String, SparkplugBProto.Payload.Metric> getDeviceBirthMetrics() {
        return deviceBirthMetrics;
    }

    public void setDeviceBirthMetrics(java.util.List<org.sobeam.server.gen.transport.mqtt.SparkplugBProto.Payload.Metric> metrics) {
        this.deviceBirthMetrics.putAll(metrics.stream()
                .collect(Collectors.toMap(SparkplugBProto.Payload.Metric::getName, metric -> metric)));
    }


    @Override
    public void onAttributeUpdate(UUID sessionId, TransportProtos.AttributeUpdateNotificationMsg notification) {
        log.trace("[{}] Received attributes update notification to sparkplug device", sessionId);
        notification.getSharedUpdatedList().forEach(tsKvProto -> {
            if (getDeviceBirthMetrics().containsKey(tsKvProto.getKv().getKey())) {
                SparkplugTopic sparkplugTopic = new SparkplugTopic(parent.getSparkplugTopicNode(),
                        SparkplugMessageType.DCMD, deviceInfo.getDeviceName());
                parent.createSparkplugMqttPublishMsg(tsKvProto,
                        sparkplugTopic.toString(),
                        getDeviceBirthMetrics().get(tsKvProto.getKv().getKey()))
                        .ifPresent(this.parent::writeAndFlush);
            }
        });
    }

    @Override
    public void onToDeviceRpcRequest(UUID sessionId, TransportProtos.ToDeviceRpcRequestMsg rpcRequest) {
        log.trace("[{}] Received RPC Request notification to sparkplug device", sessionId);
        try {
            SparkplugMessageType messageType = SparkplugMessageType.parseMessageType(rpcRequest.getMethodName());
            SparkplugRpcRequestHeader header = JacksonUtil.fromString(rpcRequest.getParams(), SparkplugRpcRequestHeader.class);
            header.setMessageType(messageType.name());
            TransportProtos.TsKvProto tsKvProto = getTsKvProto(header.getMetricName(), header.getValue(), new Date().getTime());
            if (getDeviceBirthMetrics().containsKey(tsKvProto.getKv().getKey())) {
                SparkplugTopic sparkplugTopic = new SparkplugTopic(parent.getSparkplugTopicNode(),
                        messageType, deviceInfo.getDeviceName());
                parent.createSparkplugMqttPublishMsg(tsKvProto,
                        sparkplugTopic.toString(),
                        getDeviceBirthMetrics().get(tsKvProto.getKv().getKey()))
                        .ifPresent(payload -> parent.sendToDeviceRpcRequest(payload, rpcRequest, sessionInfo));
            } else {
                parent.sendErrorRpcResponse(sessionInfo, rpcRequest.getRequestId(),
                        SobeamErrorCode.BAD_REQUEST_PARAMS, " Failed send To Device Rpc Request: " +
                                rpcRequest.getMethodName() + ". This device does not have a metricName: [" + tsKvProto.getKv().getKey() + "]");
            }
        } catch (SobeamException e) {
            parent.sendErrorRpcResponse(sessionInfo, rpcRequest.getRequestId(),
                    SobeamErrorCode.BAD_REQUEST_PARAMS, " Failed send To Device Rpc Request: " +
                            rpcRequest.getMethodName() + ". " + e.getMessage());
        }
    }

}
