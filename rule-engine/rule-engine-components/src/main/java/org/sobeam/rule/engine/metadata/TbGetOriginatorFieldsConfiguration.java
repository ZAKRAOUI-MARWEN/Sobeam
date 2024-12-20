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
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class TbGetOriginatorFieldsConfiguration extends TbGetMappedDataNodeConfiguration implements NodeConfiguration<TbGetOriginatorFieldsConfiguration> {

    private boolean ignoreNullStrings;

    @Override
    public TbGetOriginatorFieldsConfiguration defaultConfiguration() {
        var configuration = new TbGetOriginatorFieldsConfiguration();
        var dataMapping = new HashMap<String, String>();
        dataMapping.put("name", "originatorName");
        dataMapping.put("type", "originatorType");
        configuration.setDataMapping(dataMapping);
        configuration.setIgnoreNullStrings(false);
        configuration.setFetchTo(TbMsgSource.METADATA);
        return configuration;
    }

}
