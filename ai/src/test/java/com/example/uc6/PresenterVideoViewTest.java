package com.example.uc6;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import com.example.AiTestConfiguration;
import com.example.AiTestConfiguration.FakeHeyGen;
import com.example.home.HomeView;
import com.example.uc6.HeyGenClient.VideoStatus;
import com.example.uc6.PresenterVideoView.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(AiTestConfiguration.class)
@ViewPackages(classes = { PresenterVideoView.class, HomeView.class })
class PresenterVideoViewTest extends SpringBrowserlessTest {

    @Autowired
    private FakeHeyGen heyGen;

    @BeforeEach
    void resetHeyGen() {
        heyGen.reset();
    }

    @Test
    void pollingFollowsTheJobUntilTheVideoIsReadyToPlay() throws Exception {
        navigate(PresenterVideoView.class);

        test(find(Button.class).withText("Generate video").single()).click();
        // Start and polls run on background threads; the fake HeyGen
        // completes the job on the third status call.
        long deadline = System.currentTimeMillis() + 5000;
        while (find(Video.class).all().isEmpty()
                && System.currentTimeMillis() < deadline) {
            runPendingSignalsTasks(100, TimeUnit.MILLISECONDS);
        }

        assertEquals(1, find(Video.class).all().size());
        assertTrue(find(Span.class).withClassName("uc6-badge").single()
                .getText().startsWith("Ready after"));
        assertEquals(3, heyGen.statusCalls(),
                "polling stops once the job is finished");
    }

    @Test
    void pollingStopsWhenTheUserLeavesTheView() throws Exception {
        navigate(PresenterVideoView.class);
        test(find(Button.class).withText("Generate video").single()).click();
        runPendingSignalsTasks(1, TimeUnit.SECONDS);

        navigate(HomeView.class);
        int callsWhenLeaving = heyGen.statusCalls();
        Thread.sleep(200);

        assertEquals(callsWhenLeaving, heyGen.statusCalls());
    }

    @Test
    void statusTextExplainsEachStageOfTheJob() {
        Instant start = Instant.parse("2026-10-15T10:00:00Z");
        Instant later = start.plusSeconds(75);

        assertEquals("Starting the video…", PresenterVideoView
                .statusText(new Job(null, start, start, null, null)));
        assertEquals("Waiting in HeyGen's queue · 1:15",
                PresenterVideoView.statusText(new Job("v", start, later,
                        new VideoStatus("pending", null, null, null), null)));
        assertEquals("Rendering · 1:15",
                PresenterVideoView.statusText(new Job("v", start, later,
                        new VideoStatus("processing", null, null, null),
                        null)));
        assertEquals("Rendering failed: Script too long",
                PresenterVideoView.statusText(
                        new Job("v", start, later, new VideoStatus("failed",
                                null, null, "Script too long"), null)));
        assertEquals("HeyGen: Insufficient credits",
                PresenterVideoView.statusText(new Job(null, start, later, null,
                        "HeyGen: Insufficient credits")));
    }
}
