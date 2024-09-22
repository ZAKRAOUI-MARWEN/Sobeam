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
package org.sobeam.server.queue.provider;

import org.sobeam.server.common.data.queue.Queue;
import org.sobeam.server.gen.js.JsInvokeProtos;
import org.sobeam.server.gen.transport.TransportProtos;
import org.sobeam.server.gen.transport.TransportProtos.ToCoreMsg;
import org.sobeam.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.sobeam.server.gen.transport.TransportProtos.ToRuleEngineNotificationMsg;
import org.sobeam.server.gen.transport.TransportProtos.ToTransportMsg;
import org.sobeam.server.queue.TbQueueConsumer;
import org.sobeam.server.queue.TbQueueProducer;
import org.sobeam.server.queue.TbQueueRequestTemplate;
import org.sobeam.server.queue.common.TbProtoJsQueueMsg;
import org.sobeam.server.queue.common.TbProtoQueueMsg;

/**
 * Responsible for initialization of various Producers and Consumers used by TB Core Node.
 * Implementation Depends on the queue queue.type from yml or TB_QUEUE_TYPE environment variable
 */
public interface TbRuleEngineQueueFactory extends TbUsageStatsClientQueueFactory, HousekeeperClientQueueFactory {

    /**
     * Used to push messages to instances of TB Transport Service
     *
     * @return
     */
    TbQueueProducer<TbProtoQueueMsg<ToTransportMsg>> createTransportNotificationsMsgProducer();

    /**
     * Used to push messages to instances of TB RuleEngine Service
     *
     * @return
     */
    TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> createRuleEngineMsgProducer();

    /**
     * Used to push notifications to instances of TB RuleEngine Service
     *
     * @return
     */
    TbQueueProducer<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> createRuleEngineNotificationsMsgProducer();

    /**
     * Used to push messages to other instances of TB Core Service
     *
     * @return
     */
    TbQueueProducer<TbProtoQueueMsg<ToCoreMsg>> createTbCoreMsgProducer();

    /**
     * Used to push notifications to other instances of TB Core Service
     *
     * @return
     */
    TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToCoreNotificationMsg>> createTbCoreNotificationsMsgProducer();

    /**
     * Used to consume messages about firmware update notifications to TB Core Service
     *
     * @return
     */
    TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToOtaPackageStateServiceMsg>> createToOtaPackageStateServiceMsgProducer();

    /**
     * Used to consume messages by TB Rule Engine Service
     *
     * @return
     * @param configuration
     */
    TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>> createToRuleEngineMsgConsumer(Queue configuration);

    /**
     * Used to consume messages by TB Rule Engine Service
     * Intended usage for consumer per partition strategy
     *
     * @return TbQueueConsumer
     * @param configuration
     * @param partitionId as a suffix for consumer name
     */
    default TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>> createToRuleEngineMsgConsumer(Queue configuration, Integer partitionId) {
        return createToRuleEngineMsgConsumer(configuration);
    }

    /**
     * Used to consume high priority messages by TB Core Service
     *
     * @return
     */
    TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> createToRuleEngineNotificationsMsgConsumer();

    TbQueueRequestTemplate<TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>, TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> createRemoteJsRequestTemplate();
}
