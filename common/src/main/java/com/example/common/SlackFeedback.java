package com.example.common;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.json.JsonMapper;

/**
 * Posts feedback on a use case to a Slack channel through an
 * <a href="https://api.slack.com/messaging/webhooks">incoming webhook</a>, so
 * the feedback from every app ends up in one place.
 * <p>
 * The webhook URL is read from the {@value #WEBHOOK_URL_ENV} environment
 * variable. It is a secret: anyone holding it can post to the channel, so it
 * stays on the server and is never sent to the browser. Without it the feedback
 * is disabled, which is the case when running an app locally.
 */
public class SlackFeedback {

    public static final String WEBHOOK_URL_ENV = "FEEDBACK_SLACK_WEBHOOK_URL";

    static final int MAX_MESSAGE_LENGTH = 2000;
    static final int MAX_CONTACT_LENGTH = 200;

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    /**
     * The use case feedback is given on.
     *
     * @param app
     *            the title of the app the use case belongs to
     * @param title
     *            the title of the use case
     * @param viewClass
     *            the fully qualified name of the use case's view
     * @param url
     *            the absolute URL of the use case
     */
    public record UseCase(String app, String title, String viewClass,
            String url) {
    }

    /**
     * Feedback on one use case.
     *
     * @param message
     *            the feedback itself, as typed by the user
     * @param contact
     *            whatever the user entered to say who they are, such as a name
     *            or an email address, or blank to stay anonymous
     */
    public record Feedback(UseCase useCase, String message, String contact) {
    }

    private final @Nullable URI webhook;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT).build();

    SlackFeedback(@Nullable String webhookUrl) {
        webhook = webhookUrl == null || webhookUrl.isBlank() ? null
                : URI.create(webhookUrl.trim());
    }

    public static SlackFeedback fromEnvironment() {
        return new SlackFeedback(System.getenv(WEBHOOK_URL_ENV));
    }

    public boolean isEnabled() {
        return webhook != null;
    }

    /**
     * Posts the feedback to Slack.
     *
     * @throws IOException
     *             if Slack could not be reached or did not accept the message
     * @throws IllegalStateException
     *             if the feedback is not {@link #isEnabled() enabled}
     */
    public void send(Feedback feedback)
            throws IOException, InterruptedException {
        if (webhook == null) {
            throw new IllegalStateException(
                    WEBHOOK_URL_ENV + " is not set, feedback is disabled");
        }
        HttpRequest request = HttpRequest.newBuilder(webhook).timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload(feedback)))
                .build();
        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Slack rejected the feedback with status "
                    + response.statusCode() + ": " + response.body());
        }
    }

    static String payload(Feedback feedback) {
        String contact = feedback.contact().isBlank() ? "_anonymous_"
                : escape(truncate(feedback.contact().strip(),
                        MAX_CONTACT_LENGTH));
        String quote = escape(
                truncate(feedback.message().strip(), MAX_MESSAGE_LENGTH))
                .lines().map(line -> "> " + line)
                .collect(Collectors.joining("\n"));
        UseCase useCase = feedback.useCase();
        String text = "*Use-case feedback*\n" + "*App:* "
                + escape(useCase.app()) + "\n" + "*Use case:* <" + useCase.url()
                + "|" + escape(useCase.title()) + "> (`" + useCase.viewClass()
                + "`)\n" + "*From:* " + contact + "\n" + quote;
        return JsonMapper.shared().writeValueAsString(
                Map.of("text", text, "unfurl_links", false));
    }

    /**
     * Escapes the characters Slack uses for its own markup, so a user cannot
     * ping {@code <!channel>} or disguise a link.
     */
    static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">",
                "&gt;");
    }

    private static String truncate(String text, int maxLength) {
        return text.length() <= maxLength ? text
                : text.substring(0, maxLength) + "…";
    }
}
