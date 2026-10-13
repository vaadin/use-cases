package com.example.uc6;

import com.example.common.UseCaseDescription;
import com.example.orders.ReportService;
import com.example.views.MainLayout;
import com.example.views.TestShowcase;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC6 — Async work and push.
 * <p>
 * "Build report" starts a slow job in the background; the button is disabled
 * and a progress bar runs until the result arrives through server push.
 * <p>
 * The job writes its state to signals, which can be set from any thread, and
 * the button, progress bar and result are bound to them. Testing that without
 * {@code Thread.sleep} takes one seam: the job runs on an executor bean, which
 * the test replaces with one that queues tasks until the test runs them, so the
 * test decides when the job finishes, then lets the signal bindings run.
 */
@Route(value = "uc6", layout = MainLayout.class)
@PageTitle("UC6 — Async work and push")
@UseCaseDescription("Testing background jobs and push without sleeping in tests")
@Menu(order = 6, title = "UC6 — Async work and push")
@AnonymousAllowed
public class ReportView extends VerticalLayout {

    private final Button build = new Button("Build report");
    private final ProgressBar progress = new ProgressBar();
    private final Span result = new Span();
    private final ValueSignal<Boolean> building = new ValueSignal<>(false);
    private final ValueSignal<String> status = new ValueSignal<>(
            "No report yet");

    public ReportView(ReportService reports) {
        add(new H1("UC6 — Async work and push"));
        add(new Paragraph("Build the report: the button waits while the job "
                + "runs in the background, and the result is pushed when it "
                + "is ready."));

        progress.setIndeterminate(true);
        progress.setWidth("16rem");
        progress.bindVisible(building);
        build.addThemeVariants(ButtonVariant.PRIMARY);
        build.bindEnabled(building.map(running -> !running));
        result.bindText(status);
        build.addClickListener(e -> {
            building.set(true);
            status.set("Building…");
            // The background thread only sets signals; the bindings update
            // the components under the session lock and push the change.
            reports.build().whenComplete((report, error) -> {
                status.set(error != null ? "Report failed"
                        : "%,d orders · top customer %s".formatted(
                                report.orders(), report.topCustomer()));
                building.set(false);
            });
        });

        Div sample = new Div(build, progress, result);
        sample.addClassName("sample");
        add(sample, TestShowcase.browserless("uc6/ReportViewTest"));
    }

    // Package-private test seams.
    Button buildButton() {
        return build;
    }

    ProgressBar progress() {
        return progress;
    }

    String result() {
        return result.getText();
    }
}
