/*
 * Copyright © 2024 The Sobeam Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import * as React from 'react';
import JsonFormUtils from './json-form-utils';

import SobeamArray from './json-form-array';
import SobeamJavaScript from './json-form-javascript';
import SobeamJson from './json-form-json';
import SobeamHtml from './json-form-html';
import SobeamCss from './json-form-css';
import SobeamColor from './json-form-color';
import SobeamRcSelect from './json-form-rc-select';
import SobeamNumber from './json-form-number';
import SobeamText from './json-form-text';
import SobeamSelect from './json-form-select';
import SobeamRadios from './json-form-radios';
import SobeamDate from './json-form-date';
import SobeamImage from './json-form-image';
import SobeamCheckbox from './json-form-checkbox';
import SobeamHelp from './json-form-help';
import SobeamFieldSet from './json-form-fieldset';
import SobeamIcon from './json-form-icon';
import {
  JsonFormData,
  JsonFormProps,
  onChangeFn,
  OnColorClickFn, onHelpClickFn,
  OnIconClickFn,
  onToggleFullscreenFn
} from './json-form.models';

import _ from 'lodash';
import tinycolor from 'tinycolor2';
import { GroupInfo } from '@shared/models/widget.models';
import SobeamMarkdown from '@shared/components/json-form/react/json-form-markdown';
import { MouseEvent } from 'react';

class SobeamSchemaForm extends React.Component<JsonFormProps, any> {

  private hasConditions: boolean;
  private readonly mapper: {[type: string]: any};

  constructor(props) {
    super(props);

    this.mapper = {
      number: SobeamNumber,
      text: SobeamText,
      password: SobeamText,
      textarea: SobeamText,
      select: SobeamSelect,
      radios: SobeamRadios,
      date: SobeamDate,
      image: SobeamImage,
      checkbox: SobeamCheckbox,
      help: SobeamHelp,
      array: SobeamArray,
      javascript: SobeamJavaScript,
      json: SobeamJson,
      html: SobeamHtml,
      css: SobeamCss,
      markdown: SobeamMarkdown,
      color: SobeamColor,
      'rc-select': SobeamRcSelect,
      fieldset: SobeamFieldSet,
      icon: SobeamIcon
    };

    this.onChange = this.onChange.bind(this);
    this.onColorClick = this.onColorClick.bind(this);
    this.onIconClick = this.onIconClick.bind(this);
    this.onToggleFullscreen = this.onToggleFullscreen.bind(this);
    this.onHelpClick = this.onHelpClick.bind(this);
    this.hasConditions = false;
  }

  onChange(key: (string | number)[], val: any, forceUpdate?: boolean) {
    this.props.onModelChange(key, val, forceUpdate);
    if (this.hasConditions) {
      this.forceUpdate();
    }
  }

  onColorClick(key: (string | number)[], val: tinycolor.ColorFormats.RGBA,
               colorSelectedFn: (color: tinycolor.ColorFormats.RGBA) => void) {
    this.props.onColorClick(key, val, colorSelectedFn);
  }

  onIconClick(key: (string | number)[], val: string,
              iconSelectedFn: (icon: string) => void) {
    this.props.onIconClick(key, val, iconSelectedFn);
  }

  onToggleFullscreen(fullscreenFinishFn?: (el: Element) => void) {
    this.props.onToggleFullscreen(fullscreenFinishFn);
  }

  onHelpClick(event: MouseEvent, helpId: string, helpVisibleFn: (visible: boolean) => void, helpReadyFn: (ready: boolean) => void) {
    this.props.onHelpClick(event, helpId, helpVisibleFn, helpReadyFn);
  }


  builder(form: JsonFormData,
          model: any,
          index: number,
          onChange: onChangeFn,
          onColorClick: OnColorClickFn,
          onIconClick: OnIconClickFn,
          onToggleFullscreen: onToggleFullscreenFn,
          onHelpClick: onHelpClickFn,
          mapper: {[type: string]: any}): JSX.Element {
    const type = form.type;
    const Field = this.mapper[type];
    if (!Field) {
      console.log('Invalid field: \"' + form.key[0] + '\"!');
      return null;
    }
    if (form.condition) {
      this.hasConditions = true;
      // tslint:disable-next-line:no-eval
      if (eval(form.condition) === false) {
        return null;
      }
    }
    return <Field model={model} form={form} key={index} onChange={onChange}
                  onColorClick={onColorClick}
                  onIconClick={onIconClick}
                  onToggleFullscreen={onToggleFullscreen}
                  onHelpClick={onHelpClick}
                  mapper={mapper} builder={this.builder}/>;
  }

  createSchema(theForm: any[]): JSX.Element {
    const merged = JsonFormUtils.merge(this.props.schema, theForm, this.props.ignore, this.props.option);
    let mapper = this.mapper;
    if (this.props.mapper) {
      mapper = _.merge(this.mapper, this.props.mapper);
    }
    const forms = merged.map(function(form, index) {
      return this.builder(form, this.props.model, index, this.onChange, this.onColorClick,
        this.onIconClick, this.onToggleFullscreen, this.onHelpClick, mapper);
    }.bind(this));

    let formClass = 'SchemaForm';
    if (this.props.isFullscreen) {
      formClass += ' SchemaFormFullscreen';
    }

    return (
      <div style={{width: '100%'}} className={formClass}>{forms}</div>
    );
  }

  render() {
    if (this.props.groupInfoes && this.props.groupInfoes.length > 0) {
      const content: JSX.Element[] = [];
      for (const info of this.props.groupInfoes) {
        const forms = this.createSchema(this.props.form[info.formIndex]);
        const item = <SobeamSchemaGroup key={content.length} forms={forms} info={info}></SobeamSchemaGroup>;
        content.push(item);
      }
      return (<div>{content}</div>);
    } else {
      return this.createSchema(this.props.form);
    }
  }
}
export default SobeamSchemaForm;

interface SobeamSchemaGroupProps {
  info: GroupInfo;
  forms: JSX.Element;
}

interface SobeamSchemaGroupState {
  showGroup: boolean;
}

class SobeamSchemaGroup extends React.Component<SobeamSchemaGroupProps, SobeamSchemaGroupState> {
  constructor(props) {
    super(props);
    this.state = {
      showGroup: true
    };
  }

  toogleGroup(index) {
    this.setState({
      showGroup: !this.state.showGroup
    });
  }

  render() {
    const theCla = 'pull-right fa fa-chevron-down tb-toggle-icon' + (this.state.showGroup ? '' : ' tb-toggled');
    return (<section className='mat-elevation-z1' style={{marginTop: '10px'}}>
      <div className='SchemaGroupname tb-button-toggle'
           onClick={this.toogleGroup.bind(this)}>{this.props.info.GroupTitle}<span className={theCla}></span></div>
      <div style={{padding: '20px'}} className={this.state.showGroup ? '' : 'invisible'}>{this.props.forms}</div>
    </section>);
  }
}
