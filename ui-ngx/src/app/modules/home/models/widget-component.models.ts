///
/// Copyright © 2024 The Sobeam Authors
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

import { IDashboardComponent } from '@home/models/dashboard-component.models';
import {
  DataSet,
  Datasource,
  DatasourceData,
  FormattedData,
  fullWidgetTypeFqn,
  JsonSettingsSchema,
  Widget,
  WidgetActionDescriptor,
  WidgetActionSource,
  WidgetConfig,
  WidgetControllerDescriptor,
  WidgetType,
  widgetType,
  WidgetTypeDescriptor,
  WidgetTypeDetails,
  widgetTypeFqn,
  WidgetTypeParameters
} from '@shared/models/widget.models';
import { Timewindow, WidgetTimewindow } from '@shared/models/time/time.models';
import {
  IAliasController,
  IStateController,
  IWidgetSubscription,
  IWidgetUtils,
  RpcApi,
  StateParams,
  SubscriptionEntityInfo,
  TimewindowFunctions,
  WidgetActionsApi,
  WidgetSubscriptionApi
} from '@core/api/widget-api.models';
import { ChangeDetectorRef, Injector, NgModuleRef, NgZone, Type } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { RafService } from '@core/services/raf.service';
import { WidgetTypeId } from '@shared/models/id/widget-type-id';
import { TenantId } from '@shared/models/id/tenant-id';
import { WidgetLayout } from '@shared/models/dashboard.models';
import {
  createLabelFromDatasource,
  createLabelFromSubscriptionEntityInfo,
  formatValue,
  getEntityDetailsPageURL,
  hasDatasourceLabelsVariables,
  isDefined
} from '@core/utils';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  NotificationHorizontalPosition,
  NotificationType,
  NotificationVerticalPosition
} from '@core/notification/notification.models';
import { ActionNotificationHide, ActionNotificationShow } from '@core/notification/notification.actions';
import { AuthUser } from '@shared/models/user.model';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { DeviceService } from '@core/http/device.service';
import { AssetService } from '@core/http/asset.service';
import { EntityViewService } from '@core/http/entity-view.service';
import { CustomerService } from '@core/http/customer.service';
import { DashboardService } from '@core/http/dashboard.service';
import { UserService } from '@core/http/user.service';
import { AttributeService } from '@core/http/attribute.service';
import { EntityRelationService } from '@core/http/entity-relation.service';
import { EntityService } from '@core/http/entity.service';
import { DialogService } from '@core/services/dialog.service';
import { CustomDialogService } from '@home/components/widget/dialog/custom-dialog.service';
import { AuthService } from '@core/auth/auth.service';
import { ResourceService } from '@core/http/resource.service';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { DatePipe } from '@angular/common';
import { TranslateService } from '@ngx-translate/core';
import { PageLink, TimePageLink } from '@shared/models/page/page-link';
import { SortOrder } from '@shared/models/page/sort-order';
import { DomSanitizer } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { EdgeService } from '@core/http/edge.service';
import * as RxJS from 'rxjs';
import { BehaviorSubject, Observable } from 'rxjs';
import * as RxJSOperators from 'rxjs/operators';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { EntityId } from '@shared/models/id/entity-id';
import { AlarmQuery, AlarmSearchStatus, AlarmStatus } from '@app/shared/models/alarm.models';
import { ImagePipe, MillisecondsToTimeStringPipe, TelemetrySubscriber } from '@app/shared/public-api';
import { UserId } from '@shared/models/id/user-id';
import { UserSettingsService } from '@core/http/user-settings.service';
import { DynamicComponentModule } from '@core/services/dynamic-component-factory.service';
import { DataKeySettingsFunction } from '@home/components/widget/config/data-keys.component.models';
import { UtilsService } from '@core/services/utils.service';

export interface IWidgetAction {
  name: string;
  icon: string;
  onAction: ($event: Event) => void;
}

export type ShowWidgetHeaderActionFunction = (ctx: WidgetContext, data: FormattedData[]) => boolean;

export interface WidgetHeaderAction extends IWidgetAction {
  displayName: string;
  descriptor: WidgetActionDescriptor;
  useShowWidgetHeaderActionFunction: boolean;
  showWidgetHeaderActionFunction: ShowWidgetHeaderActionFunction;
}

