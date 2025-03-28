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
package org.sobeam.server.common.data.sync.vc;

import lombok.Data;

import java.io.Serializable;

@Data
public class RepositorySettings implements Serializable {
    private static final long serialVersionUID = -3211552851889198721L;

    private String repositoryUri;
    private RepositoryAuthMethod authMethod;
    private String username;
    private String password;
    private String privateKeyFileName;
    private String privateKey;
    private String privateKeyPassword;
    private String defaultBranch;
    private boolean readOnly;
    private boolean showMergeCommits;

    public RepositorySettings() {
    }

    public RepositorySettings(RepositorySettings settings) {
        this.repositoryUri = settings.getRepositoryUri();
        this.authMethod = settings.getAuthMethod();
        this.username = settings.getUsername();
        this.password = settings.getPassword();
        this.privateKeyFileName = settings.getPrivateKeyFileName();
        this.privateKey = settings.getPrivateKey();
        this.privateKeyPassword = settings.getPrivateKeyPassword();
        this.defaultBranch = settings.getDefaultBranch();
        this.readOnly = settings.isReadOnly();
        this.showMergeCommits = settings.isShowMergeCommits();
    }
}
