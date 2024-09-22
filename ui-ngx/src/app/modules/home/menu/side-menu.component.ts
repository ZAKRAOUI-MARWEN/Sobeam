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

import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { MenuService } from '@core/services/menu.service';
import { MenuSection } from '@core/services/menu.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DashboardService } from '@core/http/dashboard.service';
import { Router } from '@angular/router';
@Component({
  selector: 'tb-side-menu',
  templateUrl: './side-menu.component.html',
  styleUrls: ['./side-menu.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SideMenuComponent implements OnInit {

  menuSections$: any;

  constructor(private router: Router,
    private store: Store<AppState>,
    private menuService: MenuService,
    private dashboardService: DashboardService) {
  }

  trackByMenuSection(index: number, section: MenuSection){
    return section.id;
  }

  ngOnInit() {
    this.menuSections$ = this.menuService.menuSections();
    this.menuService.refresh$.subscribe(value => {
      if(value){
      this.menuService=new MenuService(this.store, this.router, this.dashboardService);
      this.menuSections$ = null;
      this.menuSections$ = this.menuService.menuSections();
    }
    });
  }
  }