export interface WidgetAction extends IWidgetAction {
  show: boolean;
}

export interface IDashboardWidget {
  updateWidgetParams(): void;
}

export class WidgetContext {

  constructor(public dashboard: IDashboardComponent,
              private dashboardWidget: IDashboardWidget,
              private widget: Widget,
              public parentDashboard?: IDashboardComponent,
              public popoverComponent?: TbPopoverComponent) {}

  get stateController(): IStateController {
    return this.parentDashboard ? this.parentDashboard.stateController : this.dashboard.stateController;
  }

  get aliasController(): IAliasController {
    return this.dashboard.aliasController;
  }

  get dashboardTimewindow(): Timewindow {
    return this.dashboard.dashboardTimewindow;
  }

  get widgetConfig(): WidgetConfig {
    return this.widget.config;
  }

  get settings(): any {
    return this.widget.config.settings;
  }

  get units(): string {
    return this.widget.config.units || '';
  }

  get decimals(): number {
    return isDefined(this.widget.config.decimals) ? this.widget.config.decimals : 2;
  }

  set changeDetector(cd: ChangeDetectorRef) {
    this.changeDetectorValue = cd;
  }

  set containerChangeDetector(cd: ChangeDetectorRef) {
    this.containerChangeDetectorValue = cd;
  }

  get currentUser(): AuthUser {
    if (this.store) {
      return getCurrentAuthUser(this.store);
    } else {
      return null;
    }
  }

  authService: AuthService;
  deviceService: DeviceService;
  assetService: AssetService;
  entityViewService: EntityViewService;
  edgeService: EdgeService;
  customerService: CustomerService;
  dashboardService: DashboardService;
  userService: UserService;
  attributeService: AttributeService;
  entityRelationService: EntityRelationService;
  entityService: EntityService;
  dialogs: DialogService;
  customDialog: CustomDialogService;
  resourceService: ResourceService;
  userSettingsService: UserSettingsService;
  utilsService: UtilsService;
  telemetryWsService: TelemetryWebsocketService;
  telemetrySubscribers?: TelemetrySubscriber[];
  date: DatePipe;
  imagePipe: ImagePipe;
  milliSecondsToTimeString: MillisecondsToTimeStringPipe;
  translate: TranslateService;
  http: HttpClient;
  sanitizer: DomSanitizer;
  router: Router;

  private changeDetectorValue: ChangeDetectorRef;
  private containerChangeDetectorValue: ChangeDetectorRef;

  inited = false;
  destroyed = false;

  subscriptions: {[id: string]: IWidgetSubscription} = {};
  defaultSubscription: IWidgetSubscription = null;

  labelPatterns = new Map<Observable<string>, LabelVariablePattern>();

  timewindowFunctions: TimewindowFunctions = {
    onUpdateTimewindow: (startTimeMs, endTimeMs, interval) => {
      if (this.defaultSubscription) {
        this.defaultSubscription.onUpdateTimewindow(startTimeMs, endTimeMs, interval);
      }
    },
    onResetTimewindow: () => {
      if (this.defaultSubscription) {
        this.defaultSubscription.onResetTimewindow();
      }
    }
  };

  controlApi: RpcApi = {
    sendOneWayCommand: (method, params, timeout, persistent,
                        retries, additionalInfo, requestUUID) => {
      if (this.defaultSubscription) {
        return this.defaultSubscription.sendOneWayCommand(method, params, timeout, persistent, retries, additionalInfo, requestUUID);
      } else {
        return RxJS.of(null);
      }
    },
    sendTwoWayCommand: (method, params, timeout, persistent,
                        retries, additionalInfo, requestUUID) => {
      if (this.defaultSubscription) {
        return this.defaultSubscription.sendTwoWayCommand(method, params, timeout, persistent, retries, additionalInfo, requestUUID);
      } else {
        return RxJS.of(null);
      }
    },
    completedCommand: () => {
      if (this.defaultSubscription) {
        return this.defaultSubscription.completedCommand();
      } else {
        return RxJS.of(null);
      }
    }
  };

