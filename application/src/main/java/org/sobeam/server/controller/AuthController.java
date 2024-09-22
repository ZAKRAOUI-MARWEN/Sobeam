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

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.sobeam.rule.engine.api.MailService;
import org.sobeam.server.cache.limits.RateLimitService;
import org.sobeam.server.common.data.Customer;
import org.sobeam.server.common.data.Tenant;
import org.sobeam.server.common.data.TenantProfile;
import org.sobeam.server.common.data.User;
import org.sobeam.server.common.data.audit.ActionType;
import org.sobeam.server.common.data.exception.SobeamErrorCode;
import org.sobeam.server.common.data.exception.SobeamException;
import org.sobeam.server.common.data.id.CustomerId;
import org.sobeam.server.common.data.id.EntityId;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.id.TenantProfileId;
import org.sobeam.server.common.data.limit.LimitedApi;
import org.sobeam.server.common.data.security.Authority;
import org.sobeam.server.common.data.security.UserCredentials;
import org.sobeam.server.common.data.security.event.UserCredentialsInvalidationEvent;
import org.sobeam.server.common.data.security.event.UserSessionInvalidationEvent;
import org.sobeam.server.common.data.security.model.JwtPair;
import org.sobeam.server.common.data.security.model.SecuritySettings;
import org.sobeam.server.common.data.security.model.UserPasswordPolicy;
import org.sobeam.server.config.annotations.ApiOperation;
import org.sobeam.server.dao.tenant.TbTenantProfileCache;
import org.sobeam.server.dao.user.UserService;
import org.sobeam.server.queue.util.TbCoreComponent;
import org.sobeam.server.service.entitiy.customer.TbCustomerService;
import org.sobeam.server.service.entitiy.tenant.TbTenantService;
import org.sobeam.server.service.entitiy.tenant.profile.TbTenantProfileService;
import org.sobeam.server.service.entitiy.user.TbUserService;
import org.sobeam.server.service.entitiy.user.TbUserSettingsService;
import org.sobeam.server.service.security.auth.rest.RestAuthenticationDetails;
import org.sobeam.server.service.security.model.*;
import org.sobeam.server.service.security.model.token.JwtTokenFactory;
import org.sobeam.server.service.security.permission.Resource;
import org.sobeam.server.service.security.system.SystemSecurityService;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.UUID;

