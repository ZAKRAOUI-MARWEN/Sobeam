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
package org.sobeam.server.common.data.sync.ie;

import lombok.Data;
import org.sobeam.server.common.data.EntityType;
import org.sobeam.server.common.data.ExportableEntity;
import org.sobeam.server.common.data.id.EntityId;
import org.sobeam.server.common.data.util.ThrowingRunnable;

@Data
public class EntityImportResult<E extends ExportableEntity<? extends EntityId>> {

    private E savedEntity;
    private E oldEntity;
    private EntityType entityType;

    private ThrowingRunnable saveReferencesCallback = () -> {};
    private ThrowingRunnable sendEventsCallback = () -> {};

    private boolean updatedAllExternalIds = true;

    private boolean created;
    private boolean updated;
    private boolean updatedRelatedEntities;

    public void addSaveReferencesCallback(ThrowingRunnable callback) {
        this.saveReferencesCallback = this.saveReferencesCallback.andThen(callback);
    }

    public void addSendEventsCallback(ThrowingRunnable callback) {
        this.sendEventsCallback = this.sendEventsCallback.andThen(callback);
    }

}