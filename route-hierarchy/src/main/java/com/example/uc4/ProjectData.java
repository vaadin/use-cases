package com.example.uc4;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory project/task data backing UC4's parameterised hierarchy.
 */
final class ProjectData {

    static final Map<String, String> PROJECTS = new LinkedHashMap<>();
    static final Map<String, String> TASKS = new LinkedHashMap<>();

    static {
        PROJECTS.put("apollo", "Project Apollo");
        PROJECTS.put("zephyr", "Project Zephyr");
        TASKS.put("1", "Draft the spec");
        TASKS.put("2", "Wire the backend");
        TASKS.put("3", "Ship the demo");
    }

    private ProjectData() {
    }

    static String projectName(String projectId) {
        return PROJECTS.getOrDefault(projectId, "Unknown project");
    }

    static String taskName(String taskId) {
        return TASKS.getOrDefault(taskId, "Unknown task");
    }

    static List<String> taskIds() {
        return List.copyOf(TASKS.keySet());
    }
}
