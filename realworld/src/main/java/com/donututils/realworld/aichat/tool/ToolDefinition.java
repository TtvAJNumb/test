package com.donututils.realworld.aichat.tool;

import java.util.Map;

public record ToolDefinition(String name, Map<String, Object> schema, ToolExecutor executor) {
}
