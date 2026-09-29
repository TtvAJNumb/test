package com.donututils.donutrep.crates;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public record CrateConfig(Map<String, CrateDefinition> crates) {
    public CrateDefinition crate(String id) {
        return crates.get(id.toLowerCase(Locale.ROOT));
    }

    public static Map<String, CrateDefinition> emptyMap() {
        return new LinkedHashMap<>();
    }
}
