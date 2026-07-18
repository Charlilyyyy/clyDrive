package com.clydrive.controller;

import com.clydrive.enums.Role;
import com.clydrive.enums.UserStatus;
import com.clydrive.module.User;
import com.clydrive.repository.EmailVerificationTokenRepository;
import com.clydrive.repository.FileRepository;
import com.clydrive.repository.FolderRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FolderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FolderRepository folderRepository;

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

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        tokenBlacklistRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        loginAttemptRepository.deleteAll();
        emailVerificationTokenRepository.deleteAll();
        fileRepository.deleteAll();
        folderRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(User.builder()
                .firstName("Folder")
                .lastName("Owner")
                .username("folderowner")
                .email("folderowner@example.com")
                .password(passwordEncoder.encode("Secure@123"))
                .phoneNumber("9000000020")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .storageQuota(1073741824L)
                .storageUsed(0L)
                .emailVerified(true)
                .enabled(true)
                .accountNonLocked(true)
                .failedAttempts(0)
                .build());

        token = login("folderowner", "Secure@123").path("tokens").path("accessToken").asText();
    }

    @Test
    void createNestedFoldersAndResolveBreadcrumbs() throws Exception {
        long parentId = createFolder("Documents", null);
        long childId = createFolder("Invoices", parentId);

        mockMvc.perform(get("/api/v1/folders/{id}", childId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.folderName", is("Invoices")))
                .andExpect(jsonPath("$.data.breadcrumbs[0].name", is("Documents")))
                .andExpect(jsonPath("$.data.breadcrumbs[1].name", is("Invoices")));

        mockMvc.perform(get("/api/v1/folders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()", is(1)));
    }

    @Test
    void renameFolderUpdatesName() throws Exception {
        long id = createFolder("Temp", null);

        mockMvc.perform(put("/api/v1/folders/{id}/rename", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Archive\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Archive")));
    }

    @Test
    void deleteNonEmptyFolderIsRejected() throws Exception {
        long parentId = createFolder("Parent", null);
        createFolder("Child", parentId);

        mockMvc.perform(delete("/api/v1/folders/{id}", parentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Folder is not empty")));
    }

    @Test
    void duplicateFolderNameIsRejected() throws Exception {
        createFolder("Photos", null);

        mockMvc.perform(post("/api/v1/folders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Photos\"}"))
                .andExpect(status().isConflict());
    }

    private long createFolder(String name, Long parentId) throws Exception {
        String body = parentId == null
                ? "{\"name\":\"%s\"}".formatted(name)
                : "{\"name\":\"%s\",\"parentId\":%d}".formatted(name, parentId);

        MvcResult result = mockMvc.perform(post("/api/v1/folders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
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
