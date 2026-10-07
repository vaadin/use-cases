package com.example.security;

import com.example.views.LoginView;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    /**
     * Query parameter that makes any page require authentication. Adding it to
     * the current URL sends a guest through the login page and, thanks to
     * Spring Security's saved request, back to the same page afterwards.
     */
    public static final String LOGIN_PARAMETER = "login";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        // Allow access to profile pictures and the per-view stylesheets
        // (loaded via @StyleSheet) without authentication
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/profile-pictures/**", "/*.css").permitAll()
                .requestMatchers(request -> request
                        .getParameter(LOGIN_PARAMETER) != null)
                .authenticated());

        // Configure Vaadin's security using VaadinSecurityConfigurer
        http.with(VaadinSecurityConfigurer.vaadin(), configurer -> {
            // Register the login view for navigation access control
            configurer.loginView(LoginView.class);
        });

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(
            PasswordEncoder passwordEncoder) {
        // Hardcoded users with different roles
        return new InMemoryUserDetailsManager(
                // viewer: password / Roles: VIEWER
                User.withUsername("viewer")
                        .password(passwordEncoder.encode("password"))
                        .roles("VIEWER").build(),

                // editor: password / Roles: EDITOR
                User.withUsername("editor")
                        .password(passwordEncoder.encode("password"))
                        .roles("EDITOR").build(),

                // admin: password / Roles: ADMIN
                User.withUsername("admin")
                        .password(passwordEncoder.encode("password"))
                        .roles("ADMIN").build(),

                // superadmin: password / Roles: SUPER_ADMIN
                User.withUsername("superadmin")
                        .password(passwordEncoder.encode("password"))
                        .roles("SUPER_ADMIN").build());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
