package com.example.home;

import java.util.List;

import com.example.common.BaseHomeView;
import com.example.common.UseCaseDescription;
import com.example.uc1.BookAStayView;
import com.example.uc2.FilterOrdersView;
import com.example.uc3.RequestTimeOffView;
import com.example.uc4.ChangeReturnDateView;
import com.example.uc5.DatesInYourFormatView;
import com.example.uc6.OneFieldView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@UseCaseDescription("Picking a start and an end date in one field")
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    private static final String API_GAPS_URL = "https://github.com/vaadin/use-cases/blob/main/date-range-picker/API-GAPS.md";

    public HomeView() {
        super("Date Range Picker — use cases",
                "Stays, rentals, time off and report filters all need a "
                        + "start and an end date. Each card below is one such "
                        + "task, built on the prototype "
                        + "<vaadin-date-range-picker> web component.");
        add(new Paragraph(new Text("There is no Flow component for the picker "
                + "yet, so this app wraps the web component itself. Where the "
                + "API falls short, the use case says so and links to "),
                new Anchor(API_GAPS_URL, "API-GAPS.md"), new Text(".")));

        group("Book and request", List.of(BookAStayView.class,
                RequestTimeOffView.class, ChangeReturnDateView.class));
        group("Filter", List.of(FilterOrdersView.class, OneFieldView.class));
        group("Fit the user", List.of(DatesInYourFormatView.class));
    }

    private void group(String heading, List<Class<? extends Component>> views) {
        add(new H2(heading), menuCards(views));
    }
}
