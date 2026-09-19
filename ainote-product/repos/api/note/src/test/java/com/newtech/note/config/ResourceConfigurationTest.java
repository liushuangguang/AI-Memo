package com.newtech.note.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.CacheControl;
import org.springframework.web.reactive.config.ResourceHandlerRegistration;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResourceConfigurationTest {
    @Test
    void uploadedCapabilityUrlsArePrivateAndNeverStored() {
        ImageFallbackStorageProperties properties =
                new ImageFallbackStorageProperties("upload-files", "upload-files");
        ResourceHandlerRegistry registry = mock(ResourceHandlerRegistry.class);
        ResourceHandlerRegistration registration = mock(ResourceHandlerRegistration.class);
        when(registry.addResourceHandler("/upload-files/**")).thenReturn(registration);
        when(registration.addResourceLocations(properties.resourceLocation())).thenReturn(registration);

        new ResourceConfiguration(properties).addResourceHandlers(registry);

        org.mockito.ArgumentCaptor<CacheControl> cacheControl =
                org.mockito.ArgumentCaptor.forClass(CacheControl.class);
        verify(registration).setCacheControl(cacheControl.capture());
        assertThat(cacheControl.getValue().getHeaderValue())
                .contains("no-store")
                .contains("private");
    }

    @Test
    void storageConfigurationUsesAbsoluteFileUriAndPreservesDefaultPublicPath() {
        ImageFallbackStorageProperties properties =
                new ImageFallbackStorageProperties("upload-files", "/upload-files/");

        assertThat(properties.uploadDirectory()).isAbsolute();
        assertThat(properties.resourceLocation())
                .startsWith("file:")
                .endsWith("/");
        assertThat(properties.resourceHandlerPattern()).isEqualTo("/upload-files/**");
        assertThat(properties.urlPath()).isEqualTo("upload-files");
    }

    @Test
    void rejectsBlankTraversalEmptySegmentAndWildcardPublicPaths() {
        for (String unsafe : new String[]{"", " ", "/", "..", "images/../private",
                "images//private", "images/**", "images/{name}", "images\\private",
                "images/%2e%2e/private"}) {
            assertThatThrownBy(() -> new ImageFallbackStorageProperties("upload-files", unsafe))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
