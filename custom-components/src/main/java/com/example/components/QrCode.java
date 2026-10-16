package com.example.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;

/**
 * A typed Java API for a third-party web component from npm (UC6):
 * {@code <qr-code>} from the {@code webcomponent-qr-code} package.
 * <p>
 * {@link NpmPackage} adds the package to the application's {@code package.json}
 * when the frontend is built, and {@link JsModule} imports it; the element's
 * attributes become typed setters. The npm package is declared on the Java
 * class, so an add-on that contains this class brings its dependency along.
 */
@Tag("qr-code")
@NpmPackage(value = "webcomponent-qr-code", version = "2.0.0")
@JsModule("webcomponent-qr-code")
public class QrCode extends Component implements HasStyle {

    /** How much of the code may be damaged and still read. */
    public enum ErrorCorrection {
        LOW("L"), MEDIUM("M"), QUARTILE("Q"), HIGH("H");

        private final String level;

        ErrorCorrection(String level) {
            this.level = level;
        }
    }

    public QrCode(String data) {
        setData(data);
        getElement().setAttribute("format", "svg");
        setModuleSize(4);
    }

    public void setData(String data) {
        getElement().setAttribute("data", data);
        getElement().setAttribute("aria-label", "QR code: " + data);
        getElement().setAttribute("role", "img");
    }

    public String getData() {
        return getElement().getAttribute("data");
    }

    /** The size of one square of the code, in pixels. */
    public void setModuleSize(int pixels) {
        getElement().setAttribute("modulesize", String.valueOf(pixels));
    }

    public void setErrorCorrection(ErrorCorrection correction) {
        getElement().setAttribute("ecclevel", correction.level);
    }
}
