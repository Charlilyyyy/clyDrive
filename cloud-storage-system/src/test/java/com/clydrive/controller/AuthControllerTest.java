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
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

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
        emailVerificationTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void loginWithValidCredentialsReturnsTokens() throws Exception {
        User user = createVerifiedUser("johndoe", "john@example.com", "9876543210");

        String body = """
                {
                  "identifier": "johndoe",
                  "password": "Secure@123"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.message", is("Login successful")))
                .andExpect(jsonPath("$.data.username", is("johndoe")))
                .andExpect(jsonPath("$.data.email", is("john@example.com")))
                .andExpect(jsonPath("$.data.tokens.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.tokens.refreshToken", notNullValue()));

        User updated = userRepository.findById(user.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(0, updated.getFailedAttempts());
    }

    @Test
    void loginWithInvalidPasswordReturnsBadRequest() throws Exception {
        createVerifiedUser("johndoe", "john@example.com", "9876543210");

        String body = """
                {
                  "identifier": "johndoe",
                  "password": "WrongPass@1"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Invalid username/email/phone or password")));
    }

    @Test
    void loginWhenEmailNotVerifiedReturnsBadRequest() throws Exception {
        userRepository.save(User.builder()
                .firstName("John")
                .lastName("Doe")
                .username("pendinguser")
                .email("pending@example.com")
                .password(passwordEncoder.encode("Secure@123"))
                .phoneNumber("9123456780")
                .role(Role.USER)
                .status(UserStatus.PENDING)
                .storageQuota(1073741824L)
                .emailVerified(false)
                .enabled(true)
                .accountNonLocked(true)
                .build());

        String body = """
                {
                  "identifier": "pendinguser",
                  "password": "Secure@123"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Please verify your email first")));
    }

    @Test
    void refreshTokenRotatesTokens() throws Exception {
        createVerifiedUser("johndoe", "john@example.com", "9876543210");

        JsonNode loginData = loginAndGetData("johndoe", "Secure@123");
        String refreshToken = loginData.path("tokens").path("refreshToken").asText();

        String body = """
                {
                  "refreshToken": "%s"
                }
                """.formatted(refreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.message", is("Token refreshed successfully")))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()));
    }

    @Test
    void logoutBlacklistsAccessToken() throws Exception {
        createVerifiedUser("johndoe", "john@example.com", "9876543210");

        JsonNode loginData = loginAndGetData("johndoe", "Secure@123");
        String accessToken = loginData.path("tokens").path("accessToken").asText();
        String refreshToken = loginData.path("tokens").path("refreshToken").asText();

        String body = """
                {
                  "refreshToken": "%s"
                }
                """.formatted(refreshToken);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.data.loggedOut", is(true)))
                .andExpect(jsonPath("$.data.tokenRevoked", is(true)));

        mockMvc.perform(get("/api/v1/auth/login-history")
                        .param("userId", "1")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginHistoryReturnsUserAttempts() throws Exception {
        User user = createVerifiedUser("johndoe", "john@example.com", "9876543210");

        JsonNode loginData = loginAndGetData("johndoe", "Secure@123");
        String accessToken = loginData.path("tokens").path("accessToken").asText();

        mockMvc.perform(get("/api/v1/auth/login-history")
                        .param("userId", String.valueOf(user.getId()))
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(200)))
                .andExpect(jsonPath("$.message", is("Login history fetched successfully")))
                .andExpect(jsonPath("$.data[0].status", is("SUCCESS")));
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

    private JsonNode loginAndGetData(String identifier, String password) throws Exception {
        String body = """
                {
                  "identifier": "%s",
                  "password": "%s"
                }
                """.formatted(identifier, password);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }
}
