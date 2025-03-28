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
package org.sobeam.server.dao.widget;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.sobeam.server.common.data.EntityType;
import org.sobeam.server.common.data.id.EntityId;
import org.sobeam.server.common.data.id.HasId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.id.WidgetTypeId;
import org.sobeam.server.common.data.id.WidgetsBundleId;
import org.sobeam.server.common.data.page.PageData;
import org.sobeam.server.common.data.page.PageLink;
import org.sobeam.server.common.data.widget.DeprecatedFilter;
import org.sobeam.server.common.data.widget.WidgetType;
import org.sobeam.server.common.data.widget.WidgetTypeDetails;
import org.sobeam.server.common.data.widget.WidgetTypeInfo;
import org.sobeam.server.common.data.widget.WidgetsBundleWidget;
import org.sobeam.server.dao.entity.AbstractCachedEntityService;
import org.sobeam.server.dao.eventsourcing.DeleteEntityEvent;
import org.sobeam.server.dao.eventsourcing.SaveEntityEvent;
import org.sobeam.server.dao.resource.ImageService;
import org.sobeam.server.dao.service.DataValidator;
import org.sobeam.server.dao.service.PaginatedRemover;
import org.sobeam.server.dao.service.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.sobeam.server.dao.service.Validator.validateIds;

