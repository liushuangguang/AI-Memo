package com.newtech.note.common.enumeration;

import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;

@Getter
public enum ProductPlatform {
    DUO_DUO_KE(1, "多多客");

    private final int platformId;
    private final String platformName;

    ProductPlatform(int platformId, String platformName) {
        this.platformId = platformId;
        this.platformName = platformName;
    }

    public static List<Pair<String, String>> getDictEntries() {
        return Arrays.stream(ProductPlatform.values()).map(e -> Pair.of(String.valueOf(e.getPlatformId()), e.getPlatformName())).toList();

    }
}
