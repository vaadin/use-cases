package com.example.common;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.sun.net.httpserver.HttpServer;
import tools.jackson.databind.json.JsonMapper;

/**
 * A local stand-in for a Slack incoming webhook that records the messages
 * posted to it.
 */
class FakeSlackWebhook implements AutoCloseable {

    private final HttpServer server;
    private final List<String> requests = new ArrayList<>();
    private int status = 200;

    FakeSlackWebhook() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/hook", exchange -> {
            requests.add(new String(exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8));
            byte[] body = (status == 200 ? "ok" : "invalid_payload")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    SlackFeedback slackFeedback() {
        return new SlackFeedback(
                "http://localhost:" + server.getAddress().getPort() + "/hook");
    }

    void respondWith(int status) {
        this.status = status;
    }

    /** The text of each message posted so far. */
    List<String> texts() {
        return requests.stream().map(request -> JsonMapper.shared()
                .readTree(request).get("text").asString()).toList();
    }

    List<String> requests() {
        return requests;
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
