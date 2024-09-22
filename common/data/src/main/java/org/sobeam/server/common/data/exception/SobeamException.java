/**
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
package org.sobeam.server.common.data.exception;

public class SobeamException extends Exception {

    private static final long serialVersionUID = 1L;

    private SobeamErrorCode errorCode;

    public SobeamException() {
        super();
    }

    public SobeamException(SobeamErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    public SobeamException(String message, SobeamErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public SobeamException(String message, Throwable cause, SobeamErrorCode errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public SobeamException(Throwable cause, SobeamErrorCode errorCode) {
        super(cause);
        this.errorCode = errorCode;
    }

    public SobeamErrorCode getErrorCode() {
        return errorCode;
    }

}
