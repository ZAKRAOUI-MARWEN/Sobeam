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
package org.sobeam.server.service.edge.rpc.constructor.alarm;

import org.sobeam.server.common.data.alarm.Alarm;
import org.sobeam.server.common.data.alarm.AlarmComment;
import org.sobeam.server.gen.edge.v1.AlarmCommentUpdateMsg;
import org.sobeam.server.gen.edge.v1.AlarmUpdateMsg;
import org.sobeam.server.gen.edge.v1.UpdateMsgType;
import org.sobeam.server.service.edge.rpc.constructor.MsgConstructor;

public interface AlarmMsgConstructor extends MsgConstructor {

    AlarmUpdateMsg constructAlarmUpdatedMsg(UpdateMsgType msgType, Alarm alarm, String entityName);

    AlarmCommentUpdateMsg constructAlarmCommentUpdatedMsg(UpdateMsgType msgType, AlarmComment alarmComment);
}
