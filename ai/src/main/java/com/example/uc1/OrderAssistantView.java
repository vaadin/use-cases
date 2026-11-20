package com.example.uc1;

import java.util.List;

import com.example.ai.AiProvider;
import com.example.ai.AiProviders;
import com.example.ai.AssistantPanel;
import com.example.ai.LlmProviderFactory;
import com.example.ai.ProviderNotice;
import com.example.ai.ToolCallLog;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.ai.orchestrator.AIOrchestrator;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

/**
 * UC1 — Ask about your orders.
 * <p>
 * Users ask questions about the order data in their own words ("Which orders
 * are late?", "What has Contoso ordered?"). An {@link AIOrchestrator} connects
 * a message input and a message list to the LLM and gives it the
 * {@link OrderTools}: plain Java methods marked with Spring AI's {@code @Tool}.
 * The model answers by calling those methods, so its answer comes from the
 * application's data rather than from its training. The log beside the chat
 * shows every call the model made.
 */
@Route(value = "uc1", layout = MainLayout.class)
@UseCaseDescription("Answering questions about business data in plain language, through the app's own Java methods")
@Menu(order = 1, title = "UC1 — Ask about your orders")
public class OrderAssistantView extends VerticalLayout {

    static final String SYSTEM_PROMPT = """
            You are the assistant of an order management application. Answer \
            questions about the company's sales orders. Look the orders up \
            with the tools; never invent an order, a customer or an amount. \
            Amounts are in euros. Keep answers short. When you list several \
            orders, use a Markdown table with the order id, customer, product, \
            amount and status. If a question is not about the orders, say \
            that you can only help with orders.""";

    static final List<String> SUGGESTIONS = List.of("Which orders are late?",
            "What has Contoso ordered, and what is the total?",
            "Which customer has the most open orders?");

    public OrderAssistantView(OrderBook orderBook,
            LlmProviderFactory llmProviders, AiProviders providers) {
        add(new H1("UC1 — Ask about your orders"));
        add(new Paragraph("Ask a question about the orders in your own "
                + "words. The assistant cannot see the data directly: it "
                + "calls two Java methods of the application, findOrders and "
                + "getOrder, and answers from what they return. The log next to "
                + "the chat shows each call, so you can check the answer."));

        ProviderNotice notice = ProviderNotice.forMissing(providers,
                AiProvider.OPENAI);
        if (notice != null) {
            add(notice);
        }

        ToolCallLog log = new ToolCallLog("What the assistant looked up",
                "No calls yet. Ask a question to see which methods the "
                        + "assistant calls.");
        AssistantPanel panel = new AssistantPanel(log);

        AIOrchestrator orchestrator = AIOrchestrator
                .builder(llmProviders.create(), SYSTEM_PROMPT)
                .withMessageList(panel.messages()).withInput(panel.input())
                .withTools(new OrderTools(orderBook, log))
                .withAssistantName("Order assistant").build();

        panel.addSuggestions(SUGGESTIONS, orchestrator);

        panel.setReady(notice == null);

        add(panel);

        add(new Details("The order data the methods read",
                ordersGrid(orderBook)));
    }

    private static Grid<Order> ordersGrid(OrderBook orderBook) {
        Grid<Order> grid = new Grid<>();
        grid.addColumn(Order::id).setHeader("Order").setAutoWidth(true);
        grid.addColumn(Order::customer).setHeader("Customer")
                .setAutoWidth(true);
        grid.addColumn(Order::product).setHeader("Product").setAutoWidth(true);
        grid.addColumn(Order::amount).setHeader("Amount (€)")
                .setAutoWidth(true);
        grid.addColumn(Order::status).setHeader("Status").setAutoWidth(true);
        grid.addColumn(Order::dueDate).setHeader("Due").setAutoWidth(true);
        grid.setItems(orderBook.all());
        grid.setAllRowsVisible(true);
        return grid;
    }
}
