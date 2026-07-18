package com.clydrive.controller;

import com.clydrive.enums.OtpPurpose;
import com.clydrive.enums.Role;
import com.clydrive.enums.UserStatus;
import com.clydrive.module.EmailVerificationToken;
import com.clydrive.module.Otp;
import com.clydrive.module.User;
import com.clydrive.repository.EmailVerificationTokenRepository;
import com.clydrive.repository.LoginAttemptRepository;
import com.clydrive.repository.OtpRepository;
import com.clydrive.repository.PasswordHistoryRepository;
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

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private PasswordHistoryRepository passwordHistoryRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private TokenBlacklistRepository tokenBlacklistRepository;

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        tokenBlacklistRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        loginAttemptRepository.deleteAll();
        otpRepository.deleteAll();
        emailVerificationTokenRepository.deleteAll();
        passwordHistoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void verifyEmailActivatesPendingUser() throws Exception {
        User user = createPendingUser("pendinguser", "pending@example.com", "9123456780");

        EmailVerificationToken token = emailVerificationTokenRepository.save(EmailVerificationToken.builder()
                .token("verify-token-123")
                .user(user)
                .used(false)
                .expiryDate(LocalDateTime.now().plusMinutes(15))
                .build());

        mockMvc.perform(get("/api/v1/auth/verify-email").param("token", token.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.data.verified", is(true)));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(updated.isEmailVerified());
        org.junit.jupiter.api.Assertions.assertEquals(UserStatus.ACTIVE, updated.getStatus());
    }

    @Test
    void forgotVerifyAndResetPasswordFlow() throws Exception {
        User user = createVerifiedUser("johndoe", "john@example.com", "9876543210");

        mockMvc.perform(post("/api/v1/auth/forgot-password/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"john@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.emailSent", is(true)));

        Otp otp = otpRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc("john@example.com", OtpPurpose.FORGOT_PASSWORD)
                .orElseThrow();

        mockMvc.perform(post("/api/v1/auth/verify-password-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"john@example.com\",\"otp\":\"%s\"}".formatted(otp.getOtpCode())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verified", is(true)))
                .andExpect(jsonPath("$.data.canResetPassword", is(true)));

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "john@example.com",
                                  "newPassword": "NewSecure@456",
                                  "confirmPassword": "NewSecure@456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.passwordUpdated", is(true)))
                .andExpect(jsonPath("$.data.tokensRevoked", is(true)));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("NewSecure@456", updated.getPassword()));
    }

    @Test
    void changePasswordUpdatesAuthenticatedUserPassword() throws Exception {
        createVerifiedUser("johndoe", "john@example.com", "9876543210");

        JsonNode loginData = loginAndGetData("johndoe", "Secure@123");
        String accessToken = loginData.path("tokens").path("accessToken").asText();

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "Secure@123",
                                  "newPassword": "NewSecure@456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.passwordChanged", is(true)));
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() throws Exception {
        createVerifiedUser("johndoe", "john@example.com", "9876543210");

        JsonNode loginData = loginAndGetData("johndoe", "Secure@123");
        String accessToken = loginData.path("tokens").path("accessToken").asText();

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "WrongPass@1",
                                  "newPassword": "NewSecure@456"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Current password is incorrect")));
    }

    private User createVerifiedUser(String username, String email, String phone) {
        return userRepository.save(User.builder()
                .firstName("John")
                .lastName("Doe")
                .username(username)
                .email(email)
                .password(passwordEncoder.encode("Secure@123"))
                .phoneNumber(phone)
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .storageQuota(1073741824L)
                .storageUsed(0L)
                .emailVerified(true)
                .enabled(true)
                .accountNonLocked(true)
                .failedAttempts(0)
                .build());
    }

    private User createPendingUser(String username, String email, String phone) {
        return userRepository.save(User.builder()
                .firstName("Jane")
                .lastName("Doe")
                .username(username)
                .email(email)
                .password(passwordEncoder.encode("Secure@123"))
                .phoneNumber(phone)
                .role(Role.USER)
                .status(UserStatus.PENDING)
                .storageQuota(1073741824L)
                .storageUsed(0L)
                .emailVerified(false)
                .enabled(true)
                .accountNonLocked(true)
                .failedAttempts(0)
                .build());
    }

    private JsonNode loginAndGetData(String identifier, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"%s\",\"password\":\"%s\"}".formatted(identifier, password)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }
}
