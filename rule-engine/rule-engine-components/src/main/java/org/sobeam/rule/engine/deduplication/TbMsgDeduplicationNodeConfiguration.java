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
package org.sobeam.rule.engine.deduplication;

import lombok.Data;
import org.sobeam.rule.engine.api.NodeConfiguration;

@Data
public class TbMsgDeduplicationNodeConfiguration implements NodeConfiguration<TbMsgDeduplicationNodeConfiguration> {

    private int interval;
    private DeduplicationStrategy strategy;

    // only for DeduplicationStrategy.ALL:
    private String outMsgType;

    // Advanced settings:
    private int maxPendingMsgs;
    private int maxRetries;

    @Override
    public TbMsgDeduplicationNodeConfiguration defaultConfiguration() {
        TbMsgDeduplicationNodeConfiguration configuration = new TbMsgDeduplicationNodeConfiguration();
        configuration.setInterval(60);
        configuration.setStrategy(DeduplicationStrategy.FIRST);
        configuration.setMaxPendingMsgs(100);
        configuration.setMaxRetries(3);
        return configuration;
    }
}