  utils: IWidgetUtils = {
    formatValue,
    getEntityDetailsPageURL
  };

  $widgetElement: JQuery<HTMLElement>;
  $container: JQuery<HTMLElement>;
  $containerParent: JQuery<HTMLElement>;
  width: number;
  height: number;
  $scope: IDynamicWidgetComponent;
  isEdit: boolean;
  isPreview: boolean;
  isMobile: boolean;
  toastTargetId: string;

  widgetNamespace?: string;
  subscriptionApi?: WidgetSubscriptionApi;

  actionsApi?: WidgetActionsApi;
  activeEntityInfo?: SubscriptionEntityInfo;

  datasources?: Array<Datasource>;
  data?: Array<DatasourceData>;
  latestData?: Array<DatasourceData>;
  hiddenData?: Array<{data: DataSet}>;
  timeWindow?: WidgetTimewindow;

  embedTitlePanel?: boolean;
  overflowVisible?: boolean;

  hideTitlePanel = false;

  widgetTitle?: string;
  widgetTitleTooltip?: string;
  customHeaderActions?: Array<WidgetHeaderAction>;
  widgetActions?: Array<WidgetAction>;

  servicesMap?: Map<string, Type<any>>;

  $injector?: Injector;

  ngZone?: NgZone;

  store?: Store<AppState>;

  private popoverComponents: TbPopoverComponent[] = [];

  rxjs = {

    ...RxJS,
    ...RxJSOperators
  };

  registerPopoverComponent(popoverComponent: TbPopoverComponent) {
    this.popoverComponents.push(popoverComponent);
    popoverComponent.tbDestroy.subscribe(() => {
      const index = this.popoverComponents.indexOf(popoverComponent, 0);
      if (index > -1) {
        this.popoverComponents.splice(index, 1);
      }
    });
  }

  updatePopoverPositions() {
    this.popoverComponents.forEach(comp => {
      comp.updatePosition();
    });
  }

  setPopoversHidden(hidden: boolean) {
    this.popoverComponents.forEach(comp => {
      comp.tbHidden = hidden;
    });
  }

  registerLabelPattern(label: string, label$: Observable<string>): Observable<string> {
    let labelPattern = label$ ? this.labelPatterns.get(label$) : null;
    if (labelPattern) {
      labelPattern.setupPattern(label);
    } else {
      labelPattern = new LabelVariablePattern(label, this);
      this.labelPatterns.set(labelPattern.label$, labelPattern);
    }
    return labelPattern.label$;
  }

  updateLabelPatterns() {
    for (const labelPattern of this.labelPatterns.values()) {
      labelPattern.update();
    }
  }

  showSuccessToast(message: string, duration: number = 1000,
                   verticalPosition: NotificationVerticalPosition = 'bottom',
                   horizontalPosition: NotificationHorizontalPosition = 'left',
                   target: string = 'dashboardRoot', modern = false) {
    this.showToast('success', message, duration, verticalPosition, horizontalPosition, target, modern);
  }

  showInfoToast(message: string,
                verticalPosition: NotificationVerticalPosition = 'bottom',
                horizontalPosition: NotificationHorizontalPosition = 'left',
                target: string = 'dashboardRoot', modern = false) {
    this.showToast('info', message, undefined, verticalPosition, horizontalPosition, target, modern);
  }

  showWarnToast(message: string,
                verticalPosition: NotificationVerticalPosition = 'bottom',
                horizontalPosition: NotificationHorizontalPosition = 'left',
                target: string = 'dashboardRoot', modern = false) {
    this.showToast('warn', message, undefined, verticalPosition, horizontalPosition, target, modern);
  }

  showErrorToast(message: string,
                 verticalPosition: NotificationVerticalPosition = 'bottom',
                 horizontalPosition: NotificationHorizontalPosition = 'left',
                 target: string = 'dashboardRoot', modern = false) {
    this.showToast('error', message, undefined, verticalPosition, horizontalPosition, target, modern);
  }

