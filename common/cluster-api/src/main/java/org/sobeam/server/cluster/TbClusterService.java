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
package org.sobeam.server.cluster;

import org.sobeam.server.common.data.ApiUsageState;
import org.sobeam.server.common.data.Device;
import org.sobeam.server.common.data.DeviceProfile;
import org.sobeam.server.common.data.TbResource;
import org.sobeam.server.common.data.TbResourceInfo;
import org.sobeam.server.common.data.Tenant;
import org.sobeam.server.common.data.TenantProfile;
import org.sobeam.server.common.data.edge.EdgeEventActionType;
import org.sobeam.server.common.data.edge.EdgeEventType;
import org.sobeam.server.common.data.id.EdgeId;
import org.sobeam.server.common.data.id.EntityId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.plugin.ComponentLifecycleEvent;
import org.sobeam.server.common.msg.TbMsg;
import org.sobeam.server.common.msg.ToDeviceActorNotificationMsg;
import org.sobeam.server.common.msg.edge.FromEdgeSyncResponse;
import org.sobeam.server.common.msg.edge.ToEdgeSyncRequest;
import org.sobeam.server.common.msg.queue.TopicPartitionInfo;
import org.sobeam.server.common.msg.rpc.FromDeviceRpcResponse;
import org.sobeam.server.gen.transport.TransportProtos;
import org.sobeam.server.gen.transport.TransportProtos.ToCoreMsg;
import org.sobeam.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.sobeam.server.gen.transport.TransportProtos.ToTransportMsg;
import org.sobeam.server.gen.transport.TransportProtos.ToVersionControlServiceMsg;
import org.sobeam.server.queue.TbQueueCallback;
import org.sobeam.server.queue.TbQueueClusterService;

import java.util.UUID;

public interface TbClusterService extends TbQueueClusterService {

    void pushMsgToCore(TopicPartitionInfo tpi, UUID msgKey, ToCoreMsg msg, TbQueueCallback callback);

    void pushMsgToCore(TenantId tenantId, EntityId entityId, ToCoreMsg msg, TbQueueCallback callback);

    void pushMsgToCore(ToDeviceActorNotificationMsg msg, TbQueueCallback callback);

    void broadcastToCore(TransportProtos.ToCoreNotificationMsg msg);

    void pushMsgToVersionControl(TenantId tenantId, ToVersionControlServiceMsg msg, TbQueueCallback callback);

    void pushNotificationToCore(String targetServiceId, FromDeviceRpcResponse response, TbQueueCallback callback);

    void pushMsgToRuleEngine(TopicPartitionInfo tpi, UUID msgId, ToRuleEngineMsg msg, TbQueueCallback callback);

    void pushMsgToRuleEngine(TenantId tenantId, EntityId entityId, TbMsg msg, TbQueueCallback callback);

    void pushNotificationToRuleEngine(String targetServiceId, FromDeviceRpcResponse response, TbQueueCallback callback);

    void pushNotificationToTransport(String targetServiceId, ToTransportMsg response, TbQueueCallback callback);

    void broadcastEntityStateChangeEvent(TenantId tenantId, EntityId entityId, ComponentLifecycleEvent state);

    void onDeviceProfileChange(DeviceProfile deviceProfile, DeviceProfile oldDeviceProfile, TbQueueCallback callback);

    void onDeviceProfileDelete(DeviceProfile deviceProfile, TbQueueCallback callback);

    void onTenantProfileChange(TenantProfile tenantProfile, TbQueueCallback callback);

    void onTenantProfileDelete(TenantProfile tenantProfile, TbQueueCallback callback);

    void onTenantChange(Tenant tenant, TbQueueCallback callback);

    void onTenantDelete(Tenant tenant, TbQueueCallback callback);

    void onApiStateChange(ApiUsageState apiUsageState, TbQueueCallback callback);

    void onDeviceUpdated(Device device, Device old);

    void onDeviceDeleted(TenantId tenantId, Device device, TbQueueCallback callback);

    void onDeviceAssignedToTenant(TenantId oldTenantId, Device device);

    void onResourceChange(TbResourceInfo resource, TbQueueCallback callback);

    void onResourceDeleted(TbResourceInfo resource, TbQueueCallback callback);

    void onEdgeEventUpdate(TenantId tenantId, EdgeId edgeId);

    void pushEdgeSyncRequestToCore(ToEdgeSyncRequest toEdgeSyncRequest);

    void pushEdgeSyncResponseToCore(FromEdgeSyncResponse fromEdgeSyncResponse);

    void sendNotificationMsgToEdge(TenantId tenantId, EdgeId edgeId, EntityId entityId, String body, EdgeEventType type, EdgeEventActionType action, EdgeId sourceEdgeId);

}
