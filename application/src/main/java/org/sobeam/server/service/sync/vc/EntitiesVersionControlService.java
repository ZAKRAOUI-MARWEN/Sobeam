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
package org.sobeam.server.service.sync.vc;

import com.google.common.util.concurrent.ListenableFuture;
import org.sobeam.server.common.data.EntityType;
import org.sobeam.server.common.data.User;
import org.sobeam.server.common.data.exception.SobeamException;
import org.sobeam.server.common.data.id.EntityId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.page.PageData;
import org.sobeam.server.common.data.page.PageLink;
import org.sobeam.server.common.data.sync.vc.BranchInfo;
import org.sobeam.server.common.data.sync.vc.EntityDataDiff;
import org.sobeam.server.common.data.sync.vc.EntityDataInfo;
import org.sobeam.server.common.data.sync.vc.EntityVersion;
import org.sobeam.server.common.data.sync.vc.RepositorySettings;
import org.sobeam.server.common.data.sync.vc.VersionCreationResult;
import org.sobeam.server.common.data.sync.vc.VersionLoadResult;
import org.sobeam.server.common.data.sync.vc.VersionedEntityInfo;
import org.sobeam.server.common.data.sync.vc.request.create.VersionCreateRequest;
import org.sobeam.server.common.data.sync.vc.request.load.VersionLoadRequest;

import java.util.List;
import java.util.UUID;

public interface EntitiesVersionControlService {

    ListenableFuture<UUID> saveEntitiesVersion(User user, VersionCreateRequest request) throws Exception;

    VersionCreationResult getVersionCreateStatus(User user, UUID requestId) throws SobeamException;

    ListenableFuture<PageData<EntityVersion>> listEntityVersions(TenantId tenantId, String branch, EntityId externalId, PageLink pageLink) throws Exception;

    ListenableFuture<PageData<EntityVersion>> listEntityTypeVersions(TenantId tenantId, String branch, EntityType entityType, PageLink pageLink) throws Exception;

    ListenableFuture<PageData<EntityVersion>> listVersions(TenantId tenantId, String branch, PageLink pageLink) throws Exception;

    ListenableFuture<List<VersionedEntityInfo>> listEntitiesAtVersion(TenantId tenantId, String versionId, EntityType entityType) throws Exception;

    ListenableFuture<List<VersionedEntityInfo>> listAllEntitiesAtVersion(TenantId tenantId, String versionId) throws Exception;

    UUID loadEntitiesVersion(User user, VersionLoadRequest request) throws Exception;

    VersionLoadResult getVersionLoadStatus(User user, UUID requestId) throws SobeamException;

    ListenableFuture<EntityDataDiff> compareEntityDataToVersion(User user, EntityId entityId, String versionId) throws Exception;

    ListenableFuture<List<BranchInfo>> listBranches(TenantId tenantId) throws Exception;

    RepositorySettings getVersionControlSettings(TenantId tenantId);

    ListenableFuture<RepositorySettings> saveVersionControlSettings(TenantId tenantId, RepositorySettings versionControlSettings);

    ListenableFuture<Void> deleteVersionControlSettings(TenantId tenantId) throws Exception;

    ListenableFuture<Void> checkVersionControlAccess(TenantId tenantId, RepositorySettings settings) throws Exception;

    ListenableFuture<UUID> autoCommit(User user, EntityId entityId) throws Exception;

    ListenableFuture<UUID> autoCommit(User user, EntityType entityType, List<UUID> entityIds) throws Exception;

    ListenableFuture<EntityDataInfo> getEntityDataInfo(User user, EntityId entityId, String versionId);

}
