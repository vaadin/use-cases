package com.example.common;

import java.io.IOException;

import com.example.common.SlackFeedback.Feedback;
import com.example.common.SlackFeedback.Kind;
import com.example.common.SlackFeedback.UseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlackFeedbackTest {

    private static final UseCase USE_CASE = new UseCase("Signal API",
            "Use Case 19: Parallel Loading",
            "com.example.usecase19.UseCase19View",
            "https://signals-cases.fly.dev/use-case-19");

    private FakeSlackWebhook webhook;

    @BeforeEach
    void startWebhook() throws IOException {
        webhook = new FakeSlackWebhook();
    }

    @AfterEach
    void stopWebhook() {
        webhook.close();
    }

    @Test
    void send_postsTheFeedbackToTheWebhook() throws Exception {
        webhook.slackFeedback()
                .send(new Feedback(USE_CASE, Kind.WRONG,
                        "The button <!channel> is\nmisaligned & ugly",
                        "me@example.com"));

        assertEquals(1, webhook.requests().size());
        JsonNode payload = JsonMapper.shared()
                .readTree(webhook.requests().get(0));
        assertFalse(payload.get("unfurl_links").asBoolean());
        assertEquals(
                """
                        *Use-case feedback: Something is wrong*
                        *App:* Signal API
                        *Use case:* <https://signals-cases.fly.dev/use-case-19|Use Case 19: Parallel Loading> (`com.example.usecase19.UseCase19View`)
                        *From:* me@example.com
                        > The button &lt;!channel&gt; is
                        > misaligned &amp; ugly""",
                payload.get("text").asString());
    }

    @Test
    void send_withoutContact_isAnonymousAndTruncatesLongMessages()
            throws Exception {
        webhook.slackFeedback().send(new Feedback(USE_CASE, Kind.OTHER,
                "x".repeat(SlackFeedback.MAX_MESSAGE_LENGTH + 100), " "));

        String text = webhook.texts().get(0);
        assertTrue(text.contains("*From:* _anonymous_\n"), text);
        assertTrue(
                text.endsWith("> "
                        + "x".repeat(SlackFeedback.MAX_MESSAGE_LENGTH) + "…"),
                text);
    }

    @Test
    void send_rejectedBySlack_throws() {
        webhook.respondWith(400);

        IOException e = assertThrows(IOException.class,
                () -> webhook.slackFeedback().send(new Feedback(USE_CASE,
                        Kind.IMPROVEMENT, "More examples please", "")));
        assertTrue(e.getMessage().contains("invalid_payload"), e.getMessage());
    }

    @Test
    void withoutWebhookUrl_isDisabled() {
        for (String url : new String[] { null, "", "  " }) {
            SlackFeedback slack = new SlackFeedback(url);
            assertFalse(slack.isEnabled());
            assertThrows(IllegalStateException.class, () -> slack
                    .send(new Feedback(USE_CASE, Kind.OTHER, "Hello", "")));
        }
        assertTrue(webhook.slackFeedback().isEnabled());
    }
}
