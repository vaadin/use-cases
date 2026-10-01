package com.example.uc3;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

/**
 * How the user wants their order export to look.
 * <p>
 * Spring's {@code @SessionScope} (CDI: {@code @SessionScoped}) binds the bean
 * to the HTTP session. Unlike {@code @VaadinSessionScope}, it is also visible
 * to plain Spring MVC controllers, filters and anything else that runs in a
 * request of the same HTTP session — here the {@link OrderExportController}
 * that streams the CSV file. Spring injects a proxy which resolves the real
 * instance from the current request, so it only works on threads that are
 * handling an HTTP request.
 */
@Component
@SessionScope
public class ExportPreferences implements Serializable {

    public enum Column {
        ORDER_ID("Order"), CUSTOMER("Customer"), DATE("Date"), TOTAL("Total");

        private final String label;

        Column(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private Set<Column> columns = EnumSet.allOf(Column.class);
    private String delimiter = ",";

    public Set<Column> getColumns() {
        return EnumSet.copyOf(columns);
    }

    public void setColumns(Set<Column> columns) {
        this.columns = columns.isEmpty() ? EnumSet.noneOf(Column.class)
                : EnumSet.copyOf(columns);
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }
}
