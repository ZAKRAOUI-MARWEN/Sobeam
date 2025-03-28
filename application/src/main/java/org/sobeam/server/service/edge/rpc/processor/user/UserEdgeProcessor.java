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
package org.sobeam.server.service.edge.rpc.processor.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.sobeam.server.common.data.EdgeUtils;
import org.sobeam.server.common.data.User;
import org.sobeam.server.common.data.edge.EdgeEvent;
import org.sobeam.server.common.data.id.UserId;
import org.sobeam.server.common.data.security.UserCredentials;
import org.sobeam.server.gen.edge.v1.DownlinkMsg;
import org.sobeam.server.gen.edge.v1.EdgeVersion;
import org.sobeam.server.gen.edge.v1.UpdateMsgType;
import org.sobeam.server.gen.edge.v1.UserCredentialsUpdateMsg;
import org.sobeam.server.queue.util.TbCoreComponent;
import org.sobeam.server.service.edge.rpc.constructor.user.UserMsgConstructor;
import org.sobeam.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
@Component
@TbCoreComponent
public class UserEdgeProcessor extends BaseEdgeProcessor {

    public DownlinkMsg convertUserEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        UserId userId = new UserId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED -> {
                User user = userService.findUserById(edgeEvent.getTenantId(), userId);
                if (user != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addUserUpdateMsg(((UserMsgConstructor) userMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion)).constructUserUpdatedMsg(msgType, user))
                            .build();
                }
            }
            case DELETED -> downlinkMsg = DownlinkMsg.newBuilder()
                    .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                    .addUserUpdateMsg(((UserMsgConstructor) userMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion)).constructUserDeleteMsg(userId))
                    .build();
            case CREDENTIALS_UPDATED -> {
                UserCredentials userCredentialsByUserId = userService.findUserCredentialsByUserId(edgeEvent.getTenantId(), userId);
                if (userCredentialsByUserId != null && userCredentialsByUserId.isEnabled()) {
                    UserCredentialsUpdateMsg userCredentialsUpdateMsg =
                            ((UserMsgConstructor) userMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion)).constructUserCredentialsUpdatedMsg(userCredentialsByUserId);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addUserCredentialsUpdateMsg(userCredentialsUpdateMsg)
                            .build();
                }
            }
        }
        return downlinkMsg;
    }

}
