package com.example.uc2;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import com.example.common.UseCaseDescription;
import com.example.data.Order;
import com.example.daterangepicker.DateRangePicker;
import com.example.views.ApiGap;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DateRange;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC2 — Filter by period.
 * <p>
 * The order list is filtered to the picked period, with buttons for the periods
 * people ask for most ("Last 7 days", "This month"). The picker has no place
 * for such presets, so they are buttons next to it that set the value.
 * <p>
 * A filter often only needs one end: "since 1 September". The picker allows
 * that (pick a start and press Escape, or clear the end), but Flow's
 * {@code DateRange} can not hold a range with one end, so the value is
 * {@code null}, and picking only a start on an empty filter fires no value
 * change at all. The view therefore also listens to the change event, and reads
 * {@code getStart()} and {@code getEnd()} instead of the value, so an open end
 * still filters.
 */
@Route(value = "uc2", layout = MainLayout.class)
@PageTitle("UC2 — Filter by period")
@UseCaseDescription("Filtering a list to a period, with quick buttons for common periods")
@Menu(order = 2, title = "UC2 — Filter by period")
public class FilterOrdersView extends VerticalLayout {

    private static final DateTimeFormatter DAY = DateTimeFormatter
            .ofPattern("d MMM yyyy", Locale.ENGLISH);

    private final LocalDate today;
    private final List<Order> orders;
    private final DateRangePicker period = new DateRangePicker("Period");
    private final Grid<Order> grid = new Grid<>();
    private final Span summary = new Span();

    public FilterOrdersView() {
        this(LocalDate.now());
    }

    FilterOrdersView(LocalDate today) {
        this.today = today;
        this.orders = Order.sample(today);
        add(new H1("UC2 — Filter by period"));
        add(new Paragraph("Pick a period, or use a quick button. To filter "
                + "from a date onwards, pick only a start date and press "
                + "Escape, or clear the end date."));

        period.setMax(today);
        period.setClearButtonVisible(true);
        period.setWidth("22rem");
        // The quick buttons set the value; the user's own edits may leave it
        // null (a range with one end), so they are followed with the change
        // event instead.
        period.addValueChangeListener(event -> filter());
        period.addChangeListener(event -> filter());

        HorizontalLayout presets = new HorizontalLayout(
                preset("Today", today, today),
                preset("Last 7 days", today.minusDays(6), today),
                preset("This month", today.withDayOfMonth(1), today),
                preset("Last month", today.minusMonths(1).withDayOfMonth(1),
                        today.withDayOfMonth(1).minusDays(1)),
                preset("Last 90 days", today.minusDays(89), today));
        presets.setWrap(true);
        HorizontalLayout filters = new HorizontalLayout(period, presets);
        filters.setAlignItems(Alignment.END);
        filters.setWrap(true);

        grid.addColumn(Order::number).setHeader("Order");
        grid.addColumn(order -> DAY.format(order.date())).setHeader("Date");
        grid.addColumn(Order::customer).setHeader("Customer");
        grid.addColumn(order -> order.total() + " EUR").setHeader("Total");
        summary.addClassName("summary");

        add(filters, summary, grid);
        add(ApiGap.note(7, "the picker has no presets; the quick buttons are "
                + "the app's own."));
        add(ApiGap.note(3, "a period with one end has no DateRange value, so "
                + "the filter reads the start and the end separately."));
        filter();
    }

    private Button preset(String label, LocalDate start, LocalDate end) {
        Button button = new Button(label,
                event -> period.setValue(new DateRange(start, end)));
        button.addThemeVariants(ButtonVariant.TERTIARY);
        return button;
    }

    private void filter() {
        @Nullable
        LocalDate start = period.getStart();
        @Nullable
        LocalDate end = period.getEnd();
        List<Order> shown = orders.stream()
                .filter(order -> start == null || !order.date().isBefore(start))
                .filter(order -> end == null || !order.date().isAfter(end))
                .toList();
        grid.setItems(shown);
        BigDecimal total = shown.stream().map(Order::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setText(describe(start, end) + ": " + shown.size()
                + (shown.size() == 1 ? " order, " : " orders, ") + total
                + " EUR");
    }

    static String describe(@Nullable LocalDate start, @Nullable LocalDate end) {
        if (start == null && end == null) {
            return "All orders";
        }
        if (end == null) {
            return "Since " + DAY.format(start);
        }
        if (start == null) {
            return "Until " + DAY.format(end);
        }
        return DAY.format(start) + " – " + DAY.format(end);
    }
}