@Service("WidgetTypeDaoService")
@Slf4j
public class WidgetTypeServiceImpl implements WidgetTypeService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_RESOURCE_ID = "Incorrect resourceId ";
    public static final String INCORRECT_BUNDLE_ALIAS = "Incorrect bundleAlias ";
    public static final String INCORRECT_WIDGETS_BUNDLE_ID = "Incorrect widgetsBundleId ";

    @Autowired
    private WidgetTypeDao widgetTypeDao;

    @Autowired
    private DataValidator<WidgetTypeDetails> widgetTypeValidator;

    @Autowired
    protected ApplicationEventPublisher eventPublisher;

    @Autowired
    protected ImageService imageService;

    @Override
    public WidgetType findWidgetTypeById(TenantId tenantId, WidgetTypeId widgetTypeId) {
        log.trace("Executing findWidgetTypeById [{}]", widgetTypeId);
        Validator.validateId(widgetTypeId, id -> "Incorrect widgetTypeId " + id);
        return widgetTypeDao.findWidgetTypeById(tenantId, widgetTypeId.getId());
    }

    @Override
    public WidgetTypeDetails findWidgetTypeDetailsById(TenantId tenantId, WidgetTypeId widgetTypeId) {
        log.trace("Executing findWidgetTypeDetailsById [{}]", widgetTypeId);
        Validator.validateId(widgetTypeId, id -> "Incorrect widgetTypeId " + id);
        return widgetTypeDao.findById(tenantId, widgetTypeId.getId());
    }

    @Override
    public boolean widgetTypeExistsByTenantIdAndWidgetTypeId(TenantId tenantId, WidgetTypeId widgetTypeId) {
        log.trace("Executing widgetTypeExistsByTenantIdAndWidgetTypeId, tenantId [{}],  widgetTypeId [{}]", tenantId, widgetTypeId);
        Validator.validateId(widgetTypeId, id -> "Incorrect widgetTypeId " + id);
        return widgetTypeDao.existsByTenantIdAndId(tenantId, widgetTypeId.getId());
    }

    @Override
    public WidgetTypeDetails saveWidgetType(WidgetTypeDetails widgetTypeDetails) {
        log.trace("Executing saveWidgetType [{}]", widgetTypeDetails);
        widgetTypeValidator.validate(widgetTypeDetails, WidgetType::getTenantId);
        try {
            imageService.replaceBase64WithImageUrl(widgetTypeDetails);
            WidgetTypeDetails result = widgetTypeDao.save(widgetTypeDetails.getTenantId(), widgetTypeDetails);
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(result.getTenantId())
                    .entityId(result.getId()).created(widgetTypeDetails.getId() == null).build());
            return result;
        } catch (Exception t) {
            AbstractCachedEntityService.checkConstraintViolation(t,
                    "uq_widget_type_fqn", "Widget type with such fqn already exists!");
            AbstractCachedEntityService.checkConstraintViolation(t, "widget_type_external_id_unq_key", "Widget type with such external id already exists!");
            throw t;
        }
    }

    @Override
    public void deleteWidgetType(TenantId tenantId, WidgetTypeId widgetTypeId) {
        log.trace("Executing deleteWidgetType [{}]", widgetTypeId);
        Validator.validateId(widgetTypeId, id -> "Incorrect widgetTypeId " + id);
        widgetTypeDao.removeById(tenantId, widgetTypeId.getId());
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(widgetTypeId).build());
    }

    @Override
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        deleteWidgetType(tenantId, (WidgetTypeId) id);
    }

    @Override
    public PageData<WidgetTypeInfo> findSystemWidgetTypesByPageLink(TenantId tenantId, boolean fullSearch, DeprecatedFilter deprecatedFilter, List<String> widgetTypes, PageLink pageLink) {
        log.trace("Executing findSystemWidgetTypesByPageLink, fullSearch [{}], deprecatedFilter [{}], widgetTypes [{}], pageLink [{}]", fullSearch, deprecatedFilter, widgetTypes, pageLink);
        Validator.validatePageLink(pageLink);
        return widgetTypeDao.findSystemWidgetTypes(tenantId, fullSearch, deprecatedFilter, widgetTypes, pageLink);
    }

    @Override
    public PageData<WidgetTypeInfo> findAllTenantWidgetTypesByTenantIdAndPageLink(TenantId tenantId, boolean fullSearch, DeprecatedFilter deprecatedFilter, List<String> widgetTypes, PageLink pageLink) {
        log.trace("Executing findAllTenantWidgetTypesByTenantIdAndPageLink, tenantId [{}], fullSearch [{}], deprecatedFilter [{}], widgetTypes [{}], pageLink [{}]",
                tenantId, fullSearch, deprecatedFilter, widgetTypes, pageLink);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validatePageLink(pageLink);
        return widgetTypeDao.findAllTenantWidgetTypesByTenantId(tenantId.getId(), fullSearch, deprecatedFilter, widgetTypes, pageLink);
    }

    @Override
    public PageData<WidgetTypeInfo> findTenantWidgetTypesByTenantIdAndPageLink(TenantId tenantId, boolean fullSearch, DeprecatedFilter deprecatedFilter, List<String> widgetTypes, PageLink pageLink) {
        log.trace("Executing findTenantWidgetTypesByTenantIdAndPageLink, tenantId [{}], fullSearch [{}], deprecatedFilter [{}], widgetTypes [{}], pageLink [{}]",
                tenantId, fullSearch, deprecatedFilter, widgetTypes, pageLink);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validatePageLink(pageLink);
        return widgetTypeDao.findTenantWidgetTypesByTenantId(tenantId.getId(), fullSearch, deprecatedFilter, widgetTypes, pageLink);
    }

    @Override
    public List<WidgetType> findWidgetTypesByWidgetsBundleId(TenantId tenantId, WidgetsBundleId widgetsBundleId) {
        log.trace("Executing findWidgetTypesByWidgetsBundleId, tenantId [{}], widgetsBundleId [{}]", tenantId, widgetsBundleId);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validateId(widgetsBundleId, id -> INCORRECT_WIDGETS_BUNDLE_ID + id);
        return widgetTypeDao.findWidgetTypesByWidgetsBundleId(tenantId.getId(), widgetsBundleId.getId());
    }

    @Override
    public List<WidgetTypeDetails> findWidgetTypesDetailsByWidgetsBundleId(TenantId tenantId, WidgetsBundleId widgetsBundleId) {
        log.trace("Executing findWidgetTypesDetailsByWidgetsBundleId, tenantId [{}], widgetsBundleId [{}]", tenantId, widgetsBundleId);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validateId(widgetsBundleId, id -> INCORRECT_WIDGETS_BUNDLE_ID + id);
        return widgetTypeDao.findWidgetTypesDetailsByWidgetsBundleId(tenantId.getId(), widgetsBundleId.getId());

    }

    @Override
    public PageData<WidgetTypeInfo> findWidgetTypesInfosByWidgetsBundleId(TenantId tenantId, WidgetsBundleId widgetsBundleId, boolean fullSearch,
                                                                          DeprecatedFilter deprecatedFilter, List<String> widgetTypes, PageLink pageLink) {
        log.trace("Executing findWidgetTypesInfosByWidgetsBundleId, tenantId [{}], widgetsBundleId [{}], fullSearch [{}], deprecatedFilter [{}], widgetTypes [{}], pageLink [{}]",
                tenantId, widgetsBundleId, fullSearch, deprecatedFilter, widgetTypes, pageLink);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validateId(widgetsBundleId, id -> INCORRECT_WIDGETS_BUNDLE_ID + id);
        Validator.validatePageLink(pageLink);
        return widgetTypeDao.findWidgetTypesInfosByWidgetsBundleId(tenantId.getId(), widgetsBundleId.getId(), fullSearch, deprecatedFilter, widgetTypes, pageLink);
    }

    @Override
    public List<String> findWidgetFqnsByWidgetsBundleId(TenantId tenantId, WidgetsBundleId widgetsBundleId) {
        log.trace("Executing findWidgetTypesInfosByWidgetsBundleId, tenantId [{}], widgetsBundleId [{}]", tenantId, widgetsBundleId);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validateId(widgetsBundleId, id -> INCORRECT_WIDGETS_BUNDLE_ID + id);
        return widgetTypeDao.findWidgetFqnsByWidgetsBundleId(tenantId.getId(), widgetsBundleId.getId());
    }

    @Override
    public WidgetType findWidgetTypeByTenantIdAndFqn(TenantId tenantId, String fqn) {
        log.trace("Executing findWidgetTypeByTenantIdAndFqn, tenantId [{}], fqn [{}]", tenantId, fqn);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validateString(fqn, f -> "Incorrect fqn " + f);
        return widgetTypeDao.findByTenantIdAndFqn(tenantId.getId(), fqn);
    }

    @Override
    public void updateWidgetsBundleWidgetTypes(TenantId tenantId, WidgetsBundleId widgetsBundleId, List<WidgetTypeId> widgetTypeIds) {
        log.trace("Executing updateWidgetsBundleWidgetTypes, tenantId [{}], widgetsBundleId [{}], widgetTypeIds [{}]", tenantId, widgetsBundleId, widgetTypeIds);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validateId(widgetsBundleId, id -> INCORRECT_WIDGETS_BUNDLE_ID + id);
        Validator.checkNotNull(widgetTypeIds, "Incorrect widgetTypeIds " + widgetTypeIds);
        if (!widgetTypeIds.isEmpty()) {
            validateIds(widgetTypeIds, ids -> "Incorrect widgetTypeIds " + ids);
        }
        List<WidgetsBundleWidget> bundleWidgets = new ArrayList<>();
        for (int index = 0; index < widgetTypeIds.size(); index++) {
            bundleWidgets.add(new WidgetsBundleWidget(widgetsBundleId, widgetTypeIds.get(index), index));
        }
        List<WidgetsBundleWidget> existingBundleWidgets = widgetTypeDao.findWidgetsBundleWidgetsByWidgetsBundleId(tenantId.getId(), widgetsBundleId.getId());
        List<WidgetTypeId> toRemove = existingBundleWidgets.stream()
                .map(WidgetsBundleWidget::getWidgetTypeId)
                .filter(widgetTypeId -> bundleWidgets.stream().noneMatch(newBundleWidget ->
                        newBundleWidget.getWidgetTypeId().equals(widgetTypeId))).collect(Collectors.toList());
        for (WidgetTypeId widgetTypeId : toRemove) {
            widgetTypeDao.removeWidgetTypeFromWidgetsBundle(widgetsBundleId.getId(), widgetTypeId.getId());
        }
        for (WidgetsBundleWidget widgetsBundleWidget : bundleWidgets) {
            widgetTypeDao.saveWidgetsBundleWidget(widgetsBundleWidget);
        }
        eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId)
                .entityId(widgetsBundleId).created(false).build());
    }

    @Override
    public void updateWidgetsBundleWidgetFqns(TenantId tenantId, WidgetsBundleId widgetsBundleId, List<String> widgetFqns) {
        log.trace("Executing updateWidgetsBundleWidgetFqns, tenantId [{}], widgetsBundleId [{}], widgetFqns [{}]", tenantId, widgetsBundleId, widgetFqns);
        List<WidgetTypeId> widgetTypeIds = widgetTypeDao.findWidgetTypeIdsByTenantIdAndFqns(tenantId.getId(), widgetFqns);
        this.updateWidgetsBundleWidgetTypes(tenantId, widgetsBundleId, widgetTypeIds);
    }

    @Override
    public void deleteWidgetTypesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteWidgetTypesByTenantId, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        tenantWidgetTypeRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        deleteWidgetTypesByTenantId(tenantId);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findWidgetTypeById(tenantId, new WidgetTypeId(entityId.getId())));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.WIDGET_TYPE;
    }

    private PaginatedRemover<TenantId, WidgetTypeInfo> tenantWidgetTypeRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<WidgetTypeInfo> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return widgetTypeDao.findTenantWidgetTypesByTenantId(id.getId(), false, DeprecatedFilter.ALL, null, pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, WidgetTypeInfo entity) {
                    deleteWidgetType(tenantId, new WidgetTypeId(entity.getUuidId()));
                }
            };

}
