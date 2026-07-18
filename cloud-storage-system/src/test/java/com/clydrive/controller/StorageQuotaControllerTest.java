package com.clydrive.controller;

import com.clydrive.enums.Role;
import com.clydrive.enums.UserStatus;
import com.clydrive.module.User;
import com.clydrive.repository.EmailVerificationTokenRepository;
import com.clydrive.repository.FileRepository;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StorageQuotaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileRepository fileRepository;

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

    private String userToken;
    private String adminToken;
    private Long userId;

    @BeforeEach
    void setUp() throws Exception {
        tokenBlacklistRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        loginAttemptRepository.deleteAll();
        emailVerificationTokenRepository.deleteAll();
        fileRepository.deleteAll();
        userRepository.deleteAll();

        userId = save("quotauser", "quotauser@example.com", "9000000040", Role.USER, 20L).getId();
        save("quotaadmin", "quotaadmin@example.com", "9000000041", Role.ADMIN, 1073741824L);

        userToken = login("quotauser", "Secure@123").path("tokens").path("accessToken").asText();
        adminToken = login("quotaadmin", "Secure@123").path("tokens").path("accessToken").asText();
    }

    @Test
    void storageEndpointReflectsUsage() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/storage")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storageQuota", is(20)))
                .andExpect(jsonPath("$.data.storageUsed", is(0)));

        upload("a.txt", "1234567890".getBytes(), status().isCreated());

        mockMvc.perform(get("/api/v1/users/me/storage")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storageUsed", is(10)))
                .andExpect(jsonPath("$.data.storageAvailable", is(10)));
    }

    @Test
    void uploadBeyondQuotaIsRejected() throws Exception {
        upload("big.txt", "this content is definitely more than twenty bytes".getBytes(), status().isBadRequest());
    }

    @Test
    void adminCanRaiseQuotaThenUploadSucceeds() throws Exception {
        upload("big.txt", "this content is definitely more than twenty bytes".getBytes(), status().isBadRequest());

        mockMvc.perform(patch("/api/v1/admin/users/{id}/quota", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storageQuota\":1000000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storageQuota", is(1000000)));

        upload("big.txt", "this content is definitely more than twenty bytes".getBytes(), status().isCreated());
    }

    private void upload(String name, byte[] content, org.springframework.test.web.servlet.ResultMatcher expected)
            throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", name, "text/plain", content);
        mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(expected);
    }

    private User save(String username, String email, String phone, Role role, long quota) {
        return userRepository.save(User.builder()
                .firstName("Quota")
                .lastName("User")
                .username(username)
                .email(email)
                .password(passwordEncoder.encode("Secure@123"))
                .phoneNumber(phone)
                .role(role)
                .status(UserStatus.ACTIVE)
                .storageQuota(quota)
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
