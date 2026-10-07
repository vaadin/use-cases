package com.example;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.shared.Registration;

/**
 * Fires when a key + modifier combination is pressed while focus is in the
 * host's subtree. A local stand-in for the {@code ShortcutTrigger} that lives
 * on the {@code feature/triggers-actions} branch of vaadin/flow and has not
 * landed in mainline yet. Once the upstream class ships, delete this file and
 * import {@code com.vaadin.flow.component.trigger.internal.ShortcutTrigger}
 * instead.
 *
 * <p>
 * Listens for {@code keydown} on {@code window} in capture phase — that's the
 * only reliable way to beat the browser's built-in shortcut handling (e.g.
 * {@code Ctrl+S} opens the Save Page dialog before any element-scoped listener
 * can see the event). The listener is installed when the host element attaches
 * and removed when it detaches, so the shortcut is "active while the view is
 * mounted" — not "active only when focus is in the host's subtree".
 * <p>
 * Filters by an exact modifier match (required modifiers must be pressed, all
 * others must NOT be pressed — so {@code Ctrl+S} doesn't also fire on
 * {@code Ctrl+Shift+S}, leaving that combo free to bind separately), matches
 * the key against both {@code event.key} and {@code event.code} so a single
 * binding handles {@link Key#KEY_S} regardless of which representation the
 * browser sends, and calls {@code preventDefault()} and
 * {@code stopPropagation()} so the browser's default action and any other
 * handler do not double-fire.
 *
 * <p>
 * Example:
 *
 * <pre>{@code
 * new ShortcutTrigger(layout, Key.KEY_S, KeyModifier.CONTROL)
 *         .triggers(saveAction);
 * }</pre>
 *
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
        List<String> keyNames = key.getKeys();
        StringBuilder allowed = new StringBuilder("[");
        for (int i = 0; i < keyNames.size(); i++) {
            if (i > 0) {
                allowed.append(',');
            }
            allowed.append(JacksonUtils.getMapper().valueToTree(keyNames.get(i))
                    .toString());
        }
        allowed.append("]");

        StringBuilder js = new StringBuilder();
        js.append("const allowed=").append(allowed).append(";");
        js.append("const listener=(e)=>{");
        // Exact-match modifier guard.
        js.append("if(");
        js.append(modifiers.contains(KeyModifier.CONTROL) ? "!e.ctrlKey"
                : "e.ctrlKey");
        js.append("||");
        js.append(modifiers.contains(KeyModifier.SHIFT) ? "!e.shiftKey"
                : "e.shiftKey");
        js.append("||");
        js.append(
                modifiers.contains(KeyModifier.ALT) ? "!e.altKey" : "e.altKey");
        js.append("||");
        js.append(modifiers.contains(KeyModifier.META) ? "!e.metaKey"
                : "e.metaKey");
        js.append(")return;");
        // Key match — try event.key and event.code so KEY_S works either way.
        js.append(
                "if(!allowed.includes(e.key)&&!allowed.includes(e.code))return;");
        js.append("e.preventDefault();e.stopPropagation();$0(e);");
        js.append("};");
        // Window + capture: beats the browser's built-in shortcut handler
        // (e.g. Ctrl+S Save Page) and any descendant handler that might
        // stopPropagation before the event reaches the host's bubble phase.
        js.append("window.addEventListener('keydown',listener,true);");
        js.append(
                "return ()=>window.removeEventListener('keydown',listener,true);");
        return getHost().addJsInitializer(js.toString(), action);
    }
}
