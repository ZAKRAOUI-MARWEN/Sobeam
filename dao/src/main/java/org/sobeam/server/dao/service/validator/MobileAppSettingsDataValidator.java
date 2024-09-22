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
package org.sobeam.server.dao.service.validator;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.mobile.AndroidConfig;
import org.sobeam.server.common.data.mobile.IosConfig;
import org.sobeam.server.common.data.mobile.MobileAppSettings;
import org.sobeam.server.common.data.mobile.QRCodeConfig;
import org.sobeam.server.dao.exception.DataValidationException;
import org.sobeam.server.dao.service.DataValidator;

@Component
@AllArgsConstructor
public class MobileAppSettingsDataValidator extends DataValidator<MobileAppSettings> {

    @Override
    protected void validateDataImpl(TenantId tenantId, MobileAppSettings mobileAppSettings) {
        AndroidConfig androidConfig = mobileAppSettings.getAndroidConfig();
        IosConfig iosConfig = mobileAppSettings.getIosConfig();
        QRCodeConfig qrCodeConfig = mobileAppSettings.getQrCodeConfig();
        if (!mobileAppSettings.isUseDefaultApp() && (androidConfig == null || iosConfig == null)) {
            throw new DataValidationException("Android/ios settings are required to use custom application!");
        }
        if (qrCodeConfig == null) {
            throw new DataValidationException("Qr code configuration is required!");
        }
        if (androidConfig != null && androidConfig.isEnabled() && !mobileAppSettings.isUseDefaultApp() &&
                (androidConfig.getAppPackage() == null || androidConfig.getSha256CertFingerprints() == null)) {
            throw new DataValidationException("Application package and sha256 cert fingerprints are required for custom android application!");
        }
        if (iosConfig != null && iosConfig.isEnabled() && !mobileAppSettings.isUseDefaultApp() && iosConfig.getAppId() == null) {
            throw new DataValidationException("Application id is required for custom ios application!");
        }
    }
}
