package com.example.desk;

import java.util.List;

import com.example.components.Address;
import com.example.components.Money;
import org.jspecify.annotations.Nullable;

/** A support ticket of the service desk (UC10). */
public record Ticket(int id, String title, String customer, Address address,
        List<Double> responseMinutes, int rating, @Nullable Money refund,
        String labelColor, boolean closed) {

    public String trackingLink() {
        return "https://example.com/track/" + id;
    }

    public Ticket withEdits(int rating, @Nullable Money refund,
            String labelColor, boolean closed) {
        return new Ticket(id, title, customer, address, responseMinutes, rating,
                refund, labelColor, closed);
    }
}
