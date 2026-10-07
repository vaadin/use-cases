package com.example.uc3;

import com.example.views.MainLayout;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.screenorientation.ScreenOrientation;
import com.vaadin.flow.component.screenorientation.ScreenOrientationData;
import com.vaadin.flow.component.screenorientation.ScreenOrientationType;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC3 — "Rotate your device" overlay.
 * <p>
 * Some content is best viewed in a specific orientation — landscape for game
 * boards, portrait for vertical video feeds, etc. This view shows a stage with
 * content that is reactively covered by an overlay whenever the user is holding
 * the device the "wrong" way for the selected required orientation. The overlay
 * hides itself as soon as the orientation signal reports the desired side, with
 * no explicit refresh.
 * <p>
 * On the UNSUPPORTED platform (no Screen Orientation API), the overlay is
 * always hidden — there is no reliable way to enforce a target orientation.
 */
@Route(value = "uc3", layout = MainLayout.class)
@PageTitle("UC3 — Rotate-your-device overlay")
@Menu(order = 3, title = "UC3 — Rotate prompt")
@StyleSheet("uc3.css")
public class RotatePromptView extends VerticalLayout {

    enum Required {
        LANDSCAPE("landscape"), PORTRAIT("portrait");

        private final String label;

        Required(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final ValueSignal<Required> required = new ValueSignal<>(
            Required.LANDSCAPE);

    private final Div overlay = new Div();
    private final Span message = new Span();
    private final Span statusBadge = new Span();

    public RotatePromptView() {
        addClassName("uc3-view");
        add(new H1("UC3 — Rotate-your-device overlay"));
        add(new Paragraph("Pick a required orientation, then rotate the "
                + "device (or use the devtools rotate button in mobile "
                + "emulation). The overlay covers the content until the "
                + "device matches; it disappears as soon as it does."));

        RadioButtonGroup<Required> picker = new RadioButtonGroup<>();
        picker.setItems(Required.LANDSCAPE, Required.PORTRAIT);
        picker.setValue(Required.LANDSCAPE);
        picker.setLabel("Required orientation");
        picker.addValueChangeListener(e -> required.set(e.getValue()));
        add(picker);

        Div stage = new Div();
        stage.addClassName("uc3-stage");

        Div content = new Div();
        content.add(new Span(
                "This is the page content. It is fully interactive only "
                        + "when the device is held in the required "
                        + "orientation."));
        stage.add(content);

        overlay.addClassName("uc3-overlay");
        Span icon = new Span("⟳");
        icon.addClassName("uc3-overlay-icon");
        overlay.add(icon, message);
        stage.add(overlay);

        add(stage);

        HorizontalLayout statusRow = new HorizontalLayout();
        statusBadge.addClassName("status-badge");
        statusRow.add(new Span("Status:"), statusBadge);
        statusRow.setSpacing(true);
        statusRow.setAlignItems(Alignment.BASELINE);
        add(statusRow);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        Signal<ScreenOrientationData> orientation = ScreenOrientation
                .orientationSignal(attachEvent.getUI());

        Signal<Boolean> mismatch = Signal.computed(
                () -> isMismatch(required.get(), orientation.get().type()));

        overlay.bindClassName("hidden", mismatch.map(b -> !b));
        message.bindText(Signal.computed(
                () -> "Please rotate to " + required.get() + " mode."));
        statusBadge.bindText(Signal.computed(
                () -> describe(required.get(), orientation.get().type())));
        statusBadge.bindClassName("warn", mismatch);
        statusBadge.bindClassName("error", orientation
                .map(d -> d.type() == ScreenOrientationType.UNSUPPORTED));
    }

    /**
     * UNKNOWN and UNSUPPORTED never block: UNKNOWN is a brief pre-bootstrap gap
     * and UNSUPPORTED platforms cannot be expected to rotate at all.
     */
    private static boolean isMismatch(Required required,
            ScreenOrientationType type) {
        if (type == ScreenOrientationType.UNKNOWN
                || type == ScreenOrientationType.UNSUPPORTED) {
            return false;
        }
        return switch (required) {
        case LANDSCAPE -> !type.isLandscape();
        case PORTRAIT -> !type.isPortrait();
        };
    }

    private static String describe(Required required,
            ScreenOrientationType type) {
        return switch (type) {
        case UNKNOWN -> "Waiting for orientation…";
        case UNSUPPORTED ->
            "Screen Orientation API not supported — overlay disabled";
        case LANDSCAPE_PRIMARY, LANDSCAPE_SECONDARY ->
            required == Required.LANDSCAPE ? "Landscape OK"
                    : "Currently landscape, rotate to portrait";
        case PORTRAIT_PRIMARY, PORTRAIT_SECONDARY ->
            required == Required.PORTRAIT ? "Portrait OK"
                    : "Currently portrait, rotate to landscape";
        };
    }
}
