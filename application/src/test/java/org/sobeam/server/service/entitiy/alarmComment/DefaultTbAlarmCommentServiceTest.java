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
package org.sobeam.server.service.entitiy.alarmComment;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.sobeam.server.cluster.TbClusterService;
import org.sobeam.server.common.data.User;
import org.sobeam.server.common.data.alarm.Alarm;
import org.sobeam.server.common.data.alarm.AlarmComment;
import org.sobeam.server.common.data.alarm.AlarmCommentType;
import org.sobeam.server.common.data.audit.ActionType;
import org.sobeam.server.common.data.exception.SobeamException;
import org.sobeam.server.common.data.id.AlarmId;
import org.sobeam.server.common.data.id.UserId;
import org.sobeam.server.dao.alarm.AlarmCommentService;
import org.sobeam.server.dao.alarm.AlarmService;
import org.sobeam.server.dao.customer.CustomerService;
import org.sobeam.server.service.entitiy.TbLogEntityActionService;
import org.sobeam.server.service.entitiy.alarm.DefaultTbAlarmCommentService;
import org.sobeam.server.service.executors.DbCallbackExecutorService;
import org.sobeam.server.service.telemetry.AlarmSubscriptionService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = DefaultTbAlarmCommentService.class)
@TestPropertySource(properties = {
        "server.log_controller_error_stack_trace=false"
})
public class DefaultTbAlarmCommentServiceTest {

    @MockBean
    protected DbCallbackExecutorService dbExecutor;
    @MockBean
    protected TbLogEntityActionService logEntityActionService;
    @MockBean
    protected AlarmService alarmService;
    @MockBean
    protected AlarmCommentService alarmCommentService;
    @MockBean
    protected AlarmSubscriptionService alarmSubscriptionService;
    @MockBean
    protected CustomerService customerService;
    @MockBean
    protected TbClusterService tbClusterService;
    @SpyBean
    DefaultTbAlarmCommentService service;

    @Test
    public void testSave() throws SobeamException {
        var alarm = new Alarm();
        var alarmComment = new AlarmComment();
        when(alarmCommentService.createOrUpdateAlarmComment(Mockito.any(), eq(alarmComment))).thenReturn(alarmComment);
        service.saveAlarmComment(alarm, alarmComment, new User());

        verify(logEntityActionService, times(1)).logEntityAction(any(), any(), any(), any(), eq(ActionType.ADDED_COMMENT), any(), any());
    }

    @Test
    public void testDelete() throws SobeamException {
        var alarmId = new AlarmId(UUID.randomUUID());
        var alarmComment = new AlarmComment();
        alarmComment.setAlarmId(alarmId);
        alarmComment.setUserId(new UserId(UUID.randomUUID()));
        alarmComment.setType(AlarmCommentType.OTHER);

        when(alarmCommentService.saveAlarmComment(Mockito.any(), eq(alarmComment))).thenReturn(alarmComment);
        service.deleteAlarmComment(new Alarm(alarmId), alarmComment, new User());

        verify(logEntityActionService, times(1)).logEntityAction(any(), any(), any(), any(), eq(ActionType.DELETED_COMMENT), any(), any());
    }

    @Test
    public void testShouldNotDeleteSystemComment() {
        var alarmId = new AlarmId(UUID.randomUUID());
        var alarmComment = new AlarmComment();
        alarmComment.setAlarmId(alarmId);
        alarmComment.setType(AlarmCommentType.SYSTEM);

        assertThatThrownBy(() -> service.deleteAlarmComment(new Alarm(alarmId), alarmComment, new User()))
                .isInstanceOf(SobeamException.class)
                .hasMessageContaining("System comment could not be deleted");
    }
}