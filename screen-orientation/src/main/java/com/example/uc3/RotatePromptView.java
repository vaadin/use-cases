package com.example.uc3;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

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
 * UC3 — Rotate prompt.
 * <p>
 * Some content is best viewed in a specific orientation — landscape for game
 * boards, portrait for vertical video feeds, etc. This view shows a stage with
 * content that is covered by an overlay whenever the user is holding the device
 * the "wrong" way for the selected required orientation. The overlay hides
 * itself as soon as {@link ScreenOrientation#orientationSignal()} reports the
 * desired side, with no explicit refresh.
 * <p>
 * When the browser does not support the Screen Orientation API, the overlay is
 * always hidden: the user could not satisfy it, so it must not block the
 * content.
 */
@Route(value = "uc3", layout = MainLayout.class)
@PageTitle("UC3 — Rotate prompt")
@UseCaseDescription("Asking users to rotate the device for content that needs it")
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

    public RotatePromptView() {
        addClassName("uc3-view");
        add(new H1("UC3 — Rotate prompt"));
        add(new Paragraph("Pick a required orientation, then rotate the "
                + "device (or use the rotate button of the browser's device "
                + "emulation). The overlay covers the content until the "
                + "device matches; it disappears as soon as it does."));

        RadioButtonGroup<Required> picker = new RadioButtonGroup<>();
        picker.setItems(Required.LANDSCAPE, Required.PORTRAIT);
        picker.setValue(Required.LANDSCAPE);
        picker.setLabel("Required orientation");
        picker.addValueChangeListener(e -> required.set(e.getValue()));
        add(picker);

        Signal<ScreenOrientationData> orientation = ScreenOrientation
                .orientationSignal();
        Signal<Boolean> mismatch = Signal.computed(
                () -> isMismatch(required.get(), orientation.get().type()));

        Div stage = new Div();
        stage.addClassName("uc3-stage");
        stage.add(new Div(new Span(
                "This is the page content. It is fully interactive only "
                        + "when the device is held in the required "
                        + "orientation.")));

        Div overlay = new Div();
        overlay.addClassName("uc3-overlay");
        overlay.bindClassName("uc3-hidden", mismatch.map(b -> !b));
        Span icon = new Span("⟳");
        icon.addClassName("uc3-overlay-icon");
        Span message = new Span();
        message.bindText(required.map(r -> "Please rotate to " + r + " mode."));
        overlay.add(icon, message);
        stage.add(overlay);
        add(stage);

        Span statusBadge = new Span();
        statusBadge.addClassName("status-badge");
        statusBadge.bindText(Signal.computed(
                () -> describe(required.get(), orientation.get().type())));
        statusBadge.bindClassName("warn", mismatch);
        statusBadge.bindClassName("error", orientation
                .map(d -> d.type() == ScreenOrientationType.UNSUPPORTED));
        HorizontalLayout statusRow = new HorizontalLayout(new Span("Status:"),
                statusBadge);
        statusRow.setAlignItems(Alignment.BASELINE);
        add(statusRow);
    }

    /**
     * UNKNOWN and UNSUPPORTED never block: UNKNOWN means the client has not
     * reported yet (in practice only in tests, as the bootstrap seeds it) and
     * UNSUPPORTED platforms cannot be expected to rotate at all.
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
