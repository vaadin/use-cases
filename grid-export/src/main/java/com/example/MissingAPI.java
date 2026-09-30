package com.example;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.ColumnPathRenderer;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.data.provider.DataCommunicator;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.ValueProvider;

/**
 * The parts of a Grid data export that Flow has no API for today, implemented
 * here so the use cases in this module can be built at all.
 * <p>
 * Everything in this class is a stand-in for
 * <a href="https://github.com/vaadin/platform/issues/7196">vaadin/platform
 * #7196 — Grid data export API</a>. When that API lands, each method below
 * should collapse into a call on the real facade; {@code API-GAPS.md} records
 * what each one is standing in for and how to migrate.
 */
public final class MissingAPI {

    /**
     * {@code ColumnPathRenderer.provider} — the {@link ValueProvider} behind
     * the most common column of all, {@code grid.addColumn(Employee::name)}.
     * The renderer keeps it in a private field and exposes nothing, so the
     * plain-text value of such a column is only reachable reflectively.
     */
    private static final @Nullable Field COLUMN_PATH_PROVIDER = columnPathProviderField();

    private MissingAPI() {
    }

    /**
     * The grid's rows, in the order and the selection the user currently sees:
     * the active filter applied, the active sorting applied.
     * <p>
     * Stands in for a missing {@code Grid#getItems()}. Flow does have
     * {@code getListDataView().getItems()}, but only for in-memory data; this
     * works for any data provider because it asks the {@link DataCommunicator}
     * for the same {@link Query} the grid itself would send.
     */
    public static <T> List<T> rowsInViewOrder(Grid<T> grid) {
        return fetchPage(grid, 0, Integer.MAX_VALUE);
    }

    /**
     * The same rows, fetched lazily {@code pageSize} at a time, so a report
     * over a large backend data set never holds more than one page in memory.
     * <p>
     * Stands in for a missing paged/streaming export entry point.
     */
    public static <T> Stream<T> rowsInViewOrder(Grid<T> grid, int pageSize) {
        if (pageSize < 1) {
            throw new IllegalArgumentException(
                    "pageSize must be at least 1, was " + pageSize);
        }
        Iterator<T> paged = new Iterator<>() {

            private int offset;
            private boolean exhausted;
            private Iterator<T> page = Collections.emptyIterator();

            @Override
            public boolean hasNext() {
                while (!page.hasNext() && !exhausted) {
                    List<T> fetched = fetchPage(grid, offset, pageSize);
                    offset += pageSize;
                    exhausted = fetched.size() < pageSize;
                    page = fetched.iterator();
                }
                return page.hasNext();
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return page.next();
            }
        };
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(paged, Spliterator.ORDERED),
                false);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <T> List<T> fetchPage(Grid<T> grid, int offset, int limit) {
        DataProvider dataProvider = grid.getDataProvider();
        Query query = grid.getDataCommunicator().buildQuery(offset, limit);
        return ((Stream<T>) dataProvider.fetch(query)).toList();
    }

    /**
     * The plain text a column shows for an item — what
     * {@code Column#getCellContent(item)} would return if it existed.
     *
     * @return the cell's text, or {@code null} when the renderer keeps it out
     *         of reach and the caller has to supply an extractor instead
     */
    public static <T> @Nullable String cellText(Grid.Column<T> column, T item) {
        Renderer<T> renderer = column.getRenderer();
        if (renderer == null) {
            return "";
        }
        // Order matters: BasicRenderer extends ComponentRenderer extends
        // LitRenderer, so the checks run most specific first.
        if (renderer instanceof ColumnPathRenderer) {
            Object value = columnPathValue(renderer, item);
            return value == null ? "" : String.valueOf(value);
        }
        if (renderer instanceof ComponentRenderer<?, ?> componentRenderer) {
            @SuppressWarnings("unchecked")
            ComponentRenderer<?, T> typed = (ComponentRenderer<?, T>) componentRenderer;
            // Covers NumberRenderer / LocalDateRenderer / TextRenderer too:
            // BasicRenderer#createComponent wraps the formatted value in a
            // Span, so a whole component is built just to read a string.
            return componentText(typed.createComponent(item));
        }
        if (renderer instanceof LitRenderer<?> litRenderer) {
            @SuppressWarnings("unchecked")
            Map<String, ValueProvider<T, ?>> providers = ((LitRenderer<T>) litRenderer)
                    .getValueProviders();
            if (providers.size() == 1) {
                Object value = providers.values().iterator().next().apply(item);
                return value == null ? "" : String.valueOf(value);
            }
            // The template string is protected and the properties are
            // unordered, so which of them the cell actually shows is unknowable
            // from here.
            return null;
        }
        return null;
    }

