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
package org.sobeam.server.edge;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.AbstractMessage;
import org.junit.Assert;
import org.junit.Test;
import org.sobeam.common.util.JacksonUtil;
import org.sobeam.server.common.data.widget.WidgetType;
import org.sobeam.server.common.data.widget.WidgetsBundle;
import org.sobeam.server.dao.service.DaoSqlTest;
import org.sobeam.server.gen.edge.v1.UpdateMsgType;
import org.sobeam.server.gen.edge.v1.WidgetTypeUpdateMsg;
import org.sobeam.server.gen.edge.v1.WidgetsBundleUpdateMsg;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class WidgetEdgeTest extends AbstractEdgeTest {

    @Test
    public void testWidgetsBundleAndWidgetType() throws Exception {
        // create widget bundle
        edgeImitator.expectMessageAmount(1);
        WidgetsBundle widgetsBundle = new WidgetsBundle();
        widgetsBundle.setTitle("Test Widget Bundle");
        WidgetsBundle savedWidgetsBundle = doPost("/api/widgetsBundle", widgetsBundle, WidgetsBundle.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof WidgetsBundleUpdateMsg);
        WidgetsBundleUpdateMsg widgetsBundleUpdateMsg = (WidgetsBundleUpdateMsg) latestMessage;
        WidgetsBundle widgetsBundleMsg = JacksonUtil.fromString(widgetsBundleUpdateMsg.getEntity(), WidgetsBundle.class, true);
        Assert.assertNotNull(widgetsBundleMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, widgetsBundleUpdateMsg.getMsgType());
        Assert.assertEquals(savedWidgetsBundle.getId(), widgetsBundleMsg.getId());
        Assert.assertEquals(savedWidgetsBundle.getAlias(), widgetsBundleMsg.getAlias());
        Assert.assertEquals(savedWidgetsBundle.getTitle(), widgetsBundleMsg.getTitle());
        testAutoGeneratedCodeByProtobuf(widgetsBundleUpdateMsg);

        // create widget type
        edgeImitator.expectMessageAmount(1);
        WidgetType widgetType = new WidgetType();
        widgetType.setName("Test Widget Type");
        ObjectNode descriptor = JacksonUtil.newObjectNode();
        descriptor.put("key", "value");
        widgetType.setDescriptor(descriptor);
        widgetType.setDeprecated(true);
        widgetType.setFqn("bundle_alias.type_alias");
        WidgetType savedWidgetType = doPost("/api/widgetType", widgetType, WidgetType.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof WidgetTypeUpdateMsg);
        WidgetTypeUpdateMsg widgetTypeUpdateMsg = (WidgetTypeUpdateMsg) latestMessage;
        WidgetType widgetsType = JacksonUtil.fromString(widgetTypeUpdateMsg.getEntity(), WidgetType.class, true);
        Assert.assertNotNull(widgetsType);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, widgetTypeUpdateMsg.getMsgType());
        Assert.assertEquals(savedWidgetType, widgetsType);

        // update widget bundle
        edgeImitator.expectMessageAmount(1);
        savedWidgetsBundle.setTitle("Test Widget Bundle - Updated");
        savedWidgetsBundle = doPost("/api/widgetsBundle", savedWidgetsBundle, WidgetsBundle.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof WidgetsBundleUpdateMsg);
        widgetsBundleUpdateMsg = (WidgetsBundleUpdateMsg) latestMessage;
        widgetsBundleMsg = JacksonUtil.fromString(widgetsBundleUpdateMsg.getEntity(), WidgetsBundle.class, true);
        Assert.assertNotNull(widgetsBundleMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, widgetsBundleUpdateMsg.getMsgType());
        Assert.assertEquals(savedWidgetsBundle.getTitle(), widgetsBundleMsg.getTitle());

        // update widget type
        edgeImitator.expectMessageAmount(1);
        savedWidgetType.setName("Test Widget Type - Updated");
        savedWidgetType = doPost("/api/widgetType", savedWidgetType, WidgetType.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof WidgetTypeUpdateMsg);
        widgetTypeUpdateMsg = (WidgetTypeUpdateMsg) latestMessage;
        widgetsType = JacksonUtil.fromString(widgetTypeUpdateMsg.getEntity(), WidgetType.class, true);
        Assert.assertNotNull(widgetsType);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, widgetTypeUpdateMsg.getMsgType());
        Assert.assertEquals(savedWidgetType.getName(), widgetsType.getName());

        // delete widget type
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/widgetType/" + savedWidgetType.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof WidgetTypeUpdateMsg);
        widgetTypeUpdateMsg = (WidgetTypeUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, widgetTypeUpdateMsg.getMsgType());
        Assert.assertEquals(savedWidgetType.getUuidId().getMostSignificantBits(), widgetTypeUpdateMsg.getIdMSB());
        Assert.assertEquals(savedWidgetType.getUuidId().getLeastSignificantBits(), widgetTypeUpdateMsg.getIdLSB());

        // delete widget bundle
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/widgetsBundle/" + savedWidgetsBundle.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof WidgetsBundleUpdateMsg);
        widgetsBundleUpdateMsg = (WidgetsBundleUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, widgetsBundleUpdateMsg.getMsgType());
        Assert.assertEquals(savedWidgetsBundle.getUuidId().getMostSignificantBits(), widgetsBundleUpdateMsg.getIdMSB());
        Assert.assertEquals(savedWidgetsBundle.getUuidId().getLeastSignificantBits(), widgetsBundleUpdateMsg.getIdLSB());
    }

}