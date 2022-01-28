package com.battybuilds.webclientoauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class WebClientIntegrationTest {

    public static MockWebServer mockServer;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    JwtDecoder jwtDecoder;

    @BeforeAll
    static void beforeAll() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();
    }

    @DynamicPropertySource
    static void backendProperties(DynamicPropertyRegistry registry) {
        registry.add("base-url", () -> mockServer.url("/").toString());
    }

    @AfterAll
    static void afterAll() throws IOException {
        mockServer.shutdown();
    }

    @Test
    void successResponse() throws Exception {
        WokeResponse wokeResponse = WokeResponse.builder()
                .alarm1("Time to get up")
                .alarm2("You're gonna be late")
                .alarm3("Your boss is calling")
                .build();
        String responseBody = objectMapper.writeValueAsString(wokeResponse);
        mockExternalEndpoint(200, responseBody);

        ResultActions resultActions = executeRequest();

        verify200Results(resultActions, 200, "\"alarm1\":\"Time to get up\"", "\"alarm2\":\"You're gonna be late\"");
        RecordedRequest recordedRequest = mockServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getPath()).isEqualTo("/api/clock/alarms");
    }

    @Test
    void handles500ErrorsFromBackendServer() throws Exception {
        WokeResponse wokeResponse = WokeResponse.builder().error("What does that even mean?").build();
        String responseBody = objectMapper.writeValueAsString(wokeResponse);
        mockExternalEndpoint(500, responseBody);

        ResultActions resultActions = executeRequest();
        mockServer.takeRequest();
        verify200Results(resultActions, 500, "\"error\":\"500 Internal Server Error", "context: WAKEUP");
    }

    @Test
    void forPOST_requestBodyIsCorrect() throws Exception {
        mockExternalEndpoint(200, "{\"alarm1\": \"Hello World\"}");
        String requestBody = new ObjectMapper().writeValueAsString(AlarmRequest.builder().day(10).hour(10).month(10).year(1972).message("Hi").build());
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/v1/alarm")
                        .header("Identification-No", "app-id")
                        .header("Authorization", "Bearer 123")
                        .header("Content-Type", "application/json")
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().is(200));
        RecordedRequest recordedRequest = mockServer.takeRequest(5L, TimeUnit.SECONDS);
        assertThat(recordedRequest).as("Request not showing at MockWebServer").isNotNull();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        String expectedRequestBody = "[text={\"year\":1972,\"month\":10,\"day\":10,\"hour\":10,\"message\":\"Hi\"}]";
        assertThat(recordedRequest.getBody().readByteString().toString()).isEqualTo(expectedRequestBody);
    }

    private void mockExternalEndpoint(int responseCode, String body) {
        MockResponse mockResponse = new MockResponse().setResponseCode(responseCode)
                .setBody(body)
                .addHeader("Content-Type", "application/json");
        mockServer.enqueue(mockResponse);
    }

    private ResultActions executeRequest() throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders
                .get("/v1/alarms")
                .header("Identification-No", "app-id")
                .header("Authorization", "Bearer 123"));
    }

    private void verify200Results(ResultActions resultActions, int status, String... message) throws Exception {
        resultActions
                .andDo(print())
                .andExpect(status().is(status));
        String responseBody = resultActions.andReturn().getResponse().getContentAsString();
        Assertions.assertThat(responseBody).contains(message);
    }
}
