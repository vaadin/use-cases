package com.example.uc6;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * In-memory team roster backing UC6's {@code :member} parameter.
 */
final class TeamMembers {

    static final Map<String, String> MEMBERS = new LinkedHashMap<>();

    static {
        MEMBERS.put("kim", "Kim Park");
        MEMBERS.put("lee", "Lee Wong");
        MEMBERS.put("rao", "Priya Rao");
    }

    private TeamMembers() {
    }

    static String nameOf(String memberId) {
        return MEMBERS.getOrDefault(memberId, "Unknown member");
    }
}
