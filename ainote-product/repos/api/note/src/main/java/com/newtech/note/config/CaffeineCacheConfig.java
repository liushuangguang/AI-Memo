package com.newtech.note.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.newtech.note.common.constant.CaffeineCacheEnum;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableCaching
public class CaffeineCacheConfig implements Serializable {

    @Bean
    public CacheManager caffeineCacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        List<Cache> cacheList = new ArrayList<>();

        // 遍历枚举类型，为每个缓存创建一个 CaffeineCache
        for (CaffeineCacheEnum item : CaffeineCacheEnum.values()) {
            cacheList.add(new CaffeineCache(
                    item.name(),
                    Caffeine.newBuilder()
                            .maximumSize(item.getMaxSize())
                            .expireAfterWrite(item.getDuration(), item.getUnit())
                            .build()
            ));
        }

        // 设置缓存
        cacheManager.setCaches(cacheList);
        return cacheManager;
    }
}
