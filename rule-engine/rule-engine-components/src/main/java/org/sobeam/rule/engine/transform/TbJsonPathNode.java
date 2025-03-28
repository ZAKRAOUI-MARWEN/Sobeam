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
package org.sobeam.rule.engine.transform;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import lombok.extern.slf4j.Slf4j;
import org.sobeam.common.util.JacksonUtil;
import org.sobeam.rule.engine.api.RuleNode;
import org.sobeam.rule.engine.api.TbContext;
import org.sobeam.rule.engine.api.TbNode;
import org.sobeam.rule.engine.api.TbNodeConfiguration;
import org.sobeam.rule.engine.api.TbNodeException;
import org.sobeam.rule.engine.api.util.TbNodeUtils;
import org.sobeam.server.common.data.plugin.ComponentType;
import org.sobeam.server.common.msg.TbMsg;

import java.util.concurrent.ExecutionException;

@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "json path",
        configClazz = TbJsonPathNodeConfiguration.class,
        nodeDescription = "Transforms incoming message body using JSONPath expression.",
        nodeDetails = "JSONPath expression specifies a path to an element or a set of elements in a JSON structure.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        icon = "functions",
        configDirective = "tbTransformationNodeJsonPathConfig"
)
public class TbJsonPathNode implements TbNode {

    private TbJsonPathNodeConfiguration config;
    private Configuration configurationJsonPath;
    private JsonPath jsonPath;
    private String jsonPathValue;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbJsonPathNodeConfiguration.class);
        this.jsonPathValue = config.getJsonPath();
        if (!TbJsonPathNodeConfiguration.DEFAULT_JSON_PATH.equals(this.jsonPathValue)) {
            this.configurationJsonPath = Configuration.builder()
                    .jsonProvider(new JacksonJsonNodeJsonProvider())
                    .build();
            this.jsonPath = JsonPath.compile(config.getJsonPath());
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        if (!TbJsonPathNodeConfiguration.DEFAULT_JSON_PATH.equals(this.jsonPathValue)) {
            try {
                Object jsonPathData = jsonPath.read(msg.getData(), this.configurationJsonPath);
                ctx.tellSuccess(TbMsg.transformMsgData(msg, JacksonUtil.toString(jsonPathData)));
            } catch (PathNotFoundException e) {
                ctx.tellFailure(msg, e);
            }
        } else {
            ctx.tellSuccess(msg);
        }
    }
}
