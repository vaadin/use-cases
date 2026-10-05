package com.example.security;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The demo app must be explorable without logging in first: an anonymous
 * visitor opening a use case is served the page instead of being redirected
 * to the login view.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AnonymousAccessTest {

    @LocalServerPort
    private int port;

    @ParameterizedTest
    @ValueSource(strings = { "/", "/use-case-01", "/muc-01" })
    void anonymousVisitor_isServedView_notRedirectedToLogin(String path)
            throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER).build();
        HttpResponse<Void> response = client.send(HttpRequest
                .newBuilder(URI.create("http://localhost:" + port + path))
                .build(), HttpResponse.BodyHandlers.discarding());

        assertEquals(200, response.statusCode(),
                () -> "Expected " + path + " to be served, got "
                        + response.statusCode() + " → "
                        + response.headers().firstValue("Location")
                                .orElse(""));
    }
}
