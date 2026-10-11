package com.example.components;

import com.vaadin.flow.component.AbstractSinglePropertyField;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.shared.Registration;

/**
 * The Java API of the {@code <signature-pad>} Lit element (UC5).
 * <p>
 * {@link AbstractSinglePropertyField} keeps the element's {@code value}
 * property in sync: the browser sends it on {@code value-changed}, and it
 * becomes this field's value (a PNG data URL, empty when nothing is signed).
 * {@link SignatureStartedEvent} maps a browser event to a server event with
 * {@link DomEvent}, and {@link #clear()} calls a method on the element. The
 * element's TypeScript lives in {@code META-INF/resources/frontend}, where an
 * add-on jar would carry it.
 */
@Tag("signature-pad")
@JsModule("./signature-pad.ts")
public class SignaturePad
        extends AbstractSinglePropertyField<SignaturePad, String>
        implements HasSize, HasStyle {

    /** Fired when the user starts signing on an empty pad. */
    @DomEvent("signature-started")
    public static class SignatureStartedEvent
            extends ComponentEvent<SignaturePad> {

        public SignatureStartedEvent(SignaturePad source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    public SignaturePad() {
        super("value", "", false);
    }

    public boolean isSigned() {
        return !getValue().isEmpty();
    }

    /** Empties the pad, in the browser and on the server. */
    public void clear() {
        getElement().callJsFunction("clear");
        setValue("");
    }

    public Registration addSignatureStartedListener(
            ComponentEventListener<SignatureStartedEvent> listener) {
        return addListener(SignatureStartedEvent.class, listener);
    }
}
