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
package org.sobeam.server.service.sync.ie;

import org.sobeam.server.common.data.EntityType;
import org.sobeam.server.common.data.ExportableEntity;
import org.sobeam.server.common.data.exception.SobeamException;
import org.sobeam.server.common.data.id.EntityId;
import org.sobeam.server.common.data.sync.ie.EntityExportData;
import org.sobeam.server.common.data.sync.ie.EntityImportResult;
import org.sobeam.server.service.sync.vc.data.EntitiesExportCtx;
import org.sobeam.server.service.sync.vc.data.EntitiesImportCtx;

import java.util.Comparator;

public interface EntitiesExportImportService {

    <E extends ExportableEntity<I>, I extends EntityId> EntityExportData<E> exportEntity(EntitiesExportCtx<?> ctx, I entityId) throws SobeamException;

    <E extends ExportableEntity<I>, I extends EntityId> EntityImportResult<E> importEntity(EntitiesImportCtx ctx, EntityExportData<E> exportData) throws SobeamException;


    void saveReferencesAndRelations(EntitiesImportCtx ctx) throws SobeamException;

    Comparator<EntityType> getEntityTypeComparatorForImport();

}
