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
package org.sobeam.server.msa.ui.tests.customerSmoke;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.sobeam.server.msa.ui.base.AbstractDriverBaseTest;
import org.sobeam.server.msa.ui.pages.CustomerPageHelper;
import org.sobeam.server.msa.ui.pages.LoginPageHelper;
import org.sobeam.server.msa.ui.pages.SideBarMenuViewElements;

public class ManageCustomersEdgesTest extends AbstractDriverBaseTest {

    private SideBarMenuViewElements sideBarMenuView;
    private CustomerPageHelper customerPage;
    private final String iconText = "Edge instances";

    @BeforeClass
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewElements(driver);
        customerPage = new CustomerPageHelper(driver);
    }

    @Epic("Customers smoke tests")
    @Feature("Manage customer edges")
    @Test(groups = "smoke")
    @Description("Open manage window by right corner btn")
    public void openWindowByRightCornerBtn() {
        sideBarMenuView.customerBtn().click();
        customerPage.setCustomerName();
        customerPage.manageCustomersEdgeBtn(customerPage.getCustomerName()).click();

        Assert.assertTrue(urlContains("edgeInstances"));
        Assert.assertNotNull(customerPage.customerEdgeIconHeader());
        Assert.assertTrue(customerPage.customerEdgeIconHeader().isDisplayed());
        Assert.assertTrue(customerPage.customerManageWindowIconHead().getText().contains(iconText));
    }

    @Epic("Customers smoke tests")
    @Feature("Manage customer edges")
    @Test(groups = "smoke")
    @Description("Open manage window by btn in entity view")
    public void openWindowByView() {
        sideBarMenuView.customerBtn().click();
        customerPage.setCustomerName();
        customerPage.entity(customerPage.getCustomerName()).click();
        jsClick(customerPage.manageCustomersEdgeBtnView());

        Assert.assertTrue(urlContains("edgeInstances"));
        Assert.assertNotNull(customerPage.customerEdgeIconHeader());
        Assert.assertTrue(customerPage.customerEdgeIconHeader().isDisplayed());
        Assert.assertTrue(customerPage.customerManageWindowIconHead().getText().contains(iconText));
    }
}
