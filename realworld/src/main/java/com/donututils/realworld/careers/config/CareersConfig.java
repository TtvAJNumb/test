package com.donututils.realworld.careers.config;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public record CareersConfig(Map<String, JobDefinition> jobs, String welcomeMessage) {

    public JobDefinition job(String id) {
        return jobs.get(id.toLowerCase(Locale.ROOT));
    }

    public Map<String, JobDefinition> jobsFor(AgeTier tier) {
        Map<String, JobDefinition> result = new LinkedHashMap<>();
        for (Map.Entry<String, JobDefinition> entry : jobs.entrySet()) {
            if (entry.getValue().availableTo(tier)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }
}
