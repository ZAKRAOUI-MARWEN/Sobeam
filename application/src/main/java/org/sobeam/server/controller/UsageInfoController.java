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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.sobeam.server.common.data.UsageInfo;
import org.sobeam.server.common.data.exception.SobeamException;
import org.sobeam.server.dao.usage.UsageInfoService;
import org.sobeam.server.queue.util.TbCoreComponent;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
public class UsageInfoController extends BaseController {

    @Autowired
    private UsageInfoService usageInfoService;

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/usage", method = RequestMethod.GET)
    @ResponseBody
    public UsageInfo getTenantUsageInfo() throws SobeamException {
        return checkNotNull(usageInfoService.getUsageInfo(getCurrentUser().getTenantId()));
    }
}
