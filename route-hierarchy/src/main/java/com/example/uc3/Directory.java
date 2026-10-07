package com.example.uc3;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tiny in-memory people directory backing UC3, so the dynamic breadcrumb label
 * has real data to resolve a {@code :userId} against.
 */
final class Directory {

    static final Map<String, String> USERS = new LinkedHashMap<>();

    static {
        USERS.put("ada", "Ada Lovelace");
        USERS.put("alan", "Alan Turing");
        USERS.put("grace", "Grace Hopper");
    }

    private Directory() {
    }

    static String nameOf(String userId) {
        return USERS.getOrDefault(userId, "Unknown user");
    }
}
