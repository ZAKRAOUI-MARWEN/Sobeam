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
package org.sobeam.server.common.data.oauth2;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.sobeam.server.common.data.BaseData;
import org.sobeam.server.common.data.id.OAuth2DomainId;
import org.sobeam.server.common.data.id.OAuth2ParamsId;

@EqualsAndHashCode(callSuper = true)
@Data
@ToString
@NoArgsConstructor
public class OAuth2Domain extends BaseData<OAuth2DomainId> {

    private OAuth2ParamsId oauth2ParamsId;
    private String domainName;
    private SchemeType domainScheme;

    public OAuth2Domain(OAuth2Domain domain) {
        super(domain);
        this.oauth2ParamsId = domain.oauth2ParamsId;
        this.domainName = domain.domainName;
        this.domainScheme = domain.domainScheme;
    }
}
