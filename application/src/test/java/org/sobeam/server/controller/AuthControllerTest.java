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
import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.sobeam.common.util.JacksonUtil;
import org.sobeam.server.common.data.Tenant;
import org.sobeam.server.common.data.User;
import org.sobeam.server.common.data.id.TenantId;
import org.sobeam.server.common.data.id.UserCredentialsId;
import org.sobeam.server.common.data.id.UserId;
import org.sobeam.server.common.data.security.Authority;
import org.sobeam.server.common.data.security.UserCredentials;
import org.sobeam.server.common.data.security.model.SecuritySettings;
import org.sobeam.server.dao.service.DaoSqlTest;
import org.sobeam.server.dao.user.UserService;
import org.sobeam.server.service.entitiy.tenant.TbTenantService;
import org.sobeam.server.service.security.auth.rest.LoginRequest;
import org.sobeam.server.service.security.model.ChangePasswordRequest;
import org.sobeam.server.service.security.model.SignUpRequest;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;  // Pour convertir les objets en JSON
import org.springframework.beans.factory.annotation.Autowired;  // Pour l'injection des dépendances
import org.springframework.http.MediaType;  // Pour spécifier le type de contenu JSON dans les requêtes
import org.springframework.test.web.servlet.MockMvc;  // Pour les tests avec MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;  // Pour construire des requêtes MockMvc

// Imports statiques pour MockMvc
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;  // Pour la méthode POST
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;  // Pour vérifier les statuts de la réponse
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;  // Pour vérifier les valeurs JSON dans la réponse

// Pour les assertions avec Hamcrest (optionnel si vous avez besoin de matcher des valeurs spécifiques dans la réponse JSON)
import static org.hamcrest.Matchers.is;

@DaoSqlTest
public class AuthControllerTest extends AbstractControllerTest {
    @Autowired
    UserService userService;
    @Autowired

    TbTenantService tbTenantService;
    @Autowired
    private ObjectMapper objectMapper;  // Injecte l'instance d'ObjectMapper
    @After
    public void tearDown() throws Exception {
        loginSysAdmin();
        SecuritySettings securitySettings = doGet("/api/admin/securitySettings", SecuritySettings.class);

        securitySettings.getPasswordPolicy().setMaximumLength(72);
        securitySettings.getPasswordPolicy().setForceUserToResetPasswordIfNotValid(false);

        doPost("/api/admin/securitySettings", securitySettings).andExpect(status().isOk());
    }

    @Test
    public void testGetUser() throws Exception {
        
        doGet("/api/auth/user")
        .andExpect(status().isUnauthorized());
        
        loginSysAdmin();
        doGet("/api/auth/user")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authority",is(Authority.SYS_ADMIN.name())))
        .andExpect(jsonPath("$.email",is(SYS_ADMIN_EMAIL)));
        
        loginTenantAdmin();
        doGet("/api/auth/user")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authority",is(Authority.TENANT_ADMIN.name())))
        .andExpect(jsonPath("$.email",is(TENANT_ADMIN_EMAIL)));
        
        loginCustomerUser();
        doGet("/api/auth/user")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authority",is(Authority.CUSTOMER_USER.name())))
        .andExpect(jsonPath("$.email",is(CUSTOMER_USER_EMAIL)));
    }
    
    @Test
    public void testLoginLogout() throws Exception {
        loginSysAdmin();
        doGet("/api/auth/user")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authority",is(Authority.SYS_ADMIN.name())))
        .andExpect(jsonPath("$.email",is(SYS_ADMIN_EMAIL)));

        TimeUnit.SECONDS.sleep(1); //We need to make sure that event for invalidating token was successfully processed

        logout();
        doGet("/api/auth/user")
        .andExpect(status().isUnauthorized());

        resetTokens();
    }

    @Test
    public void testRefreshToken() throws Exception {
        loginSysAdmin();
        doGet("/api/auth/user")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authority",is(Authority.SYS_ADMIN.name())))
                .andExpect(jsonPath("$.email",is(SYS_ADMIN_EMAIL)));

        refreshToken();
        doGet("/api/auth/user")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authority",is(Authority.SYS_ADMIN.name())))
                .andExpect(jsonPath("$.email",is(SYS_ADMIN_EMAIL)));
    }

    @Test
    public void testShouldNotUpdatePasswordWithValueLongerThanDefaultLimit() throws Exception {
        loginTenantAdmin();
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setCurrentPassword("tenant");
        changePasswordRequest.setNewPassword(RandomStringUtils.randomAlphanumeric(73));
        doPost("/api/auth/changePassword", changePasswordRequest)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Password must be no more than 72 characters in length.")));
    }

    @Test
    public void testShouldNotAuthorizeUserIfHisPasswordBecameTooLong() throws Exception {
        loginTenantAdmin();

        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setCurrentPassword("tenant");
        String newPassword = RandomStringUtils.randomAlphanumeric(16);
        changePasswordRequest.setNewPassword(newPassword);
        doPost("/api/auth/changePassword", changePasswordRequest)
                .andExpect(status().isOk());
        loginUser(TENANT_ADMIN_EMAIL, newPassword);

        loginSysAdmin();
        SecuritySettings securitySettings = doGet("/api/admin/securitySettings", SecuritySettings.class);
        securitySettings.getPasswordPolicy().setMaximumLength(15);
        securitySettings.getPasswordPolicy().setForceUserToResetPasswordIfNotValid(true);
        doPost("/api/admin/securitySettings", securitySettings).andExpect(status().isOk());

        //try to login with user password that is not valid after security settings was updated
        doPost("/api/auth/login", new LoginRequest(TENANT_ADMIN_EMAIL, newPassword))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("The entered password violates our policies. If this is your real password, please reset it.")));
    }

    @Test
    public void testShouldNotResetPasswordToTooLongValue() throws Exception {
        loginTenantAdmin();

        JsonNode resetPasswordByEmailRequest = JacksonUtil.newObjectNode()
                .put("email", TENANT_ADMIN_EMAIL);

        doPost("/api/noauth/resetPasswordByEmail", resetPasswordByEmailRequest)
                .andExpect(status().isOk());
        Thread.sleep(1000);
        doGet("/api/noauth/resetPassword?resetToken={resetToken}", this.currentResetPasswordToken)
                .andExpect(status().isSeeOther())
                .andExpect(header().string(HttpHeaders.LOCATION, "/login/resetPassword?resetToken=" + this.currentResetPasswordToken));

        String newPassword = RandomStringUtils.randomAlphanumeric(73);
        JsonNode resetPasswordRequest = JacksonUtil.newObjectNode()
                .put("resetToken", this.currentResetPasswordToken)
                .put("password", newPassword);

        Mockito.doNothing().when(mailService).sendPasswordWasResetEmail(anyString(), anyString());
        doPost("/api/noauth/resetPassword", resetPasswordRequest)
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message",
                                is("Password must be no more than 72 characters in length.")));
    }

    @Test
    public void testGetPageWithoutRedirect() throws Exception {
        doGet("/login").andExpect(status().isOk());
        doGet("/home").andExpect(status().isOk());
    }
}
