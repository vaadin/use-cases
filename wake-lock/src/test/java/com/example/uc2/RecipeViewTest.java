package com.example.uc2;

import java.util.List;

import com.example.home.HomeView;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.wakelock.WakeLockAvailability;

import static com.example.WakeLockTestSupport.RELEASE;
import static com.example.WakeLockTestSupport.REQUEST;
import static com.example.WakeLockTestSupport.answerWakeLockCalls;
import static com.example.WakeLockTestSupport.simulateAvailability;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = { RecipeView.class, HomeView.class })
class RecipeViewTest extends SpringBrowserlessTest {

    @Test
    void openingRequestsTheLockAndLeavingReleasesIt() {
        navigate(RecipeView.class);
        runPendingSignalsTasks();
        assertBadge("Requested — waiting for the browser");

        assertEquals(List.of(REQUEST), answerWakeLockCalls());
        runPendingSignalsTasks();
        assertBadge("Holding — the screen stays on");

        navigate(HomeView.class);
        assertEquals(List.of(RELEASE), answerWakeLockCalls());
    }

    @Test
    void unsupportedBrowserSaysSoInsteadOfWaiting() {
        navigate(RecipeView.class);

        simulateAvailability(WakeLockAvailability.UNSUPPORTED);
        runPendingSignalsTasks();

        assertBadge("Not available — needs a supporting browser and HTTPS");
    }

    @Test
    void nextAndPreviousMoveTheCurrentStep() {
        navigate(RecipeView.class);
        runPendingSignalsTasks();
        assertEquals(List.of("current", "", "", "", "", ""), stepStates());

        test(findInView(Button.class).withText("Next step").single()).click();
        test(findInView(Button.class).withText("Next step").single()).click();
        runPendingSignalsTasks();
        assertEquals(List.of("done", "done", "current", "", "", ""),
                stepStates());

        test(findInView(Button.class).withText("Previous").single()).click();
        runPendingSignalsTasks();
        assertEquals(List.of("done", "current", "", "", "", ""), stepStates());
    }

    private List<String> stepStates() {
        return findInView(Div.class).withClassName("uc2-step").all().stream()
                .map(step -> step.hasClassName("current") ? "current"
                        : step.hasClassName("done") ? "done" : "")
                .toList();
    }

    private void assertBadge(String text) {
        assertEquals(text, findInView(Span.class).withClassName("status-badge")
                .single().getText());
    }
}
