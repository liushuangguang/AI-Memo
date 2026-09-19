package com.newtech.note.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class ResourceConfiguration implements WebFluxConfigurer {
    private final ImageFallbackStorageProperties fallbackStorage;

    @Autowired
    public ResourceConfiguration(ImageFallbackStorageProperties fallbackStorage) {
        this.fallbackStorage = fallbackStorage;
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler(fallbackStorage.resourceHandlerPattern())
                .addResourceLocations(fallbackStorage.resourceLocation())
                .setCacheControl(CacheControl.noStore().cachePrivate());
    }
}
