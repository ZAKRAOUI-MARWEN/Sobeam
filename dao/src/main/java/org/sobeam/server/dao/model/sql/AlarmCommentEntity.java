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
package org.sobeam.server.dao.model.sql;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.sobeam.server.common.data.alarm.AlarmComment;
import org.sobeam.server.common.data.alarm.AlarmCommentInfo;

import static org.sobeam.server.dao.model.ModelConstants.ALARM_COMMENT_TABLE_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ALARM_COMMENT_TABLE_NAME)
public class AlarmCommentEntity extends AbstractAlarmCommentEntity<AlarmComment> {

    public AlarmCommentEntity() {
        super();
    }

    public AlarmCommentEntity(AlarmCommentInfo alarmCommentInfo) {
        super(alarmCommentInfo);
    }

    public AlarmCommentEntity(AlarmComment alarmComment) {
        super(alarmComment);
    }

    @Override
    public AlarmComment toData() {
        return super.toAlarmComment();
    }

}
