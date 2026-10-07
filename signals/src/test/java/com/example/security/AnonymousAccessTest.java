package com.example.security;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The demo app must be explorable without logging in first: an anonymous
 * visitor opening a use case is served the page (and its per-view stylesheet)
 * instead of being redirected to the login view.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AnonymousAccessTest {

    @LocalServerPort
    private int port;

    @ParameterizedTest
    @ValueSource(strings = { "/", "/use-case-01", "/muc-01", "/muc08.css" })
    void anonymousVisitor_isServedPage_notRedirectedToLogin(String path)
            throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER).build();
        HttpResponse<Void> response = client.send(HttpRequest
                .newBuilder(URI.create("http://localhost:" + port + path))
                .build(), HttpResponse.BodyHandlers.discarding());

        assertEquals(200, response.statusCode(),
                () -> "Expected " + path + " to be served, got "
                        + response.statusCode() + " → "
                        + response.headers().firstValue("Location").orElse(""));
    }

    @Test
    void loginParameter_returnsToSamePageAfterLogin() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .cookieHandler(new CookieManager()).build();

        HttpResponse<Void> page = client.send(
                HttpRequest.newBuilder(url("/use-case-01?login")).build(),
                HttpResponse.BodyHandlers.discarding());
        assertEquals(302, page.statusCode());
        assertTrue(page.headers().firstValue("Location").orElse("")
                .endsWith("/login"));

        HttpResponse<Void> login = client.send(HttpRequest
                .newBuilder(url("/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers
                        .ofString("username=viewer&password=password"))
                .build(), HttpResponse.BodyHandlers.discarding());
        assertEquals(302, login.statusCode());
        String target = login.headers().firstValue("Location").orElse("");
        assertTrue(target.contains("/use-case-01?login"),
                () -> "Expected to return to the use case, got " + target);
    }

    private URI url(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
