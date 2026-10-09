package com.example.uc7;

import com.example.backend.MarketFeed;
import com.example.backend.MarketFeed.Tick;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// The generator is off: the test publishes every tick itself.
@SpringBootTest(properties = "app.feed.ticks-per-second=0")
@ViewPackages(classes = ThrottledFeedView.class)
class ThrottledFeedViewTest extends SpringBrowserlessTest {

    private static final int TICKS = 50;

    @Autowired
    private MarketFeed feed;

    @Test
    void viewRendersBoardInBatchedMode() {
        ThrottledFeedView view = navigate(ThrottledFeedView.class);

        assertEquals("UC7 — Throttle a live feed",
                findInView(H1.class).single().getText());
        assertEquals(ThrottledFeedView.Mode.BATCHED,
                findInView(RadioButtonGroup.class).single().getValue());
        assertEquals("–", view.priceOf("AURA"));
    }

    @Test
    void pushingEveryTickUpdatesTheUiOncePerTick() {
        ThrottledFeedView view = navigate(ThrottledFeedView.class);
        @SuppressWarnings("unchecked")
        RadioButtonGroup<ThrottledFeedView.Mode> mode = findInView(
                RadioButtonGroup.class).single();
        test(mode).selectItem("Push every tick");

        publishTicks();

        assertEquals(TICKS, view.uiUpdates());
        assertEquals("%.2f".formatted(price(TICKS - 1)), view.priceOf("AURA"));
    }

    @Test
    void batchingMergesTicksIntoOneUpdate() {
        ThrottledFeedView view = navigate(ThrottledFeedView.class);

        publishTicks();
        view.flushNow();

        // A scheduled flush may have run before flushNow(); either way the
        // fifty ticks became a single UI update.
        assertTrue(view.uiUpdates() == 1,
                "expected one update, got " + view.uiUpdates());
        assertEquals("%.2f".formatted(price(TICKS - 1)), view.priceOf("AURA"));
    }

    private void publishTicks() {
        for (int i = 0; i < TICKS; i++) {
            feed.publish(new Tick("AURA", price(i)));
        }
        runPendingSignalsTasks();
    }

    private static double price(int i) {
        return 100 + i * 0.5;
    }
}
