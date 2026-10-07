package com.example.usecase25;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = UseCase25View.class)
@WithMockUser
class UseCase25ViewTest extends SpringBrowserlessTest {

    @Test
    void allEightStockSymbolsRendered() {
        navigate(UseCase25View.class);
        runPendingSignalsTasks();

        // All 8 stock symbols from StockPriceSimulator.INITIAL_STOCKS
        for (String symbol : new String[] { "AAPL", "GOOGL", "MSFT", "AMZN",
                "TSLA", "NVDA", "META", "NFLX" }) {
            assertTrue(
                    findInView(Span.class).all().stream()
                            .anyMatch(s -> symbol.equals(s.getText())),
                    "Missing stock symbol: " + symbol);
        }
    }

    @Test
    void initialPricesDisplayed() {
        navigate(UseCase25View.class);
        runPendingSignalsTasks();

        // Verify some initial prices are displayed
        assertTrue(findInView(Span.class).all().stream().anyMatch(
                s -> s.getText() != null && s.getText().contains("$189.84")));
        assertTrue(findInView(Span.class).all().stream().anyMatch(
                s -> s.getText() != null && s.getText().contains("$141.80")));
    }

    @Test
    void initialChangesAreAllZero() {
        navigate(UseCase25View.class);
        runPendingSignalsTasks();

        // All initial change values should be +0.00
        long zeroChanges = findInView(Span.class).all().stream()
                .filter(s -> "+0.00".equals(s.getText())).count();
        // 8 stocks × 1 change column = 8 spans with "+0.00"
        assertTrue(zeroChanges >= 8,
                "Expected at least 8 zero-change spans, found " + zeroChanges);
    }

    @Test
    void priceUpdateChangesDisplayedValues() {
        navigate(UseCase25View.class);
        runPendingSignalsTasks();

        // Access the view's package-private stockSignals field
        UseCase25View view = (UseCase25View) getCurrentView();

        // Run multiple updates to ensure at least some prices change
        for (int i = 0; i < 5; i++) {
            StockPriceSimulator.updatePrices(view.stockSignals);
        }
        runPendingSignalsTasks();

        // At least some change values should no longer be +0.00
        long zeroChanges = findInView(Span.class).all().stream()
                .filter(s -> "+0.00".equals(s.getText())).count();
        assertTrue(zeroChanges < 8,
                "Expected some stocks to have non-zero changes after update");

        // At least some percentage changes should no longer be +0.00%
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> s.getText() != null && s.getText().endsWith("%")
                        && !"+0.00%".equals(s.getText())),
                "Expected some non-zero percentage changes after update");
    }
}
