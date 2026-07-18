package com.clydrive.controller;

import com.clydrive.enums.Role;
import com.clydrive.enums.UserStatus;
import com.clydrive.module.User;
import com.clydrive.repository.EmailVerificationTokenRepository;
import com.clydrive.repository.LoginAttemptRepository;
import com.clydrive.repository.RefreshTokenRepository;
import com.clydrive.repository.TokenBlacklistRepository;
import com.clydrive.repository.UserRepository;
import com.clydrive.service.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private TokenBlacklistRepository tokenBlacklistRepository;

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private EmailService emailService;

    private String adminToken;
    private Long targetUserId;

    @BeforeEach
    void setUp() throws Exception {
        tokenBlacklistRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        loginAttemptRepository.deleteAll();
        emailVerificationTokenRepository.deleteAll();
        userRepository.deleteAll();

        saveUser("superadmin", "superadmin@clydrive.local", "9000000001", Role.ADMIN);
        targetUserId = saveUser("targetuser", "target@example.com", "9000000002", Role.USER).getId();

        adminToken = login("superadmin", "Secure@123").path("tokens").path("accessToken").asText();
    }

    @Test
    void adminCanFetchStats() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.data.totalUsers", is(2)))
                .andExpect(jsonPath("$.data.activeUsers", is(2)));
    }

    @Test
    void adminCanListUsers() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()", is(2)));
    }

    @Test
    void adminCanLockAndUnlockUser() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/{id}/lock", targetUserId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountNonLocked", is(false)));

        User locked = userRepository.findById(targetUserId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(UserStatus.LOCKED, locked.getStatus());

        mockMvc.perform(post("/api/v1/admin/users/{id}/unlock", targetUserId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountNonLocked", is(true)));
    }

    @Test
    void adminCanPromoteUserRole() throws Exception {
        mockMvc.perform(put("/api/v1/admin/users/{id}/role", targetUserId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role", is("ADMIN")));
    }

    @Test
    void adminCanSoftDeleteUser() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/admin/users/{id}", targetUserId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("DELETED")));
    }

    @Test
    void nonAdminIsForbidden() throws Exception {
        String userToken = login("targetuser", "Secure@123").path("tokens").path("accessToken").asText();

        mockMvc.perform(get("/api/v1/admin/stats")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    private User saveUser(String username, String email, String phone, Role role) {
        return userRepository.save(User.builder()
                .firstName("Test")
                .lastName("User")
                .username(username)
                .email(email)
                .password(passwordEncoder.encode("Secure@123"))
                .phoneNumber(phone)
                .role(role)
                .status(UserStatus.ACTIVE)
                .storageQuota(1073741824L)
                .storageUsed(0L)
                .emailVerified(true)
                .enabled(true)
                .accountNonLocked(true)
                .failedAttempts(0)
                .build());
    }

    private JsonNode login(String identifier, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"%s\",\"password\":\"%s\"}".formatted(identifier, password)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }
}
