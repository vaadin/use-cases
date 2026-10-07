package com.example.uc5;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.fullscreen.Fullscreen;
import com.vaadin.flow.component.fullscreen.FullscreenState;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC5 — Kiosk: visitor sign-in with PIN-protected staff exit.
 * <p>
 * A realistic kiosk: the kiosk stage is fullscreened with
 * {@link Fullscreen#onClick(com.vaadin.flow.component.Component)
 * Fullscreen.onClick(enter).enter(stage)}, so only the kiosk UI fills the
 * viewport — the app's heading, navigation, intro paragraph and activity log
 * are naturally hidden by the fullscreen wrapper, just like a dedicated
 * terminal. Visitors sign in via a small three-screen flow (landing → form →
 * confirmation). A "Staff" button in the kiosk header opens an inline PIN
 * prompt; the correct PIN ({@value #STAFF_PIN}) calls {@link Fullscreen#exit()}
 * for an expected exit. The fullscreen API exposes a single global
 * {@link Fullscreen#stateSignal() state signal}, so expected exits are told
 * apart from Escape with an {@code expectingExit} flag set right before the
 * programmatic exit: a transition out of {@link FullscreenState#FULLSCREEN
 * FULLSCREEN} with the flag unset means the user pressed Escape, which surfaces
 * a warning.
 */
@Route(value = "uc5", layout = MainLayout.class)
@Menu(order = 5, title = "UC5 — Kiosk")
@StyleSheet("uc5.css")
public class KioskExitDetectionView extends VerticalLayout {

    private static final DateTimeFormatter TIME = DateTimeFormatter
            .ofPattern("HH:mm:ss");
    static final String STAFF_PIN = "1234";

    private enum Screen {
        LANDING, FORM, CONFIRMATION
    }

    private final ValueSignal<Screen> screenSignal = new ValueSignal<>(
            Screen.LANDING);
    private final ValueSignal<String> lastVisitorName = new ValueSignal<>("");
    private final ValueSignal<Boolean> staffPromptOpen = new ValueSignal<>(
            false);
    private final ValueSignal<String> staffError = new ValueSignal<>("");

    private final H1 heading = new H1("UC5 — Kiosk: visitor sign-in");
    private final Paragraph intro = new Paragraph(
            "Click ‘Enter kiosk’ to fullscreen the kiosk stage with "
                    + "Fullscreen.onClick(enter).enter(stage) — only the kiosk "
                    + "UI is shown, the surrounding app chrome is hidden by the "
                    + "wrapper. Inside, the Staff button opens a PIN prompt "
                    + "(hint shown in the dialog) that calls Fullscreen.exit() "
                    + "— an expected exit. Pressing Escape leaves unexpectedly "
                    + "and is recorded with a warning in the activity log.");
    private final HorizontalLayout controlsRow = new HorizontalLayout();
    private final Paragraph logHeading = new Paragraph("Activity log:");
    private final Div log = new Div();

    private final Span stateBadge = new Span();
    private final Span unexpectedWarning = new Span();
    private final Div stage = new Div();

    private final TextField nameField = new TextField("Your name");
    private final Select<Integer> partySizeField = new Select<>();
    private final Select<String> purposeField = new Select<>();
    private final PasswordField pinField = new PasswordField("Enter staff PIN");
    private final Button staffButton = new Button("Staff", e -> {
        staffError.set("");
        pinField.clear();
        staffPromptOpen.set(true);
    });

    {
        // Tell the browser this is an ephemeral code, not a saved credential,
        // so it won't prompt to save the PIN. Vaadin's Autocomplete enum
        // doesn't include ONE_TIME_CODE, so set the attribute directly.
        pinField.getElement().setAttribute("autocomplete", "one-time-code");
    }

    // The fullscreen API exposes only the global state signal, so expected
    // (staff-PIN) exits are tracked here: set right before the programmatic
    // exit, read when the state signal leaves FULLSCREEN.
    private boolean expectingExit;
    private FullscreenState previousState = FullscreenState.UNKNOWN;

    public KioskExitDetectionView() {
        addClassName("uc5-view");
        add(heading, intro);

        stateBadge.addClassName("status-badge");
        unexpectedWarning.addClassName("unexpected-warning");

        Button enter = new Button("Enter kiosk", e -> {
            unexpectedWarning.setText("");
            staffPromptOpen.set(false);
            staffError.set("");
            screenSignal.set(Screen.LANDING);
        });
        enter.addThemeVariants(ButtonVariant.PRIMARY);
        // Fullscreen needs the click's user gesture, so bind the request to the
        // Enter button's click trigger; a rejected request is logged.
        Fullscreen.onClick(enter).enter(stage, () -> {
        }, err -> appendLog("Request REJECTED: " + err.message(),
                "unexpected"));

        Button clear = new Button("Clear log", e -> log.removeAll());

        controlsRow.add(enter, clear, stateBadge, unexpectedWarning);
        controlsRow.setAlignItems(Alignment.CENTER);
        add(controlsRow);

        stage.addClassName("kiosk-stage");
        stage.add(buildKioskShell());
        add(stage);

        log.addClassName("kiosk-log");
        add(logHeading, log);
    }

    private Div buildKioskShell() {
        Div shell = new Div();
        shell.addClassName("kiosk-shell");

        Span title = new Span("Acme HQ Visitor Sign-In");
        title.addClassName("kiosk-title");
        staffButton.addClassName("kiosk-staff-btn");
        HorizontalLayout header = new HorizontalLayout(title, staffButton);
        header.addClassName("kiosk-header");
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.CENTER);

        Div content = new Div();
        content.addClassName("kiosk-content");

        Div landingPanel = buildLandingPanel();
        Div formPanel = buildFormPanel();
        Div confirmationPanel = buildConfirmationPanel();
        Div staffPanel = buildStaffPanel();

        landingPanel.bindVisible(screenSignal.map(s -> s == Screen.LANDING)
                .map(b -> b && !staffPromptOpen.get()));
        formPanel.bindVisible(screenSignal.map(s -> s == Screen.FORM)
                .map(b -> b && !staffPromptOpen.get()));
        confirmationPanel
                .bindVisible(screenSignal.map(s -> s == Screen.CONFIRMATION)
                        .map(b -> b && !staffPromptOpen.get()));
        staffPanel.bindVisible(staffPromptOpen);

        content.add(landingPanel, formPanel, confirmationPanel, staffPanel);
        shell.add(header, content);
        return shell;
    }

    private Div buildLandingPanel() {
        Div panel = new Div();
        panel.addClassName("kiosk-panel");
        H2 title = new H2("Welcome to Acme HQ");
        Paragraph subtitle = new Paragraph("Touch Start to sign in.");
        Button start = new Button("Start sign-in", e -> {
            nameField.clear();
            partySizeField.setValue(1);
            purposeField.clear();
            screenSignal.set(Screen.FORM);
        });
        start.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.LARGE);
        panel.add(title, subtitle, start);
        return panel;
    }

    private Div buildFormPanel() {
        Div panel = new Div();
        panel.addClassName("kiosk-panel");
        H2 title = new H2("Sign in");

        nameField.setRequiredIndicatorVisible(true);
        partySizeField.setLabel("Party size");
        partySizeField.setItems(1, 2, 3, 4, 5);
        partySizeField.setValue(1);
        purposeField.setLabel("Purpose of visit");
        purposeField.setItems("Meeting", "Interview", "Delivery", "Other");

        FormLayout form = new FormLayout(nameField, partySizeField,
                purposeField);

        Button cancel = new Button("Cancel",
                e -> screenSignal.set(Screen.LANDING));
        Button submit = new Button("Submit", e -> submitForm());
        submit.addThemeVariants(ButtonVariant.PRIMARY);
        HorizontalLayout actions = new HorizontalLayout(cancel, submit);
        actions.setJustifyContentMode(JustifyContentMode.END);

        panel.add(title, form, actions);
        return panel;
    }

    private Div buildConfirmationPanel() {
        Div panel = new Div();
        panel.addClassName("kiosk-panel");
        H2 title = new H2();
        title.bindText(lastVisitorName.map(n -> "Thanks, "
                + (n == null || n.isBlank() ? "visitor" : n) + "!"));
        Paragraph subtitle = new Paragraph(
                "Please take a seat. Reception will call you shortly.");
        Button newVisitor = new Button("New visitor",
                e -> screenSignal.set(Screen.LANDING));
        newVisitor.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.LARGE);
        panel.add(title, subtitle, newVisitor);
        return panel;
    }

    private Div buildStaffPanel() {
        Div panel = new Div();
        panel.addClassName("kiosk-panel");
        panel.addClassName("kiosk-staff-panel");
        H2 title = new H2("Staff exit");
        Paragraph hint = new Paragraph(
                "Demo hint: the staff PIN is " + STAFF_PIN + ".");
        hint.addClassName("kiosk-staff-hint");
        Span error = new Span();
        error.addClassName("unexpected-warning");
        error.bindText(staffError);

        Button cancel = new Button("Cancel", e -> {
            staffPromptOpen.set(false);
            staffError.set("");
        });
        Button confirm = new Button("Confirm", e -> attemptStaffExit());
        confirm.addThemeVariants(ButtonVariant.PRIMARY);
        HorizontalLayout actions = new HorizontalLayout(cancel, confirm);
        actions.setJustifyContentMode(JustifyContentMode.END);

        panel.add(title, hint, pinField, error, actions);
        return panel;
    }

    private void submitForm() {
        if (nameField.isEmpty()) {
            nameField.setInvalid(true);
            nameField.setErrorMessage("Name is required");
            return;
        }
        nameField.setInvalid(false);
        if (purposeField.getValue() == null) {
            purposeField.setInvalid(true);
            purposeField.setErrorMessage("Select a purpose");
            return;
        }
        purposeField.setInvalid(false);
        String name = nameField.getValue().trim();
        Integer partySize = partySizeField.getValue();
        String purpose = purposeField.getValue();
        lastVisitorName.set(name);
        appendLog("Signed in: " + name + " (party of " + partySize + ", "
                + purpose + ")", null);
        screenSignal.set(Screen.CONFIRMATION);
    }

    private void attemptStaffExit() {
        if (STAFF_PIN.equals(pinField.getValue())) {
            appendLog("Staff exit confirmed", null);
            staffPromptOpen.set(false);
            staffError.set("");
            // Mark this as an expected exit before requesting it, so the
            // state-signal effect classifies the FULLSCREEN → NOT_FULLSCREEN
            // transition as code-initiated rather than an Escape press.
            expectingExit = true;
            Fullscreen.exit();
        } else {
            appendLog("Failed staff exit attempt", "unexpected");
            staffError.set("Incorrect PIN");
            pinField.clear();
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        Signal<FullscreenState> fs = Fullscreen.stateSignal();
        Signal<Boolean> isFullscreen = fs
                .map(s -> s == FullscreenState.FULLSCREEN);

        // Derive entered / expected-exit / unexpected-exit transitions from the
        // global state signal.
        Signal.effect(this, () -> {
            FullscreenState state = fs.get();
            if (state == FullscreenState.FULLSCREEN
                    && previousState != FullscreenState.FULLSCREEN) {
                appendLog("Entered kiosk mode", null);
            } else if (previousState == FullscreenState.FULLSCREEN
                    && state == FullscreenState.NOT_FULLSCREEN) {
                if (expectingExit) {
                    appendLog("Exit (expected)", null);
                } else {
                    appendLog("Exit (UNEXPECTED — user pressed Escape)",
                            "unexpected");
                    unexpectedWarning.setText("Kiosk exited unexpectedly!");
                }
                expectingExit = false;
            }
            previousState = state;
        });

        stateBadge.bindText(fs.map(KioskExitDetectionView::badgeText));
        stateBadge.bindClassName("unsupported",
                fs.map(s -> s == FullscreenState.UNSUPPORTED));
        stage.bindClassName("live", isFullscreen);
        // Staff exit only makes sense once the kiosk is locked; outside
        // fullscreen the small kiosk preview shouldn't expose the button.
        staffButton.bindVisible(isFullscreen);
        // The Component#requestFullscreen() wrapper visually hides the rest
        // of the document, so we don't need to bindVisible developer chrome
        // ourselves — heading, intro, controls row and log are simply not
        // rendered while the kiosk stage is fullscreen.
    }

    private void appendLog(String message, @Nullable String cls) {
        Div line = new Div();
        line.setText("[" + LocalTime.now().format(TIME) + "] " + message);
        if (cls != null) {
            line.addClassName(cls);
        }
        log.addComponentAsFirst(line);
    }

    private static String badgeText(FullscreenState state) {
        // FULLSCREEN: chrome (badge included) is hidden while kiosk is live,
        // so the FULLSCREEN-specific copy was invisible. Keep the idle text.
        return switch (state) {
        case FULLSCREEN, NOT_FULLSCREEN -> "Ready — click Enter kiosk to start";
        case UNSUPPORTED -> "Fullscreen not supported in this browser";
        case UNKNOWN -> "Detecting fullscreen support…";
        };
    }
}
