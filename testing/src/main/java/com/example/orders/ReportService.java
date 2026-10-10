package com.example.orders;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Builds a sales report in the background (UC6). Runs its work on the
 * {@code reportExecutor}, which a test replaces with one it controls.
 */
@Component
public class ReportService {

    private final OrderHistory history;
    private final Executor executor;

    public ReportService(OrderHistory history,
            @Qualifier("reportExecutor") Executor executor) {
        this.history = history;
        this.executor = executor;
    }

    /** A finished report. */
    public record Report(int orders, String topCustomer) {
    }

    public CompletableFuture<Report> build() {
        return CompletableFuture.supplyAsync(
                () -> new Report(history.count(""), history.topCustomer()),
                executor);
    }
}
