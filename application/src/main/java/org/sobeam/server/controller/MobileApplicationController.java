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
package org.sobeam.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.sobeam.common.util.JacksonUtil;
import org.sobeam.server.common.data.exception.SobeamException;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.mobile.AndroidConfig;
import org.sobeam.server.common.data.mobile.IosConfig;
import org.sobeam.server.common.data.mobile.MobileAppSettings;
import org.sobeam.server.common.data.security.model.JwtPair;
import org.sobeam.server.config.annotations.ApiOperation;
import org.sobeam.server.dao.mobile.MobileAppSettingsService;
import org.sobeam.server.queue.util.TbCoreComponent;
import org.sobeam.server.service.mobile.secret.MobileAppSecretService;
import org.sobeam.server.service.security.model.SecurityUser;
import org.sobeam.server.service.security.permission.Operation;
import org.sobeam.server.service.security.permission.Resource;
import org.sobeam.server.service.security.system.SystemSecurityService;

import java.net.URI;
import java.net.URISyntaxException;

import static org.sobeam.server.controller.ControllerConstants.AVAILABLE_FOR_ANY_AUTHORIZED_USER;
import static org.sobeam.server.controller.ControllerConstants.SYSTEM_AUTHORITY_PARAGRAPH;

@RequiredArgsConstructor
@RestController
@TbCoreComponent
public class MobileApplicationController extends BaseController {

    @Value("${cache.specs.mobileSecretKey.timeToLiveInMinutes:2}")
    private int mobileSecretKeyTtl;
    @Value("${mobileApp.domain:demo.sobeam.io}")
    private String defaultAppDomain;

    public static final String ASSET_LINKS_PATTERN = "[{\n" +
            "  \"relation\": [\"delegate_permission/common.handle_all_urls\"],\n" +
            "  \"target\": {\n" +
            "    \"namespace\": \"android_app\",\n" +
            "    \"package_name\": \"%s\",\n" +
            "    \"sha256_cert_fingerprints\":\n" +
            "    [\"%s\"]\n" +
            "  }\n" +
            "}]";

    public static final String APPLE_APP_SITE_ASSOCIATION_PATTERN = "{\n" +
            "    \"applinks\": {\n" +
            "        \"apps\": [],\n" +
            "        \"details\": [\n" +
            "            {\n" +
            "                \"appID\": \"%s\",\n" +
            "                \"paths\": [ \"/api/noauth/qr\" ]\n" +
            "            }\n" +
            "        ]\n" +
            "    }\n" +
            "}";

    public static final String SECRET = "secret";
    public static final String SECRET_PARAM_DESCRIPTION = "A string value representing short-lived secret key";
    public static final String DEEP_LINK_PATTERN = "https://%s/api/noauth/qr?secret=%s&ttl=%s";

    private final SystemSecurityService systemSecurityService;
    private final MobileAppSecretService mobileAppSecretService;
    private final MobileAppSettingsService mobileAppSettingsService;

