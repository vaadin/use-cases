package com.example.home;

import java.util.List;

import com.example.common.BaseHomeView;
import com.example.common.UseCaseDescription;
import com.example.uc1.QuoteView;
import com.example.uc10.AccessibleFormView;
import com.example.uc11.LoadTestView;
import com.example.uc2.NewOrderView;
import com.example.uc3.RegistrationView;
import com.example.uc4.OrdersView;
import com.example.uc5.SecurityView;
import com.example.uc6.ReportView;
import com.example.uc7.BrowserApisView;
import com.example.uc8.EndToEndView;
import com.example.uc9.InvoiceView;
import com.example.views.MainLayout;

import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Testing Use Cases")
@UseCaseDescription("Testing a Vaadin application at every level, from unit tests to load tests")
@Menu(order = 0, title = "Home")
@AnonymousAllowed
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Testing — use cases",
                "Each card below is a small part of an order desk and the "
                        + "tests that cover it, from the cheapest layer to "
                        + "the most expensive: plain unit tests, view tests "
                        + "without a browser, forms, grids, security, "
                        + "background work and browser APIs, then "
                        + "end-to-end, screenshot and accessibility tests in "
                        + "a real browser, and a load test recorded from an "
                        + "end-to-end test. The tests are in "
                        + "src/test/java; API-GAPS.md records where Flow "
                        + "made testing harder than it should be.");
        addMenuCards(List.of(QuoteView.class, NewOrderView.class,
                RegistrationView.class, OrdersView.class, SecurityView.class,
                ReportView.class, BrowserApisView.class, EndToEndView.class,
                InvoiceView.class, AccessibleFormView.class,
                LoadTestView.class));
    }
}
