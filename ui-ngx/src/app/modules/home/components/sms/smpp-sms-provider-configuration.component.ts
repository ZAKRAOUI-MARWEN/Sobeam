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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, UntypedFormBuilder, UntypedFormGroup, NG_VALUE_ACCESSOR, Validators } from '@angular/forms';
import {
  AwsSnsSmsProviderConfiguration,
  BindTypes,
  bindTypesTranslationMap,
  CodingSchemes,
  codingSchemesMap,
  NumberingPlanIdentification,
  numberingPlanIdentificationMap,
  SmppSmsProviderConfiguration,
  smppVersions,
  SmsProviderConfiguration,
  SmsProviderType,
  TypeOfNumber,
  typeOfNumberMap
} from '@shared/models/settings.models';
import { isDefinedAndNotNull } from '@core/utils';
import { coerceBooleanProperty } from '@angular/cdk/coercion';

@Component({
  selector: 'tb-smpp-sms-provider-configuration',
  templateUrl: './smpp-sms-provider-configuration.component.html',
  styleUrls: [],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => SmppSmsProviderConfigurationComponent),
    multi: true
  }]
})

export class SmppSmsProviderConfigurationComponent  implements ControlValueAccessor, OnInit{
  constructor(private fb: UntypedFormBuilder) {
  }
  private requiredValue: boolean;

  get required(): boolean {
    return this.requiredValue;
  }

  @Input()
  set required(value: boolean) {
    this.requiredValue = coerceBooleanProperty(value);
  }
  @Input()
  disabled: boolean;

  smppSmsProviderConfigurationFormGroup: UntypedFormGroup;

  smppVersions = smppVersions;

  bindTypes = Object.keys(BindTypes);
  bindTypesTranslation = bindTypesTranslationMap;

  typeOfNumber = Object.keys(TypeOfNumber);
  typeOfNumberMap = typeOfNumberMap;

  numberingPlanIdentification = Object.keys(NumberingPlanIdentification);
  numberingPlanIdentificationMap = numberingPlanIdentificationMap;

  codingSchemes = Object.keys(CodingSchemes);
  codingSchemesMap = codingSchemesMap;

  private propagateChange = (v: any) => { };

  ngOnInit(): void {
    this.smppSmsProviderConfigurationFormGroup = this.fb.group({
      protocolVersion: [null, [Validators.required]],
      host: [null, [Validators.required]],
      port: [null, [Validators.required]],
      systemId: [null, [Validators.required]],
      password: [null, [Validators.required]],
      systemType: [null],
      bindType: [null, []],
      serviceType: [null, []],
      sourceAddress: [null, []],
      sourceTon: [null, []],
      sourceNpi: [null, []],
      destinationTon: [null, []],
      destinationNpi: [null, []],
      addressRange: [null, []],
      codingScheme: [null, []],
    });

    this.smppSmsProviderConfigurationFormGroup.valueChanges.subscribe(() => {
      this.updateValue();
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.smppSmsProviderConfigurationFormGroup.disable({emitEvent: false});
    } else {
      this.smppSmsProviderConfigurationFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(value: AwsSnsSmsProviderConfiguration | null): void {
    if (isDefinedAndNotNull(value)) {
      this.smppSmsProviderConfigurationFormGroup.patchValue(value, {emitEvent: false});
    }
  }

  private updateValue() {
    let configuration: SmppSmsProviderConfiguration = null;
    if (this.smppSmsProviderConfigurationFormGroup.valid) {
      configuration = this.smppSmsProviderConfigurationFormGroup.value;
      (configuration as SmsProviderConfiguration).type = SmsProviderType.SMPP;
    }
    this.propagateChange(configuration);
  }

}
