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
package org.sobeam.server.dao;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.config.BootstrapMode;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.sobeam.server.dao.util.TbAutoConfiguration;
import org.sobeam.server.dao.util.TimescaleDBTsDao;

@Configuration
@TbAutoConfiguration
@ComponentScan({"org.sobeam.server.dao.sqlts.timescale"})
@EnableJpaRepositories(value = {"org.sobeam.server.dao.sqlts.timescale", "org.sobeam.server.dao.sqlts.insert.timescale"}, bootstrapMode = BootstrapMode.LAZY)
@EntityScan({"org.sobeam.server.dao.model.sqlts.timescale"})
@EnableTransactionManagement
@TimescaleDBTsDao
public class TimescaleDaoConfig {

}
