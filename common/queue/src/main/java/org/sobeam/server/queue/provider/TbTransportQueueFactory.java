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
import org.sobeam.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.sobeam.server.gen.transport.TransportProtos.ToTransportMsg;
import org.sobeam.server.gen.transport.TransportProtos.TransportApiRequestMsg;
import org.sobeam.server.gen.transport.TransportProtos.TransportApiResponseMsg;
import org.sobeam.server.queue.TbQueueConsumer;
import org.sobeam.server.queue.TbQueueProducer;
import org.sobeam.server.queue.TbQueueRequestTemplate;
import org.sobeam.server.queue.common.TbProtoQueueMsg;

public interface TbTransportQueueFactory extends TbUsageStatsClientQueueFactory, HousekeeperClientQueueFactory {

    TbQueueRequestTemplate<TbProtoQueueMsg<TransportApiRequestMsg>, TbProtoQueueMsg<TransportApiResponseMsg>> createTransportApiRequestTemplate();

    TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> createRuleEngineMsgProducer();

    TbQueueProducer<TbProtoQueueMsg<ToCoreMsg>> createTbCoreMsgProducer();

    TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> createTbCoreNotificationsMsgProducer();

    TbQueueConsumer<TbProtoQueueMsg<ToTransportMsg>> createTransportNotificationsConsumer();

}
