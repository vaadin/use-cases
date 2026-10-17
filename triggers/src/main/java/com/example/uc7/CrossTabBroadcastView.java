package com.example.uc7;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.trigger.internal.ClickTrigger;
import com.vaadin.flow.component.trigger.internal.PropertyInput;
import com.vaadin.flow.component.trigger.internal.SetSignalAction;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC7 — Cross-tab broadcast.
 * <p>
 * Open this view in two browser tabs. Clicking "Send to all tabs" in one tab
 * runs a {@link BroadcastSendAction} that posts the message on a
 * {@code BroadcastChannel}, without going through the server. In the other tab,
 * a {@link BroadcastChannelTrigger} fires, a {@link SetSignalAction} passes the
 * message to a {@link ValueSignal} on the server, and the "Last received"
 * badge, bound to that signal, updates. The sending tab shows the message too:
 * a {@code BroadcastChannel} skips only the channel object that posted, and the
 * action posts on its own object, not on the trigger's.
 */
@Route(value = "uc7", layout = MainLayout.class)
@PageTitle("UC7 — Cross-tab broadcast")
@UseCaseDescription("Sending messages between browser tabs of the same app")
@Menu(order = 7, title = "UC7 — Cross-tab broadcast")
@StyleSheet("uc7.css")
public class CrossTabBroadcastView extends VerticalLayout {

    private static final String CHANNEL = "uc7-demo-channel";

    public CrossTabBroadcastView() {
        addClassName("uc7-view");
        add(new H1("UC7 — Cross-tab broadcast"));
        add(new Paragraph("Open this view in two browser tabs. Type a message "
                + "in one tab and click \"Send to all tabs\": the badge in "
                + "the other tab shows it, and so does the tab that sent it. "
                + "The message goes from tab to tab in the browser, not "
                + "through the server."));

        ValueSignal<String> lastMessage = new ValueSignal<>("(none yet)");

        TextField message = new TextField("Message");
        message.setId("message");
        message.setValue("Hello from the other tab");

        Button send = new Button("Send to all tabs");
        send.setId("send");
        new ClickTrigger(send).triggers(new BroadcastSendAction(CHANNEL,
                new PropertyInput<>(message, "value", String.class)));

        Span badge = new Span();
        badge.setId("last");
        badge.addClassName("uc7-last-message");
        badge.bindText(lastMessage.map(text -> "Last received: " + text));

        new BroadcastChannelTrigger(this, CHANNEL)
                .triggers(new SetSignalAction<>(lastMessage, String.class,
                        BroadcastChannelTrigger.EventData.data));

        HorizontalLayout row = new HorizontalLayout(send, badge);
        row.addClassName("uc7-row");
        add(message, row);
    }
}
