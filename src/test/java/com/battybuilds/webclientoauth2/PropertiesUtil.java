package com.battybuilds.webclientoauth2;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesUtil {

    public static final String SPRING_PROFILES_ACTIVE = "spring.profiles.active";

    public static Properties getProperties() throws IOException {
        String env = System.getenv("SPRING_PROFILES_ACTIVE");
        String springProfile = env != null ? env : System.getProperty(SPRING_PROFILES_ACTIVE);

        if (null == springProfile) {
            springProfile = "";
        } else {
            springProfile = "-" + springProfile;
        }

        Properties properties = new Properties();
        InputStream base = PropertiesUtil.class.getClassLoader().getResourceAsStream("application.properties");
        InputStream profile = PropertiesUtil.class.getClassLoader().getResourceAsStream("application" + springProfile + ".properties");
        if (base != null)
            properties.load(base);
        if (profile != null)
            properties.load(profile);
        return properties;
    }
}
