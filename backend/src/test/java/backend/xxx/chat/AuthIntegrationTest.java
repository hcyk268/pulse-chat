package backend.xxx.chat;

import java.util.Map;

import backend.xxx.chat.auth.model.RefreshTokenSession;
import backend.xxx.chat.common.util.TokenHash;
import backend.xxx.chat.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class AuthIntegrationTest {

    private static final String PASSWORD = "Password123!";
    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh:";
    private static final String TRUNCATE_SQL = "TRUNCATE TABLE message_attachments, message_reads, message_reactions, message_pins, messages, conversation_participants, conversations, presences, uploaded_assets, upload_parts, upload_sessions, outbox_events, users RESTART IDENTITY CASCADE";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("chat_app")
            .withUsername("chat_user")
            .withPassword("chat_password");

    @Container
    private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private TokenHash tokenHash;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void resetState() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.serverCommands().flushDb();
        }

        jdbcTemplate.execute(TRUNCATE_SQL);
    }

    @Test
    void registerPersistsUserAndRefreshSessionInPostgresAndRedis() throws Exception {
        String username = "alice-register";
        String email = "alice.register@example.com";

        JsonNode data = register(username, email, "Alice Register");

        assertThat(data.path("tokenType").asText()).isEqualTo("Bearer");
        assertThat(data.path("accessToken").asText()).isNotBlank();
        assertThat(data.path("refreshToken").asText()).isNotBlank();
        assertThat(data.path("user").path("username").asText()).isEqualTo(username);
        assertThat(userRepository.existsByUsernameIgnoreCase(username)).isTrue();

        String refreshToken = data.path("refreshToken").asText();
        RefreshTokenSession session = refreshSession(refreshToken);
        assertThat(session).isNotNull();
        assertThat(session.username()).isEqualTo(username);
        assertThat(session.userId()).isEqualTo(userRepository.findByUsernameIgnoreCase(username).orElseThrow().getId());
    }

    @Test
    void loginAllowsBearerAccessToCurrentProfile() throws Exception {
        String username = "alice-profile";
        String email = "alice.profile@example.com";
        String displayName = "Alice Profile";

        register(username, email, displayName);
        JsonNode loginData = login(email, PASSWORD);
        String accessToken = loginData.path("accessToken").asText();

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.displayName").value(displayName));
    }

    @Test
    void refreshRotatesRefreshTokenAndLogoutInvalidatesIt() throws Exception {
        String username = "alice-refresh";
        String email = "alice.refresh@example.com";

        JsonNode registerData = register(username, email, "Alice Refresh");
        String oldRefreshToken = registerData.path("refreshToken").asText();

        JsonNode refreshedData = refresh(oldRefreshToken);
        String newRefreshToken = refreshedData.path("refreshToken").asText();

        assertThat(newRefreshToken).isNotEqualTo(oldRefreshToken);
        assertThat(refreshSession(oldRefreshToken)).isNull();
        assertThat(refreshSession(newRefreshToken)).isNotNull();

        logout(newRefreshToken);
        assertThat(refreshSession(newRefreshToken)).isNull();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody(Map.of("refreshToken", newRefreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("auth.invalid.refresh.token"));
    }

    private JsonNode register(String username, String email, String displayName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody(Map.of(
                                "username", username,
                                "email", email,
                                "displayName", displayName,
                                "password", PASSWORD,
                                "confirmPassword", PASSWORD
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        return readData(result);
    }

    private JsonNode login(String usernameOrEmail, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody(Map.of(
                                "usernameOrEmail", usernameOrEmail,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        return readData(result);
    }

    private JsonNode refresh(String refreshToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        return readData(result);
    }

    private void logout(String refreshToken) throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private RefreshTokenSession refreshSession(String refreshToken) {
        String key = REFRESH_TOKEN_KEY_PREFIX + tokenHash.hashRefreshToken(refreshToken);
        return (RefreshTokenSession) redisTemplate.opsForValue().get(key);
    }

    private JsonNode readData(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data");
    }

    private String jsonBody(Map<String, Object> payload) throws Exception {
        return objectMapper.writeValueAsString(payload);
    }
}