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
package org.sobeam.server.common.data.security;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.sobeam.server.common.data.BaseData;
import org.sobeam.server.common.data.id.UserAuthSettingsId;
import org.sobeam.server.common.data.id.UserId;
import org.sobeam.server.common.data.security.model.mfa.account.AccountTwoFaSettings;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserAuthSettings extends BaseData<UserAuthSettingsId> {

    private static final long serialVersionUID = 2628320657987010348L;

    private UserId userId;
    private AccountTwoFaSettings twoFaSettings;

}