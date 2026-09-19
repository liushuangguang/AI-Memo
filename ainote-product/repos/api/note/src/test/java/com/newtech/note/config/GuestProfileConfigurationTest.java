package com.newtech.note.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GuestProfileConfigurationTest {

    private static final String GUEST_ENABLED = "app.guest.enabled";

    @Test
    void localDeliveryProfileEnablesTemporaryGuestModeByDefault() throws IOException {
        MockEnvironment environment = environmentWithLocalProperties();

        assertThat(environment.resolvePlaceholders(environment.getProperty(GUEST_ENABLED)))
                .isEqualTo("true");
    }

    @Test
    void deploymentCanDisableTemporaryGuestMode() throws IOException {
        MockEnvironment environment = environmentWithLocalProperties();
        environment.setProperty("APP_GUEST_ENABLED", "false");

        assertThat(environment.resolvePlaceholders(environment.getProperty(GUEST_ENABLED)))
                .isEqualTo("false");
    }

    private static MockEnvironment environmentWithLocalProperties() throws IOException {
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);

        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("application-local", new ClassPathResource("application-local.yml"));
        sources.forEach(environment.getPropertySources()::addLast);
        return environment;
    }
}
