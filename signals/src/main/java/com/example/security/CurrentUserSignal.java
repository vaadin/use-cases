package com.example.security;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vaadin.flow.spring.security.AuthenticationContext;

/**
 * Session-scoped signal holding the current user information. This signal is
 * reactive to authentication changes including login, logout, and
 * impersonation. Each user session has its own instance.
 * <p>
 * Visitors who have not logged in are represented by a guest user with a
 * generated username that stays the same for the whole session, so that
 * multi-user views can tell anonymous sessions apart.
 */
@Component
@VaadinSessionScope
public class CurrentUserSignal {

    public static class UserInfo {
        private final String username;
        private final Set<String> roles;
        private final boolean authenticated;

        public UserInfo(String username, Set<String> roles,
                boolean authenticated) {
            this.username = username;
            this.roles = roles;
            this.authenticated = authenticated;
        }

        public static UserInfo anonymous(String guestName) {
            return new UserInfo(guestName, Set.of(), false);
        }

        public static UserInfo authenticated(String username,
                Set<String> roles) {
            return new UserInfo(username, roles, true);
        }

        public String getUsername() {
            return username;
        }

        public Set<String> getRoles() {
            return roles;
        }

        public boolean isAuthenticated() {
            return authenticated;
        }

        public boolean hasRole(String role) {
            return roles.contains(role);
        }

        public boolean hasAnyRole(String... roles) {
            for (String role : roles) {
                if (this.roles.contains(role)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            if (!authenticated) {
                return username + " [anonymous]";
            }
            return username + " [" + String.join(", ", roles) + "]";
        }
    }

    private static final AtomicInteger GUEST_COUNTER = new AtomicInteger();

    private final ValueSignal<UserInfo> userSignal;
    private final AuthenticationContext authenticationContext;
    private final String guestName = "guest-"
            + GUEST_COUNTER.incrementAndGet();

    public CurrentUserSignal(AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;
        this.userSignal = new ValueSignal<>(getCurrentUserInfo());
    }

    /**
     * Get the signal holding current user information. This signal updates when
     * authentication state changes.
     */
    public ValueSignal<UserInfo> getUserSignal() {
        return userSignal;
    }

    /**
     * Refresh the user signal from the current authentication context. Call
     * this after login, logout, or impersonation changes.
     */
    public void refresh() {
        userSignal.set(getCurrentUserInfo());
    }

    private UserInfo getCurrentUserInfo() {
        return authenticationContext.getAuthenticatedUser(UserDetails.class)
                .map(user -> {
                    Set<String> roles = new java.util.HashSet<>(
                            authenticationContext.getGrantedRoles());
                    return UserInfo.authenticated(user.getUsername(), roles);
                }).orElse(UserInfo.anonymous(guestName));
    }
}
