package com.example.uc1;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * For each account, the last version of the app whose tour the account has seen
 * (finished or skipped), kept on the server for the whole application. A real
 * app would store this with the user's profile.
 */
@Component
public class SeenVersions {

    private final Map<String, String> versions = new ConcurrentHashMap<>();

    /** The last version the account has seen the tour of, or {@code null}. */
    public @Nullable String get(String account) {
        return versions.get(account);
    }

    public void set(String account, @Nullable String version) {
        if (version == null) {
            versions.remove(account);
        } else {
            versions.put(account, version);
        }
    }
}
