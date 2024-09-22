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
package org.sobeam.server.queue.housekeeper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.sobeam.common.util.JacksonUtil;
import org.sobeam.server.common.data.housekeeper.HousekeeperTask;
import org.sobeam.server.common.data.housekeeper.HousekeeperTaskType;
import org.sobeam.server.common.msg.housekeeper.HousekeeperClient;
import org.sobeam.server.common.msg.queue.TopicPartitionInfo;
import org.sobeam.server.gen.transport.TransportProtos;
import org.sobeam.server.gen.transport.TransportProtos.ToHousekeeperServiceMsg;
import org.sobeam.server.queue.TbQueueCallback;
import org.sobeam.server.queue.TbQueueMsgMetadata;
import org.sobeam.server.queue.TbQueueProducer;
import org.sobeam.server.queue.common.TbProtoQueueMsg;
import org.sobeam.server.queue.provider.TbQueueProducerProvider;

@Service
@Slf4j
public class DefaultHousekeeperClient implements HousekeeperClient {

    private final HousekeeperConfig config;
    private final TbQueueProducer<TbProtoQueueMsg<ToHousekeeperServiceMsg>> producer;
    private final TopicPartitionInfo submitTpi;
    private final TbQueueCallback submitCallback;

    public DefaultHousekeeperClient(HousekeeperConfig config,
                                    TbQueueProducerProvider producerProvider) {
        this.config = config;
        this.producer = producerProvider.getHousekeeperMsgProducer();
        this.submitTpi = TopicPartitionInfo.builder().topic(producer.getDefaultTopic()).build();
        this.submitCallback = new TbQueueCallback() {
            @Override
            public void onSuccess(TbQueueMsgMetadata metadata) {
                log.trace("Submitted Housekeeper task");
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("Failed to submit Housekeeper task", t);
            }
        };
    }

    @Override
    public void submitTask(HousekeeperTask task) {
        HousekeeperTaskType taskType = task.getTaskType();
        if (config.getDisabledTaskTypes().contains(taskType)) {
            log.trace("Task type {} is disabled, ignoring {}", taskType, task);
            return;
        }

        log.debug("[{}][{}][{}] Submitting task: {}", task.getTenantId(), task.getEntityId().getEntityType(), task.getEntityId(), task);
        /*
         * using msg key as entity id so that msgs related to certain entity are pushed to same partition,
         * e.g. on tenant deletion (entity id is tenant id), we need to clean up tenant entities in certain order
         * */
        try {
            producer.send(submitTpi, new TbProtoQueueMsg<>(task.getEntityId().getId(), ToHousekeeperServiceMsg.newBuilder()
                    .setTask(TransportProtos.HousekeeperTaskProto.newBuilder()
                            .setValue(JacksonUtil.toString(task))
                            .setTs(task.getTs())
                            .setAttempt(0)
                            .build())
                    .build()), submitCallback);
        } catch (Throwable t) {
            log.error("Failed to submit Housekeeper task {}", task, t);
        }
    }

}
