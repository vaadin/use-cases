package com.example.muc04;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.fieldhighlighter.FieldHighlighterInitializer;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.Signal;

/**
 * Provides a signal-driven Java API for the {@code vaadin-field-highlighter}
 * web component.
 * <p>
 * Extends {@link FieldHighlighterInitializer} to inherit the {@code @JsModule}
 * and {@code @NpmPackage} annotations and access the {@code init()} method.
 * <p>
 * Prototype for a future addition to the Flow framework, replacing
 * Collaboration Engine's field-highlighter integration with a signal-based
 * approach.
 */
public class SignalFieldHighlighter extends FieldHighlighterInitializer {

    private static final String FH_CLASS = "customElements.get('vaadin-field-highlighter')";

    /**
     * Represents a user to display in the field-highlighter.
     *
     * @param id
     *            unique user identifier
     * @param name
     *            display name shown in the user tag
     * @param colorIndex
     *            index into the user color palette
     *            ({@code --vaadin-user-color-N} CSS properties)
     */
    public record User(int id, String name, int colorIndex) {
    }

    private SignalFieldHighlighter() {
    }

    /**
     * Initializes the field-highlighter on the given field and binds it to a
     * signal of a list of user signals. Each inner signal is read inside the
     * effect, so changes to both the list structure and individual entries
     * trigger an update.
     * <p>
     * This is designed for use with {@code SharedListSignal<User>} or similar
     * signal-of-signals patterns.
     *
     * @param field
     *            the field component to highlight
     * @param usersSignal
     *            a signal providing a list of signals, each holding a user
     * @param <C>
     *            the component type
     * @return a registration that removes the binding when called
     */
    public static <C extends Component & HasElement> Registration bind(C field,
            Signal<? extends List<? extends Signal<User>>> usersSignal) {
        return bind(field, usersSignal, null);
    }

    /**
     * Initializes the field-highlighter on the given field and binds it to a
     * signal of a list of user signals, excluding the local user. Each inner
     * signal is read inside the effect, so changes to both the list structure
     * and individual entries trigger an update.
     * <p>
     * This is designed for use with {@code SharedListSignal<User>} or similar
     * signal-of-signals patterns.
     *
     * @param field
     *            the field component to highlight
     * @param usersSignal
     *            a signal providing a list of signals, each holding a user
     * @param localUser
     *            the local user to exclude from highlighting, or {@code null}
     *            to include all users
     * @param <C>
     *            the component type
     * @return a registration that removes the binding when called
     */
    public static <C extends Component & HasElement> Registration bind(C field,
            Signal<? extends List<? extends Signal<User>>> usersSignal,
            @Nullable User localUser) {
        Registration initReg = init(field.getElement());
        Registration effectReg = Signal.effect(field, () -> {
            List<User> users = usersSignal.get().stream().map(Signal::get)
                    .filter(u -> u != null
                            && (localUser == null || u.id() != localUser.id()))
                    .toList();
            setUsers(field.getElement(), users);
        });
        return Registration.combine(effectReg, initReg);
    }

    /**
     * Sets the users highlighting a field. Replaces any previously set users.
     *
     * @param element
     *            the field element
     * @param users
     *            the users to display
     */
    public static void setUsers(Element element, Collection<User> users) {
        element.executeJs(FieldHighlighterJs.class).setUsers(toJson(users));
    }

    /**
     * Adds a single user to the field's highlighter.
     *
     * @param element
     *            the field element
     * @param user
     *            the user to add
     */
    public static void addUser(Element element, User user) {
        element.executeJs(FieldHighlighterJs.class).addUser(toJson(user));
    }

    /**
     * Removes a single user from the field's highlighter.
     *
     * @param element
     *            the field element
     * @param user
     *            the user to remove
     */
    public static void removeUser(Element element, User user) {
        element.executeJs(FieldHighlighterJs.class).removeUser(toJson(user));
    }

    private static ArrayNode toJson(Collection<User> users) {
        return users.stream().map(SignalFieldHighlighter::toJson)
                .collect(JacksonUtils.asArray());
    }

    private static ObjectNode toJson(User user) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("id", user.id());
        node.put("name", user.name());
        node.put("colorIndex", user.colorIndex());
        return node;
    }

    @JsDefinition
    private interface FieldHighlighterJs extends Serializable {

        @JsExpression(FH_CLASS + ".setUsers(this, $0)")
        void setUsers(ArrayNode users);

        @JsExpression(FH_CLASS + ".addUser(this, $0)")
        void addUser(ObjectNode user);

        @JsExpression(FH_CLASS + ".removeUser(this, $0)")
        void removeUser(ObjectNode user);
    }
}
