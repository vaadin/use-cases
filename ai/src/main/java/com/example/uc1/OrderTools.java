package com.example.uc1;

import java.util.List;

import com.example.ai.ToolCallLog;
import com.example.uc1.Order.Status;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import tools.jackson.databind.json.JsonMapper;

/**
 * The application methods the assistant may call. Each one is an ordinary Java
 * method marked with Spring AI's {@link Tool}; the description tells the model
 * when to use it. They only read data: the assistant can look things up, but
 * not change an order.
 */
public class OrderTools {

    private static final JsonMapper JSON = JsonMapper.shared();

    private final OrderBook orderBook;
    private final ToolCallLog log;

    public OrderTools(OrderBook orderBook, ToolCallLog log) {
        this.orderBook = orderBook;
        this.log = log;
    }

    @Tool(description = "Finds sales orders. Every filter is optional; "
            + "leave a filter out to match all orders. Returns the orders "
            + "as JSON.")
    public String findOrders(
            @ToolParam(required = false, description = ""
                    + "Part of the customer name") @Nullable String customer,
            @ToolParam(required = false, description = "Order status") @Nullable Status status,
            @ToolParam(required = false, description = "Only orders "
                    + "that are still open after their due date") @Nullable Boolean lateOnly) {
        List<Order> found = orderBook.find(customer, status,
                Boolean.TRUE.equals(lateOnly));
        String result = JSON.writeValueAsString(found);
        log.record("findOrders", describe("customer", customer, "status",
                status, "lateOnly", lateOnly), result);
        return result;
    }

    @Tool(description = "Returns one sales order by its id, such as "
            + "SO-1004, as JSON.")
    public String getOrder(
            @ToolParam(description = "The order id") String orderId) {
        String result = orderBook.get(orderId).map(JSON::writeValueAsString)
                .orElse("No order with id " + orderId);
        log.record("getOrder", describe("orderId", orderId), result);
        return result;
    }

    /**
     * Renders the non-null arguments as {@code name=value} pairs for the log.
     */
    private static String describe(@Nullable Object... namesAndValues) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i + 1 < namesAndValues.length; i += 2) {
            if (namesAndValues[i + 1] != null) {
                if (!text.isEmpty()) {
                    text.append(", ");
                }
                text.append(namesAndValues[i]).append('=')
                        .append(namesAndValues[i + 1]);
            }
        }
        return text.toString();
    }
}
