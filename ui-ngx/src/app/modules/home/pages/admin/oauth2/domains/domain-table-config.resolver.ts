///
/// Copyright © 2016-2024 The Sobeam Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, Resolve } from '@angular/router';
import {
  DateEntityTableColumn,
  EntityActionTableColumn,
  EntityChipsEntityTableColumn,
  EntityTableColumn,
  EntityTableConfig
} from '@home/models/entity/entities-table-config.models';
import { DomainInfo } from '@shared/models/oauth2.models';
import { UtilsService } from '@core/services/utils.service';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { DomainService } from '@app/core/http/domain.service';
import { DomainComponent } from '@home/pages/admin/oauth2/domains/domain.component';
import { isEqual } from '@core/utils';
import { DomainTableHeaderComponent } from '@home/pages/admin/oauth2/domains/domain-table-header.component';
import { Direction } from '@app/shared/models/page/sort-order';
import { map, Observable, of, mergeMap } from 'rxjs';

@Injectable()
export class DomainTableConfigResolver implements Resolve<EntityTableConfig<DomainInfo>> {

  private readonly config: EntityTableConfig<DomainInfo> = new EntityTableConfig<DomainInfo>();

  constructor(private translate: TranslateService,
              private datePipe: DatePipe,
              private utilsService: UtilsService,
              private domainService: DomainService) {
    this.config.tableTitle = this.translate.instant('admin.oauth2.domains');
    this.config.selectionEnabled = false;
    this.config.entityType = EntityType.DOMAIN;
    this.config.rowPointer = true;
    this.config.entityTranslations = entityTypeTranslations.get(EntityType.DOMAIN);
    this.config.entityResources = entityTypeResources.get(EntityType.DOMAIN);
    this.config.entityComponent = DomainComponent;
    this.config.headerComponent = DomainTableHeaderComponent;
    this.config.addDialogStyle = {width: '850px', maxHeight: '100vh'};
    this.config.defaultSortOrder = {property: 'createdTime', direction: Direction.DESC};

    this.config.columns.push(
      new DateEntityTableColumn<DomainInfo>('createdTime', 'common.created-time', this.datePipe, '170px'),
      new EntityTableColumn<DomainInfo>('name', 'admin.oauth2.domain-name', '170px'),
      new EntityChipsEntityTableColumn<DomainInfo>('oauth2ClientInfos', 'admin.oauth2.clients', '40%'),
      new EntityActionTableColumn('oauth2Enabled', 'admin.oauth2.enable',
        {
          name: '',
          nameFunction: (domain) =>
            this.translate.instant(domain.oauth2Enabled ? 'admin.oauth2.disable' : 'admin.oauth2.enable'),
          icon: 'mdi:toggle-switch',
          iconFunction: (domain) => domain.oauth2Enabled ? 'mdi:toggle-switch' : 'mdi:toggle-switch-off-outline',
          isEnabled: () => true,
          onAction: ($event, entity) => this.toggleEnableOAuth($event, entity)
        }),
      new EntityActionTableColumn('propagateToEdge', 'admin.oauth2.edge',
        {
          name: '',
          nameFunction: (domain) =>
            this.translate.instant(domain.propagateToEdge ? 'admin.oauth2.edge-disable' : 'admin.oauth2.edge-enable'),
          icon: 'mdi:toggle-switch',
          iconFunction: (entity) => entity.propagateToEdge ? 'mdi:toggle-switch' : 'mdi:toggle-switch-off-outline',
          isEnabled: () => true,
          onAction: ($event, entity) => this.togglePropagateToEdge($event, entity)
        })
    );

    this.config.deleteEntityTitle = (domain) => this.translate.instant('admin.oauth2.delete-domain-title', {domainName: domain.name});
    this.config.deleteEntityContent = () => this.translate.instant('admin.oauth2.delete-domain-text');
    this.config.entitiesFetchFunction = pageLink => this.domainService.getTenantDomainInfos(pageLink);
    this.config.loadEntity = id => this.domainService.getDomainInfoById(id.id);
    this.config.saveEntity = (domain, originalDomain) => {
      const clientsIds = domain.oauth2ClientInfos as Array<string> || [];
      let clientsTask: Observable<void>;
      if (domain.id && !isEqual(domain.oauth2ClientInfos?.sort(),
        originalDomain.oauth2ClientInfos?.map(info => info.id ? info.id.id : info).sort())) {
        clientsTask = this.domainService.updateOauth2Clients(domain.id.id, clientsIds);
      } else {
        clientsTask = of(null);
      }
      delete domain.oauth2ClientInfos;
      return clientsTask.pipe(
        mergeMap(() => this.domainService.saveDomain(domain, domain.id ? [] : clientsIds)),
        map(savedDomain => {
          (savedDomain as DomainInfo).oauth2ClientInfos = clientsIds;
          return savedDomain;
        })
      );
    };
    this.config.deleteEntity = id => this.domainService.deleteDomain(id.id);
  }

  resolve(route: ActivatedRouteSnapshot): EntityTableConfig<DomainInfo> {
    return this.config;
  }

  private toggleEnableOAuth($event: Event, domain: DomainInfo): void {
    if ($event) {
      $event.stopPropagation();
    }

    const modifiedDomain: DomainInfo = {
      ...domain,
      oauth2Enabled: !domain.oauth2Enabled
    };

    this.domainService.saveDomain(modifiedDomain, domain.oauth2ClientInfos.map(clientInfo => clientInfo.id.id),
      {ignoreLoading: true})
      .subscribe((result) => {
        domain.oauth2Enabled = result.oauth2Enabled;
        this.config.getTable().detectChanges();
      });
  }

  private togglePropagateToEdge($event: Event, domain: DomainInfo): void {
    if ($event) {
      $event.stopPropagation();
    }

    const modifiedDomain: DomainInfo = {
      ...domain,
      propagateToEdge: !domain.propagateToEdge
    };

    this.domainService.saveDomain(modifiedDomain, domain.oauth2ClientInfos.map(clientInfo => clientInfo.id.id),
      {ignoreLoading: true})
      .subscribe((result) => {
        domain.propagateToEdge = result.propagateToEdge;
        this.config.getTable().detectChanges();
      });
  }

}