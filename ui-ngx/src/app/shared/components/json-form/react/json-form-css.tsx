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
import SobeamAceEditor from './json-form-ace-editor';
import { JsonFormFieldProps, JsonFormFieldState } from '@shared/components/json-form/react/json-form.models';
import { Observable } from 'rxjs/internal/Observable';
import { beautifyCss } from '@shared/models/beautify.models';

class SobeamCss extends React.Component<JsonFormFieldProps, JsonFormFieldState> {

    constructor(props) {
        super(props);
        this.onTidyCss = this.onTidyCss.bind(this);
    }

    onTidyCss(css: string): Observable<string> {
        return beautifyCss(css, {indent_size: 4});
    }

    render() {
        return (
            <SobeamAceEditor {...this.props} mode='css' onTidy={this.onTidyCss} {...this.state}></SobeamAceEditor>
        );
    }
}

export default SobeamCss;
