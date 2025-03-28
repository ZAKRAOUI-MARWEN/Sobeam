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
package org.sobeam.server.msa.connectivity;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.sobeam.server.msa.AbstractLwm2mClientTest;
import org.sobeam.server.msa.DisableUIListeners;

import static org.sobeam.server.msa.ui.utils.Const.TENANT_EMAIL;
import static org.sobeam.server.msa.ui.utils.Const.TENANT_PASSWORD;

@DisableUIListeners
public class Lwm2mClientPskTest extends AbstractLwm2mClientTest {

    @BeforeMethod
    public void setUp() throws Exception {
        testRestClient.login(TENANT_EMAIL, TENANT_PASSWORD);
        initTest("lwm2m-Psk");
    }

    @AfterMethod
    public void tearDown() {
        destroyAfter();
    }

    @Test
    public void connectLwm2mClientPskWithLwm2mServer() throws Exception {
        connectLwm2mClientPsk();
    }
}
