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
package org.sobeam.server.dao.sql.query;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EntityDataAdapterTest {

    @Test
    public void testConvertValue() {
        assertThat(EntityDataAdapter.convertValue("500")).isEqualTo("500");
        assertThat(EntityDataAdapter.convertValue("500D")).isEqualTo("500D"); //do not convert to Double !!!
        assertThat(EntityDataAdapter.convertValue("0101010521130565")).isEqualTo("0101010521130565"); //do not convert to Double !!!
        assertThat(EntityDataAdapter.convertValue("89010303310033979663")).isEqualTo("89010303310033979663"); //do not convert to Double !!!
        assertThat(EntityDataAdapter.convertValue("89914009129080723322")).isEqualTo("89914009129080723322");
        assertThat(EntityDataAdapter.convertValue("899140091AAAA29080723322")).isEqualTo("899140091AAAA29080723322");
        assertThat(EntityDataAdapter.convertValue("899140091.29080723322")).isEqualTo("899140091.29080723322");
    }
}
