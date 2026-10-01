package com.example;

import java.io.Serializable;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.BrowserTab;
import com.vaadin.flow.server.VaadinSession;

/**
 * Temporary home for API that the use cases in this module need but that Vaadin
 * does not provide yet.
 * <p>
 * vaadin/flow#25901 adds {@link BrowserTab}, a programmatic store for state
 * that belongs to one browser tab and survives reloads and full page
 * navigation. It does not add a Spring scope, so this class provides one:
 * annotate a bean with {@link BrowserTabScope @BrowserTabScope} and inject it
 * like any {@code @UIScope} or {@code @VaadinSessionScope} bean.
 * <p>
 * When Vaadin ships an official {@code @BrowserTabScope} (expected next to the
 * existing ones in {@code com.vaadin.flow.spring.annotation}), replace the
 * import of {@link BrowserTabScope} and delete {@link BrowserTabScopeConfig}
 * and {@link VaadinBrowserTabScope}.
 */
public final class MissingAPI {

    /**
     * Spring scope name registered by {@link BrowserTabScopeConfig}.
     */
    public static final String BROWSER_TAB_SCOPE_NAME = "vaadin-browser-tab";

    private MissingAPI() {
    }

    /**
     * Scopes a Spring bean to the current {@link BrowserTab}.
     * <p>
     * All UIs opened in the same browser tab — including the new UI created by
     * a page reload or a full page navigation — get the same instance. A
     * different browser tab gets its own instance. The bean is destroyed
     * (including its {@code @PreDestroy} methods) when the browser tab is
     * destroyed: after the tab's last UI stops sending heartbeats, or when the
     * session ends.
     * <p>
     * The same caveats as for {@link BrowserTab} apply: a duplicated tab shares
     * the instance with the original, and coming back from an external site
     * starts a new tab.
     */
    @Scope(BROWSER_TAB_SCOPE_NAME)
    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface BrowserTabScope {
    }

    /**
     * Registers {@link VaadinBrowserTabScope} with Spring. Picked up by
     * component scanning; the bean method is static so that the scope is
     * registered before any other bean is created.
     */
    @Configuration(proxyBeanMethods = false)
    public static class BrowserTabScopeConfig {

        @Bean
        public static BeanFactoryPostProcessor vaadinBrowserTabScope() {
            return new VaadinBrowserTabScope();
        }
    }

    /**
     * Spring {@link org.springframework.beans.factory.config.Scope} that keeps
     * bean instances as an attribute of the current {@link BrowserTab},
     * modelled on Vaadin's own {@code VaadinUIScope}.
     */
    public static class VaadinBrowserTabScope
            implements org.springframework.beans.factory.config.Scope,
            BeanFactoryPostProcessor {

        @Override
        public void postProcessBeanFactory(
                ConfigurableListableBeanFactory beanFactory) {
            beanFactory.registerScope(BROWSER_TAB_SCOPE_NAME, this);
        }

        @Override
        public Object get(String name, ObjectFactory<?> objectFactory) {
            return withBeanStore(store -> {
                // Not computeIfAbsent: creating the bean may resolve other
                // browser-tab-scoped beans and modify the map re-entrantly.
                Object bean = store.beans.get(name);
                if (bean == null) {
                    bean = objectFactory.getObject();
                    store.beans.put(name, bean);
                }
                return bean;
            });
        }

        @Override
        public @Nullable Object remove(String name) {
            return withBeanStore(store -> {
                store.destructionCallbacks.remove(name);
                return store.beans.remove(name);
            });
        }

        @Override
        public void registerDestructionCallback(String name,
                Runnable callback) {
            withBeanStore(
                    store -> store.destructionCallbacks.put(name, callback));
        }

        @Override
        public @Nullable Object resolveContextualObject(String key) {
            return null;
        }

        @Override
        public String getConversationId() {
            return withBeanStore(store -> store.tabId);
        }

        private static <T> T withBeanStore(Function<TabBeanStore, T> action) {
            UI ui = UI.getCurrentOrThrow();
            VaadinSession session = ui.getSession();
            if (session == null) {
                throw new IllegalStateException(
                        "The current UI does not belong to a session");
            }
            session.lock();
            try {
                BrowserTab tab = BrowserTab.get(ui);
                TabBeanStore store = tab.getAttribute(TabBeanStore.class);
                if (store == null) {
                    store = new TabBeanStore(tab.getId());
                    tab.setAttribute(TabBeanStore.class, store);
                    tab.addDestroyListener(store::destroy);
                }
                return action.apply(store);
            } finally {
                session.unlock();
            }
        }
    }

    private static final class TabBeanStore implements Serializable {

        private static final Logger LOGGER = LoggerFactory
                .getLogger(TabBeanStore.class);

        private final String tabId;
        private final Map<String, Object> beans = new HashMap<>();
        private final Map<String, Runnable> destructionCallbacks = new LinkedHashMap<>();

        private TabBeanStore(String tabId) {
            this.tabId = tabId;
        }

        private void destroy() {
            List<Runnable> callbacks = new ArrayList<>(
                    destructionCallbacks.values());
            destructionCallbacks.clear();
            beans.clear();
            for (Runnable callback : callbacks) {
                try {
                    callback.run();
                } catch (RuntimeException e) {
                    LOGGER.error("Browser tab bean destruction callback failed",
                            e);
                }
            }
        }
    }
}