import static org.sobeam.server.common.data.StringUtils.generateSafeToken;
import static org.sobeam.server.common.data.id.EntityId.NULL_UUID;
import static org.sobeam.server.controller.UserController.ACTIVATE_URL_PATTERN;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class AuthController extends BaseController {

    @Value("${server.rest.rate_limits.reset_password_per_user:5:3600}")
    private String defaultLimitsConfiguration;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenFactory tokenFactory;
    private final MailService mailService;
    private final SystemSecurityService systemSecurityService;
    private final RateLimitService rateLimitService;
    private final ApplicationEventPublisher eventPublisher;
    private final TbTenantService tbTenantService;
    private final TbTenantProfileService tbTenantProfileService;
    private final TbUserService tbUserService;
    private final UserService userService;
    private static final int DEFAULT_TOKEN_LENGTH = 30;


    @ApiOperation(value = "Get current User (getUser)",
            notes = "Get the information about the User which credentials are used to perform this REST API call.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/auth/user", method = RequestMethod.GET)
    public @ResponseBody
    User getUser() throws SobeamException {
        SecurityUser securityUser = getCurrentUser();
        return userService.findUserById(securityUser.getTenantId(), securityUser.getId());
    }

    @ApiOperation(value = "Logout (logout)",
            notes = "Special API call to record the 'logout' of the user to the Audit Logs. Since platform uses [JWT](https://jwt.io/), the actual logout is the procedure of clearing the [JWT](https://jwt.io/) token on the client side. ")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/auth/logout", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void logout(HttpServletRequest request) throws SobeamException {
        logLogoutAction(request);
    }

    @ApiOperation(value = "Change password for current User (changePassword)",
            notes = "Change the password for the User which credentials are used to perform this REST API call. Be aware that previously generated [JWT](https://jwt.io/) tokens will be still valid until they expire.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/auth/changePassword", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public JwtPair changePassword(@Parameter(description = "Change Password Request")
                                  @RequestBody ChangePasswordRequest changePasswordRequest) throws SobeamException {
        String currentPassword = changePasswordRequest.getCurrentPassword();
        String newPassword = changePasswordRequest.getNewPassword();
        SecurityUser securityUser = getCurrentUser();
        UserCredentials userCredentials = userService.findUserCredentialsByUserId(TenantId.SYS_TENANT_ID, securityUser.getId());
        if (!passwordEncoder.matches(currentPassword, userCredentials.getPassword())) {
            throw new SobeamException("Current password doesn't match!", SobeamErrorCode.BAD_REQUEST_PARAMS);
        }
        systemSecurityService.validatePassword(newPassword, userCredentials);
        if (passwordEncoder.matches(newPassword, userCredentials.getPassword())) {
            throw new SobeamException("New password should be different from existing!", SobeamErrorCode.BAD_REQUEST_PARAMS);
        }
        userCredentials.setPassword(passwordEncoder.encode(newPassword));
        userService.replaceUserCredentials(securityUser.getTenantId(), userCredentials);

        eventPublisher.publishEvent(new UserCredentialsInvalidationEvent(securityUser.getId()));
        return tokenFactory.createTokenPair(securityUser);
    }

    @ApiOperation(value = "Get the current User password policy (getUserPasswordPolicy)",
            notes = "API call to get the password policy for the password validation form(s).")
    @RequestMapping(value = "/noauth/userPasswordPolicy", method = RequestMethod.GET)
    @ResponseBody
    public UserPasswordPolicy getUserPasswordPolicy() throws SobeamException {
        SecuritySettings securitySettings =
                checkNotNull(systemSecurityService.getSecuritySettings());
        return securitySettings.getPasswordPolicy();
    }

    @ApiOperation(value = "Check Activate User Token (checkActivateToken)",
            notes = "Checks the activation token and forwards user to 'Create Password' page. " +
                    "If token is valid, returns '303 See Other' (redirect) response code with the correct address of 'Create Password' page and same 'activateToken' specified in the URL parameters. " +
                    "If token is not valid, returns '409 Conflict'.")
    @RequestMapping(value = "/noauth/activate", params = {"activateToken"}, method = RequestMethod.GET)
    public ResponseEntity<String> checkActivateToken(
            @Parameter(description = "The activate token string.")
            @RequestParam(value = "activateToken") String activateToken) {
        HttpHeaders headers = new HttpHeaders();
        HttpStatus responseStatus;
        UserCredentials userCredentials = userService.findUserCredentialsByActivateToken(TenantId.SYS_TENANT_ID, activateToken);
        if (userCredentials != null) {
            String createURI = "/login/createPassword";
            try {
                URI location = new URI(createURI + "?activateToken=" + activateToken);
                headers.setLocation(location);
                responseStatus = HttpStatus.SEE_OTHER;
            } catch (URISyntaxException e) {
                log.error("Unable to create URI with address [{}]", createURI);
                responseStatus = HttpStatus.BAD_REQUEST;
            }
        } else {
            responseStatus = HttpStatus.CONFLICT;
        }
        return new ResponseEntity<>(headers, responseStatus);
    }

    @ApiOperation(value = "Request reset password email (requestResetPasswordByEmail)",
            notes = "Request to send the reset password email if the user with specified email address is present in the database. " +
                    "Always return '200 OK' status for security purposes.")
    @RequestMapping(value = "/noauth/resetPasswordByEmail", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void requestResetPasswordByEmail(
            @Parameter(description = "The JSON object representing the reset password email request.")
            @RequestBody ResetPasswordEmailRequest resetPasswordByEmailRequest,
            HttpServletRequest request) throws SobeamException {
        try {
            String email = resetPasswordByEmailRequest.getEmail();
            UserCredentials userCredentials = userService.requestPasswordReset(TenantId.SYS_TENANT_ID, email);
            User user = userService.findUserById(TenantId.SYS_TENANT_ID, userCredentials.getUserId());
            String baseUrl = systemSecurityService.getBaseUrl(user.getTenantId(), user.getCustomerId(), request);
            String resetUrl = String.format("%s/api/noauth/resetPassword?resetToken=%s", baseUrl,
                    userCredentials.getResetToken());

            mailService.sendResetPasswordEmailAsync(resetUrl, email);
        } catch (Exception e) {
            log.warn("Error occurred: {}", e.getMessage());
        }
    }

    @ApiOperation(value = "Check password reset token (checkResetToken)",
            notes = "Checks the password reset token and forwards user to 'Reset Password' page. " +
                    "If token is valid, returns '303 See Other' (redirect) response code with the correct address of 'Reset Password' page and same 'resetToken' specified in the URL parameters. " +
                    "If token is not valid, returns '409 Conflict'.")
    @RequestMapping(value = "/noauth/resetPassword", params = {"resetToken"}, method = RequestMethod.GET)
    public ResponseEntity<String> checkResetToken(
            @Parameter(description = "The reset token string.")
            @RequestParam(value = "resetToken") String resetToken) {
        HttpHeaders headers = new HttpHeaders();
        HttpStatus responseStatus;
        String resetURI = "/login/resetPassword";
        UserCredentials userCredentials = userService.findUserCredentialsByResetToken(TenantId.SYS_TENANT_ID, resetToken);

        if (userCredentials != null) {
            if (!rateLimitService.checkRateLimit(LimitedApi.PASSWORD_RESET, userCredentials.getUserId(), defaultLimitsConfiguration)) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
            }
            try {
                URI location = new URI(resetURI + "?resetToken=" + resetToken);
                headers.setLocation(location);
                responseStatus = HttpStatus.SEE_OTHER;
            } catch (URISyntaxException e) {
                log.error("Unable to create URI with address [{}]", resetURI);
                responseStatus = HttpStatus.BAD_REQUEST;
            }
        } else {
            responseStatus = HttpStatus.CONFLICT;
        }
        return new ResponseEntity<>(headers, responseStatus);
    }

    @ApiOperation(value = "Activate User",
            notes = "Checks the activation token and updates corresponding user password in the database. " +
                    "Now the user may start using his password to login. " +
                    "The response already contains the [JWT](https://jwt.io) activation and refresh tokens, " +
                    "to simplify the user activation flow and avoid asking user to input password again after activation. " +
                    "If token is valid, returns the object that contains [JWT](https://jwt.io/) access and refresh tokens. " +
                    "If token is not valid, returns '404 Bad Request'.")
    @RequestMapping(value = "/noauth/activate", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    @ResponseBody
    public JwtPair activateUser(
            @Parameter(description = "Activate user request.")
            @RequestBody ActivateUserRequest activateRequest,
            @RequestParam(required = false, defaultValue = "true") boolean sendActivationMail,
            HttpServletRequest request) throws SobeamException {
        String activateToken = activateRequest.getActivateToken();
        String password = activateRequest.getPassword();
        systemSecurityService.validatePassword(password, null);
        String encodedPassword = passwordEncoder.encode(password);
        UserCredentials credentials = userService.activateUserCredentials(TenantId.SYS_TENANT_ID, activateToken, encodedPassword);
        User user = userService.findUserById(TenantId.SYS_TENANT_ID, credentials.getUserId());
        UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail());
        SecurityUser securityUser = new SecurityUser(user, credentials.isEnabled(), principal);
        userService.setUserCredentialsEnabled(user.getTenantId(), user.getId(), true);
        String baseUrl = systemSecurityService.getBaseUrl(user.getTenantId(), user.getCustomerId(), request);
        String loginUrl = String.format("%s/login", baseUrl);
        String email = user.getEmail();

        if (sendActivationMail) {
            try {
                mailService.sendAccountActivatedEmail(loginUrl, email);
            } catch (Exception e) {
                log.info("Unable to send account activation email [{}]", e.getMessage());
            }
        }

        var tokenPair = tokenFactory.createTokenPair(securityUser);
        systemSecurityService.logLoginAction(user, new RestAuthenticationDetails(request), ActionType.LOGIN, null);
        return tokenPair;
    }

    @ApiOperation(value = "Reset password (resetPassword)",
            notes = "Checks the password reset token and updates the password. " +
                    "If token is valid, returns the object that contains [JWT](https://jwt.io/) access and refresh tokens. " +
                    "If token is not valid, returns '404 Bad Request'.")
    @RequestMapping(value = "/noauth/resetPassword", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    @ResponseBody
    public JwtPair resetPassword(
            @Parameter(description = "Reset password request.")
            @RequestBody ResetPasswordRequest resetPasswordRequest,
            HttpServletRequest request) throws SobeamException {
        String resetToken = resetPasswordRequest.getResetToken();
        String password = resetPasswordRequest.getPassword();
        UserCredentials userCredentials = userService.findUserCredentialsByResetToken(TenantId.SYS_TENANT_ID, resetToken);
        if (userCredentials != null) {
            systemSecurityService.validatePassword(password, userCredentials);
            if (passwordEncoder.matches(password, userCredentials.getPassword())) {
                throw new SobeamException("New password should be different from existing!", SobeamErrorCode.BAD_REQUEST_PARAMS);
            }
            String encodedPassword = passwordEncoder.encode(password);
            userCredentials.setPassword(encodedPassword);
            userCredentials.setResetToken(null);
            userCredentials = userService.replaceUserCredentials(TenantId.SYS_TENANT_ID, userCredentials);
            User user = userService.findUserById(TenantId.SYS_TENANT_ID, userCredentials.getUserId());
            UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail());
            SecurityUser securityUser = new SecurityUser(user, userCredentials.isEnabled(), principal);
            String baseUrl = systemSecurityService.getBaseUrl(user.getTenantId(), user.getCustomerId(), request);
            String loginUrl = String.format("%s/login", baseUrl);
            String email = user.getEmail();
            mailService.sendPasswordWasResetEmail(loginUrl, email);

            eventPublisher.publishEvent(new UserCredentialsInvalidationEvent(securityUser.getId()));

            return tokenFactory.createTokenPair(securityUser);
        } else {
            throw new SobeamException("Invalid reset token!", SobeamErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    private void logLogoutAction(HttpServletRequest request) throws SobeamException {
        var user = getCurrentUser();
        systemSecurityService.logLoginAction(user, new RestAuthenticationDetails(request), ActionType.LOGOUT, null);
        eventPublisher.publishEvent(new UserSessionInvalidationEvent(user.getSessionId()));
    }


    @ApiOperation(value = "Sign up a new Tenant (signup)",
            notes = "Creates a new tenant with the provided title and returns the details of the newly created tenant.")
    @RequestMapping(value = "/noauth/signup", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Tenant> signUp(
            @Parameter(description = "Sign up request")
            @RequestBody SignUpRequest signUpRequest,
            HttpServletRequest request) throws Exception {

        // Vérifier si un utilisateur avec cet email existe déjà
        if (tbTenantService.findTenantByEmail(signUpRequest.getEmail())) {
            throw new Exception("User with this email already exists.");
        }

        // Valider le mot de passe
        systemSecurityService.validatePassword(signUpRequest.getPassword(), null);

        // Créer un nouveau Tenant
        Tenant tenant = new Tenant();
        tenant.setTitle("New Tenant"); // Définir le titre du tenant
        tenant.setEmail(signUpRequest.getEmail());
        Tenant savedTenant = tbTenantService.save(tenant);

        // Créer un utilisateur tenant associé
        User tenantUser = new User();
        tenantUser.setEmail(signUpRequest.getEmail());
        tenantUser.setTenantId(savedTenant.getTenantId());
        tenantUser.setAuthority(Authority.TENANT_ADMIN); // Rôle d'administrateur tenant
        tenantUser.setTenantId(savedTenant.getId());
        User savedTenantUser = userService.saveUser(TenantId.SYS_TENANT_ID, tenantUser);

        // Créer un UserCredentials  associé
        UserCredentials userCredentials = userService.findUserCredentialsByUserId(TenantId.SYS_TENANT_ID, savedTenantUser.getId());
        userCredentials.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        userCredentials.setActivateToken(generateSafeToken(DEFAULT_TOKEN_LENGTH));
        userCredentials.setEnabled(false);
        userService.saveUserCredentials(TenantId.SYS_TENANT_ID, userCredentials);


        String baseUrl = systemSecurityService.getBaseUrl(savedTenantUser.getTenantId(),null, request);
        UserCredentials userCredentialsToken = userService.findUserCredentialsByUserId(TenantId.SYS_TENANT_ID, savedTenantUser.getId());

        String activateUrl = String.format("%s/api/noauth/activateUser?activateToken=%s", baseUrl,userCredentialsToken.getActivateToken());
        mailService.sendAccountActivatedEmail(activateUrl,savedTenantUser.getEmail());

        // Retourner une réponse avec le tenant créé
        return ResponseEntity.status(HttpStatus.CREATED).body(savedTenant);
    }
    @ApiOperation(
            value = "Activate User",
            notes = "Activates a user's account using the provided activation token. " +
                    "The token must be valid and associated with a user with a non-null password. " +
                    "Upon successful activation, the user is redirected to the login page."
    )
    @RequestMapping(value = "/noauth/activateUser",params = {"activateToken"},method = RequestMethod.GET)
    public ResponseEntity<?> userActivateToken(
            @Parameter(description = "The activation token string used to activate the user's account.")
            @RequestParam(value = "activateToken") String activateToken) {

        // Find user credentials by activation token
        UserCredentials userCredentials = userService.findUserCredentialsByActivateToken(TenantId.SYS_TENANT_ID, activateToken);

        if (userCredentials == null || userCredentials.getPassword() == null || !userCredentials.getActivateToken().equals(activateToken)) {
            // If the token is invalid, return a conflict response
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Invalid activation token.");
        }

        // Validate password
        String password = userCredentials.getPassword();

        // Encode the password and activate user credentials
        userService.activateUserCredentials(TenantId.SYS_TENANT_ID, activateToken, password);

        // Redirect to the login page with a 303 See Other response
        HttpHeaders headers = new HttpHeaders();
        String loginUrl = String.format("/login");
        headers.setLocation(URI.create(loginUrl));

        return new ResponseEntity<>(headers, HttpStatus.SEE_OTHER);
    }
    @ApiOperation(
            value = "Renvoyer l'email",
            notes = "Reverifie si l'email est enregistré et renvoie un message en conséquence."
    )
    @RequestMapping(value = "/noauth/renvoyerEmail", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity<?> renvoyerEmailSignUp(
            @RequestBody String email, HttpServletRequest request) throws Exception {

        // Vérifiez si l'email est non nul et valide
        if (email == null || email.trim().isEmpty()) {
            throw new Exception("L'email ne peut pas être nul ou vide.");

       }

        // Trouvez l'utilisateur par email
        User user = userService.findUserByEmail(TenantId.SYS_TENANT_ID, email);

        // Si l'utilisateur n'est pas trouvé, retournez un message d'email non existant
        if (user == null) {
            throw new Exception("L'email n'existe pas.");

        }

        // Trouvez les informations de connexion de l'utilisateur
        UserCredentials userCredentials = userService.findUserCredentialsByUserId(TenantId.SYS_TENANT_ID, user.getId());

        // Si les informations de connexion ne sont pas trouvées, retournez un message indiquant que l'utilisateur n'est pas enregistré
        if (userCredentials == null) {
            throw new Exception("Les informations de connexion de l'utilisateur sont manquantes.");

        }

        // Vérifiez si l'utilisateur est activé ou non
        if (userCredentials.isEnabled()) {
            throw new Exception("Le compte avec cet email est déjà actif.");

        } else {
            // Générer l'URL d'activation
            String baseUrl = systemSecurityService.getBaseUrl(TenantId.SYS_TENANT_ID, null, request);
            String activateUrl = String.format("%s/api/noauth/activateUser?activateToken=%s", baseUrl, userCredentials.getActivateToken());

            // Envoyer l'email avec le lien d'activation
            mailService.sendAccountActivatedEmail(activateUrl, email);

            // Retourner une réponse réussie
            return ResponseEntity.ok("L'email d'activation a été renvoyé.");
        }
    }
}
