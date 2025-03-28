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
package org.sobeam.server.service.install;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.sobeam.server.dao.cassandra.CassandraCluster;
import org.sobeam.server.dao.cassandra.CassandraInstallCluster;
import org.sobeam.server.service.install.cql.CQLStatementsParser;

import java.nio.file.Path;
import java.util.List;

@Slf4j
public abstract class AbstractCassandraDatabaseUpgradeService {
    @Autowired
    protected CassandraCluster cluster;

    @Autowired
    @Qualifier("CassandraInstallCluster")
    private CassandraInstallCluster installCluster;

    protected void loadCql(Path cql) throws Exception {
        List<String> statements = new CQLStatementsParser(cql).getStatements();
        statements.forEach(statement -> {
            installCluster.getSession().execute(statement);
            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
            }
        });
        Thread.sleep(5000);
    }
}
