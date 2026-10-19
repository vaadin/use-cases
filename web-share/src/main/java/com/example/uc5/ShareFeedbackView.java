package com.example.uc5;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.webshare.ShareContent;
import com.vaadin.flow.component.webshare.WebShare;
import com.vaadin.flow.component.webshare.WebShareSupport;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC5 — Completion feedback.
 * <p>
 * The native share sheet has three outcomes: the user picks a target (success),
 * the user closes the sheet (the browser reports an {@code AbortError}), or the
 * share fails for another reason. The observed form
 * {@code WebShare.onClick(button).share(content, onShared, onError)} runs
 * {@code onShared} or {@code onError} on the server once the browser's share
 * finishes. This view writes each outcome to a log.
 * <p>
 * The "Simulate" buttons in the separate box call the same handlers as the real
 * callbacks. They exist because nothing else can produce an outcome without a
 * real share sheet: a browser without {@code navigator.share} never opens one,
 * and browserless tests can not fire {@code onShared} or {@code onError}
 * (API-GAPS.md #3). The test drives the log through these buttons. Remove them
 * once a Web Share simulator exists.
 */
@Route(value = "uc5", layout = MainLayout.class)
@PageTitle(ShareFeedbackView.TITLE)
@UseCaseDescription("Telling users whether a share completed, was cancelled or failed")
@Menu(order = 5, title = ShareFeedbackView.TITLE)
@StyleSheet("uc5.css")
public class ShareFeedbackView extends VerticalLayout {

    static final String TITLE = "UC5 — Completion feedback";

    private static final DateTimeFormatter TIME = DateTimeFormatter
            .ofPattern("HH:mm:ss");

    private final Div log = new Div();
    private final Div emptyLog = new Div("No share yet.");

    public ShareFeedbackView() {
        addClassName("uc5-view");
        add(new H1(TITLE));
        add(new Paragraph("The Share button reports the outcome of the "
                + "share back to the server. The log below shows whether "
                + "you picked a target, closed the share sheet "
                + "(AbortError), or the share failed."));

        Button shareButton = new Button("Share with feedback",
                VaadinIcon.SHARE.create());
        shareButton.addThemeVariants(ButtonVariant.PRIMARY);
        shareButton.bindEnabled(WebShare.supportSignal()
                .map(s -> s == WebShareSupport.SUPPORTED));

        // Bound once; the callbacks run on the server when the browser's
        // share resolves or rejects.
        WebShare.onClick(shareButton).share(
                ShareContent.create()
                        .title("Web Share completion feedback demo")
                        .text("Close the share sheet to see the AbortError "
                                + "outcome.")
                        .url("https://vaadin.com/"),
                this::handleSuccess,
                err -> handleError(err.name() + ": " + err.message()));

        log.addClassName("uc5-log");
        emptyLog.addClassName("uc5-empty");
        log.add(emptyLog);

        add(shareButton, new H2("Outcome log"), log, simulateBox());
    }

    private Div simulateBox() {
        Div box = new Div();
        box.addClassName("uc5-simulate");
        box.add(new H2("Simulate an outcome"));
        box.add(new Paragraph("Not part of the feature. In a browser without "
                + "navigator.share there is no share sheet, and the "
                + "browserless tests can not produce an outcome either "
                + "(API-GAPS.md #3). These buttons call the same handlers "
                + "as the real callbacks."));
        Button ok = new Button("Simulate success", e -> handleSuccess());
        Button cancel = new Button("Simulate cancel",
                e -> handleError("AbortError: Share canceled"));
        Button fail = new Button("Simulate failure",
                e -> handleError("NotAllowedError: Permission denied"));
        box.add(new Div(ok, cancel, fail));
        return box;
    }

    private void handleSuccess() {
        appendLog(now() + "  ✓ shared");
    }

    private void handleError(String error) {
        appendLog(now() + "  ✗ " + error);
    }

    private static String now() {
        return LocalTime.now().format(TIME);
    }

    private void appendLog(String line) {
        emptyLog.removeFromParent();
        log.addComponentAsFirst(new Div(line));
    }
}
