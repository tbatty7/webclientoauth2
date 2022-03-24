package com.battybuilds.webclientoauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class WebClientIntegrationTest {

    public static MockWebServer mockServer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

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
    void handlesSuccessResponseForGET() throws Exception {
        WokeResponse wokeResponse = WokeResponse.builder()
                .alarm1("Time to get up")
                .alarm2("You're gonna be late")
                .alarm3("Your boss is calling")
                .build();
        String responseBody = objectMapper.writeValueAsString(wokeResponse);
        mockExternalEndpoint(200, responseBody);

        ResultActions result = executeRequest();

        assertBackendServerWasCalledCorrectlyForGET(mockServer.takeRequest());
        verifyResults(result, 200, "\"alarm1\":\"Time to get up\"", "\"alarm2\":\"You're gonna be late\"");
    }

    @Test
    void handles500ErrorsFromBackendServerForGET() throws Exception {
        WokeResponse wokeResponse = WokeResponse.builder().error("What does that even mean?").build();
        String responseBody = objectMapper.writeValueAsString(wokeResponse);
        mockExternalEndpoint(500, responseBody);

        ResultActions result = executeRequest();

        assertBackendServerWasCalledCorrectlyForGET(mockServer.takeRequest());
        verifyResults(result, 500, "\"error\":\"500 Internal Server Error", "context: WAKEUP");
    }

    @Test
    void handlesSuccessResponseForPOST() throws Exception {
        mockExternalEndpoint(200, "{\"alarm1\": \"Hello World\"}");
        String requestBody = new ObjectMapper().writeValueAsString(AlarmRequest.builder().day(10).hour(10).month(10).year(1972).message("Hi").build());

        ResultActions result = executePostRequest(requestBody, "app-id");

        assertBackendServerWasCalledCorrectlyForPOST(mockServer.takeRequest(5L, TimeUnit.SECONDS));
        verifyResults(result, 200, "\"alarm1\":\"Hello World\"", "\"identificationNumber\":\"app-id\"");
    }

    private ResultActions executePostRequest(String requestBody, String identificationNumber) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders
                .post("/v1/alarm")
                .header("Identification-No", identificationNumber)
                .header("Content-Type", "application/json")
                .content(requestBody));
    }

    private void assertBackendServerWasCalledCorrectlyForPOST(RecordedRequest recordedRequest) {
        assertThat(recordedRequest).as("Request did not reach MockWebServer").isNotNull();
        assertThat(recordedRequest.getMethod()).isEqualTo("POST");
        String expectedRequestBody = "[text={\"year\":1972,\"month\":10,\"day\":10,\"hour\":10,\"message\":\"Hi\"}]";
        assertThat(recordedRequest.getBody().readByteString().toString()).isEqualTo(expectedRequestBody);
    }

    private void assertBackendServerWasCalledCorrectlyForGET(RecordedRequest recordedRequest) {
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getPath()).isEqualTo("/api/clock/alarms");
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

    private void verifyResults(ResultActions resultActions, int status, String... message) throws Exception {
        resultActions
                .andDo(print())
                .andExpect(status().is(status));
        String responseBody = resultActions.andReturn().getResponse().getContentAsString();
        Assertions.assertThat(responseBody).contains(message);
    }
}
