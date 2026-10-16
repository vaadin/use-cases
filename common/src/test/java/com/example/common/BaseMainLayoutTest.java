package com.example.common;

import java.io.IOException;
import java.util.Optional;

import com.example.common.layout.About;
import com.example.common.layout.ParallelLoadingView;
import com.example.common.layout.UnreleasedApiView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextArea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ViewPackages(classes = ParallelLoadingView.class)
class BaseMainLayoutTest extends BrowserlessTest {

    private FakeSlackWebhook webhook;

    @BeforeEach
    void startWebhook() throws IOException {
        webhook = new FakeSlackWebhook();
        TestMainLayout.slackFeedback = webhook.slackFeedback();
    }

    @AfterEach
    void stopWebhook() {
        webhook.close();
        TestMainLayout.slackFeedback = new SlackFeedback(null);
    }

    @Test
    void feedback_sendsTheOpenUseCaseToSlack() {
        navigate(ParallelLoadingView.class);
        test(feedbackButton()).click();

        Dialog dialog = $(Dialog.class).single();
        assertEquals("Feedback on Use Case 19: Parallel Loading",
                dialog.getHeaderTitle());
        test($(TextArea.class).single()).setValue("Loading never finishes");
        test(button("Send")).click();

        assertFalse(dialog.isOpened());
        assertEquals(1, webhook.texts().size());
        String text = webhook.texts().get(0);
        assertTrue(text.contains("*App:* Signal API"), text);
        assertTrue(text.contains(
                "<https://signals-cases.fly.dev/parallel-loading|Use Case 19: Parallel Loading>"),
                text);
        assertTrue(text.contains(ParallelLoadingView.class.getName()), text);
        assertTrue(text.endsWith("> Loading never finishes"), text);
    }

    @Test
    void appUrl_trustsOnlyAPreviewOfTheApp() {
        String production = "https://signals-cases.fly.dev/";

        assertEquals("https://signals-cases-pr-12.fly.dev/",
                BaseMainLayout.appUrl(production,
                        Optional.of("https://signals-cases-pr-12.fly.dev/")));
        for (String other : new String[] { "https://evil.example/",
                "https://signals-cases-pr-12.fly.dev.evil.example/",
                "https://signals-cases-pr-x.fly.dev/",
                "https://web-share-cases-pr-12.fly.dev/",
                "http://localhost:8080/" }) {
            assertEquals(production,
                    BaseMainLayout.appUrl(production, Optional.of(other)),
                    other);
        }
        assertEquals(production,
                BaseMainLayout.appUrl(production, Optional.empty()));
    }

    @Test
    void sourceUrl_linksToTheGivenRefOrElseMain() {
        String path = "/signals/src/main/java/com/example/common/layout/ParallelLoadingView.java";

        assertEquals("https://github.com/vaadin/use-cases/tree/0a1b2c3" + path,
                BaseMainLayout.sourceUrl("0a1b2c3", "signals",
                        ParallelLoadingView.class));
        for (String none : new String[] { null, "", " " }) {
            assertEquals("https://github.com/vaadin/use-cases/tree/main" + path,
                    BaseMainLayout.sourceUrl(none, "signals",
                            ParallelLoadingView.class),
                    String.valueOf(none));
        }
    }

    @Test
    void feedback_slackFails_keepsTheDialogOpen() {
        webhook.respondWith(500);
        navigate(ParallelLoadingView.class);
        test(feedbackButton()).click();

        test($(TextArea.class).single()).setValue("Loading never finishes");
        test(button("Send")).click();

        assertTrue($(Dialog.class).single().isOpened());
    }

    @Test
    void feedback_isOnlyOfferedOnUseCaseViews() {
        navigate(About.class);

        assertTrue($(Button.class).withText("Feedback").all().isEmpty());
    }

    @Test
    void feedback_withoutWebhook_isNotOffered() {
        TestMainLayout.slackFeedback = new SlackFeedback(null);
        navigate(ParallelLoadingView.class);

        assertTrue($(Button.class).withText("Feedback").all().isEmpty());
    }

    @Test
    void unreleasedApi_showsABannerLinkingToThePullRequest() {
        navigate(UnreleasedApiView.class);

        Anchor link = $(Anchor.class).withText("vaadin/flow#26495").single();
        assertEquals("https://github.com/vaadin/flow/pull/26495",
                link.getHref());
    }

    @Test
    void unreleasedApi_bannerIsHiddenOnAViewWithoutIt() {
        navigate(UnreleasedApiView.class);
        navigate(ParallelLoadingView.class);

        assertTrue($(Div.class).withClassName("unreleased-api-banner").all()
                .isEmpty());
    }

    @Test
    void pullRequestLabel_shortensAGitHubPullRequestUrl() {
        assertEquals("vaadin/flow-components#10356",
                BaseMainLayout.pullRequestLabel(
                        "https://github.com/vaadin/flow-components/pull/10356"));
        assertEquals("https://example.com/change/1", BaseMainLayout
                .pullRequestLabel("https://example.com/change/1"));
    }

    private Button feedbackButton() {
        return button("Feedback");
    }

    private Button button(String text) {
        return $(Button.class).withText(text).single();
    }
}
