package com.battybuilds.webclientoauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebAppConfiguration
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
//@ActiveProfiles("local")  Can be added if needed - still works
class SecureWebClientIntegrationTest {

    public static MockWebServer mockAbcServer;
    public static MockWebServer mockAuthServer;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    JwtDecoder jwtDecoder;

    @BeforeAll
    static void beforeAll() throws IOException {
        mockAbcServer = new MockWebServer();
        mockAuthServer = new MockWebServer();
        mockAbcServer.start();
    }

    @DynamicPropertySource
    static void backendUrlProperties(DynamicPropertyRegistry registry) {
        registry.add("abc-base-url", () -> mockAbcServer.url("/").toString());
        registry.add("azure-token-url", () -> mockAuthServer.url("/") + "/token");
    }

    @AfterAll
    static void afterAll() throws IOException {
        mockAbcServer.shutdown();
        mockAuthServer.shutdown();
    }

    @Test
    void successResponse() throws Exception {
        WokeResponse wokeResponse = WokeResponse.builder()
                .alarm1("Time to get up")
                .alarm2("You're gonna be late")
                .alarm3("Your boss is calling")
                .build();
        String responseBody = objectMapper.writeValueAsString(wokeResponse);
        mockBackendEndpoint(200, responseBody);

        ResultActions resultActions = executeRequest();

        assertThat(mockAbcServer.getRequestCount()).isEqualTo(2);
        assertThat(mockAuthServer.getRequestCount()).isEqualTo(1);
        assertCorrectResponse(resultActions, 200, "\"alarm1\":\"Time to get up\"", "\"alarm2\":\"You're gonna be late\"");

        RecordedRequest recordedTokenRequest = mockAuthServer.takeRequest();
        assertThat(recordedTokenRequest.getPath()).isEqualTo("/token");
        String expectedTokenRequestBody = "[size=74 text=grant_type=client_credentials&client_id=456&client_secret=abc&re…]";
        assertThat(recordedTokenRequest.getBody().readByteString().toString()).isEqualTo(expectedTokenRequestBody);
        RecordedRequest recordedAbcRequest = mockAbcServer.takeRequest();
        assertThat(recordedAbcRequest.getPath()).isEqualTo("/api/clock/alarms");
        assertThat(recordedAbcRequest.getMethod()).isEqualTo("GET");
    }

    @Test
    void handles500ErrorsFromBackendServer() throws Exception {
        WokeResponse wokeResponse = WokeResponse.builder().error("What does that even mean?").build();
        String responseBody = objectMapper.writeValueAsString(wokeResponse);
        mockTokenCall();
        mockBackendEndpoint(500, responseBody);

        ResultActions resultActions = executeRequest();

        assertThat(mockAbcServer.getRequestCount()).isEqualTo(1);
        assertThat(mockAuthServer.getRequestCount()).isEqualTo(1);
        assertCorrectResponse(resultActions, 500, "\"error\":\"500 Internal Server Error", "context: WAKEUP");
    }

    private void mockBackendEndpoint(int responseCode, String body) {
        MockResponse mockResponse = new MockResponse().setResponseCode(responseCode)
                .setBody(body)
                .addHeader("Content-Type", "application/json");
        mockAbcServer.enqueue(mockResponse);
    }

    private void mockTokenCall() {
        MockResponse mockTokenResponse = createMockTokenResponse();
        mockAuthServer.enqueue(mockTokenResponse);
    }

    private MockResponse createMockTokenResponse() {
        String tokenResponse = "{\n" +
                "    \"token_type\": \"Bearer\",\n" +
                "    \"expires_in\": \"3599\",\n" +
                "    \"ext_expires_in\": \"3599\",\n" +
                "    \"expires_on\": \"1643312777\",\n" +
                "    \"not_before\": \"1643308877\",\n" +
                "    \"resource\": \"abc\",\n" +
                "    \"access_token\": \"mock-Token\"\n" +
                "}";

        return new MockResponse().setResponseCode(200)
                .setBody(tokenResponse)
                .addHeader("Content-Type", "application/json; charset=utf-8");
    }

    private ResultActions executeRequest() throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders
                .get("/v2/alarms")
                .header("Identification-No", "app-id")
                .header("Authorization", "Bearer 123"));
    }

    private void assertCorrectResponse(ResultActions resultActions, int status, String... message) throws Exception {
        resultActions
                .andDo(print())
                .andExpect(status().is(status));
        String responseBody = resultActions.andReturn().getResponse().getContentAsString();
        assertThat(responseBody).contains(message);
    }
}
