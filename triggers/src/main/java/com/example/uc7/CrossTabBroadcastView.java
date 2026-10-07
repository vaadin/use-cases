package com.example.uc7;

import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.trigger.internal.SetSignalAction;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC7 — Cross-tab broadcast.
 * <p>
 * Open this view in two browser tabs. Clicking "Broadcast hello" in tab A posts
 * a message on a {@code BroadcastChannel}; tab B's
 * {@link BroadcastChannelTrigger} fires, {@link SetSignalAction} pushes the
 * payload into a server-side signal, and tab B's "Last received" badge updates.
 * The sender does NOT receive its own broadcast — that's BroadcastChannel
 * semantics.
 * <p>
 * The send button uses {@code Element.executeJs} to post on the same channel
 * the trigger listens on. A production view would wrap that as a custom
 * {@code Action} (e.g. {@code BroadcastSendAction}) so the send is also part of
 * the trigger system.
 */
@Route(value = "uc7", layout = MainLayout.class)
@PageTitle("UC7 — Cross-tab broadcast")
@Menu(order = 7, title = "UC7 — Cross-tab broadcast")
@StyleSheet("uc7.css")
public class CrossTabBroadcastView extends VerticalLayout {

    private static final String CHANNEL = "uc7-demo-channel";

    public CrossTabBroadcastView() {
        addClassName("uc7-view");
        add(new H1("UC7 — Cross-tab broadcast"));
        add(new Paragraph(
                "Open this view in two browser tabs. Click \"Broadcast hello\" "
                        + "in one tab; the other tab's badge updates. The "
                        + "BroadcastChannel API doesn't echo to the sender, so the "
                        + "tab that clicks won't see its own message."));

        ValueSignal<String> lastMessage = new ValueSignal<>("(none yet)");

        Span badge = new Span();
        badge.setId("last");
        badge.addClassName("last-message");
        badge.bindText(lastMessage);

        Button send = new Button("Broadcast hello");
        send.setId("send");
        send.getElement().executeJs(
                "this.addEventListener('click', () => new BroadcastChannel($0)"
                        + ".postMessage('hello from ' + Math.floor(Math.random()*1000)));",
                CHANNEL);

        new BroadcastChannelTrigger(this, CHANNEL)
                .triggers(new SetSignalAction<>(lastMessage, String.class,
                        BroadcastChannelTrigger.EventData.data));

        add(new HorizontalLayout(send, badge));
    }
}
