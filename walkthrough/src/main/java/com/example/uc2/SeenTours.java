package com.example.uc2;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * The accounts that have finished or skipped the tour, kept on the server for
 * the whole application. A real app would store this with the user's profile.
 */
@Component
public class SeenTours {

    private final Set<String> accounts = ConcurrentHashMap.newKeySet();

    public boolean hasSeen(String account) {
        return accounts.contains(account);
    }

    public void markSeen(String account) {
        accounts.add(account);
    }

    public void forget(String account) {
        accounts.remove(account);
    }
}
