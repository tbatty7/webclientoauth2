package com.battybuilds.webclientoauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@SpringBootTest
@TestPropertySource(properties = {"spring.main.allow-bean-definition-overriding=true"})
@AutoConfigureMockMvc(addFilters = false)
//@ActiveProfiles("local")  Profiles work differently now
@ContextConfiguration(classes = {Webclientoauth2Application.class})
class SecureWebClientIntegrationTest {

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
    static void backendUrlProperties(DynamicPropertyRegistry registry) {
        registry.add("abc-base-url", () -> mockServer.url("/").toString());
        registry.add("azure-token-url", () -> mockServer.url("/") + "/token");
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
        mockTokenAndBackendEndpoint(200, responseBody);

        ResultActions resultActions = executeRequest();

        verify200Results(resultActions, 200, "\"alarm1\":\"Time to get up\"", "\"alarm2\":\"You're gonna be late\"");

        assertThat(mockServer.getRequestCount()).isEqualTo(2);
        RecordedRequest recordedTokenRequest = mockServer.takeRequest();
        assertThat(recordedTokenRequest.getPath()).isEqualTo("/token");
        String expectedTokenRequestBody = "[size=74 text=grant_type=client_credentials&client_id=456&client_secret=abc&re…]";
        assertThat(recordedTokenRequest.getBody().readByteString().toString()).isEqualTo(expectedTokenRequestBody);
        RecordedRequest recordedAbcRequest = mockServer.takeRequest();
        assertThat(recordedAbcRequest.getPath()).isEqualTo("/api/clock/alarms");
        assertThat(recordedAbcRequest.getMethod()).isEqualTo("GET");
    }

    //    @Test
    void handles500ErrorsFromBackendServer() throws Exception {
        WokeResponse wokeResponse = WokeResponse.builder().error("What does that even mean?").build();
        String responseBody = objectMapper.writeValueAsString(wokeResponse);
        mockTokenAndBackendEndpoint(500, responseBody);

        ResultActions resultActions = executeRequest();

        verify200Results(resultActions, 500, "\"error\":\"500 Internal Server Error", "context: WAKEUP");
    }

    private void mockTokenAndBackendEndpoint(int responseCode, String body) {
        MockResponse mockTokenResponse = createMockTokenResponse();
        MockResponse mockResponse = new MockResponse().setResponseCode(responseCode)
                .setBody(body)
                .addHeader("Content-Type", "application/json");
        mockServer.enqueue(mockTokenResponse);
        mockServer.enqueue(mockResponse);
    }

    private MockResponse createMockTokenResponse() {
        String tokenResponse = "{\n" +
                "    \"token_type\": \"Bearer\",\n" +
                "    \"expires_in\": \"3599\",\n" +
                "    \"ext_expires_in\": \"3599\",\n" +
                "    \"expires_on\": \"1643312777\",\n" +
                "    \"not_before\": \"1643308877\",\n" +
                "    \"resource\": \"abc\",\n" +
                "    \"access_token\": \"eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6Ik1yNS1BVWliZkJpaTdOZDFqQmViYXhib1hXMCIsImtpZCI6Ik1yNS1BVWliZkJpaTdOZDFqQmViYXhib1hXMCJ9.eyJhdWQiOiIzOGE1YjZiMi1lNTI5LTQwMGEtOWM2NC0wYTQ0Yjk2NDdhZWMiLCJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC9jOTkwYmI3YS01MWY0LTQzOWItYmQzNi05YzA3ZmIxMDQxYzAvIiwiaWF0IjoxNjQzMzA4ODc3LCJuYmYiOjE2NDMzMDg4NzcsImV4cCI6MTY0MzMxMjc3NywiYWlvIjoiRTJaZ1lQaHFwSGt5dXNEOHZVdkZncDhGc3ozekFBPT0iLCJhcHBpZCI6IjM4YTViNmIyLWU1MjktNDAwYS05YzY0LTBhNDRiOTY0N2FlYyIsImFwcGlkYWNyIjoiMSIsImlkcCI6Imh0dHBzOi8vc3RzLndpbmRvd3MubmV0L2M5OTBiYjdhLTUxZjQtNDM5Yi1iZDM2LTljMDdmYjEwNDFjMC8iLCJvaWQiOiIwZjBlNDViYi0xNzA2LTQzYWEtODE4Zi0xZjVmMTVmZTQzMjUiLCJyaCI6IjAuQVJJQWVydVF5ZlJSbTBPOU5wd0gteEJCd0xLMnBUZ3A1UXBBbkdRS1JMbGtldXdTQUFBLiIsInN1YiI6IjBmMGU0NWJiLTE3MDYtNDNhYS04MThmLTFmNWYxNWZlNDMyNSIsInRpZCI6ImM5OTBiYjdhLTUxZjQtNDM5Yi1iZDM2LTljMDdmYjEwNDFjMCIsInV0aSI6Im02Q1BBeTd2ZEVXd3lKYWo4Z21ZQUEiLCJ2ZXIiOiIxLjAifQ.HrN33Rx1yTq70nYhqIdiD3eDV0mnaRXVP4eYVoQCNaKMxjl6A105GQY2vVtE-rhuWZspPWi1miSO48sUOmVCHucyDvdekRkcpOKTOCtPA7VzOXwn0rM3pnflAtO1ri33P1RvbvlooOtqx2R2kR9bQFkOqpMux1_LGC9PVBBM0nIES_QiE64ARqlQXgYdmNFGzyasoCXonuDA5-22-JErxsNj8uEb9lWGT2wbwjHg62NayYobXPCRx4TxIrcEfxfryfE2u7oc2Tr_s-R6coLQ6_x9Zruwq6Z7Byb3X0flXpg3OEUiRUrSGtBv7DEQg-7QO2bPfeWTjsKLaa7rfDtj9g\"\n" +
                "}";

        return new MockResponse().setResponseCode(200)
                .setBody(tokenResponse)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Cache-Control", "no-store, no-cache")
                .addHeader("Pragma", "no-cache")
                .addHeader("Expires", -1)
                .addHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
                .addHeader("X-Content-Type-Options", "nosniff")
                .addHeader("P3P", "CP=\"DSP CUR OTPi IND OTRi ONL FIN\"")
                .addHeader("x-ms-request-id", "038fa09b-ef2e-4574-b0c8-96a3f2099800")
                .addHeader("x-ms-ests-server", "2.1.12261.22 - EUS ProdSlices")
                .addHeader("Set-Cookie", "fpc=Ak0izF_GI7ZPmDum3vA5wLyvDvVdAQAAAHjfhNkOAAAA; expires=Sat, 26-Feb-2022 18:46:17 GMT; path=/; secure; HttpOnly; SameSite=None")
                .addHeader("Set-Cookie", "x-ms-gateway-slice=estsfd; path=/; secure; samesite=none; httponly")
                .addHeader("Set-Cookie", "stsservicecookie=estsfd; path=/; secure; samesite=none; httponly")
                .addHeader("Date", "Thu, 27 Jan 2022 18:46:16 GMT");
    }

    private ResultActions executeRequest() throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders
                .get("/v2/alarms")
                .header("Identification-No", "app-id")
                .header("Authorization", "Bearer 123"));
    }

    private void verify200Results(ResultActions resultActions, int status, String... message) throws Exception {
        resultActions
                .andDo(print())
                .andExpect(status().is(status));
        String responseBody = resultActions.andReturn().getResponse().getContentAsString();
        assertThat(responseBody).contains(message);
    }
}
