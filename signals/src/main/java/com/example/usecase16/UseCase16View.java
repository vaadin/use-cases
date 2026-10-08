package com.example.usecase16;

import java.util.List;
import java.util.Map;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Use Case 16: Search with URL State (Router Integration)
 * <p>
 * Demonstrates deep linking and URL state management: - Query parameters as
 * signals (two-way binding) - Update URL when signal changes - Load signal from
 * URL on navigation - Back button support (browser history) - Shareable URLs
 * with search state
 * <p>
 * Key Patterns: - Router integration with signals - Query parameter binding -
 * URL state synchronization - Deep linking support
 */
@Route(value = "use-case-16", layout = MainLayout.class)
@PageTitle("Use Case 16: URL State Integration")
@Menu(order = 16, title = "UC16 — URL State Integration")
@UseCaseDescription("Keeping search filters in the URL to share and go back")
@StyleSheet("usecase16.css")
@AnonymousAllowed
public class UseCase16View extends VerticalLayout
        implements BeforeEnterObserver {

    record Article(String id, String title, String category, String content) {

        boolean matches(String query, String category) {
            boolean categoryMatch = category.equals("All")
                    || this.category.equals(category);
            if (query.isEmpty()) {
                return categoryMatch;
            }
            String lowerQuery = query.toLowerCase();
            return categoryMatch && (title.toLowerCase().contains(lowerQuery)
                    || content.toLowerCase().contains(lowerQuery));
        }
    }

    private static final List<Article> ALL_ARTICLES = List.of(
            new Article("1", "Getting Started with Signals", "Tutorial",
                    "Learn the basics of reactive signals..."),
            new Article("2", "Advanced Signal Patterns", "Tutorial",
                    "Deep dive into computed signals and effects..."),
            new Article("3", "Building Reactive UIs", "Guide",
                    "Create dynamic user interfaces with signals..."),
            new Article("4", "Signal API Reference", "Documentation",
                    "Complete API documentation for signals..."),
            new Article("5", "Performance Optimization", "Guide",
                    "Best practices for signal performance..."),
            new Article("6", "Common Pitfalls", "Tutorial",
                    "Avoid these common mistakes when using signals..."),
            new Article("7", "Integration with Binder", "Documentation",
                    "How to use signals with Vaadin Binder..."),
            new Article("8", "Multi-User Signals", "Guide",
                    "Implementing collaborative features..."),
            new Article("9", "Testing Signal-Based Code", "Tutorial",
                    "Unit testing patterns for signals..."),
            new Article("10", "Migration Guide", "Documentation",
                    "Migrating from traditional state to signals..."));

    private final ValueSignal<String> searchQuerySignal = new ValueSignal<>("");
    private final ValueSignal<String> categorySignal = new ValueSignal<>("All");
    private final ListSignal<Article> filteredArticlesSignal = new ListSignal<>();

    private boolean isInitializing = true;

    public UseCase16View() {
        addClassName("usecase16-view");
        setSpacing(true);
        setPadding(true);

        H2 title = new H2("Use Case 16: Search with URL State Integration");

        Paragraph description = new Paragraph(
                "This use case demonstrates router integration where search filters are synchronized with URL query parameters. "
                        + "Try searching or changing the category - the URL updates automatically. "
                        + "Share the URL or use the back button to see state restoration in action. "
                        + "This enables deep linking: users can bookmark or share specific search results.");

        // Search controls
        HorizontalLayout controls = new HorizontalLayout();
        controls.setSpacing(true);
        controls.setWidthFull();

        TextField searchField = new TextField("Search");
        searchField.setPlaceholder("Search articles...");
        searchField.setWidth("300px");
        searchField.setClearButtonVisible(true);
        searchField.bindValue(searchQuerySignal, searchQuerySignal::set);

        Select<String> categorySelect = new Select<>();
        categorySelect.setLabel("Category");
        categorySelect.setItems("All", "Tutorial", "Guide", "Documentation");
        categorySelect.setWidth("200px");
        categorySelect.bindValue(categorySignal, categorySignal::set);

        controls.add(searchField, categorySelect);

        // Current URL display
        Div urlBox = new Div();
        urlBox.addClassName("url-box");

        H3 urlTitle = new H3("Current URL");
        urlTitle.addClassName("url-title");

        // Update URL display based on signals
        Signal<String> currentUrlSignal = Signal.computed(() -> {
            String baseUrl = getBaseUrl();
            String query = searchQuerySignal.get();
            String category = categorySignal.get();

            return buildUrl(baseUrl, query, category);
        });

        Paragraph urlDisplay = new Paragraph(currentUrlSignal);
        urlDisplay.addClassName("url-display");

        urlBox.add(urlTitle, urlDisplay);

        // Shareable links section
        H3 shareTitle = new H3("Try These Shareable Links");

        HorizontalLayout shareLinks = new HorizontalLayout();
        shareLinks.setSpacing(true);

        Anchor link1 = new Anchor("use-case-16?q=signal", "Search: 'signal'");
        link1.getElement().setAttribute("router-link", "");

        Anchor link2 = new Anchor("use-case-16?category=Tutorial",
                "Category: Tutorial");
        link2.getElement().setAttribute("router-link", "");

        Anchor link3 = new Anchor("use-case-16?q=reactive&category=Guide",
                "Search: 'reactive' in Guide");
        link3.getElement().setAttribute("router-link", "");

        shareLinks.add(link1, link2, link3);

        // Results
        H3 resultsTitle = new H3(() -> {
            int size = filteredArticlesSignal.get().size();
            return size + " article" + (size == 1 ? "" : "s") + " found";
        });

        Div resultsContainer = new Div();
        resultsContainer.addClassName("results-container");

        resultsContainer.bindChildren(filteredArticlesSignal,
                this::createArticleCard);

        // Info box
        Div infoBox = new Div();
        infoBox.addClassName("info-box");
        infoBox.add(new Paragraph(
                "💡 Router integration with signals enables deep linking and shareable URLs. "
                        + "This pattern is essential for SEO, bookmarking, and sharing specific app states. "
                        + "The URL automatically updates as you interact with the UI, and the browser back button works naturally. "
                        + "In production, this would integrate with Vaadin Router's @QueryParameters or similar API. "
                        + "Try clicking the shareable links above to see the state restored from URL."));

        add(title, description, controls, urlBox, shareTitle, shareLinks,
                resultsTitle, resultsContainer, infoBox);

        // Subscribe to signals to update URL and filter results
        setupSignalSubscriptions();
    }

    private Div createArticleCard(ValueSignal<Article> articleSignal) {
        // Articles are read-only so no need to create bindings
        var article = articleSignal.peek();
        Div card = new Div();
        card.addClassName("article-card");

        Div headerDiv = new Div();
        headerDiv.addClassName("article-header");

        Div titleDiv = new Div(article.title());
        titleDiv.addClassName("article-title");

        Div categoryBadge = new Div(article.category());
        categoryBadge.addClassName("category-badge");

        headerDiv.add(titleDiv, categoryBadge);

        Div contentDiv = new Div(article.content());
        contentDiv.addClassName("article-content");

        card.add(headerDiv, contentDiv);
        return card;
    }

    private void setupSignalSubscriptions() {
        Signal.effect(this, () -> {
            String query = searchQuerySignal.get();
            String category = categorySignal.get();

            if (!isInitializing) {
                updateUrl(query, category);
            }

            filteredArticlesSignal.clear();
            ALL_ARTICLES.stream()
                    .filter(article -> article.matches(query, category))
                    .forEach(filteredArticlesSignal::insertLast);
        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Extract query parameters from URL
        QueryParameters queryParameters = event.getLocation()
                .getQueryParameters();
        Map<String, List<String>> params = queryParameters.getParameters();

        isInitializing = true;

        // Load search query from URL
        if (params.containsKey("q")) {
            String query = params.get("q").getFirst();
            searchQuerySignal.set(query);
        } else {
            searchQuerySignal.set("");
        }

        // Load category from URL
        if (params.containsKey("category")) {
            String category = params.get("category").getFirst();
            categorySignal.set(category);
        } else {
            categorySignal.set("All");
        }

        isInitializing = false;
    }

    private String buildUrl(String baseUrl, String query, String category) {
        StringBuilder url = new StringBuilder(baseUrl);
        if (!query.isEmpty() || !category.equals("All")) {
            url.append("?");
            if (!query.isEmpty()) {
                url.append("q=").append(query);
            }
            if (!category.equals("All")) {
                if (!query.isEmpty())
                    url.append("&");
                url.append("category=").append(category);
            }
        }
        return url.toString();
    }

    private void updateUrl(String query, String category) {
        String url = buildUrl("use-case-16", query, category);

        // Update browser URL without triggering navigation
        UI.getCurrent().getPage().getHistory().replaceState(null, url);
    }

    private String getBaseUrl() {
        VaadinServletRequest request = (VaadinServletRequest) VaadinRequest
                .getCurrent();
        if (request != null) {
            String scheme = request.getScheme();
            String serverName = request.getServerName();
            int serverPort = request.getServerPort();
            String contextPath = request.getContextPath();

            StringBuilder url = new StringBuilder();
            url.append(scheme).append("://").append(serverName);
            if ((scheme.equals("http") && serverPort != 80)
                    || (scheme.equals("https") && serverPort != 443)) {
                url.append(":").append(serverPort);
            }
            url.append(contextPath).append("/use-case-16");
            return url.toString();
        }
        return "http://localhost:8080/use-case-16";
    }
}
