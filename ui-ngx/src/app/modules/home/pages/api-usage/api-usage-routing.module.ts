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

import { inject, NgModule } from '@angular/core';
import { ActivatedRouteSnapshot, ResolveFn, RouterModule, RouterStateSnapshot, Routes } from '@angular/router';
import { Authority } from '@shared/models/authority.enum';
import { ApiUsageComponent } from '@home/pages/api-usage/api-usage.component';
import { Dashboard } from '@shared/models/dashboard.models';
import { ResourcesService } from '@core/services/resources.service';
import { Observable } from 'rxjs';

const apiUsageDashboardJson = '/assets/dashboard/api_usage.json';

export const apiUsageDashboardResolver: ResolveFn<Dashboard> = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot,
  resourcesService = inject(ResourcesService)
): Observable<Dashboard> => resourcesService.loadJsonResource(apiUsageDashboardJson);

const routes: Routes = [
  {
    path: 'usage',
    component: ApiUsageComponent,
    data: {
      auth: [Authority.TENANT_ADMIN],
      title: 'api-usage.api-usage',
      breadcrumb: {
        label: 'api-usage.api-usage',
        icon: 'insert_chart'
      }
    },
    resolve: {
      apiUsageDashboard: apiUsageDashboardResolver
    }
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class ApiUsageRoutingModule { }
