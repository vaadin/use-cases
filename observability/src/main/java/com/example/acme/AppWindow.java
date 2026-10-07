package com.example.acme;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

/**
 * A window-chrome frame that hosts a scene from the fictional <em>Acme Supply
 * Co.</em> application the observability use cases tell their stories in.
 * <p>
 * Each use case view opens with one of these: inside the window is what an
 * Acme user sees and interacts with — a real screen, not a test rig — and
 * below the window the view shows what Observability Kit records about those
 * interactions. The frame is pure cosmetics: the content is ordinary Vaadin
 * components rendered on the view's own route, so the kit's meters attribute
 * everything done inside the window to that route.
 */
public class AppWindow extends Div {

    /**
     * @param appName
     *            the Acme screen's name, shown in the title bar (e.g.
     *            "Acme Supply — Order Desk")
     * @param location
     *            the path shown in the fake address pill, normally the view's
     *            route so the chrome and the meters tell the same story
     * @param content
     *            the scene: the components the Acme user works with
     */
    public AppWindow(String appName, String location, Component... content) {
        addClassName("app-window");

        Div dots = new Div(new Div(), new Div(), new Div());
        dots.addClassName("app-window-dots");
        Span title = new Span(appName);
        title.addClassName("app-window-title");
        Span url = new Span("acme.example/" + location);
        url.addClassName("app-window-url");
        Div chrome = new Div(dots, title, url);
        chrome.addClassName("app-window-chrome");

        Div body = new Div(content);
        body.addClassName("app-window-body");

        add(chrome, body);
    }
}
