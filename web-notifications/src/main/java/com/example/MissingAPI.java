package com.example;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Temporary shim for the browser
 * <a href="https://developer.mozilla.org/en-US/docs/Web/API/Notifications_API">
 * Web Notifications API</a> until Vaadin Flow ships a real server-side API for
 * it.
 * <p>
 * Flow has no API for {@code Notification.requestPermission()},
 * {@code new Notification(title, options)} or {@code Notification.permission}.
 * (The {@code flow-webpush} module requests the same permission as a side
 * effect of {@code WebPush#subscribe}, but only to create a push subscription,
 * and exposes the permission as one-shot callbacks.) This class wraps the
 * browser API with public Flow primitives only:
 * <ul>
 * <li>{@link #permissionSignal(UI)} — a per-UI signal of the current
 * permission, kept up to date from the browser (Permissions API
 * {@code change} event, plus a re-check whenever the tab becomes visible or
 * focused again).</li>
 * <li>{@link #requestPermissionOnClick(Component)} — binds
 * {@code Notification.requestPermission()} to the component's native
 * {@code click} listener on the client, so the call happens inside the user
 * gesture that browsers require. A server round-trip would lose the
 * gesture.</li>
 * <li>{@link #showNotification(UI, String, NotificationOptions)} — shows a
 * native OS notification and returns a handle whose click / close / error
 * events are delivered back to the server as DOM events on the UI
 * element.</li>
 * </ul>
 * All values are passed to the client as {@code executeJs} parameters
 * (options are serialised with Jackson), never concatenated into JavaScript.
 * <p>
 * <b>Migration:</b> once a Flow API lands, replace
 * {@code MissingAPI.permissionSignal(ui)} with the framework's permission
 * signal, {@code requestPermissionOnClick} with the framework's click-bound
 * request (e.g. a trigger action in the style of {@code WebShare.onClick}),
 * and {@code showNotification} with the framework's show method. See
 * {@code API-GAPS.md} for the suggested shape.
 */
public final class MissingAPI {

    /** Client → server: the current permission, {@code detail.permission}. */
    static final String PERMISSION_EVENT = "vaadin-web-notification-permission";
    /** Client → server: a notification was clicked, {@code detail.id}. */
    static final String CLICK_EVENT = "vaadin-web-notification-click";
    /** Client → server: a notification was closed, {@code detail.id}. */
    static final String CLOSE_EVENT = "vaadin-web-notification-close";
    /**
     * Client → server: a notification could not be shown, {@code detail.id} +
     * {@code detail.reason}.
     */
    static final String ERROR_EVENT = "vaadin-web-notification-error";

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    /**
     * Installed once per UI on the UI (body) element: reports the current
     * permission and re-reports it whenever it may have changed.
     */
    private static final String INSTALL_JS = """
            const body = this;
            const report = (force) => {
              const p = (!('Notification' in window) || !window.isSecureContext)
                  ? 'unsupported' : Notification.permission;
              if (!force && body.__vaadinWebNotificationPermission === p) {
                return;
              }
              body.__vaadinWebNotificationPermission = p;
              body.dispatchEvent(new CustomEvent($0, { detail: { permission: p } }));
            };
            body.__vaadinWebNotificationReport = report;
            report(true);
            if (navigator.permissions && navigator.permissions.query) {
              navigator.permissions.query({ name: 'notifications' })
                  .then((status) => { status.onchange = () => report(false); })
                  .catch(() => {});
            }
            document.addEventListener('visibilitychange', () => report(false));
            window.addEventListener('focus', () => report(false));
            """;

    /**
     * Installed on the trigger element: calls requestPermission() from the
     * native click listener, i.e. inside the user gesture. Handles both the
     * Promise form and the legacy callback form (older Safari).
     */
    private static final String REQUEST_ON_CLICK_JS = """
            const el = this;
            const body = $0;
            if (el.__vaadinWebNotificationRequest) {
              return;
            }
            const report = () => body.__vaadinWebNotificationReport
                && body.__vaadinWebNotificationReport(true);
            el.__vaadinWebNotificationRequest = () => {
              if (!('Notification' in window) || !window.isSecureContext
                  || Notification.permission !== 'default') {
                report();
                return;
              }
              new Promise((resolve) => {
                const result = Notification.requestPermission(resolve);
                if (result && result.then) {
                  result.then(resolve, resolve);
                }
              }).then(report);
            };
            el.addEventListener('click', el.__vaadinWebNotificationRequest);
            """;

    private static final String REMOVE_REQUEST_ON_CLICK_JS = """
            if (this.__vaadinWebNotificationRequest) {
              this.removeEventListener('click', this.__vaadinWebNotificationRequest);
              delete this.__vaadinWebNotificationRequest;
            }
            """;

    private static final String SHOW_JS = """
            const body = this;
            const title = $0;
            const options = $1;
            const id = $2;
            const fire = (type, extra) => body.dispatchEvent(new CustomEvent(type,
                { detail: Object.assign({ id: id }, extra || {}) }));
            if (!('Notification' in window) || !window.isSecureContext) {
              fire($5, { reason: 'unsupported' });
              return;
            }
            if (Notification.permission !== 'granted') {
              fire($5, { reason: 'permission-' + Notification.permission });
              return;
            }
            let n;
            try {
              n = new Notification(title, options);
            } catch (e) {
              // e.g. Chrome on Android: "Illegal constructor", only
              // ServiceWorkerRegistration.showNotification() is allowed there.
              fire($5, { reason: String((e && e.name) || e) });
              return;
            }
            const registry = body.__vaadinWebNotifications
                || (body.__vaadinWebNotifications = new Map());
            registry.set(id, n);
            n.onclick = (e) => {
              e.preventDefault();
              window.focus();
              n.close();
              fire($3);
            };
            n.onclose = () => {
              registry.delete(id);
              fire($4);
            };
            n.onerror = () => {
              registry.delete(id);
              fire($5, { reason: 'error' });
            };
            """;

    private static final String CLOSE_JS = """
            const registry = this.__vaadinWebNotifications;
            if (!registry) {
              return;
            }
            registry.forEach((n, id) => {
              if ((id === $0) || ($1 !== null && n.tag === $1)) {
                n.close();
              }
            });
            """;

    private MissingAPI() {
    }

    /**
     * The browser's notification permission, plus the two states the server
     * needs on top of the three the spec defines.
     */
    public enum NotificationPermission {
        /** The browser hasn't reported yet (initial state of every UI). */
        UNKNOWN,
        /** {@code "default"} — the user hasn't decided; a prompt is possible. */
        DEFAULT,
        /** {@code "granted"} — notifications may be shown. */
        GRANTED,
        /**
         * {@code "denied"} — the user blocked notifications. Browsers never
         * prompt again; only the user can re-enable it in site settings.
         */
        DENIED,
        /**
         * The browser has no {@code Notification} constructor, or the page is
         * not a secure context (HTTPS or localhost).
         */
        UNSUPPORTED;

        static NotificationPermission fromClient(@Nullable String value) {
            if (value == null) {
                return UNKNOWN;
            }
            return switch (value) {
            case "default" -> DEFAULT;
            case "granted" -> GRANTED;
            case "denied" -> DENIED;
            case "unsupported" -> UNSUPPORTED;
            default -> UNKNOWN;
            };
        }
    }

    /**
     * Subset of the browser's {@code NotificationOptions} dictionary. Null
     * members are omitted from the JSON sent to the browser.
     *
     * @param body
     *            secondary text shown under the title
     * @param icon
     *            URL of an icon image
     * @param tag
     *            identifier used to replace an existing notification with the
     *            same tag instead of stacking a new one
     * @param requireInteraction
     *            keep the notification on screen until the user acts on it
     *            (ignored by some platforms)
     * @param renotify
     *            alert the user again when a notification replaces one with
     *            the same tag (Chromium only; requires {@code tag})
     * @param silent
     *            suppress sound and vibration
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record NotificationOptions(@Nullable String body,
            @Nullable String icon, @Nullable String tag,
            @Nullable Boolean requireInteraction, @Nullable Boolean renotify,
            @Nullable Boolean silent) implements Serializable {

        /** Options with only a body text. */
        public static NotificationOptions body(String body) {
            return new NotificationOptions(body, null, null, null, null, null);
        }

        public NotificationOptions withIcon(@Nullable String icon) {
            return new NotificationOptions(body, icon, tag, requireInteraction,
                    renotify, silent);
        }

        public NotificationOptions withTag(@Nullable String tag) {
            return new NotificationOptions(body, icon, tag, requireInteraction,
                    renotify, silent);
        }

        public NotificationOptions withRequireInteraction(boolean value) {
            return new NotificationOptions(body, icon, tag, value, renotify,
                    silent);
        }

        public NotificationOptions withRenotify(boolean value) {
            return new NotificationOptions(body, icon, tag, requireInteraction,
                    value, silent);
        }

        public NotificationOptions withSilent(boolean value) {
            return new NotificationOptions(body, icon, tag, requireInteraction,
                    renotify, value);
        }
    }

    /**
     * Server-side handle for a notification that was handed to the browser.
     * Listeners registered right after
     * {@link MissingAPI#showNotification(UI, String, NotificationOptions)}
     * returns are guaranteed to be in place before any client event arrives.
     */
    public static final class ShownNotification implements Serializable {

        private final UI ui;
        private final String id;
        private final String title;
        private final @Nullable String tag;
        private @Nullable SerializableConsumer<ShownNotification> clickHandler;
        private @Nullable SerializableRunnable closeHandler;
        private @Nullable SerializableConsumer<String> errorHandler;

        private ShownNotification(UI ui, String id, String title,
                @Nullable String tag) {
            this.ui = ui;
            this.id = id;
            this.title = title;
            this.tag = tag;
        }

        public String id() {
            return id;
        }

        public String title() {
            return title;
        }

        public @Nullable String tag() {
            return tag;
        }

        /**
         * Runs on the server when the user clicks the notification. The shim
         * has already focused the tab and closed the notification on the
         * client.
         */
        public ShownNotification onClick(
                SerializableConsumer<ShownNotification> handler) {
            this.clickHandler = handler;
            return this;
        }

        /** Runs on the server when the notification is closed. */
        public ShownNotification onClose(SerializableRunnable handler) {
            this.closeHandler = handler;
            return this;
        }

        /**
         * Runs on the server when the browser refused to show the
         * notification. The argument is a short reason such as
         * {@code "unsupported"}, {@code "permission-denied"} or the name of
         * the error thrown by the constructor.
         */
        public ShownNotification onError(SerializableConsumer<String> handler) {
            this.errorHandler = handler;
            return this;
        }

        /** Closes the notification in the browser, if it is still shown. */
        public void close() {
            ui.getElement().executeJs(CLOSE_JS, id, null);
        }
    }

    /**
     * Returns the notification permission of the browser tab behind the given
     * UI as a read-only signal. Starts as
     * {@link NotificationPermission#UNKNOWN} and is updated as soon as the
     * browser reports.
     */
    public static Signal<NotificationPermission> permissionSignal(UI ui) {
        return state(ui).permission.asReadonly();
    }

    /**
     * Asks the browser to report the current permission again. Useful after
     * the user was told to change it in the site settings.
     */
    public static void refreshPermission(UI ui) {
        state(ui);
        ui.getElement().executeJs(
                "this.__vaadinWebNotificationReport && this.__vaadinWebNotificationReport(true)");
    }

    /**
     * Makes every click on {@code trigger} call
     * {@code Notification.requestPermission()} on the client, inside the
     * click's user gesture. Once the user answers,
     * {@link #permissionSignal(UI)} updates. When the permission is already
     * decided the browser would not prompt again, so the click only refreshes
     * the signal.
     * <p>
     * Server-side click listeners on the same component still run as usual.
     *
     * @return registration that removes the client-side binding
     */
    public static Registration requestPermissionOnClick(Component trigger) {
        Element element = trigger.getElement();
        SerializableConsumer<UI> install = ui -> {
            state(ui);
            element.executeJs(REQUEST_ON_CLICK_JS, ui.getElement());
        };
        trigger.getUI().ifPresent(install);
        Registration attach = trigger
                .addAttachListener(e -> install.accept(e.getUI()));
        return () -> {
            attach.remove();
            element.executeJs(REMOVE_REQUEST_ON_CLICK_JS);
        };
    }

    /**
     * Shows a native OS notification in the browser tab behind {@code ui}.
     * The browser silently refuses unless the permission is
     * {@link NotificationPermission#GRANTED}; the refusal is reported via
     * {@link ShownNotification#onError}.
     * <p>
     * A notification with the same {@code tag} as an older one replaces it;
     * the older handle is dropped on the server, since browsers don't fire a
     * close event for replaced notifications.
     */
    public static ShownNotification showNotification(UI ui, String title,
            NotificationOptions options) {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(options, "options");
        State state = state(ui);
        String id = UUID.randomUUID().toString();
        ShownNotification handle = new ShownNotification(ui, id, title,
                options.tag());
        if (options.tag() != null) {
            state.live.values()
                    .removeIf(n -> options.tag().equals(n.tag));
        }
        state.live.put(id, handle);
        JsonNode json = MAPPER.valueToTree(options);
        ui.getElement().executeJs(SHOW_JS, title, json, id, CLICK_EVENT,
                CLOSE_EVENT, ERROR_EVENT);
        return handle;
    }

    /** Closes every shown notification that carries the given tag. */
    public static void closeNotifications(UI ui, String tag) {
        Objects.requireNonNull(tag, "tag");
        state(ui).live.values().removeIf(n -> tag.equals(n.tag));
        ui.getElement().executeJs(CLOSE_JS, null, tag);
    }

    /**
     * Notifications shown in this UI that the browser hasn't reported as
     * closed yet. Package-private: used by the test simulator.
     */
    static List<ShownNotification> liveNotifications(UI ui) {
        return new ArrayList<>(state(ui).live.values());
    }

    // ------------------------------------------------------------------

    /** Event detail sent by the client for permission reports. */
    public record PermissionDetail(@Nullable String permission) {
    }

    /** Event detail sent by the client for notification events. */
    public record NotificationDetail(@Nullable String id, @Nullable String reason) {
    }

    private static final class State implements Serializable {
        private final ValueSignal<NotificationPermission> permission = new ValueSignal<>(
                NotificationPermission.UNKNOWN);
        private final Map<String, ShownNotification> live = new LinkedHashMap<>();
    }

    private static State state(UI ui) {
        State state = ComponentUtil.getData(ui, State.class);
        if (state == null) {
            state = new State();
            ComponentUtil.setData(ui, State.class, state);
            install(ui, state);
        }
        return state;
    }

    private static void install(UI ui, State state) {
        Element body = ui.getElement();
        body.addEventListener(PERMISSION_EVENT, e -> {
            PermissionDetail detail = e.getEventDetail(PermissionDetail.class);
            state.permission.set(NotificationPermission
                    .fromClient(detail == null ? null : detail.permission()));
        }).addEventDetail().allowInert();
        body.addEventListener(CLICK_EVENT, e -> {
            ShownNotification n = find(state, e);
            if (n != null && n.clickHandler != null) {
                n.clickHandler.accept(n);
            }
        }).addEventDetail().allowInert();
        body.addEventListener(CLOSE_EVENT, e -> {
            ShownNotification n = find(state, e);
            if (n != null) {
                state.live.remove(n.id);
                if (n.closeHandler != null) {
                    n.closeHandler.run();
                }
            }
        }).addEventDetail().allowInert();
        body.addEventListener(ERROR_EVENT, e -> {
            ShownNotification n = find(state, e);
            if (n != null) {
                state.live.remove(n.id);
                if (n.errorHandler != null) {
                    NotificationDetail detail = e
                            .getEventDetail(NotificationDetail.class);
                    String reason = detail == null || detail.reason() == null
                            ? "unknown"
                            : detail.reason();
                    n.errorHandler.accept(reason);
                }
            }
        }).addEventDetail().allowInert();
        body.executeJs(INSTALL_JS, PERMISSION_EVENT);
    }

    private static @Nullable ShownNotification find(State state, DomEvent e) {
        NotificationDetail detail = e.getEventDetail(NotificationDetail.class);
        if (detail == null || detail.id() == null) {
            return null;
        }
        return state.live.get(detail.id());
    }
}
