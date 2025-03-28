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
package org.sobeam.server.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.sobeam.server.common.data.exception.SobeamException;
import org.sobeam.server.config.annotations.ApiOperation;
import org.sobeam.server.queue.util.TbCoreComponent;

@RestController
@TbCoreComponent
@RequestMapping("/api")
public class UiSettingsController extends BaseController {

    @Value("${ui.help.base-url}")
    private String helpBaseUrl;

    @ApiOperation(value = "Get UI help base url (getHelpBaseUrl)",
            notes = "Get UI help base url used to fetch help assets. " +
                    "The actual value of the base url is configurable in the system configuration file.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/uiSettings/helpBaseUrl", method = RequestMethod.GET)
    @ResponseBody
    public String getHelpBaseUrl() throws SobeamException {
        return helpBaseUrl;
    }

}
