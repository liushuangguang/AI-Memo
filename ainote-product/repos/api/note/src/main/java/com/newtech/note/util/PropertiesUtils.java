package com.newtech.note.util;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.stereotype.Component;
import org.springframework.util.StringValueResolver;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PropertiesUtils implements EmbeddedValueResolverAware {
    private static StringValueResolver stringValueResolver;

    @Override
    public void setEmbeddedValueResolver(@NotNull StringValueResolver stringValueResolver) {
        PropertiesUtils.stringValueResolver = stringValueResolver;
    }

    public static String getPropertyValue(String propertyName, String defaultValue) {
        if (PropertiesUtils.stringValueResolver == null) {
            return defaultValue; // 返回默认值而不是带有 ${} 的字符串
        }
        String finalPropertyName = "${" + propertyName + ":" + defaultValue + "}";
        return PropertiesUtils.stringValueResolver.resolveStringValue(finalPropertyName);
    }

}
