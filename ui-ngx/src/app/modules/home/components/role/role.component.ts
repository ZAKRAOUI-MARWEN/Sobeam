import { ChangeDetectorRef, Component, Inject, Input, Optional } from '@angular/core';
import { FormArray, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { AppState } from '@app/core/core.state';
import { Store } from '@ngrx/store';
import { TranslateService } from '@ngx-translate/core';
import { EntityTableConfig } from '../../models/entity/entities-table-config.models';
import { EntityComponent } from '../entity/entity.component';
import { CdkDragDrop, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { ResourceOperations, Role } from '@app/shared/models/role.models';
import { EntityType } from '@shared/models/entity-type.models';
import { AssetService } from '@app/core/public-api';

@Component({
  selector: 'tb-role',
  templateUrl: './role.component.html',
  styleUrls: ['./role.component.scss']
})
export class RoleComponent extends EntityComponent<Role> {
  @Input() standalone = false;
  entityListConfig: any;
  roleTypes = [
    { value: 'GENERIC', label: 'Tenant' },
    { value: 'GROUP', label: 'Customer' }
  ];

  resourceOperations: { [key: string]: string[] } = {
    'ALARM': ['all', 'CREATE', 'READ', 'WRITE', 'DELETE'],
    'DASHBOARD': ['all', 'CREATE', 'READ', 'WRITE', 'DELETE'],

    'DEVICE': ['all', 'CREATE', 'READ', 'WRITE', 'DELETE'],
    'ASSET': ['all', 'CREATE', 'READ', 'WRITE', 'DELETE'],
    'ENTITY_VIEW': ['all', 'CREATE', 'READ', 'WRITE', 'DELETE'],

    'CUSTOMER': ['CREATE', 'READ', 'WRITE', 'DELETE'],

    //'USER': ['CREATE', 'READ', 'WRITE', 'DELETE'],
    //  'ROLE': ['CREATE', 'READ', 'WRITE', 'DELETE'],

    'RULE_CHAIN': ['all', 'CREATE', 'READ', 'WRITE', 'DELETE'],
    'EDGE': ['all', 'CREATE', 'READ', 'WRITE', 'DELETE'],
    'TB_RESOURCE': ['all', 'CREATE', 'READ', 'WRITE', 'DELETE'],
    'API_USAGE_STATE': ['all', 'CREATE', 'READ', 'WRITE', 'DELETE'],

    'WIDGETS_BUNDLE': ['all', 'CREATE', 'READ', 'WRITE', 'DELETE'],
    'WIDGET_TYPE': ['all', 'CREATE', 'READ', 'WRITE', 'DELETE'],



  };

  private tenantRoles = ['ALARM', 'DASHBOARD', 'DEVICE', 'ASSET', 'ENTITY_VIEW', 'CUSTOMER', 'RULE_CHAIN', 'EDGE', 'API_USAGE_STATE', 'WIDGETS_BUNDLE', 'TB_RESOURCE', 'WIDGET_TYPE'];
  private customerRoles = ['ALARM', 'DASHBOARD', 'DEVICE', 'ASSET', 'ENTITY_VIEW'];
  entityType = EntityType;

  availableRole: ResourceOperations[] = [];
  assignedRole: ResourceOperations[] = [];

  constructor(
    protected store: Store<AppState>,
    protected translate: TranslateService,
    @Optional() @Inject('entity') protected entityValue: Role,
    @Optional() @Inject('entitiesTableConfig') protected entitiesTableConfigValue: EntityTableConfig<Role>,
    protected fb: UntypedFormBuilder,
    protected cd: ChangeDetectorRef,
    private assetService: AssetService
  ) { super(store, fb, entityValue, entitiesTableConfigValue, cd); }


  ngOnInit() {
    super.ngOnInit();
    const assetControl = this.entityForm.get('permissions.assetPermission.resource');

    // Store asset-device relationships
    let assetDeviceMap = new Map<string, string[]>();

    assetControl.valueChanges.subscribe(currentAssets => {
        const previousAssets = Array.from(assetDeviceMap.keys());
        const addedAssets = currentAssets?.filter(asset => !previousAssets.includes(asset)) || [];
        const removedAssets = previousAssets.filter(asset => !currentAssets?.includes(asset));

        // Get current devices list
        let currentDevices = this.entityForm.get('permissions.devicePermission.resource').value || [];

        // Handle removed assets
        if (removedAssets.length > 0) {
            let devicesToRemove = new Set<string>();

            // First collect all devices from removed assets
            removedAssets.forEach(assetId => {
                const assetDevices = assetDeviceMap.get(assetId) || [];
                assetDevices.forEach(deviceId => devicesToRemove.add(deviceId));
                assetDeviceMap.delete(assetId);
            });

            // Then check remaining assets (if any) for shared devices
            const remainingAssets = Array.from(assetDeviceMap.values()).flat();
            devicesToRemove.forEach(deviceId => {
                if (remainingAssets.includes(deviceId)) {
                    devicesToRemove.delete(deviceId);
                }
            });

            // Update devices list
            if (devicesToRemove.size > 0) {
                currentDevices = currentDevices.filter(deviceId => !devicesToRemove.has(deviceId));
                this.entityForm.get('permissions.devicePermission.resource').patchValue(currentDevices, { emitEvent: false });
            }
        }

        // Handle added assets
        addedAssets.forEach(newAsset => {
            this.assetService.getAssetInfo(newAsset).subscribe(res => {
                const deviceIds = res.entityList.map(device => device.id);
                assetDeviceMap.set(newAsset, deviceIds);

                // Add only new devices without duplicates
                const updatedDevices = [...new Set([...currentDevices, ...deviceIds])];
                this.entityForm.get('permissions.devicePermission.resource').patchValue(updatedDevices, { emitEvent: false });
            });
        });
    });  
    
     this.entityForm.get('assignedUserIds').valueChanges.subscribe(type => {
      console.log(type)
    });
}



  buildForm(entity: Role): UntypedFormGroup {
    const form = this.fb.group({
      name: [entity ? entity.name : '', [Validators.required, Validators.maxLength(255)]],
      description: [entity ? entity.description : ''],
      type: [entity ? entity.type : ''],
      permissions: this.fb.group({
        menuPermission: this.fb.array([]),
        customerPermission: this.fb.control({}),
        assetPermission: this.fb.group({
          resource: [],
          operations: this.fb.array([])
        }),
        devicePermission: this.fb.group({
          resource: [null],
          operations: this.fb.array([])
        }),
        dashboardPermission: this.fb.group({
          resource: [null],
          operations: this.fb.array([])
        })
      })
    });

    form.get('type').valueChanges.subscribe(type => {
      this.updateRolesByType(type);
    });

    form.addControl('assignedUserIds', this.fb.control([]));
    return form;
  }

  updateForm(entity: Role) {
    // 1. Patch les valeurs de base
    this.entityForm.patchValue({ name: entity.name, description: entity.description, type: entity.type }, { emitEvent: false });

    // 2. Initialisation des rôles selon le type
    const allowedRoles = entity.type.toString() === 'GROUP' ? this.customerRoles : this.tenantRoles;

    // 3. Traitement des permissions de menu
    if (entity.permissions?.menuPermission) {
      // Filtrer et traiter les permissions de menu
      const filteredMenuPermissions = entity.permissions.menuPermission.filter(permission =>
        allowedRoles.includes(permission.resource)
      );

      // Mise à jour des rôles assignés
      this.assignedRole = filteredMenuPermissions.map(permission => ({
        resource: permission.resource,
        operations: this.resourceOperations[permission.resource] || [],
        operationStates: this.createOperationStates(permission.operations)
      }));

      // Mise à jour des rôles disponibles
      this.availableRole = allowedRoles
        .filter(role => !this.assignedRole.some(assigned => assigned.resource === role))
        .map(role => ({
          resource: role,
          operations: this.resourceOperations[role] || [],
          operationStates: {}
        }));

      // Mise à jour du FormArray des permissions
      this.updateMenuPermissionsFormArray();
    } else {
      this.assignedRole = [];
      this.availableRole = allowedRoles.map(role => ({
        resource: role,
        operations: this.resourceOperations[role] || [],
        operationStates: {}
      }));
    }

    // 4. Traitement des permissions spécifiques pour le type GENERIC
    if (entity.type.toString() === 'GENERIC' && entity.permissions) {
      this.patchSpecificPermissions(entity.permissions);
    }

    if (entity?.assignedUserIds) {
      const assignedUserIds = Array.isArray(entity.assignedUserIds)
        ? entity.assignedUserIds.map(user => {
          if (typeof user === 'object' && user?.id?.getId) {
            return user.id.getId().toString();
          }
          return user?.toString() || '';
        }).filter(id => id) : [];

      this.entityForm.get('assignedUserIds')?.patchValue(assignedUserIds);
    }
  }

  private updateRolesByType(type: string) {
    const roles = type === 'GROUP' ? this.customerRoles : this.tenantRoles;
    this.availableRole = roles.map(role => ({
      resource: role,
      operations: this.resourceOperations[role] || [],
      operationStates: {}
    }));
    this.assignedRole = [];
    this.updateMenuPermissionsFormArray();
  }

  private createOperationStates(operations: string | string[]): { [key: string]: boolean } {
    const operationStates = {};
    const ops = Array.isArray(operations) ? operations : [operations];
    ops.forEach(op => {
      operationStates[op] = true;
    });
    return operationStates;
  }

  drop(event: CdkDragDrop<ResourceOperations[]>) {
    if (event.previousContainer === event.container) {
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
    } else {
      const movedItem = event.previousContainer.data[event.previousIndex];
      transferArrayItem(
        event.previousContainer.data,
        event.container.data,
        event.previousIndex,
        event.currentIndex
      );
  
      console.log(movedItem.resource, "this is resource");
  
      // Vérifier d'abord si c'est une addition à la liste assignée
      if (event.container.id === 'assigned-list') {
        this.addRoleToPermissions(movedItem);
      } else {
        this.removeRoleFromPermissions(movedItem);
      }
  
      // Traiter le cas spécial de DASHBOARD après
      if (movedItem.resource === 'DASHBOARD' && (event.container.id === 'cdk-drop-list-1' || event.container.id === 'cdk-drop-list-3')) {
        // Find WIDGET_TYPE in available roles
        const widgetTypeIndex = this.availableRole.findIndex(role => role.resource === 'WIDGET_TYPE');
        
        if (widgetTypeIndex !== -1) {
          // Get and remove WIDGET_TYPE from available roles
          const widgetTypeRole = this.availableRole[widgetTypeIndex];
          this.availableRole.splice(widgetTypeIndex, 1);
          
          // Set up WIDGET_TYPE with READ operation
          widgetTypeRole.operationStates = {
            'READ': true
          };
          
          // Add to assigned roles
          this.assignedRole.push(widgetTypeRole);
          
          // Update form array with WIDGET_TYPE permission
          const permissionsArray = this.entityForm.get('permissions.menuPermission') as FormArray;
          permissionsArray.push(this.fb.group({
            resource: ['WIDGET_TYPE'],
            operations: [['READ']]
          }));
        }
      } else if (movedItem.resource === 'DASHBOARD') {
        // Gérer la suppression de WIDGET_TYPE quand DASHBOARD est retiré
        const widgetTypeIndex = this.assignedRole.findIndex(role => role.resource === 'WIDGET_TYPE');
        
        if (widgetTypeIndex !== -1) {
          // Remove WIDGET_TYPE from assigned roles
          this.assignedRole.splice(widgetTypeIndex, 1);
          
          // Add WIDGET_TYPE back to available roles
          this.availableRole.push({
            resource: 'WIDGET_TYPE',
            operations: this.resourceOperations['WIDGET_TYPE'] || [],
            operationStates: {}
          });
          
          // Remove WIDGET_TYPE from form permissions
          const permissionsArray = this.entityForm.get('permissions.menuPermission') as FormArray;
          const widgetTypePermissionIndex = permissionsArray.controls.findIndex(
            control => control.get('resource')?.value === 'WIDGET_TYPE'
          );
          if (widgetTypePermissionIndex !== -1) {
            permissionsArray.removeAt(widgetTypePermissionIndex);
          }
        }
      }
    }
  
    this.updateMenuPermissionsFormArray();
    this.cd.detectChanges();
  }
  private addRoleToPermissions(role: ResourceOperations) {
    if (!role.operationStates) {
      role.operationStates = {};
      role.operations.forEach(op => role.operationStates[op] = false);
    }
  }

  private removeRoleFromPermissions(role: ResourceOperations) {
    const permissionsArray = this.entityForm.get('permissions.menuPermission') as FormArray;
    for (let i = permissionsArray.length - 1; i >= 0; i--) {
      const control = permissionsArray.at(i);
      if (control.value.resource === role.resource) {
        permissionsArray.removeAt(i);
      }
    }
  }

  private updateMenuPermissionsFormArray() {
    const permissionsArray = this.entityForm.get('permissions.menuPermission') as FormArray;
    permissionsArray.clear();

    this.assignedRole.forEach(role => {
      const activeOperations = Object.entries(role.operationStates)
        .filter(([_, isActive]) => isActive)
        .map(([operation]) => operation);

      if (activeOperations.length > 0) {
        permissionsArray.push(this.fb.group({
          resource: [role.resource],
          operations: [activeOperations]
        }));
      }
    });
  }

  toggleOperation(role: ResourceOperations, operation: string) {
    if (!role.operationStates) {
      role.operationStates = {};
    }

    if (operation === 'all') {
      const newState = !this.isOperationActive(role, 'all');
      role.operations.forEach(op => {
        role.operationStates[op] = newState;
      });
    } else {
      role.operationStates[operation] = !role.operationStates[operation];

      // Update 'all' state based on other operations
      const allOthersSelected = role.operations
        .filter(op => op !== 'all')
        .every(op => role.operationStates[op]);

      role.operationStates['all'] = allOthersSelected;
    }

    this.updateMenuPermissionsFormArray();
    // Forcer la mise à jour du modèle
    this.entityForm.markAsDirty();


  }

  isOperationActive(role: ResourceOperations, operation: string): boolean {
    return role.operationStates?.[operation] ?? false;
  }

  getOperationStyle(role: ResourceOperations, operation: string): any {
    const isActive = this.isOperationActive(role, operation);
    return {
      'background-color': isActive ? 'rgba(127, 203, 206, 0.66)' : 'transparent',
      'transition': 'all 0.3s ease'
    };
  }

  getOperationIcon(operation: string): string {
    const iconMap: Record<string, string> = {
      'all': 'select_all',
      'CREATE': 'add',
      'READ': 'Visibility',
      'DELETE': 'delete',
      'WRITE': 'create'
    };
    return iconMap[operation];
  }

  onRoleTypeChange(event: any) {
    this.updateRolesByType(event.value);
    this.entityForm.get('permissions').reset();
    this.entityForm.get('assignedUserIds').reset();
  }

  // Specific Permissions Methods
  toggleOperationDashboard(operation: string): void {
    this.toggleOperation1('dashboardPermission', operation);
  }

  toggleOperationDevice(operation: string): void {
    this.toggleOperation1('devicePermission', operation);
  }

  toggleOperationAsset(operation: string): void {
    this.toggleOperation1('assetPermission', operation);
  }

  private toggleOperation1(permissionType: string, operation: string): void {
    const permissionsGroup = this.entityForm?.get('permissions');
    if (!permissionsGroup) {
      return;
    }

    const permissionControl = permissionsGroup.get(`${permissionType}`);
    if (!permissionControl) {
      return;
    }

    const operationsArray = permissionControl.get('operations') as FormArray;
    if (!operationsArray) {
      return;
    }

    const existingIndex = operationsArray.controls.findIndex(
      control => control.value === operation
    );

    if (existingIndex >= 0) {
      operationsArray.removeAt(existingIndex);
      if (operation === 'all') {
        while (operationsArray.length !== 0) {
          operationsArray.removeAt(0);
        }
      }
    } else {
      operationsArray.push(this.fb.control(operation));
      if (operation === 'all') {
        ['edit', 'view', 'delete'].forEach(op => {
          if (!this.isOperationActive1(permissionType, op)) {
            operationsArray.push(this.fb.control(op));
          }
        });
      } else {
        const operations = ['edit', 'view', 'delete'];
        const allOthersSelected = operations
          .filter(op => op !== operation)
          .every(op => this.isOperationActive1(permissionType, op));

        if (allOthersSelected && !this.isOperationActive1(permissionType, 'all')) {
          operationsArray.push(this.fb.control('all'));
        }
      }
    }

  }

  isOperationActive1(permissionType: string, operation: string): boolean {
    const operationsArray = this.entityForm.get(`permissions.${permissionType}.operations`) as FormArray;
    return operationsArray?.controls.some(control => control.value === operation) ?? false;
  }

  getOperationStyle1(permissionType: string, operation: string): any {
    const isActive = this.isOperationActive1(permissionType, operation);
    return {
      'background-color': isActive ? 'rgba(127, 203, 206, 0.66)' : 'transparent',
      'transition': 'all 0.3s ease'
    };
  }

  // Version incorrecte qui cause la récursion


  // Version corrigée sans récursion
  isListEmpty(type: string): boolean {
    const permissionsGroup = this.entityForm?.get('permissions');

    if (!permissionsGroup) {
      return true;
    }

    const permissionControl = permissionsGroup.get(`${type}Permission`);
    if (!permissionControl) {
      return true;
    }

    const resourceControl = permissionControl.get('resource');
    if (!resourceControl) {
      return true;
    }

    const resourceValue = resourceControl.value;
    return !resourceValue || resourceValue.length === 0;
  }

  // Nouvelle méthode pour gérer le reset séparément
  resetPermission(type: string): void {
    const permissionsGroup = this.entityForm?.get('permissions');
    if (!permissionsGroup) {
      return;
    }

    const permissionControl = permissionsGroup.get(`${type}Permission`);
    if (!permissionControl) {
      return;
    }

    // Reset les opérations
    const operationsArray = permissionControl.get('operations') as FormArray;
    if (operationsArray) {
      operationsArray.clear();
    }

    // Reset la ressource
    const resourceControl = permissionControl.get('resource');
    if (resourceControl) {
      resourceControl.patchValue(null, { emitEvent: false });
    }
  }

  private patchSpecificPermissions(permissions: any) {
    const permissionTypes = ['dashboardPermission', 'devicePermission', 'assetPermission'];

    permissionTypes.forEach(permType => {
      if (permissions[permType]) {
        const permission = permissions[permType];
        this.patchSpecificPermission(permType, permission);
      }
    });
  }

  private patchSpecificPermission(permissionType: string, permission: any) {
    const permissionGroup = this.entityForm.get(`permissions.${permissionType}`);
    if (!permissionGroup) {
      return;
    }

    permissionGroup.get('resource').patchValue(permission.resource || null);

    const operationsArray = permissionGroup.get('operations') as FormArray;
    operationsArray.clear();

    if (permission.operations) {
      const operations = Array.isArray(permission.operations) ?
        permission.operations :
        [permission.operations];

      operations.forEach(op => {
        operationsArray.push(this.fb.control(op));
      });
    }
  }

  getOperationTooltip(op: string): string {
    const tooltips: { [key: string]: string } = {
      all: "Affected all actions",
      CREATE: "Affected action create",
      edit: "Affected action Modifier",
      DELETE: "Affected action delete",
      READ: "Affected action read"
    };
    return tooltips[op] || "Action";
  }

  getFormattedOperationName(operation: string): string {
    // Remplacer les underscores par des espaces et mettre en majuscule la première lettre de chaque mot
    return operation
      .replace(/_/g, ' ')  // Remplacer "_" par un espace
      .toLowerCase()       // Mettre tout en minuscule
      .replace(/(?:^|\s)\S/g, (match) => match.toUpperCase()); // Mettre en majuscule la première lettre de chaque mot
  }


}