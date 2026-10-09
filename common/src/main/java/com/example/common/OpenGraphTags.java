package com.example.common;

import java.util.Optional;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.communication.IndexHtmlResponse;

/**
 * Adds Open Graph and Twitter card tags to the page the server sends for a
 * route, so a link to a use case unfurls in chat apps and social media with its
 * title, its {@link UseCaseDescription} and the app's preview image.
 * <p>
 * Crawlers do not run JavaScript, so the tags go into the bootstrap HTML rather
 * than being set from a view. The title is the route's {@link PageTitle} (or
 * its {@link Menu} title), the site name is its layout's {@link PageTitle}, and
 * the image is the module's {@value #IMAGE}, served from
 * {@code src/main/resources/META-INF/resources}. Registered for every app
 * through {@code META-INF/services}.
 */
public class OpenGraphTags implements VaadinServiceInitListener {

    static final String IMAGE = "og-image.jpg";

    @Override
    public void serviceInit(ServiceInitEvent event) {
        boolean hasImage = event.getSource().getClassLoader()
                .getResource("META-INF/resources/" + IMAGE) != null;
        event.addIndexHtmlRequestListener(
                response -> addTags(response, hasImage));
    }

    private static void addTags(IndexHtmlResponse response, boolean hasImage) {
        VaadinRequest request = response.getVaadinRequest();
        String path = Optional.ofNullable(request.getPathInfo()).orElse("")
                .replaceFirst("^/", "");
        RouteConfiguration routes = RouteConfiguration.forApplicationScope();
        Optional<Class<? extends Component>> target = routes.getRoute(path);
        if (target.isEmpty()) {
            return;
        }
        String baseUrl = baseUrl(request);
        Document document = response.getDocument();

        Optional<String> siteName = siteName(target.get());

        property(document, "og:type", "website");
        property(document, "og:url", baseUrl + path);
        siteName.ifPresent(name -> property(document, "og:site_name", name));
        title(target.get()).or(() -> siteName).ifPresent(title -> {
            property(document, "og:title", title);
            name(document, "twitter:title", title);
        });
        UseCaseDescription description = target.get()
                .getAnnotation(UseCaseDescription.class);
        if (description != null) {
            name(document, "description", description.value());
            property(document, "og:description", description.value());
            name(document, "twitter:description", description.value());
        }
        if (hasImage) {
            property(document, "og:image", baseUrl + IMAGE);
            property(document, "og:image:width", "1200");
            property(document, "og:image:height", "630");
            name(document, "twitter:card", "summary_large_image");
            name(document, "twitter:image", baseUrl + IMAGE);
        }
    }

    /**
     * The view's own title. A home view without one falls back to the site
     * name, rather than to its "Home" menu title.
     */
    private static Optional<String> title(Class<?> view) {
        PageTitle pageTitle = view.getAnnotation(PageTitle.class);
        if (pageTitle != null) {
            return Optional.of(pageTitle.value());
        }
        Menu menu = view.getAnnotation(Menu.class);
        return menu == null || menu.title().isEmpty()
                || menu.title().equals("Home") ? Optional.empty()
                        : Optional.of(menu.title());
    }

    /** The {@link PageTitle} of the layout the view is shown in. */
    private static Optional<String> siteName(Class<?> view) {
        return Optional.ofNullable(view.getAnnotation(Route.class))
                .map(route -> route.layout().getAnnotation(PageTitle.class))
                .map(PageTitle::value);
    }

    /**
     * The app's absolute URL, ending with a slash. Crawlers need absolute URLs,
     * and behind a TLS-terminating proxy such as Fly.io the server only sees
     * plain HTTP, so the proxy's {@code X-Forwarded-Proto} wins.
     */
    private static String baseUrl(VaadinRequest request) {
        String scheme = Optional
                .ofNullable(request.getHeader("X-Forwarded-Proto"))
                .orElse(request.isSecure() ? "https" : "http");
        return scheme + "://" + request.getHeader("Host")
                + request.getContextPath() + "/";
    }

    private static void property(Document document, String property,
            String content) {
        meta(document).attr("property", property).attr("content", content);
    }

    private static void name(Document document, String name, String content) {
        meta(document).attr("name", name).attr("content", content);
    }

    private static Element meta(Document document) {
        return document.head().appendElement("meta");
    }
}
