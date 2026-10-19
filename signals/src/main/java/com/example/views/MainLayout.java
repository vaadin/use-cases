package com.example.views;

import java.util.Locale;
import java.util.Map;

import com.example.common.BaseMainLayout;
import com.example.common.SlackFeedback;
import com.example.preferences.UserPreferences;
import com.example.security.CurrentUserSignal;
import com.example.security.SecurityConfiguration;
import com.example.signals.SessionIdHelper;
import com.example.signals.UserSessionRegistry;
import org.jspecify.annotations.Nullable;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;

@PageTitle("Signal API Use Cases")
@AnonymousAllowed
public class MainLayout extends BaseMainLayout {

    private final CurrentUserSignal currentUserSignal;
    private final UserSessionRegistry userSessionRegistry;
    private final UserPreferences userPreferences;
    private @Nullable String currentUser;
    private @Nullable String sessionId;
    private TextField nicknameField;

    public MainLayout(CurrentUserSignal currentUserSignal,
            UserSessionRegistry userSessionRegistry,
            UserPreferences userPreferences, SlackFeedback slackFeedback) {
        super("signals", "Signal API Use Cases", slackFeedback);
        this.currentUserSignal = currentUserSignal;
        this.userSessionRegistry = userSessionRegistry;
        this.userPreferences = userPreferences;

        // The session-scoped signal outlives a login, so re-read the
        // authentication state on every page load
        currentUserSignal.refresh();
        this.currentUser = currentUserSignal.getUserSignal().peek()
                .getUsername();

        // Active users display with avatars
        HorizontalLayout activeUsersDisplay = new HorizontalLayout();
        activeUsersDisplay.setSpacing(true);
        activeUsersDisplay.setAlignItems(
                com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        activeUsersDisplay.getStyle().set("margin-left", "auto")
                .set("margin-right", "1em");

        Span activeUsersLabel = new Span(
                userSessionRegistry.getDisplayNamesSignal()
                        .map(displayNames -> displayNames.isEmpty() ? ""
                                : "👥 " + displayNames.size() + " online:"));
        activeUsersLabel.getStyle()
                .set("color", "var(--vaadin-text-color-secondary)")
                .set("font-size", "var(--aura-font-size-s)");

        com.vaadin.flow.component.html.Div avatarsContainer = new com.vaadin.flow.component.html.Div();
        avatarsContainer.getStyle().set("display", "flex").set("gap", "0.25em");

        avatarsContainer.bindChildren(
                userSessionRegistry.getActiveUsersSignal(), userSignal -> {
                    var user = userSignal.peek();
                    return new ColoredAvatar(user.username(), user.cssColor(),
                            32);
                });

        activeUsersDisplay.add(activeUsersLabel, avatarsContainer);

        // Nickname setting UI
        nicknameField = new TextField();
        nicknameField.setPlaceholder("Set nickname...");
        nicknameField.setWidth("150px");
        nicknameField.setClearButtonVisible(true);
        nicknameField.getStyle().set("margin-right", "1em");

        // Save nickname on change
        nicknameField.addValueChangeListener(event -> {
            if (currentUser != null && sessionId != null) {
                String nickname = event.getValue();
                userSessionRegistry.setNickname(currentUser, sessionId,
                        nickname);
            }
        });

        // Locale selector for i18n
        Map<String, Locale> localeMap = Map.of("English", Locale.ENGLISH,
                "Espanol", new Locale("es"), "Suomi", new Locale("fi"));

        Select<String> localeSelector = new Select<>();
        localeSelector.setItems("English", "Espanol", "Suomi");
        localeSelector.setWidth("100px");
        localeSelector.getStyle().set("margin-right", "1em");

        // Initialize selected value from UI locale signal
        Locale currentLocale = UI.getCurrent().localeSignal().peek();
        String initialSelection = localeMap.entrySet().stream()
                .filter(e -> e.getValue().getLanguage()
                        .equals(currentLocale.getLanguage()))
                .map(Map.Entry::getKey).findFirst().orElse("English");
        localeSelector.setValue(initialSelection);

        // Update locale signal on change
        localeSelector.addValueChangeListener(event -> {
            Locale selectedLocale = localeMap.get(event.getValue());
            if (selectedLocale != null) {
                UI.getCurrent().setLocale(selectedLocale);
            }
        });

        // Current user display with avatar
        HorizontalLayout userDisplay = new HorizontalLayout();
        userDisplay.setSpacing(true);
        userDisplay.setAlignItems(
                com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        userDisplay.getStyle().set("margin-right", "1em");

        Avatar userAvatar = new Avatar();
        userAvatar.getElement().bindProperty("name", currentUserSignal
                .getUserSignal().map(CurrentUserSignal.UserInfo::getUsername),
                null);
        userAvatar.getElement().bindProperty("img",
                currentUserSignal.getUserSignal()
                        .map(user -> user.isAuthenticated()
                                ? getProfilePicturePath(user.getUsername())
                                : ""),
                null);

        Span userName = new Span(currentUserSignal.getUserSignal()
                .map(CurrentUserSignal.UserInfo::getUsername));
        userName.getStyle().set("color", "var(--vaadin-text-color-secondary)")
                .set("font-size", "var(--aura-font-size-s)");

        userDisplay.add(userAvatar, userName);

        // Logout button
        @SuppressWarnings("NullAway")
        Button logoutButton = new Button("Logout", event -> {
            SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
            logoutHandler.logout(
                    VaadinServletRequest.getCurrent().getHttpServletRequest(),
                    null, null);
            getUI().ifPresent(ui -> ui.getPage().setLocation("/"));
        });
        logoutButton.addThemeVariants(ButtonVariant.TERTIARY);
        logoutButton.bindVisible(currentUserSignal.getUserSignal()
                .map(CurrentUserSignal.UserInfo::isAuthenticated));

        // Logging in is optional; guests can switch to a demo user any time
        Button loginButton = new Button("Log in",
                event -> getUI().ifPresent(this::loginAndReturn));
        loginButton.addThemeVariants(ButtonVariant.TERTIARY);
        loginButton.bindVisible(currentUserSignal.getUserSignal()
                .map(user -> !user.isAuthenticated()));

        addToNavbar(activeUsersDisplay, nicknameField, localeSelector,
                userDisplay, logoutButton, loginButton);

        // Apply session-scoped background color reactively to the whole layout
        getStyle().bind("background-color",
                userPreferences.backgroundColorSignal());

        Signal.effect(this, () -> {
            var state = UI.getCurrent().routerStateSignal().get();
            if (state.navigationTarget() != null && currentUser != null
                    && sessionId != null) {
                userSessionRegistry.registerUser(currentUser, sessionId,
                        state.location().getPath());
            }
        });
    }

    /**
     * Reloads the current page with the login parameter, so Spring Security
     * shows the login view and returns to this page after a successful login.
     */
    private void loginAndReturn(UI ui) {
        Location location = ui.getActiveViewLocation();
        QueryParameters parameters = location.getQueryParameters()
                .merging(SecurityConfiguration.LOGIN_PARAMETER, "");
        // Relative to the document base, so a context path is preserved
        ui.getPage().setLocation(new Location(location.getPath(), parameters)
                .getPathWithQueryParameters());
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        super.beforeEnter(event);
        // Drop the login parameters from the URL once the user is back
        QueryParameters parameters = event.getLocation().getQueryParameters();
        if (parameters.getParameters()
                .containsKey(SecurityConfiguration.LOGIN_PARAMETER)) {
            event.forwardTo(event.getLocation().getPath(), parameters.excluding(
                    SecurityConfiguration.LOGIN_PARAMETER, "continue"));
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.sessionId = SessionIdHelper.getCurrentSessionId();

        if (currentUser != null && sessionId != null) {
            // Load current nickname if exists
            String currentNickname = userSessionRegistry
                    .getNickname(currentUser, sessionId);
            if (currentNickname != null) {
                nicknameField.setValue(currentNickname);
            }
        }

        // Set up Page Visibility API listener to track tab activity
        getElement().executeJs("const updateVisibility = () => {"
                + "  const isVisible = document.visibilityState === 'visible';"
                + "  $0.$server.onVisibilityChange(isVisible);" + "};"
                + "document.addEventListener('visibilitychange', updateVisibility);"
                + "updateVisibility();" // Report initial state
                , getElement());
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);

        if (currentUser != null && sessionId != null) {
            userSessionRegistry.unregisterUser(currentUser, sessionId);
        }
    }

    /**
     * Called from JavaScript when the tab visibility changes.
     *
     * @param isVisible
     *            true if the tab is visible, false if hidden
     */
    @ClientCallable
    public void onVisibilityChange(boolean isVisible) {
        if (currentUser != null && sessionId != null) {
            userSessionRegistry.updateTabActivity(currentUser, sessionId,
                    isVisible);
        }
    }

    /**
     * Get the profile picture path for a username. Images are stored in
     * src/main/resources/META-INF/resources/profile-pictures/
     */
    public static String getProfilePicturePath(@Nullable String username) {
        if (username == null) {
            return "";
        }
        // Map username to profile picture (images are in
        // META-INF/resources/profile-pictures)
        return switch (username.toLowerCase()) {
        case "admin" -> "/profile-pictures/admin.png";
        case "editor" -> "/profile-pictures/editor.png";
        case "viewer" -> "/profile-pictures/viewer.png";
        case "superadmin" -> "/profile-pictures/superadmin.png";
        default -> ""; // No image for unknown users
        };
    }
}