    /**
     * A plain-text alternative for a rendered component, following the defaults
     * suggested in vaadin/platform#7196: a checkbox becomes "Yes"/"No", an icon
     * its icon name, anything else its text content.
     * <p>
     * Stands in for the "text alternative" a component renderer has no way to
     * declare.
     */
    public static String componentText(@Nullable Component component) {
        if (component == null) {
            return "";
        }
        if (component instanceof Checkbox checkbox) {
            return Boolean.TRUE.equals(checkbox.getValue()) ? "Yes" : "No";
        }
        if (component instanceof Icon icon) {
            String name = icon.getIcon();
            return name == null ? "" : name;
        }
        if (component instanceof Avatar avatar) {
            String name = avatar.getName();
            return name == null ? "" : name;
        }
        if (component instanceof Anchor anchor) {
            String text = collapse(anchor.getElement().getTextRecursively());
            return text.isEmpty() ? anchor.getHref() : text;
        }
        if (component instanceof HasValue<?, ?> field) {
            Object value = field.getValue();
            return value == null ? "" : String.valueOf(value);
        }
        return collapse(elementText(component.getElement()));
    }

    /**
     * The column's header as text, falling back to the text of a header
     * component. {@code Column#getHeaderText()} returns {@code null} when the
     * header was set as a component, which is the normal case for a sortable or
     * filterable header.
     */
    public static String headerText(Grid.Column<?> column) {
        String text = column.getHeaderText();
        if (text != null && !text.isBlank()) {
            return text;
        }
        return componentText(column.getHeaderComponent());
    }

    /**
     * The column's footer as text, with the same component fallback as
     * {@link #headerText(Grid.Column)}.
     */
    public static String footerText(Grid.Column<?> column) {
        String text = column.getFooterText();
        if (text != null && !text.isBlank()) {
            return text;
        }
        return componentText(column.getFooterComponent());
    }

    /**
     * Walks an element tree, joining the text of every node with a space, so
     * that a composite cell ("value + badge + button") exports as readable text
     * instead of one run-together word.
     */
    private static String elementText(Element element) {
        List<String> parts = new ArrayList<>();
        collectText(element, parts);
        return String.join(" ", parts);
    }

    private static void collectText(Element element, List<String> parts) {
        String own = element.getText();
        if (own != null && !own.isBlank()) {
            parts.add(own.strip());
        }
        // getText() already returns the direct text nodes, so recursing into
        // them would emit every string twice.
        element.getChildren().filter(child -> !child.isTextNode())
                .forEach(child -> collectText(child, parts));
    }

    private static String collapse(@Nullable String text) {
        return text == null ? "" : text.strip().replaceAll("\\s+", " ");
    }

    private static @Nullable Object columnPathValue(Renderer<?> renderer,
            Object item) {
        Field field = COLUMN_PATH_PROVIDER;
        if (field == null) {
            throw new IllegalStateException(
                    "ColumnPathRenderer no longer keeps its ValueProvider in a "
                            + "'provider' field — see grid-export/API-GAPS.md");
        }
        try {
            @SuppressWarnings("unchecked")
            ValueProvider<Object, ?> provider = (ValueProvider<Object, ?>) field
                    .get(renderer);
            return provider == null ? null : provider.apply(item);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Failed to read ColumnPathRenderer#provider reflectively",
                    e);
        }
    }

    private static @Nullable Field columnPathProviderField() {
        try {
            Field field = ColumnPathRenderer.class.getDeclaredField("provider");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException | SecurityException e) {
            return null;
        }
    }
}
