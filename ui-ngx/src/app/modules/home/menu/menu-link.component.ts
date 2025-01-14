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

import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';
import { MenuSection } from '@core/services/menu.models';
import { getCurrentAuthUser } from '@core/auth/auth.selectors';
import { AuthUser } from '@shared/models/user.model';
import { Store } from '@ngrx/store';
import { AppState } from '@app/core/core.state';
@Component({
  selector: 'tb-menu-link',
  templateUrl: './menu-link.component.html',
  styleUrls: ['./menu-link.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MenuLinkComponent implements OnInit {

  @Input() section: MenuSection;
   private authUser: AuthUser;
 
  constructor(private store: Store<AppState>) {
    this.authUser = getCurrentAuthUser(this.store);
  }

  ngOnInit() {
    if (this.section.id === 'user_management') {
        this.section.path = `/tenants/${this.authUser.tenantId}/users`;
    }
}


}
