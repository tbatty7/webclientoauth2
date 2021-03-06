package com.battybuilds.webclientoauth2;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
public class SpringProfilesWithActiveProfileAnnotationTest {
// Can be run without passing a profile into gradle at the command line

    @Value("${azure-token-url}")
    String azureTokenUrl;

    @Value("${spring.profiles.active}")
    String springProfile;

    @Test
    void canGetProfileSpecificProperty() {

        assertThat(azureTokenUrl).isEqualTo("http://localhost");
    }
}
