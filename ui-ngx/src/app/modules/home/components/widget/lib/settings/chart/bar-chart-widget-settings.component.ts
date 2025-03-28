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

import { Component, TemplateRef, ViewChild } from '@angular/core';
import { WidgetSettings } from '@shared/models/widget.models';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import {
  barChartWidgetDefaultSettings,
  BarChartWidgetSettings
} from '@home/components/widget/lib/chart/bar-chart-widget.models';
import {
  LatestChartWidgetSettingsComponent
} from '@home/components/widget/lib/settings/chart/latest-chart-widget-settings.component';

@Component({
  selector: 'tb-bar-chart-widget-settings',
  templateUrl: './latest-chart-widget-settings.component.html',
  styleUrls: ['./../widget-settings.scss']
})
export class BarChartWidgetSettingsComponent extends LatestChartWidgetSettingsComponent<BarChartWidgetSettings> {

  @ViewChild('barChart')
  barChartConfigTemplate: TemplateRef<any>;

  constructor(protected store: Store<AppState>,
              protected fb: UntypedFormBuilder) {
    super(store, fb);
  }

  protected defaultLatestChartSettings() {
    return barChartWidgetDefaultSettings;
  }

  public latestChartConfigTemplate(): TemplateRef<any> {
    return this.barChartConfigTemplate;
  }

  protected setupLatestChartControls(latestChartWidgetSettingsForm: UntypedFormGroup, settings: WidgetSettings) {
    latestChartWidgetSettingsForm.addControl('barSettings', this.fb.control(settings.barSettings, []));
    latestChartWidgetSettingsForm.addControl('axisMin', this.fb.control(settings.axisMin, []));
    latestChartWidgetSettingsForm.addControl('axisMax', this.fb.control(settings.axisMax, []));
    latestChartWidgetSettingsForm.addControl('axisTickLabelFont', this.fb.control(settings.axisTickLabelFont, []));
    latestChartWidgetSettingsForm.addControl('axisTickLabelColor', this.fb.control(settings.axisTickLabelColor, []));
  }
}
