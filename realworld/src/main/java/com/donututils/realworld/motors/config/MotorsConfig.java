package com.donututils.realworld.motors.config;

import java.util.Map;

public record MotorsConfig(Map<String, VehicleDefinition> vehicles) {
    public VehicleDefinition vehicle(String id) {
        return vehicles.get(id.toLowerCase(java.util.Locale.ROOT));
    }
}
