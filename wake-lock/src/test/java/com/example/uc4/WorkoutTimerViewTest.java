package com.example.uc4;

import java.time.Duration;
import java.util.List;

import com.example.home.HomeView;
import com.example.uc4.WorkoutTimerView.Countdown;
import com.example.uc4.WorkoutTimerView.Phase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;

import static com.example.WakeLockTestSupport.RELEASE;
import static com.example.WakeLockTestSupport.REQUEST;
import static com.example.WakeLockTestSupport.answerWakeLockCalls;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = { WorkoutTimerView.class, HomeView.class })
class WorkoutTimerViewTest extends SpringBrowserlessTest {

    @Test
    void lockIsHeldWhileTheTimerRuns() {
        navigate(WorkoutTimerView.class);
        answerWakeLockCalls();
        runPendingSignalsTasks();
        assertBadge("Released");

        test(button("Start")).click();
        assertEquals(List.of(REQUEST), answerWakeLockCalls());
        runPendingSignalsTasks();
        assertBadge("Holding — the screen stays on between sets");

        test(button("Pause")).click();
        assertEquals(List.of(RELEASE), answerWakeLockCalls());
        runPendingSignalsTasks();
        assertBadge("Released");
    }

    @Test
    void clockCountsDownWhileRunningAndStopsOnPause()
            throws InterruptedException {
        navigate(WorkoutTimerView.class);
        test(button("Start")).click();

        await().pollInSameThread().atMost(Duration.ofSeconds(5)).until(() -> {
            runPendingSignalsTasks();
            return !"00:30".equals(clock());
        });

        test(button("Pause")).click();
        // A tick that was already running may still land.
        Thread.sleep(500);
        runPendingSignalsTasks();
        String paused = clock();
        Thread.sleep(2000);
        runPendingSignalsTasks();
        assertEquals(paused, clock(), "no tick should arrive while paused");
    }

    @Test
    void resetReturnsToTheStartOfAWorkPhase() {
        navigate(WorkoutTimerView.class);
        test(button("Start")).click();
        answerWakeLockCalls();

        test(button("Reset")).click();
        runPendingSignalsTasks();

        assertEquals(List.of(RELEASE), answerWakeLockCalls());
        button("Start");
        assertEquals("00:30", clock());
        assertEquals("WORK", findInView(Span.class).withClassName("uc4-phase")
                .single().getText());
    }

    @Test
    void leavingWhileRunningReleasesTheLock() {
        navigate(WorkoutTimerView.class);
        answerWakeLockCalls();
        test(button("Start")).click();
        assertEquals(List.of(REQUEST), answerWakeLockCalls());

        navigate(HomeView.class);

        assertEquals(List.of(RELEASE), answerWakeLockCalls());
    }

    @Test
    void phasesAlternateWhenTheCountdownRunsOut() {
        Countdown lastWorkSecond = new Countdown(Phase.WORK, 1);
        assertEquals(new Countdown(Phase.WORK, 29), Countdown.START.tick());
        assertEquals(new Countdown(Phase.REST, WorkoutTimerView.REST_SECONDS),
                lastWorkSecond.tick());
        assertEquals(Countdown.START, new Countdown(Phase.REST, 1).tick());
    }

    private Button button(String text) {
        return findInView(Button.class).withText(text).single();
    }

    private String clock() {
        return findInView(Span.class).withClassName("uc4-clock").single()
                .getText();
    }

    private void assertBadge(String text) {
        assertEquals(text, findInView(Span.class).withClassName("status-badge")
                .single().getText());
    }
}
