package com.example.common;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

/**
 * A browser-window frame for showing a scene "as a user sees it": a title bar
 * with window dots, a title and an address pill, and a body with the scene's
 * components.
 * <p>
 * Use it where a use case shows what happens in a user's browser, so that part
 * reads apart from the explanation around it — an app screen above the readout
 * of what the feature recorded, or several users' windows side by side. The
 * frame is pure cosmetics: the content is ordinary Vaadin components rendered
 * on the view's own route.
 */
@StyleSheet("app-window.css")
public class AppWindow extends Div {

    private final Div titleBar = new Div();
    private final Span address = new Span();
    private final Div body = new Div();

    /**
     * @param title
     *            shown in the title bar, e.g. the app screen's or the user's
     *            name
     * @param address
     *            shown in the address pill, e.g. {@code "acme.example/orders"};
     *            {@code null} for no pill
     * @param content
     *            the scene: the components the user works with
     */
    public AppWindow(String title, @Nullable String address,
            Component... content) {
        addClassName("app-window");

        Div dots = new Div(new Div(), new Div(), new Div());
        dots.addClassName("app-window-dots");
        Span titleLabel = new Span(title);
        titleLabel.addClassName("app-window-title");
        this.address.addClassName("app-window-url");
        titleBar.add(dots, titleLabel, this.address);
        titleBar.addClassName("app-window-chrome");
        setAddress(address);

        body.addClassName("app-window-body");
        body.add(content);

        add(titleBar, body);
    }

    /**
     * Sets the text of the address pill, or hides the pill for {@code null}.
     */
    public void setAddress(@Nullable String address) {
        this.address.setText(address == null ? "" : address);
        this.address.setVisible(address != null);
    }

    /**
     * Adds components to the end of the title bar, e.g. a button that acts on
     * the whole window.
     */
    public void addToTitleBar(Component... components) {
        titleBar.add(components);
    }

    /** Replaces the scene in the window's body. */
    public void setContent(Component... content) {
        body.removeAll();
        body.add(content);
    }
}
