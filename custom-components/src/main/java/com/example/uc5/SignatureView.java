package com.example.uc5;

import com.example.common.UseCaseDescription;
import com.example.components.SignaturePad;
import com.example.views.MainLayout;
import com.example.views.Sample;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC5 — Own client-side web component.
 * <p>
 * Signing for a delivery on a {@link SignaturePad}, a Lit element with a Java
 * API. Drawing happens entirely in the browser; the finished signature arrives
 * as the field's value, a PNG data URL, and is shown back as an image. Starting
 * to sign is a server event of its own, and "Clear" calls a method on the
 * element.
 */
@Route(value = "uc5", layout = MainLayout.class)
@PageTitle("UC5 — Own web component")
@UseCaseDescription("Writing a Lit web component and giving it a Java API")
@Menu(order = 5, title = "UC5 — Own web component")
public class SignatureView extends VerticalLayout {

    private final SignaturePad pad = new SignaturePad();
    private final Span status = new Span("Not signed");
    private final Image preview = new Image();
    private final Button confirm = new Button("Confirm delivery");

    public SignatureView() {
        add(new H1("UC5 — Own web component"));
        add(new Paragraph("Sign in the box with the mouse, a pen or a "
                + "finger. The server receives the signature as an image."));

        status.addClassName("status");
        preview.setAlt("Your signature");
        preview.setHeight("4rem");
        preview.setVisible(false);
        confirm.addThemeVariants(ButtonVariant.PRIMARY);
        confirm.setEnabled(false);

        pad.addSignatureStartedListener(e -> status.setText("Signing…"));
        pad.addValueChangeListener(e -> {
            boolean signed = !e.getValue().isEmpty();
            status.setText(signed ? "Signed" : "Not signed");
            preview.setSrc(e.getValue());
            preview.setVisible(signed);
            confirm.setEnabled(signed);
        });
        Button clear = new Button("Clear", e -> pad.clear());
        confirm.addClickListener(e -> {
            status.setText("Delivery confirmed");
            pad.setEnabled(false);
            confirm.setEnabled(false);
        });

        add(new Sample("Delivery #4521", pad,
                new HorizontalLayout(clear, confirm), status, preview));
    }

    // Package-private test seams.
    SignaturePad pad() {
        return pad;
    }

    String status() {
        return status.getText();
    }

    Button confirm() {
        return confirm;
    }

    Image preview() {
        return preview;
    }
}
