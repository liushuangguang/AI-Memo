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

class CozeWorkflowPropertyResolutionTest {

    private static final String RELATED_PROPERTY = "workflow_id.search.related_link";
    private static final String IMAGE_PROPERTY = "workflow_id.image.gen";

    @Test
    void newEnvironmentNamesTakePrecedenceOverLegacyNames() throws IOException {
        MockEnvironment environment = environmentWithApplicationProperties();
        environment.setProperty("COZE_WORKFLOW_RELATED_LINK", "related-new");
        environment.setProperty("COZE_SEARCH_WORKFLOW_ID", "related-legacy");
        environment.setProperty("COZE_WORKFLOW_IMAGE", "image-new");
        environment.setProperty("COZE_IMAGE_WORKFLOW_ID", "image-legacy");

        assertThat(resolve(environment, RELATED_PROPERTY)).isEqualTo("related-new");
        assertThat(resolve(environment, IMAGE_PROPERTY)).isEqualTo("image-new");
    }

    @Test
    void legacyEnvironmentNamesAreUsedWhenNewNamesAreAbsent() throws IOException {
        MockEnvironment environment = environmentWithApplicationProperties();
        environment.setProperty("COZE_SEARCH_WORKFLOW_ID", "related-legacy");
        environment.setProperty("COZE_IMAGE_WORKFLOW_ID", "image-legacy");

        assertThat(resolve(environment, RELATED_PROPERTY)).isEqualTo("related-legacy");
        assertThat(resolve(environment, IMAGE_PROPERTY)).isEqualTo("image-legacy");
    }

    @Test
    void defaultsAreUsedWhenBothEnvironmentNamesAreAbsent() throws IOException {
        MockEnvironment environment = environmentWithApplicationProperties();

        assertThat(resolve(environment, RELATED_PROPERTY)).isEqualTo("7431908464874799113");
        assertThat(resolve(environment, IMAGE_PROPERTY)).isEqualTo("7379868069084069924");
    }

    private static MockEnvironment environmentWithApplicationProperties() throws IOException {
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);

        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yml"));
        sources.forEach(environment.getPropertySources()::addLast);
        return environment;
    }

    private static String resolve(MockEnvironment environment, String property) {
        return environment.resolvePlaceholders(environment.getProperty(property));
    }
}
