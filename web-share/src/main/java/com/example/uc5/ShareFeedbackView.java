package com.example.uc5;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import com.example.views.MainLayout;

import com.vaadin.flow.component.AttachEvent;
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
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;

/**
 * UC5 — Share with completion feedback.
 * <p>
 * The native share sheet has three possible outcomes: the user picks a target
 * (success), the user dismisses the sheet ({@code AbortError}), or the share
 * fails for some other reason. The observed form of {@link WebShare#onClick}'s
 * share binding takes an {@code onShared} runnable and an {@code onError}
 * consumer, both invoked on the UI thread once the client-side share resolves.
 * This view wires those callbacks into a small outcome log.
 */
@Route(value = "uc5", layout = MainLayout.class)
@Menu(order = 5, title = "UC5 — Completion feedback")
@StyleSheet("uc5.css")
public class ShareFeedbackView extends VerticalLayout {

    private static final DateTimeFormatter TIME = DateTimeFormatter
            .ofPattern("HH:mm:ss");

    private final Button shareButton = new Button("Share with feedback",
            VaadinIcon.SHARE.create());
    private final Div log = new Div();

    public ShareFeedbackView() {
        addClassName("uc5-view");
        add(new H1("UC5 — Share with completion feedback"));
        add(new Paragraph("Wires the onShared / onError callbacks of "
                + "WebShare.onClick(button).share(...). The browser resolves "
                + "on success and rejects with an AbortError when the user "
                + "dismisses the sheet — we surface both in the log below. "
                + "On desktop you can fake either branch with the buttons in "
                + "the next section."));

        shareButton.addThemeVariants(ButtonVariant.PRIMARY);

        add(shareButton, new H2("Outcome log"));

        log.addClassName("share-feedback-log");
        add(log);
        appendLog("(no share invoked yet)");

        add(new H2("Simulate without a browser"));
        Button fakeOk = new Button("Simulate success", e -> handleSuccess());
        Button fakeCancel = new Button("Simulate cancel",
                e -> handleError("AbortError: Share canceled"));
        Button fakeFail = new Button("Simulate failure",
                e -> handleError("NotAllowedError: Permission denied"));
        add(new Div(fakeOk, fakeCancel, fakeFail));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        Signal<WebShareSupport> support = WebShare.supportSignal();

        Signal.effect(this, () -> shareButton
                .setEnabled(support.get() == WebShareSupport.SUPPORTED));

        // Bind the share once; the observed callbacks run on the UI thread
        // when the client-side share resolves or rejects.
        WebShare.onClick(shareButton).share(
                ShareContent.create()
                        .title("Web Share completion feedback demo")
                        .text("Try cancelling the share sheet to see the "
                                + "AbortError path.")
                        .url("https://vaadin.com/"),
                this::handleSuccess,
                err -> handleError(err.name() + ": " + err.message()));
    }

    // Package-private outcome handlers so a browserless test can drive the
    // log without an actual share dialog.
    void handleSuccess() {
        appendLog(now() + "  ✓ shared");
    }

    void handleError(String error) {
        appendLog(now() + "  ✗ " + error);
    }

    private static String now() {
        return LocalTime.now().format(TIME);
    }

    private void appendLog(String line) {
        Div entry = new Div();
        entry.setText(line);
        log.addComponentAsFirst(entry);
    }
}