  showToast(type: NotificationType, message: string, duration: number,
            verticalPosition: NotificationVerticalPosition = 'bottom',
            horizontalPosition: NotificationHorizontalPosition = 'left',
            target: string = 'dashboardRoot', modern = false) {
    this.store.dispatch(new ActionNotificationShow(
      {
        message,
        type,
        duration,
        verticalPosition,
        horizontalPosition,
        target,
        panelClass: this.widgetNamespace,
        forceDismiss: true,
        modern
      }));
  }

  hideToast(target?: string) {
    this.store.dispatch(new ActionNotificationHide(
      {
        target,
      }));
  }

  detectChanges(updateWidgetParams: boolean = false) {
    if (!this.destroyed) {
      if (updateWidgetParams) {
        this.dashboardWidget.updateWidgetParams();
      }
      try {
        this.changeDetectorValue.detectChanges();
      } catch (e) {
        // console.log(e);
      }
    }
  }

  detectContainerChanges() {
    if (!this.destroyed) {
      try {
        this.containerChangeDetectorValue.detectChanges();
      } catch (e) {
        // console.log(e);
      }
    }
  }

  updateWidgetParams() {
    if (!this.destroyed) {
      setTimeout(() => {
        this.dashboardWidget.updateWidgetParams();
      }, 0);
    }
  }

  updateAliases(aliasIds?: Array<string>) {
    this.aliasController.updateAliases(aliasIds);
  }

  reset() {
    this.destroyed = false;
    this.hideTitlePanel = false;
    this.widgetTitle = undefined;
    this.widgetActions = undefined;
  }

  destroy() {
    for (const labelPattern of this.labelPatterns.values()) {
      labelPattern.destroy();
    }
    this.labelPatterns.clear();
    this.destroyed = true;
  }

  closeDialog(resultData: any = null) {
    const dialogRef = this.$scope.dialogRef || this.stateController.dashboardCtrl.dashboardCtx.getDashboard().dialogRef;
    if (dialogRef) {
      dialogRef.close(resultData);
    }
  }

  pageLink(pageSize: number, page: number = 0, textSearch: string = null, sortOrder: SortOrder = null): PageLink {
    return new PageLink(pageSize, page, textSearch, sortOrder);
  }

  timePageLink(startTime: number, endTime: number, pageSize: number, page: number = 0,
               textSearch: string = null, sortOrder: SortOrder = null) {
    return new TimePageLink(pageSize, page, textSearch, sortOrder, startTime, endTime);
  }

  alarmQuery(entityId: EntityId, pageLink: TimePageLink, searchStatus: AlarmSearchStatus,
             status: AlarmStatus, fetchOriginator: boolean, assigneeId: UserId) {
    return new AlarmQuery(entityId, pageLink, searchStatus, status, fetchOriginator, assigneeId);
  }
}

export class LabelVariablePattern {

  private pattern: string;
  private hasVariables: boolean;

  private labelSubject = new BehaviorSubject<string>('');

  public label$ = this.labelSubject.asObservable();

  constructor(label: string,
              private ctx: WidgetContext) {
    this.setupPattern(label);
  }

  setupPattern(label: string) {
    this.pattern = this.ctx.dashboard.utils.customTranslation(label, label);
    this.hasVariables = hasDatasourceLabelsVariables(this.pattern);
    this.update();
  }

  update() {
    let label = this.pattern;
    if (this.hasVariables) {
      if (this.ctx.defaultSubscription?.type === widgetType.rpc) {
        const entityInfo = this.ctx.defaultSubscription.getFirstEntityInfo();
        label = createLabelFromSubscriptionEntityInfo(entityInfo, label);
      } else {
        const datasource = this.ctx.defaultSubscription?.firstDatasource;
        label = createLabelFromDatasource(datasource, label);
      }
    }
    if (this.labelSubject.value !== label) {
      this.labelSubject.next(label);
    }
  }

  destroy() {
    this.labelSubject.complete();
  }
}

export interface IDynamicWidgetComponent {
  readonly ctx: WidgetContext;
  readonly errorMessages: string[];
  readonly $injector: Injector;
  executingRpcRequest: boolean;
  rpcEnabled: boolean;
  rpcErrorText: string;
  rpcRejection: HttpErrorResponse | Error;
  raf: RafService;
  [key: string]: any;
}

