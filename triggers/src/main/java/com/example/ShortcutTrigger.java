package com.example;

import java.util.EnumSet;
import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.shared.Registration;

/**
 * Fires when a key is pressed with exactly the given modifiers, while the host
 * is attached. Flow has no keyboard-shortcut {@code Trigger}, so this class is
 * a local stand-in until it has one (API-GAPS.md #1). Once it does, delete this
 * class and use Flow's.
 * <p>
 * Listens for {@code keydown} on {@code window} in the capture phase. That is
 * the only reliable way to run before the browser's own shortcut (e.g.
 * {@code Cmd+S} opens the Save Page dialog before an element listener sees the
 * event). The listener is installed when the host attaches and removed when it
 * detaches, so the shortcut works anywhere on the page while the view is open,
 * wherever the focus is.
 * <p>
 * The modifiers must match exactly: the given ones must be pressed and all
 * others must not, so {@code Ctrl+S} does not also fire on
 * {@code Ctrl+Shift+S}. The key is matched against both {@code event.key} and
 * {@code event.code}, so {@link Key#KEY_S} works whichever the browser sends.
 * Before the actions run, the trigger calls {@code preventDefault()} and
 * {@code stopPropagation()}, so the browser's default action and other handlers
 * do not run too.
 * <p>
 * To accept both {@code Ctrl+S} and {@code Cmd+S}, create one trigger for each
 * modifier:
 *
 * <pre>{@code
 * new ShortcutTrigger(view, Key.KEY_S, KeyModifier.CONTROL).triggers(save);
 * new ShortcutTrigger(view, Key.KEY_S, KeyModifier.META).triggers(save);
 * }</pre>
 * <p>
 * {@link KeyModifier#ALT_GRAPH} is rejected because {@code KeyboardEvent} has
 * no {@code altGraphKey} flag.
 */
public class ShortcutTrigger extends Trigger {

    private final Key key;
    private final EnumSet<KeyModifier> modifiers;

    /**
     * Creates a shortcut trigger that fires when {@code key} is pressed with
     * exactly the given {@code modifiers} held down.
     *
     * @param host
     *            the component whose root element listens for the shortcut
     * @param key
     *            the key that completes the shortcut, not {@code null}
     * @param modifiers
     *            the modifiers that must be held; pass none for a plain-key
     *            shortcut. Must not contain {@link KeyModifier#ALT_GRAPH}.
     */
    public ShortcutTrigger(Component host, Key key, KeyModifier... modifiers) {
        super(host);
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(modifiers, "modifiers");
        this.key = key;
        this.modifiers = EnumSet.noneOf(KeyModifier.class);
        for (KeyModifier modifier : modifiers) {
            Objects.requireNonNull(modifier, "modifier");
            if (modifier == KeyModifier.ALT_GRAPH) {
                throw new IllegalArgumentException(
                        "ALT_GRAPH is not supported as a shortcut modifier");
            }
            this.modifiers.add(modifier);
        }
    }

    @Override
    protected Registration install(JsFunction action) {
        // Listens on window in the capture phase, so it runs before the
        // browser's own shortcut (e.g. Ctrl+S Save Page) and before any
        // handler further down that might stop the event.
        return getHost().addJsInitializer(
                """
                        const [keys, ctrl, shift, alt, meta] = [$1, $2, $3, $4, $5];
                        const listener = (e) => {
                            if (e.ctrlKey !== ctrl || e.shiftKey !== shift
                                    || e.altKey !== alt || e.metaKey !== meta) return;
                            if (!keys.includes(e.key) && !keys.includes(e.code)) return;
                            e.preventDefault();
                            e.stopPropagation();
                            $0(e);
                        };
                        window.addEventListener('keydown', listener, true);
                        return () => window.removeEventListener('keydown', listener, true);""",
                action, JacksonUtils.getMapper().valueToTree(key.getKeys()),
                modifiers.contains(KeyModifier.CONTROL),
                modifiers.contains(KeyModifier.SHIFT),
                modifiers.contains(KeyModifier.ALT),
                modifiers.contains(KeyModifier.META));
    }
}
