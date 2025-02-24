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

import { RoleId } from "./id/role-Id";
import { BaseData, HasTenantId, TenantId, UserId } from "./public-api";




export interface Role extends BaseData<RoleId>, HasTenantId {
  tenantId: TenantId;
  name: string;
  description?: string;
  type: 'tenant' | 'customer';
  permissions: any;
  assignedUserIds? : any[];

  }

  export enum Resource {
    ALARM = 'Alarm',
    ASSET = 'Asset',
    ASSETPROFIL = 'Asset Profil'
  }
  
 export interface ResourceOperations {
  resource: string;
  operations: string[];
  operationStates?: { [key: string]: boolean };
}
