package com.example.usecase25;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.example.usecase23.SchedulerService;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.flow.signals.local.ValueSignal;

@PageTitle("Use Case 25: Stock Ticker")
@Route(value = "use-case-25", layout = MainLayout.class)
@Menu(order = 25, title = "UC 25: Stock Ticker")
@StyleSheet("usecase25.css")
@AnonymousAllowed
public class UseCase25View extends Main {

    final ListSignal<StockQuote> stockSignals = new ListSignal<>();
    private @Nullable String taskId;

    public UseCase25View(SchedulerService schedulerService) {
        addClassName("usecase25-view");

        var title = new H2("Use Case 25: Stock Ticker");
        var description = new Paragraph(
                "Real-time stock price updates using Element.flashClass() to "
                        + "visually highlight price direction — green flash for "
                        + "price up, red flash for price down.");

        // Table header
        Div header = createHeaderRow();

        // Stock rows container
        Div stockList = new Div();
        stockList.addClassName("stock-list");

        for (StockQuote initial : StockPriceSimulator.INITIAL_STOCKS) {
            var stockSignal = stockSignals.insertLast(initial);
            stockList.add(createStockRow(stockSignal));
        }

        add(title, description, header, stockList);

        addAttachListener(event -> {
            taskId = "stock-ticker-" + event.getUI().getUIId();
            schedulerService.scheduleTask(taskId,
                    () -> StockPriceSimulator.updatePrices(stockSignals), 1500,
                    1500, TimeUnit.MILLISECONDS);
        });

        addDetachListener(event -> {
            if (taskId != null) {
                schedulerService.cancelTask(taskId);
            }
        });
    }

    private Div createHeaderRow() {
        Div row = new Div();
        row.addClassName("header-row");

        row.add(headerCell("Symbol"), headerCell("Company"),
                headerCell("Price", true), headerCell("Change", true),
                headerCell("% Change", true));
        return row;
    }

    private Span headerCell(String text) {
        return headerCell(text, false);
    }

    private Span headerCell(String text, boolean alignRight) {
        Span span = new Span(text);
        if (alignRight) {
            span.addClassName("header-cell-right");
        }
        return span;
    }

    private Div createStockRow(ValueSignal<StockQuote> stockSignal) {
        Div row = new Div();
        row.addClassName("stock-row");

        // Symbol
        Span symbol = new Span();
        symbol.bindText(stockSignal.map(StockQuote::symbol));
        symbol.addClassName("symbol-cell");

        // Company name
        Span name = new Span();
        name.bindText(stockSignal.map(StockQuote::name));
        name.addClassName("name-cell");

        // Price
        Span price = new Span();
        price.bindText(stockSignal
                .map(q -> "$" + q.price().setScale(2, RoundingMode.HALF_UP)));
        price.addClassName("price-cell");

        // Change
        Span change = new Span();
        change.bindText(stockSignal.map(q -> {
            String prefix = q.change().compareTo(BigDecimal.ZERO) >= 0 ? "+"
                    : "";
            return prefix + q.change().setScale(2, RoundingMode.HALF_UP);
        }));
        change.addClassName("change-cell");

        // % Change
        Span pctChange = new Span();
        pctChange.bindText(stockSignal.map(q -> {
            String prefix = q.changePercent().compareTo(BigDecimal.ZERO) >= 0
                    ? "+"
                    : "";
            return prefix + q.changePercent().setScale(2, RoundingMode.HALF_UP)
                    + "%";
        }));
        pctChange.addClassName("change-cell");

        // React to price changes with flash effect
        List<Element> flashTargets = List.of(price.getElement(),
                change.getElement(), pctChange.getElement());
        Signal.effect(row, () -> {
            StockQuote current = stockSignal.get();
            String flashClass = current.change().compareTo(BigDecimal.ZERO) >= 0
                    ? "price-up"
                    : "price-down";
            flashTargets.forEach(el -> el.flashClass(flashClass));
        });

        row.add(symbol, name, price, change, pctChange);
        return row;
    }
}
