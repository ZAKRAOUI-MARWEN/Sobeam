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
package org.sobeam.server.service.mobile.secret;

import org.sobeam.server.common.data.exception.SobeamException;
import org.sobeam.server.common.data.security.model.JwtPair;
import org.sobeam.server.service.security.model.SecurityUser;

public interface MobileAppSecretService {

    String generateMobileAppSecret(SecurityUser securityUser);

    JwtPair getJwtPair(String secret) throws SobeamException;

}
