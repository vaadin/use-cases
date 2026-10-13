package com.example.uc6;

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

import com.vaadin.flow.component.datepicker.DateRange;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC6 — One field for the range.
 * <p>
 * In a narrow toolbar, or on a phone, two date inputs side by side do not fit.
 * With {@code setSingleInput(true)} the range is one text input, such as
 * "10/1/2026 – 10/14/2026". Users can type it too: the picker accepts an en
 * dash, an em dash, a hyphen with spaces or the word "to" between the dates.
 * The calendar then always picks the whole range.
 */
@Route(value = "uc6", layout = MainLayout.class)
@PageTitle("UC6 — One field for the range")
@UseCaseDescription("Fitting a range into one input in a narrow toolbar")
@Menu(order = 6, title = "UC6 — One field for the range")
@StyleSheet("uc6.css")
public class OneFieldView extends VerticalLayout {

    private static final DateTimeFormatter DAY = DateTimeFormatter
            .ofPattern("d MMM", Locale.ENGLISH);

    private final List<Order> orders;
    private final TextField search = new TextField();
    private final DateRangePicker period = new DateRangePicker();
    private final Span count = new Span();
    private final UnorderedList list = new UnorderedList();

    public OneFieldView() {
        this(LocalDate.now());
    }

    OneFieldView(LocalDate today) {
        orders = Order.sample(today);
        addClassName("uc6-view");
        add(new H1("UC6 — One field for the range"));
        add(new Paragraph("The toolbar below is as narrow as a phone. Type a "
                + "range into the period field, for example \""
                + today.minusDays(14).getMonthValue() + "/"
                + today.minusDays(14).getDayOfMonth() + "/"
                + today.minusDays(14).getYear() + " to " + today.getMonthValue()
                + "/" + today.getDayOfMonth() + "/" + today.getYear()
                + "\", or pick one from the calendar."));

        search.setPlaceholder("Customer");
        search.setClearButtonVisible(true);
        search.setValueChangeMode(ValueChangeMode.EAGER);
        search.addValueChangeListener(event -> filter());
        period.setSingleInput(true);
        // The single input joins both placeholders with a dash; it has no
        // placeholder of its own (API-GAPS.md #13).
        period.setStartPlaceholder("Start date");
        period.setEndPlaceholder("end date");
        period.setClearButtonVisible(true);
        period.setMax(today);
        period.addValueChangeListener(event -> filter());

        Div toolbar = new Div(search, period);
        toolbar.addClassName("uc6-toolbar");
        count.addClassName("summary");
        Div phone = new Div(toolbar, count, list);
        phone.addClassName("uc6-phone");
        add(phone);
        add(ApiGap.note(13, "the single input has no placeholder of its "
                + "own, so it can not say \"Any time\"; it shows the two "
                + "placeholders joined with a dash."));
        filter();
    }

    private void filter() {
        @Nullable
        DateRange range = period.getValue();
        String customer = search.getValue().trim().toLowerCase(Locale.ROOT);
        List<Order> shown = orders.stream().filter(
                order -> range == null || !order.date().isBefore(range.start())
                        && !order.date().isAfter(range.end()))
                .filter(order -> order.customer().toLowerCase(Locale.ROOT)
                        .contains(customer))
                .toList();
        count.setText(
                shown.size() + (shown.size() == 1 ? " order" : " orders"));
        list.removeAll();
        shown.forEach(order -> list.add(new ListItem(DAY.format(order.date())
                + " · " + order.customer() + " · " + order.total() + " EUR")));
    }
}
