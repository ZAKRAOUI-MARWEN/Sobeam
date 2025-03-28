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
package org.sobeam.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.sobeam.server.msa.ui.utils.Const;

public class LoginPageHelper extends LoginPageElements {
    public LoginPageHelper(WebDriver driver) {
        super(driver);
    }

    public void authorizationTenant() {
        emailField().sendKeys(Const.TENANT_EMAIL);
        passwordField().sendKeys(Const.TENANT_PASSWORD);
        submitBtn().click();
        waitUntilUrlContainsText("/home");
    }
}
