package com.example.common;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.common.SlackFeedback.UseCase;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;

/**
 * Shared {@link AppLayout} for the use-case demo apps. Renders a navbar with
 * the module title, a drawer holding the cross-app {@link AppCatalog} selector
 * and the auto-generated side navigation, and a fixed-position "View source"
 * overlay that links the current route's view class to its source on GitHub
 * next to a "Feedback" button that opens a {@link FeedbackDialog} for it. The
 * button is only shown when {@link SlackFeedback} is enabled. Above a view
 * marked {@link UnreleasedApi} it shows a banner that links to the pull request
 * the API comes from. Subclasses only need to pass their module id and display
 * title.
 */
@StyleSheet("base-main-layout.css")
public abstract class BaseMainLayout extends AppLayout
        implements BeforeEnterObserver {

    private static final SlackFeedback SLACK_FEEDBACK = SlackFeedback
            .fromEnvironment();

    private static final Pattern PULL_REQUEST_URL = Pattern
            .compile("https://github\\.com/([^/]+/[^/]+)/pull/(\\d+)/?");

    /**
     * The environment variable naming the Git commit or branch "View source"
     * links to, set by a pull request preview to the commit it is deployed
     * from. Without it the links point to {@code main}.
     */
    private static final String SOURCE_REF_ENV = "SOURCE_REF";

    private static final @Nullable String SOURCE_REF = System
            .getenv(SOURCE_REF_ENV);

    private final String moduleId;
    private final String title;
    private final SlackFeedback slackFeedback;
    private final Anchor sourceCodeLink;
    private final Button feedbackButton;
    private final Anchor unreleasedApiLink;
    private final Div unreleasedApiBanner;
    private @Nullable UseCase useCase;

    protected BaseMainLayout(String moduleId, String title) {
        this(moduleId, title, SLACK_FEEDBACK);
    }

    /**
     * Creates the layout with feedback sent to the given {@link SlackFeedback}
     * instead of the one configured by the environment, for apps that provide
     * it themselves, such as a Spring bean that a test can replace.
     */
    protected BaseMainLayout(String moduleId, String title,
            SlackFeedback slackFeedback) {
        this.moduleId = moduleId;
        this.title = title;
        this.slackFeedback = slackFeedback;

        DrawerToggle toggle = new DrawerToggle();

        H1 h1 = new H1(title);
        h1.getStyle().set("font-size", "var(--aura-font-size-l)").set("margin",
                "0");

        addToNavbar(toggle, h1);

        sourceCodeLink = new Anchor("", "View source");
        feedbackButton = new Button("Feedback", overlayIcon(VaadinIcon.COMMENT),
                e -> {
                    if (useCase != null) {
                        new FeedbackDialog(slackFeedback, useCase).open();
                    }
                });
        feedbackButton.setVisible(false);
        getElement().appendChild(
                buildSourceCodeOverlay(sourceCodeLink, feedbackButton));

        unreleasedApiLink = new Anchor();
        unreleasedApiLink.setTarget("_blank");
        unreleasedApiBanner = new Div(
                new Span("This use case uses API that is not released yet. It"
                        + " comes from "),
                unreleasedApiLink, new Span("."));
        unreleasedApiBanner.addClassName("unreleased-api-banner");
        unreleasedApiBanner.setVisible(false);
        // Without a slot name the banner shares the content slot with the
        // view, so being the first child puts it above the view.
        getElement().insertChild(0, unreleasedApiBanner.getElement());

        addToDrawer(AppCatalog.createSelector(moduleId));

        SideNav nav = new SideNav();
        mainNavEntries().forEach(entry -> {
            SideNavItem item = new SideNavItem(entry.title(), entry.path());
            UseCaseDescription description = descriptionOf(entry);
            if (description != null) {
                item.setTooltipText(description.value());
            }
            nav.addItem(item);
        });
        addToDrawer(nav);
    }

    /**
     * The menu entries listed in the main side navigation, in order. The
     * default is the flat {@link MenuConfiguration#getMenuEntries()}; a module
     * whose menu nests routes can override this to keep the deeper entries out
     * of the flat nav.
     */
    protected List<MenuEntry> mainNavEntries() {
        return MenuConfiguration.getMenuEntries();
    }

    /**
     * The {@link UseCaseDescription} of a menu entry's view, or {@code null} if
     * the entry has no view class or the view has no description.
     */
    public static @Nullable UseCaseDescription descriptionOf(MenuEntry entry) {
        return entry.menuClass() == null ? null
                : entry.menuClass().getAnnotation(UseCaseDescription.class);
    }

    private static Element buildSourceCodeOverlay(Anchor link,
            Button feedbackButton) {
        Div container = new Div();
        container.getStyle().set("position", "fixed").set("top",
                "calc(var(--vaadin-app-layout-navbar-offset-top) + 0.5em)")
                .set("right", "calc(var(--aura-app-layout-inset, 0px) + 1em)")
                .set("z-index", "100").set("pointer-events", "auto");
        container.addClassName("use-case-overlay");

        Icon codeIcon = overlayIcon(VaadinIcon.CODE);

        link.setTarget("_blank");
        styleOverlayItem(link);

        Span content = new Span(codeIcon);
        content.add("View source");
        link.removeAll();
        link.add(content);

        styleOverlayItem(feedbackButton);
        feedbackButton.getStyle().set("border", "none").set("cursor",
                "pointer");

        container.add(feedbackButton, link);
        return container.getElement();
    }

    private static Icon overlayIcon(VaadinIcon vaadinIcon) {
        Icon icon = vaadinIcon.create();
        icon.setSize("16px");
        icon.getStyle().set("color", "var(--vaadin-text-color-secondary)")
                .set("margin-right", "0.5em");
        return icon;
    }

    private static <T extends Component & HasStyle> void styleOverlayItem(
            T item) {
        item.getStyle().set("display", "inline-flex")
                .set("align-items", "center")
                .set("background-color", "rgba(255, 255, 255, 0.95)")
                .set("padding", "0.5em 0.75em").set("border-radius", "4px")
                .set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.1)")
                .set("color", "var(--aura-accent-text-color)")
                .set("text-decoration", "none")
                .set("font-size", "var(--aura-font-size-s)")
                .set("transition", "box-shadow 0.2s");

        item.getElement()
                .addEventListener("mouseenter",
                        e -> item.getStyle().set("box-shadow",
                                "0 4px 8px rgba(0, 0, 0, 0.15)"))
                .addEventData("event.preventDefault");
        item.getElement()
                .addEventListener("mouseleave",
                        e -> item.getStyle().set("box-shadow",
                                "0 2px 4px rgba(0, 0, 0, 0.1)"))
                .addEventData("event.preventDefault");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Class<?> viewClass = event.getNavigationTarget();
        if (viewClass == null) {
            return;
        }
        String className = viewClass.getSimpleName();
        sourceCodeLink.setHref(sourceUrl(SOURCE_REF, moduleId, viewClass));
        boolean isUseCase = className.endsWith("View");
        sourceCodeLink.setVisible(isUseCase);

        useCase = isUseCase
                ? new UseCase(title, useCaseTitle(viewClass),
                        viewClass.getName(),
                        appUrl() + event.getLocation()
                                .getPathWithQueryParameters())
                : null;
        feedbackButton.setVisible(useCase != null && slackFeedback.isEnabled());

        UnreleasedApi unreleasedApi = viewClass
                .getAnnotation(UnreleasedApi.class);
        if (unreleasedApi != null) {
            unreleasedApiLink.setHref(unreleasedApi.value());
            unreleasedApiLink.setText(pullRequestLabel(unreleasedApi.value()));
        }
        unreleasedApiBanner.setVisible(unreleasedApi != null);
    }

    /**
     * A short label for a GitHub pull request URL, such as
     * {@code vaadin/flow#26495}, or the URL itself when it is not one.
     */
    static String pullRequestLabel(String url) {
        Matcher matcher = PULL_REQUEST_URL.matcher(url);
        return matcher.matches() ? matcher.group(1) + "#" + matcher.group(2)
                : url;
    }

    /**
     * The GitHub URL of the given view's source at the given Git commit or
     * branch, or at {@code main} when there is none, so that a pull request
     * preview links to the code it runs rather than to {@code main}, where a
     * view the pull request adds or changes is missing or different.
     */
    static String sourceUrl(@Nullable String ref, String moduleId,
            Class<?> viewClass) {
        String sourceRef = ref == null || ref.isBlank() ? "main" : ref;
        return "https://github.com/vaadin/use-cases/tree/" + sourceRef + "/"
                + moduleId + "/src/main/java/"
                + viewClass.getName().replace('.', '/') + ".java";
    }

    private static String useCaseTitle(Class<?> viewClass) {
        PageTitle pageTitle = viewClass.getAnnotation(PageTitle.class);
        if (pageTitle != null) {
            return pageTitle.value();
        }
        Menu menu = viewClass.getAnnotation(Menu.class);
        return menu != null && !menu.title().isEmpty() ? menu.title()
                : viewClass.getSimpleName();
    }

    private String appUrl() {
        String catalogUrl = AppCatalog.APPS.stream()
                .filter(app -> app.id().equals(moduleId))
                .map(AppCatalog.App::url).findFirst().orElse("/");
        return appUrl(catalogUrl,
                Optional.ofNullable(VaadinRequest.getCurrent())
                        .flatMap(OpenGraphTags::baseUrl));
    }

    /**
     * The URL feedback links to: the one the app is served from when that is a
     * pull request preview of it, such as
     * {@code https://signals-cases-pr-12.fly.dev/} for
     * {@code https://signals-cases.fly.dev/}, so the feedback points to the
     * preview, or else the app's {@link AppCatalog} URL. The served URL comes
     * from request headers the client controls, so any other host is ignored
     * rather than ending up as a link in the feedback.
     */
    static String appUrl(String catalogUrl, Optional<String> servedFrom) {
        int dot = catalogUrl.indexOf('.');
        if (dot < 0) {
            return catalogUrl;
        }
        String preview = Pattern.quote(catalogUrl.substring(0, dot))
                + "-pr-\\d+" + Pattern.quote(catalogUrl.substring(dot));
        return servedFrom.filter(url -> url.matches(preview))
                .orElse(catalogUrl);
    }
}
