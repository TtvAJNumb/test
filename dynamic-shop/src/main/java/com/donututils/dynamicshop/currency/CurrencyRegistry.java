package com.donututils.dynamicshop.currency;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Holds whichever currencies actually got resolved this run (config-enabled AND their backing
 * plugin detected). Anything not in here is simply unavailable - items priced in a missing currency
 * are hidden rather than erroring. */
public final class CurrencyRegistry {

    private final Map<String, CurrencyProvider> providers = new LinkedHashMap<>();

    public void register(CurrencyProvider provider) {
        providers.put(provider.id(), provider);
    }

    public CurrencyProvider get(String id) {
        return providers.get(id);
    }

    public boolean isAvailable(String id) {
        return providers.containsKey(id);
    }

    public Collection<CurrencyProvider> all() {
        return providers.values();
    }

    public boolean isEmpty() {
        return providers.isEmpty();
    }
}
