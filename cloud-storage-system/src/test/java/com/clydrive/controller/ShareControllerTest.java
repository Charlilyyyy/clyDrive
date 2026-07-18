package com.clydrive.controller;

import com.clydrive.enums.Role;
import com.clydrive.enums.UserStatus;
import com.clydrive.module.User;
import com.clydrive.repository.EmailVerificationTokenRepository;
import com.clydrive.repository.FileRepository;
import com.clydrive.repository.LoginAttemptRepository;
import com.clydrive.repository.RefreshTokenRepository;
import com.clydrive.repository.ShareLinkRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShareControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private ShareLinkRepository shareLinkRepository;

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

    private String token;
    private long fileId;

    @BeforeEach
    void setUp() throws Exception {
        shareLinkRepository.deleteAll();
        tokenBlacklistRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        loginAttemptRepository.deleteAll();
        emailVerificationTokenRepository.deleteAll();
        fileRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(User.builder()
                .firstName("Share")
                .lastName("Owner")
                .username("shareowner")
                .email("shareowner@example.com")
                .password(passwordEncoder.encode("Secure@123"))
                .phoneNumber("9000000030")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .storageQuota(1073741824L)
                .storageUsed(0L)
                .emailVerified(true)
                .enabled(true)
                .accountNonLocked(true)
                .failedAttempts(0)
                .build());

        token = login("shareowner", "Secure@123").path("tokens").path("accessToken").asText();

        MockMultipartFile upload = new MockMultipartFile(
                "file", "shared.txt", "text/plain", "shared content".getBytes());
        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(upload)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();
        fileId = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    @Test
    void publicCanDownloadViaShareLink() throws Exception {
        String shareToken = createShare("{}");

        mockMvc.perform(get("/api/v1/share/{token}", shareToken))
                .andExpect(status().isOk())
                .andExpect(content().string("shared content"));
    }

    @Test
    void passwordProtectedShareRequiresPassword() throws Exception {
        String shareToken = createShare("{\"password\":\"open-sesame\"}");

        mockMvc.perform(get("/api/v1/share/{token}", shareToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/share/{token}", shareToken).param("password", "open-sesame"))
                .andExpect(status().isOk())
                .andExpect(content().string("shared content"));
    }

    @Test
    void revokedShareCannotBeAccessed() throws Exception {
        String shareToken = createShare("{}");

        mockMvc.perform(delete("/api/v1/share/{token}", shareToken)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/share/{token}", shareToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Share link has been revoked")));
    }

    private String createShare(String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/files/{fileId}/share", fileId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("token").asText();
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
