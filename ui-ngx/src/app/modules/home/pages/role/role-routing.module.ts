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

import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { RoleTabsComponent } from './role-tabs.component';
import { EntitiesTableComponent } from '../../components/entity/entities-table.component';
import { RoleTableConfigResolver } from './role-table-config.resolver';
import { MenuId } from '@app/core/public-api';

const routes: Routes = [
 {
    path: 'roles',
    data: {
    breadcrumb: {
        menuId: MenuId.role_management
      }
    },
  children: [
        {
          path: '',
          component: EntitiesTableComponent,
          data: {
            title: 'role.role'
          },
          resolve: {
            entitiesTableConfig: RoleTableConfigResolver
          }
        },
        
      ]
  }

];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule] , 
  providers:[
    RoleTableConfigResolver
  ]
})
export class RoleRoutingModule { }
