package com.example.uc6;

import com.example.common.UseCaseDescription;
import com.example.components.QrCode;
import com.example.views.MainLayout;
import com.example.views.Sample;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC6 — Wrapping an npm web component.
 * <p>
 * A QR code for a ticket's tracking link, drawn by {@code <qr-code>} from the
 * {@code webcomponent-qr-code} npm package through the typed {@link QrCode}
 * wrapper. Typing changes the code at once; the error correction level shows
 * how a string attribute becomes an enum in Java.
 */
@Route(value = "uc6", layout = MainLayout.class)
@PageTitle("UC6 — npm web component")
@UseCaseDescription("Wrapping a third-party web component from npm in a typed Java API")
@Menu(order = 6, title = "UC6 — npm web component")
public class QrCodeView extends VerticalLayout {

    static final String INITIAL = "https://example.com/track/4521";

    private final QrCode code = new QrCode(INITIAL);
    private final TextField data = new TextField("Link", INITIAL, "");

    public QrCodeView() {
        add(new H1("UC6 — npm web component"));
        add(new Paragraph("Edit the link and the QR code follows. Scan it "
                + "with a phone to check."));

        data.setWidth("24rem");
        data.setValueChangeMode(ValueChangeMode.EAGER);
        data.addValueChangeListener(e -> code.setData(e.getValue()));
        Select<QrCode.ErrorCorrection> correction = new Select<>();
        correction.setLabel("Error correction");
        correction.setItems(QrCode.ErrorCorrection.values());
        correction.setValue(QrCode.ErrorCorrection.LOW);
        correction.addValueChangeListener(
                e -> code.setErrorCorrection(e.getValue()));

        add(new Sample("Tracking link", data, correction, code));
    }

    // Package-private test seams.
    QrCode code() {
        return code;
    }

    TextField data() {
        return data;
    }
}
