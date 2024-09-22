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
package org.sobeam.server.queue.discovery;

import org.sobeam.server.common.data.id.EntityId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.msg.queue.ServiceType;
import org.sobeam.server.common.msg.queue.TopicPartitionInfo;
import org.sobeam.server.gen.transport.TransportProtos;
import org.sobeam.server.queue.discovery.event.PartitionChangeEvent;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Once application is ready or cluster topology changes, this Service will produce {@link PartitionChangeEvent}
 */
public interface PartitionService {

    TopicPartitionInfo resolve(ServiceType serviceType, String queueName, TenantId tenantId, EntityId entityId);

    TopicPartitionInfo resolve(ServiceType serviceType, String queueName, TenantId tenantId, EntityId entityId, Integer partition);

    TopicPartitionInfo resolve(ServiceType serviceType, TenantId tenantId, EntityId entityId);

    List<TopicPartitionInfo> resolveAll(ServiceType serviceType, String queueName, TenantId tenantId, EntityId entityId);

    boolean isMyPartition(ServiceType serviceType, TenantId tenantId, EntityId entityId);

    List<Integer> getMyPartitions(QueueKey queueKey);

    /**
     * Received from the Discovery service when network topology is changed.
     * @param currentService - current service information {@link org.sobeam.server.gen.transport.TransportProtos.ServiceInfo}
     * @param otherServices - all other discovered services {@link org.sobeam.server.gen.transport.TransportProtos.ServiceInfo}
     */
    void recalculatePartitions(TransportProtos.ServiceInfo currentService, List<TransportProtos.ServiceInfo> otherServices);

    /**
     * Get all active service ids by service type
     * @param serviceType to filter the list of services
     * @return list of all active services
     */
    Set<String> getAllServiceIds(ServiceType serviceType);

    Set<TransportProtos.ServiceInfo> getAllServices(ServiceType serviceType);

    Set<TransportProtos.ServiceInfo> getOtherServices(ServiceType serviceType);

    int resolvePartitionIndex(UUID entityId, int partitions);

    void evictTenantInfo(TenantId tenantId);

    int countTransportsByType(String type);

    void updateQueues(List<TransportProtos.QueueUpdateMsg> queueUpdateMsgs);

    void removeQueues(List<TransportProtos.QueueDeleteMsg> queueDeleteMsgs);

    void removeTenant(TenantId tenantId);

    boolean isManagedByCurrentService(TenantId tenantId);

}
