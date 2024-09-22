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
package org.sobeam.server.queue.notification;

import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import org.sobeam.server.common.data.JavaSerDesUtil;
import org.sobeam.server.common.data.notification.rule.trigger.NotificationRuleTrigger;
import org.sobeam.server.common.msg.notification.NotificationRuleProcessor;
import org.sobeam.server.common.msg.queue.ServiceType;
import org.sobeam.server.common.msg.queue.TopicPartitionInfo;
import org.sobeam.server.gen.transport.TransportProtos;
import org.sobeam.server.queue.common.TbProtoQueueMsg;
import org.sobeam.server.queue.discovery.PartitionService;
import org.sobeam.server.queue.discovery.TopicService;
import org.sobeam.server.queue.provider.TbQueueProducerProvider;

import java.util.UUID;

@Service
@ConditionalOnMissingBean(value = NotificationRuleProcessor.class, ignored = RemoteNotificationRuleProcessor.class)
@RequiredArgsConstructor
@Slf4j
public class RemoteNotificationRuleProcessor implements NotificationRuleProcessor {

    private final NotificationDeduplicationService deduplicationService;
    private final TbQueueProducerProvider producerProvider;
    private final TopicService topicService;
    private final PartitionService partitionService;

    @Override
    public void process(NotificationRuleTrigger trigger) {
        try {
            if (trigger.deduplicate() && deduplicationService.alreadyProcessed(trigger)) {
                return;
            }

            log.debug("Submitting notification rule trigger: {}", trigger);
            TransportProtos.NotificationRuleProcessorMsg.Builder msg = TransportProtos.NotificationRuleProcessorMsg.newBuilder()
                    .setTrigger(ByteString.copyFrom(JavaSerDesUtil.encode(trigger)));

            partitionService.getAllServiceIds(ServiceType.TB_CORE).stream().findAny().ifPresent(serviceId -> {
                TopicPartitionInfo tpi = topicService.getNotificationsTopic(ServiceType.TB_CORE, serviceId);
                producerProvider.getTbCoreNotificationsMsgProducer().send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(),
                        TransportProtos.ToCoreNotificationMsg.newBuilder()
                                .setNotificationRuleProcessorMsg(msg)
                                .build()), null);
            });
        } catch (Throwable e) {
            log.error("Failed to submit notification rule trigger: {}", trigger, e);
        }
    }

}
