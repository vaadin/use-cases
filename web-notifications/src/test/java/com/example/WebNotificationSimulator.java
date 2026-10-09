package com.example;

import java.util.List;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import com.example.MissingAPI.NotificationPermission;
import com.example.MissingAPI.ShownNotification;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;

/**
 * Test-side stand-in for the browser half of {@link MissingAPI}. Browserless
 * tests never run the client JavaScript, so this helper fires the same DOM
 * events the shim's JavaScript would dispatch on the UI element: permission
 * reports and notification click / close / error events.
 * <p>
 * Flow has no simulator for the Notification API (compare
 * {@code GeolocationSimulator}); see API-GAPS.md.
 */
public final class WebNotificationSimulator {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private WebNotificationSimulator() {
    }

    /** Pretends the browser reported the given permission. */
    public static void setPermission(NotificationPermission permission) {
        ObjectNode detail = MAPPER.createObjectNode();
        detail.put("permission", switch (permission) {
        case DEFAULT -> "default";
        case GRANTED -> "granted";
        case DENIED -> "denied";
        case UNSUPPORTED -> "unsupported";
        case UNKNOWN -> "unknown";
        });
        fire(MissingAPI.PERMISSION_EVENT, detail);
    }

    /** Notifications handed to the browser and not yet closed. */
    public static List<ShownNotification> shown() {
        return MissingAPI.liveNotifications(UI.getCurrent());
    }

    /**
     * Pretends the user clicked the notification: the shim fires a click
     * event and then, because it closes the notification, a close event.
     */
    public static void click(ShownNotification notification) {
        fire(MissingAPI.CLICK_EVENT, idDetail(notification));
        fire(MissingAPI.CLOSE_EVENT, idDetail(notification));
    }

    /** Pretends the browser refused to show the notification. */
    public static void fail(ShownNotification notification, String reason) {
        ObjectNode detail = idDetail(notification);
        detail.put("reason", reason);
        fire(MissingAPI.ERROR_EVENT, detail);
    }

    private static ObjectNode idDetail(ShownNotification notification) {
        ObjectNode detail = MAPPER.createObjectNode();
        detail.put("id", notification.id());
        return detail;
    }

    private static void fire(String type, ObjectNode detail) {
        Element body = UI.getCurrent().getElement();
        ObjectNode data = MAPPER.createObjectNode();
        data.set("event.detail", detail);
        body.getNode().getFeature(ElementListenerMap.class)
                .fireEvent(new DomEvent(body, type, data));
    }
}
