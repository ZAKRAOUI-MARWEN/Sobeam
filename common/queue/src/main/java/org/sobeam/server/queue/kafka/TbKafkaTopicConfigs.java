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
package org.sobeam.server.queue.kafka;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.sobeam.server.queue.util.PropertyUtils;

import jakarta.annotation.PostConstruct;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "queue", value = "type", havingValue = "kafka")
public class TbKafkaTopicConfigs {
    public static final String NUM_PARTITIONS_SETTING = "partitions";

    @Value("${queue.kafka.topic-properties.core:}")
    private String coreProperties;
    @Value("${queue.kafka.topic-properties.rule-engine:}")
    private String ruleEngineProperties;
    @Value("${queue.kafka.topic-properties.transport-api:}")
    private String transportApiProperties;
    @Value("${queue.kafka.topic-properties.notifications:}")
    private String notificationsProperties;
    @Value("${queue.kafka.topic-properties.js-executor:}")
    private String jsExecutorProperties;
    @Value("${queue.kafka.topic-properties.ota-updates:}")
    private String fwUpdatesProperties;
    @Value("${queue.kafka.topic-properties.version-control:}")
    private String vcProperties;
    @Value("${queue.kafka.topic-properties.housekeeper:}")
    private String housekeeperProperties;
    @Value("${queue.kafka.topic-properties.housekeeper-reprocessing:}")
    private String housekeeperReprocessingProperties;

    @Getter
    private Map<String, String> coreConfigs;
    @Getter
    private Map<String, String> ruleEngineConfigs;
    @Getter
    private Map<String, String> transportApiRequestConfigs;
    @Getter
    private Map<String, String> transportApiResponseConfigs;
    @Getter
    private Map<String, String> notificationsConfigs;
    @Getter
    private Map<String, String> jsExecutorRequestConfigs;
    @Getter
    private Map<String, String> jsExecutorResponseConfigs;
    @Getter
    private Map<String, String> fwUpdatesConfigs;
    @Getter
    private Map<String, String> vcConfigs;
    @Getter
    private Map<String, String> housekeeperConfigs;
    @Getter
    private Map<String, String> housekeeperReprocessingConfigs;

    @PostConstruct
    private void init() {
        coreConfigs = PropertyUtils.getProps(coreProperties);
        ruleEngineConfigs = PropertyUtils.getProps(ruleEngineProperties);
        transportApiRequestConfigs = PropertyUtils.getProps(transportApiProperties);
        transportApiResponseConfigs = PropertyUtils.getProps(transportApiProperties);
        transportApiResponseConfigs.put(NUM_PARTITIONS_SETTING, "1");
        notificationsConfigs = PropertyUtils.getProps(notificationsProperties);
        jsExecutorRequestConfigs = PropertyUtils.getProps(jsExecutorProperties);
        jsExecutorResponseConfigs = PropertyUtils.getProps(jsExecutorProperties);
        jsExecutorResponseConfigs.put(NUM_PARTITIONS_SETTING, "1");
        fwUpdatesConfigs = PropertyUtils.getProps(fwUpdatesProperties);
        vcConfigs = PropertyUtils.getProps(vcProperties);
        housekeeperConfigs = PropertyUtils.getProps(housekeeperProperties);
        housekeeperReprocessingConfigs = PropertyUtils.getProps(housekeeperReprocessingProperties);
    }

}
