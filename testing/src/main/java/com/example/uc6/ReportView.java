package com.example.uc6;

import com.example.common.UseCaseDescription;
import com.example.orders.ReportService;
import com.example.views.MainLayout;
import com.example.views.TestsNote;

import com.vaadin.flow.component.UI;
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

/**
 * UC6 — Async work and push.
 * <p>
 * "Build report" starts a slow job in the background; the button is disabled
 * and a progress bar runs until the result arrives through server push.
 * <p>
 * Testing that without {@code Thread.sleep} takes two seams. The job runs on an
 * executor bean, which the test replaces with one that queues tasks until the
 * test runs them, so the test decides when the job finishes. And the result
 * reaches the UI through {@code UI.access}, whose queued commands the test runs
 * explicitly before it checks the view.
 */
@Route(value = "uc6", layout = MainLayout.class)
@PageTitle("UC6 — Async work and push")
@UseCaseDescription("Testing background jobs and push without sleeping in tests")
@Menu(order = 6, title = "UC6 — Async work and push")
@AnonymousAllowed
public class ReportView extends VerticalLayout {

    private final Button build = new Button("Build report");
    private final ProgressBar progress = new ProgressBar();
    private final Span result = new Span("No report yet");

    public ReportView(ReportService reports) {
        add(new H1("UC6 — Async work and push"));
        add(new Paragraph("Build the report: the button waits while the job "
                + "runs in the background, and the result is pushed when it "
                + "is ready."));

        progress.setIndeterminate(true);
        progress.setVisible(false);
        progress.setWidth("16rem");
        build.addThemeVariants(ButtonVariant.PRIMARY);
        build.addClickListener(e -> {
            UI ui = UI.getCurrent();
            build.setEnabled(false);
            progress.setVisible(true);
            result.setText("Building…");
            reports.build().whenComplete((report, error) -> ui.access(() -> {
                build.setEnabled(true);
                progress.setVisible(false);
                result.setText(error != null ? "Report failed"
                        : "%,d orders · top customer %s".formatted(
                                report.orders(), report.topCustomer()));
            }));
        });

        Div sample = new Div(build, progress, result);
        sample.addClassName("sample");
        add(sample, new TestsNote("View test: uc6/ReportViewTest (browserless, "
                + "controlled executor, no sleeps)"));
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