export interface WidgetInfo extends WidgetTypeDescriptor, WidgetControllerDescriptor {
  widgetName: string;
  fullFqn: string;
  deprecated: boolean;
  typeSettingsSchema?: string | any;
  typeDataKeySettingsSchema?: string | any;
  typeLatestDataKeySettingsSchema?: string | any;
  image?: string;
  description?: string;
  tags?: string[];
  componentType?: Type<IDynamicWidgetComponent>;
  componentModuleRef?: NgModuleRef<DynamicComponentModule>;
}

export interface WidgetConfigComponentData {
  widgetName: string;
  config: WidgetConfig;
  layout: WidgetLayout;
  widgetType: widgetType;
  typeParameters: WidgetTypeParameters;
  actionSources: {[actionSourceId: string]: WidgetActionSource};
  isDataEnabled: boolean;
  settingsSchema: JsonSettingsSchema;
  dataKeySettingsSchema: JsonSettingsSchema;
  latestDataKeySettingsSchema: JsonSettingsSchema;
  dataKeySettingsFunction: DataKeySettingsFunction;
  settingsDirective: string;
  dataKeySettingsDirective: string;
  latestDataKeySettingsDirective: string;
  hasBasicMode: boolean;
  basicModeDirective: string;
}

export const MissingWidgetType: WidgetInfo = {
  type: widgetType.latest,
  widgetName: 'Widget type not found',
  fullFqn: 'undefined',
  deprecated: false,
  sizeX: 8,
  sizeY: 6,
  resources: [],
  templateHtml: '<div class="tb-widget-error-container">' +
    '<div class="tb-widget-error-msg" innerHTML="{{\'widget.widget-type-not-found\' | translate }}"></div>' +
    '</div>',
  templateCss: '',
  controllerScript: 'self.onInit = function() {}',
  settingsSchema: '{}\n',
  dataKeySettingsSchema: '{}\n',
  image: null,
  description: null,
  defaultConfig: '{\n' +
    '"title": "Widget type not found",\n' +
    '"datasources": [],\n' +
    '"settings": {}\n' +
    '}\n',
  typeParameters: {}
};

export const ErrorWidgetType: WidgetInfo = {
  type: widgetType.latest,
  widgetName: 'Error loading widget',
  fullFqn: 'error',
  deprecated: false,
  sizeX: 8,
  sizeY: 6,
  resources: [],
  templateHtml: '<div class="tb-widget-error-container">' +
                   '<div translate class="tb-widget-error-msg">widget.widget-type-load-error</div>' +
                   '<div *ngFor="let error of errorMessages" class="tb-widget-error-msg">{{ error }}</div>' +
                '</div>',
  templateCss: '',
  controllerScript: 'self.onInit = function() {}',
  settingsSchema: '{}\n',
  dataKeySettingsSchema: '{}\n',
  image: null,
  description: null,
  defaultConfig: '{\n' +
    '"title": "Widget failed to load",\n' +
    '"datasources": [],\n' +
    '"settings": {}\n' +
    '}\n',
  typeParameters: {}
};

export interface WidgetTypeInstance {
  getSettingsSchema?: () => string;
  getDataKeySettingsSchema?: () => string;
  getLatestDataKeySettingsSchema?: () => string;
  typeParameters?: () => WidgetTypeParameters;
  useCustomDatasources?: () => boolean;
  actionSources?: () => {[actionSourceId: string]: WidgetActionSource};

  onInit?: () => void;
  onDataUpdated?: () => void;
  onLatestDataUpdated?: () => void;
  onResize?: () => void;
  onEditModeChanged?: () => void;
  onMobileModeChanged?: () => void;
  onDestroy?: () => void;
}

