package com.example.common;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServiceInitListener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenGraphTagsTest {

    private static final Optional<String> BASE_URL = Optional
            .of("https://example.org/");

    @PageTitle("Demo Use Cases")
    static class AppLayout extends Div implements RouterLayout {
    }

    @ParentLayout(AppLayout.class)
    static class NestedLayout extends Div implements RouterLayout {
    }

    @Route(value = "", layout = AppLayout.class)
    @UseCaseDescription("What the whole app is about")
    @Menu(title = "Home")
    static class HomeView extends Div {
    }

    @Route(value = "uc1", layout = AppLayout.class)
    @PageTitle("UC1 — Page title")
    @UseCaseDescription("Solving the first problem")
    @Menu(title = "UC1 — Menu title")
    static class PageTitleView extends Div {
    }

    @Route(value = "uc2", layout = NestedLayout.class)
    @Menu(title = "UC2 — Menu title")
    static class NestedView extends Div {
    }

    @Test
    void useCaseUnfurlsWithItsTitleDescriptionAndImage() {
        Document document = tags("uc1", PageTitleView.class, BASE_URL, true);

        assertEquals("https://example.org/uc1", property(document, "og:url"));
        assertEquals("Demo Use Cases", property(document, "og:site_name"));
        assertEquals("UC1 — Page title", property(document, "og:title"));
        assertEquals("Solving the first problem",
                property(document, "og:description"));
        assertEquals("Solving the first problem",
                name(document, "description"));
        assertEquals("https://example.org/og-image.jpg",
                property(document, "og:image"));
        assertEquals("summary_large_image", name(document, "twitter:card"));
    }

    @Test
    void viewInANestedLayoutUsesTheMenuTitleAndTheOuterLayoutsSiteName() {
        Document document = tags("uc2", NestedView.class, BASE_URL, true);

        assertEquals("Demo Use Cases", property(document, "og:site_name"));
        assertEquals("UC2 — Menu title", property(document, "og:title"));
        assertNull(property(document, "og:description"));
    }

    @Test
    void homeViewIsTitledWithTheSiteNameNotHome() {
        Document document = tags("", HomeView.class, BASE_URL, true);

        assertEquals("Demo Use Cases", property(document, "og:title"));
        assertEquals("What the whole app is about",
                property(document, "og:description"));
        assertEquals("https://example.org/", property(document, "og:url"));
    }

    @Test
    void noImageTagsWithoutAnImageOrAHost() {
        assertNull(property(tags("uc1", PageTitleView.class, BASE_URL, false),
                "og:image"));

        Document noHost = tags("uc1", PageTitleView.class, Optional.empty(),
                true);
        assertNull(property(noHost, "og:image"));
        assertNull(property(noHost, "og:url"));
        assertEquals("UC1 — Page title", property(noHost, "og:title"));
    }

    @Test
    void baseUrlPrefersTheFirstForwardedProtocolOverTheConnection() {
        assertEquals(Optional.of("https://example.org/app/"),
                OpenGraphTags.baseUrl(request(Map.of("Host", "example.org",
                        "X-Forwarded-Proto", "https, http"), false, "/app")));
        assertEquals(Optional.of("https://example.org/"), OpenGraphTags
                .baseUrl(request(Map.of("Host", "example.org"), true, "")));
        assertEquals(Optional.of("http://localhost:8080/"), OpenGraphTags
                .baseUrl(request(Map.of("Host", "localhost:8080"), false, "")));
    }

    @Test
    void baseUrlIsEmptyWithoutAHostHeader() {
        assertEquals(Optional.empty(),
                OpenGraphTags.baseUrl(request(Map.of(), true, "")));
    }

    @Test
    void isRegisteredAsAServiceInitListener() {
        assertTrue(ServiceLoader.load(VaadinServiceInitListener.class).stream()
                .anyMatch(provider -> provider.type() == OpenGraphTags.class));
    }

    private static Document tags(String path, Class<?> view,
            Optional<String> baseUrl, boolean hasImage) {
        Document document = Jsoup.parse("<html><head></head></html>");
        OpenGraphTags.addTags(document, path, view, baseUrl, hasImage);
        return document;
    }

    private static String property(Document document, String property) {
        return content(
                document.selectFirst("meta[property=\"" + property + "\"]"));
    }

    private static String name(Document document, String name) {
        return content(document.selectFirst("meta[name=\"" + name + "\"]"));
    }

    private static String content(Element meta) {
        return meta == null ? null : meta.attr("content");
    }

    private static VaadinRequest request(Map<String, String> headers,
            boolean secure, String contextPath) {
        return (VaadinRequest) Proxy.newProxyInstance(
                VaadinRequest.class.getClassLoader(),
                new Class<?>[] { VaadinRequest.class },
                (proxy, method, args) -> switch (method.getName()) {
                case "getHeader" -> headers.get((String) args[0]);
                case "isSecure" -> secure;
                case "getContextPath" -> contextPath;
                default ->
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}
