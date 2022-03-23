package com.battybuilds.webclientoauth2;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class SpringProfilesWithoutActiveProfileAnnotationTest {

    // must be run with "./gradlew build -Dspring.profiles.active=local" to pass

    @Value("${azure-token-url}")
    String azureTokenUrl;

    @Value("${spring.profiles.active}")
    String springProfile;

    @Test
    void canGetProfileSpecificProperty() {
        assertThat(springProfile).isEqualTo("local");

        assertThat(azureTokenUrl).isEqualTo("http://localhost");
    }
}