export const toWidgetInfo = (widgetTypeEntity: WidgetType): WidgetInfo => ({
  widgetName: widgetTypeEntity.name,
  fullFqn: fullWidgetTypeFqn(widgetTypeEntity),
  deprecated: widgetTypeEntity.deprecated,
  type: widgetTypeEntity.descriptor.type,
  sizeX: widgetTypeEntity.descriptor.sizeX,
  sizeY: widgetTypeEntity.descriptor.sizeY,
  resources: widgetTypeEntity.descriptor.resources,
  templateHtml: widgetTypeEntity.descriptor.templateHtml,
  templateCss: widgetTypeEntity.descriptor.templateCss,
  controllerScript: widgetTypeEntity.descriptor.controllerScript,
  settingsSchema: widgetTypeEntity.descriptor.settingsSchema,
  dataKeySettingsSchema: widgetTypeEntity.descriptor.dataKeySettingsSchema,
  latestDataKeySettingsSchema: widgetTypeEntity.descriptor.latestDataKeySettingsSchema,
  settingsDirective: widgetTypeEntity.descriptor.settingsDirective,
  dataKeySettingsDirective: widgetTypeEntity.descriptor.dataKeySettingsDirective,
  latestDataKeySettingsDirective: widgetTypeEntity.descriptor.latestDataKeySettingsDirective,
  hasBasicMode: widgetTypeEntity.descriptor.hasBasicMode,
  basicModeDirective: widgetTypeEntity.descriptor.basicModeDirective,
  defaultConfig: widgetTypeEntity.descriptor.defaultConfig
});

export const detailsToWidgetInfo = (widgetTypeDetailsEntity: WidgetTypeDetails): WidgetInfo => {
  const widgetInfo = toWidgetInfo(widgetTypeDetailsEntity);
  widgetInfo.image = widgetTypeDetailsEntity.image;
  widgetInfo.description = widgetTypeDetailsEntity.description;
  widgetInfo.tags = widgetTypeDetailsEntity.tags;
  return widgetInfo;
};

export const toWidgetType = (widgetInfo: WidgetInfo, id: WidgetTypeId, tenantId: TenantId,
                             createdTime: number): WidgetType => {
  const descriptor: WidgetTypeDescriptor = {
    type: widgetInfo.type,
    sizeX: widgetInfo.sizeX,
    sizeY: widgetInfo.sizeY,
    resources: widgetInfo.resources,
    templateHtml: widgetInfo.templateHtml,
    templateCss: widgetInfo.templateCss,
    controllerScript: widgetInfo.controllerScript,
    settingsSchema: widgetInfo.settingsSchema,
    dataKeySettingsSchema: widgetInfo.dataKeySettingsSchema,
    latestDataKeySettingsSchema: widgetInfo.latestDataKeySettingsSchema,
    settingsDirective: widgetInfo.settingsDirective,
    dataKeySettingsDirective: widgetInfo.dataKeySettingsDirective,
    latestDataKeySettingsDirective: widgetInfo.latestDataKeySettingsDirective,
    hasBasicMode: widgetInfo.hasBasicMode,
    basicModeDirective: widgetInfo.basicModeDirective,
    defaultConfig: widgetInfo.defaultConfig
  };
  return {
    id,
    tenantId,
    createdTime,
    fqn: widgetTypeFqn(widgetInfo.fullFqn),
    name: widgetInfo.widgetName,
    deprecated: widgetInfo.deprecated,
    descriptor
  };
};

export const toWidgetTypeDetails = (widgetInfo: WidgetInfo, id: WidgetTypeId, tenantId: TenantId,
                                    createdTime: number): WidgetTypeDetails => {
  const widgetTypeEntity = toWidgetType(widgetInfo, id, tenantId, createdTime);
  return {
    ...widgetTypeEntity,
    description: widgetInfo.description,
    tags: widgetInfo.tags,
    image: widgetInfo.image
  };
};

export const updateEntityParams = (params: StateParams, targetEntityParamName?: string, targetEntityId?: EntityId,
                                   entityName?: string, entityLabel?: string) => {
  if (targetEntityId) {
    let targetEntityParams: StateParams;
    if (targetEntityParamName && targetEntityParamName.length) {
      targetEntityParams = params[targetEntityParamName];
      if (!targetEntityParams) {
        targetEntityParams = {};
        params[targetEntityParamName] = targetEntityParams;
        params.targetEntityParamName = targetEntityParamName;
      }
    } else {
      targetEntityParams = params;
    }
    targetEntityParams.entityId = targetEntityId;
    if (entityName) {
      targetEntityParams.entityName = entityName;
    }
    if (entityLabel) {
      targetEntityParams.entityLabel = entityLabel;
    }
  }
};
