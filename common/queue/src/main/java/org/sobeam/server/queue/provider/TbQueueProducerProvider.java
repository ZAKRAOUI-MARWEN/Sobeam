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

import org.sobeam.server.gen.transport.TransportProtos.ToCoreMsg;
import org.sobeam.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.sobeam.server.gen.transport.TransportProtos.ToHousekeeperServiceMsg;
import org.sobeam.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.sobeam.server.gen.transport.TransportProtos.ToRuleEngineNotificationMsg;
import org.sobeam.server.gen.transport.TransportProtos.ToTransportMsg;
import org.sobeam.server.gen.transport.TransportProtos.ToUsageStatsServiceMsg;
import org.sobeam.server.gen.transport.TransportProtos.ToVersionControlServiceMsg;
import org.sobeam.server.queue.TbQueueProducer;
import org.sobeam.server.queue.common.TbProtoQueueMsg;

/**
 * Responsible for providing various Producers to other services.
 */
public interface TbQueueProducerProvider {

    /**
     * Used to push messages to instances of TB Transport Service
     *
     * @return
     */
    TbQueueProducer<TbProtoQueueMsg<ToTransportMsg>> getTransportNotificationsMsgProducer();

    /**
     * Used to push messages to instances of TB RuleEngine Service
     *
     * @return
     */
    TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> getRuleEngineMsgProducer();

    /**
     * Used to push notifications to instances of TB RuleEngine Service
     *
     * @return
     */
    TbQueueProducer<TbProtoQueueMsg<ToRuleEngineNotificationMsg>> getRuleEngineNotificationsMsgProducer();

    /**
     * Used to push messages to other instances of TB Core Service
     *
     * @return
     */
    TbQueueProducer<TbProtoQueueMsg<ToCoreMsg>> getTbCoreMsgProducer();

    /**
     * Used to push messages to other instances of TB Core Service
     *
     * @return
     */
    TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> getTbCoreNotificationsMsgProducer();

    /**
     * Used to push messages to other instances of TB Core Service
     *
     * @return
     */
    TbQueueProducer<TbProtoQueueMsg<ToUsageStatsServiceMsg>> getTbUsageStatsMsgProducer();

        /**
     * Used to push messages to other instances of TB Core Service
     *
     * @return
     */
    TbQueueProducer<TbProtoQueueMsg<ToVersionControlServiceMsg>> getTbVersionControlMsgProducer();

    TbQueueProducer<TbProtoQueueMsg<ToHousekeeperServiceMsg>> getHousekeeperMsgProducer();

}
