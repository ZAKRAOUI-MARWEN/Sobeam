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
package org.sobeam.rule.engine.metadata;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.sobeam.rule.engine.api.NodeConfiguration;
import org.sobeam.rule.engine.util.TbMsgSource;

import java.util.HashMap;

@Data
@EqualsAndHashCode(callSuper = true)
public class TbGetEntityDataNodeConfiguration extends TbGetMappedDataNodeConfiguration implements NodeConfiguration<TbGetEntityDataNodeConfiguration> {

    private DataToFetch dataToFetch;

    @Override
    public TbGetEntityDataNodeConfiguration defaultConfiguration() {
        var configuration = new TbGetEntityDataNodeConfiguration();
        var dataMapping = new HashMap<String, String>();
        dataMapping.putIfAbsent("alarmThreshold", "threshold");
        configuration.setDataMapping(dataMapping);
        configuration.setDataToFetch(DataToFetch.ATTRIBUTES);
        configuration.setFetchTo(TbMsgSource.METADATA);
        return configuration;
    }

}