    @ApiOperation(value = "Get associated android applications (getAssetLinks)")
    @GetMapping(value = "/.well-known/assetlinks.json")
    public ResponseEntity<JsonNode> getAssetLinks() {
        MobileAppSettings mobileAppSettings = mobileAppSettingsService.getMobileAppSettings(TenantId.SYS_TENANT_ID);
        AndroidConfig androidConfig = mobileAppSettings.getAndroidConfig();
        if (androidConfig != null && androidConfig.isEnabled() && androidConfig.getAppPackage() != null && androidConfig.getSha256CertFingerprints() != null) {
            return ResponseEntity.ok(JacksonUtil.toJsonNode(String.format(ASSET_LINKS_PATTERN, androidConfig.getAppPackage(), androidConfig.getSha256CertFingerprints())));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @ApiOperation(value = "Get associated ios applications (getAppleAppSiteAssociation)")
    @GetMapping(value = "/.well-known/apple-app-site-association")
    public ResponseEntity<JsonNode> getAppleAppSiteAssociation() {
        MobileAppSettings mobileAppSettings = mobileAppSettingsService.getMobileAppSettings(TenantId.SYS_TENANT_ID);
        IosConfig iosConfig = mobileAppSettings.getIosConfig();
        if (iosConfig != null && iosConfig.isEnabled() && iosConfig.getAppId() != null) {
            return ResponseEntity.ok(JacksonUtil.toJsonNode(String.format(APPLE_APP_SITE_ASSOCIATION_PATTERN, iosConfig.getAppId())));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @ApiOperation(value = "Create Or Update the Mobile application settings (saveMobileAppSettings)",
            notes = "The request payload contains configuration for android/iOS applications and platform qr code widget settings." + SYSTEM_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN')")
    @PostMapping(value = "/api/mobile/app/settings")
    public MobileAppSettings saveMobileAppSettings(@Parameter(description = "A JSON value representing the mobile apps configuration")
                                                   @RequestBody MobileAppSettings mobileAppSettings) throws SobeamException {
        SecurityUser currentUser = getCurrentUser();
        accessControlService.checkPermission(currentUser, Resource.MOBILE_APP_SETTINGS, Operation.WRITE);
        mobileAppSettings.setTenantId(getTenantId());
        return mobileAppSettingsService.saveMobileAppSettings(currentUser.getTenantId(), mobileAppSettings);
    }

    @ApiOperation(value = "Get Mobile application settings (getMobileAppSettings)",
            notes = "The response payload contains configuration for android/iOS applications and platform qr code widget settings." + AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/api/mobile/app/settings")
    public MobileAppSettings getMobileAppSettings() throws SobeamException {
        SecurityUser currentUser = getCurrentUser();
        accessControlService.checkPermission(currentUser, Resource.MOBILE_APP_SETTINGS, Operation.READ);
        return mobileAppSettingsService.getMobileAppSettings(TenantId.SYS_TENANT_ID);
    }

    @ApiOperation(value = "Get the deep link to the associated mobile application (getMobileAppDeepLink)",
            notes = "Fetch the url that takes user to linked mobile application " + AVAILABLE_FOR_ANY_AUTHORIZED_USER)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/api/mobile/deepLink", produces = "text/plain")
    public String getMobileAppDeepLink(HttpServletRequest request) throws SobeamException, URISyntaxException {
        String secret = mobileAppSecretService.generateMobileAppSecret(getCurrentUser());
        String baseUrl = systemSecurityService.getBaseUrl(TenantId.SYS_TENANT_ID, null, request);
        String platformDomain = new URI(baseUrl).getHost();
        MobileAppSettings mobileAppSettings = mobileAppSettingsService.getMobileAppSettings(TenantId.SYS_TENANT_ID);
        String appDomain;
        if (!mobileAppSettings.isUseDefaultApp()) {
            appDomain = platformDomain;
        } else {
            appDomain = defaultAppDomain;
        }
        String deepLink = String.format(DEEP_LINK_PATTERN, appDomain, secret, mobileSecretKeyTtl);
        if (!appDomain.equals(platformDomain)) {
            deepLink = deepLink + "&host=" + baseUrl;
        }
        return "\"" + deepLink + "\"";
    }

    @ApiOperation(value = "Get User Token (getUserTokenByMobileSecret)",
            notes = "Returns the token of the User based on the provided secret key.")
    @GetMapping(value = "/api/noauth/qr/{secret}")
    public JwtPair getUserTokenByMobileSecret(@Parameter(description = SECRET_PARAM_DESCRIPTION)
                                              @PathVariable(SECRET) String secret) throws SobeamException {
        checkParameter(SECRET, secret);
        return mobileAppSecretService.getJwtPair(secret);
    }

    @GetMapping(value = "/api/noauth/qr")
    public ResponseEntity<?> getApplicationRedirect(@RequestHeader(value = "User-Agent") String userAgent) {
        MobileAppSettings mobileAppSettings = mobileAppSettingsService.getMobileAppSettings(TenantId.SYS_TENANT_ID);
        boolean useDefaultApp = mobileAppSettings.isUseDefaultApp();
        String googlePlayLink = useDefaultApp ? mobileAppSettings.getDefaultGooglePlayLink() : mobileAppSettings.getAndroidConfig().getStoreLink();
        String appStoreLink = useDefaultApp ? mobileAppSettings.getDefaultAppStoreLink() : mobileAppSettings.getIosConfig().getStoreLink();
        if (userAgent.contains("Android")) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", googlePlayLink)
                    .build();
        } else if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", appStoreLink)
                    .build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .build();
        }
    }

}
