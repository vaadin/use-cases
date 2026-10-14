package com.example.muc04;

import com.example.common.UseCaseDescription;
import com.example.muc04.SignalFieldHighlighter.User;
import com.example.security.CurrentUserSignal;
import com.example.security.CurrentUserSignal.UserInfo;
import com.example.signals.SessionIdHelper;
import com.example.signals.UserSessionRegistry;
import com.example.views.ActiveUsersDisplay;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.shared.SharedValueSignal;

/**
 * Multi-User Case 4: Collaborative Form Editing
 * <p>
 * Demonstrates collaborative editing with field-highlighter and optional
 * field-level locking:
 * <ul>
 * <li>Show who is editing which field via colored outlines and user tags</li>
 * <li>Optionally lock fields when another user is editing</li>
 * <li>Real-time editing indicators</li>
 * </ul>
 */
@Route(value = "muc-04", layout = MainLayout.class)
@PageTitle("Multi-User Case 4: Collaborative Editing")
@UseCaseDescription("Showing who is editing which field and locking it for others")
@Menu(order = 53, title = "MUC 4: Collaborative Editing")
@StyleSheet("muc04.css")
@AnonymousAllowed
public class MUC04View extends VerticalLayout {

    private final User currentUser;
    private final MUC04Signals muc04Signals;
    private final SharedValueSignal<Boolean> lockingEnabledSignal;

    public MUC04View(CurrentUserSignal currentUserSignal,
            MUC04Signals muc04Signals,
            UserSessionRegistry userSessionRegistry) {
        UserInfo userInfo = currentUserSignal.getUserSignal().peek();
        String username = userInfo.getUsername();
        String sessionId = SessionIdHelper.getCurrentSessionId();
        int colorIndex = userSessionRegistry.getUserColorIndex(username,
                sessionId);
        String compositeKey = username + ":" + sessionId;
        int userId = compositeKey.hashCode();
        Signal<String> displayNameSignal = userSessionRegistry
                .getDisplayNameSignal(compositeKey);
        this.currentUser = new User(userId, displayNameSignal.peek(),
                colorIndex);
        this.muc04Signals = muc04Signals;
        this.lockingEnabledSignal = muc04Signals.getLockingEnabledSignal();

        addClassName("muc04-view");
        setSpacing(true);
        setPadding(true);

        // Update field editor entries when display name changes
        Signal.effect(this, () -> muc04Signals.updateUserName(userId,
                displayNameSignal.get()));

        var lockingCheckbox = new Checkbox("Enable field locking");
        lockingCheckbox.bindValue(lockingEnabledSignal,
                lockingEnabledSignal::set);

        var companyNameField = createCollaborativeField("companyName",
                "Company Name", muc04Signals.getCompanyNameSignal());
        var addressField = createCollaborativeField("address", "Address",
                muc04Signals.getAddressSignal());
        var phoneField = createCollaborativeField("phone", "Phone Number",
                muc04Signals.getPhoneSignal());

        var activeSessionsBox = new ActiveUsersDisplay(userSessionRegistry,
                "muc-04");

        var editorsDiv = createEditorsPanel();

        var saveButton = new Button("Save Changes",
                _ -> Notification.show("Changes saved successfully"));
        saveButton.addThemeName("primary");

        add(new H2("Multi-User Case 4: Collaborative Form Editing"),
                new Paragraph(
                        "This demonstrates collaborative form editing with "
                                + "field-highlighter. When you focus a field, "
                                + "other users see who is editing via colored "
                                + "outlines. Enable field locking to prevent "
                                + "concurrent edits."),
                activeSessionsBox, new H3("Shared Form Data"), lockingCheckbox,
                companyNameField, addressField, phoneField, saveButton,
                new H3("Active Editors"), editorsDiv);
    }

    private Div createEditorsPanel() {
        var editorsDiv = new Div();
        editorsDiv.addClassName("editors-panel");

        addFieldEditorList(editorsDiv, "companyName", "Company Name");
        addFieldEditorList(editorsDiv, "address", "Address");
        addFieldEditorList(editorsDiv, "phone", "Phone Number");

        return editorsDiv;
    }

    private void addFieldEditorList(Div container, String fieldName,
            String label) {
        var editors = muc04Signals.getFieldEditors(fieldName);

        var fieldDiv = new Div();
        fieldDiv.bindVisible(editors.map(list -> !list.isEmpty()));

        var fieldLabel = new Span(label + ": ");
        fieldLabel.addClassName("editors-panel-field-label");
        fieldDiv.add(fieldLabel);

        var namesContainer = new Div();
        namesContainer.addClassName("editors-panel-names-container");
        namesContainer.bindChildren(editors, this::createEditorName);
        fieldDiv.add(namesContainer);

        container.add(fieldDiv);
    }

    private Span createEditorName(SharedValueSignal<User> userSignal) {
        var user = userSignal.peek();
        var name = new Span(user.name());
        name.addClassName("editor-name");
        return name;
    }

    private TextField createCollaborativeField(String fieldName, String label,
            SharedValueSignal<String> signal) {
        var field = new TextField(label);
        field.setWidthFull();
        field.bindValue(signal, signal::set);
        field.addFocusListener(
                _ -> muc04Signals.startEditing(fieldName, currentUser));
        field.addBlurListener(
                _ -> muc04Signals.stopEditing(fieldName, currentUser));

        var editors = muc04Signals.getFieldEditors(fieldName);
        SignalFieldHighlighter.bind(field, editors, currentUser);

        // Disable field if locking is enabled and another user is editing
        var enabledSignal = lockingEnabledSignal.map(lockingEnabled -> {
            if (!lockingEnabled) {
                return true;
            }
            return editors.get().stream().map(Signal::get)
                    .noneMatch(u -> u != null && u.id() != currentUser.id());
        });
        field.bindEnabled(enabledSignal);

        return field;
    }
}
