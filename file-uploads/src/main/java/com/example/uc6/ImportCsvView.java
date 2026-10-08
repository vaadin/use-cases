package com.example.uc6;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.server.streams.UploadMetadata;

/**
 * UC6 — Import data from a file.
 * <p>
 * A product list exported from a spreadsheet is imported into the catalogue.
 * Receiving the file is the easy part; the use case is about what happens next:
 * the file is parsed (comma- or semicolon-separated, any column order, UTF-8 or
 * the Windows encoding older spreadsheets save in), every row is checked, and
 * the user sees which rows are ready and which have problems, with the line
 * number and what is wrong, before anything is imported. Rows with problems are
 * skipped; the user can fix the file and import it again.
 */
@Route(value = "uc6", layout = MainLayout.class)
@PageTitle("UC6 — Import a file")
@UseCaseDescription("Checking every row of an uploaded spreadsheet before importing it")
@Menu(order = 6, title = "UC6 — Import a file")
public class ImportCsvView extends VerticalLayout {

    static final List<String> COLUMNS = List.of("sku", "name", "price",
            "stock");

    static final String SAMPLE = """
            sku,name,price,stock
            BK-001,Notebook A5,4.90,120
            BK-002,"Notebook A4, squared",6.50,80
            PN-010,Ballpoint pen (blue),1.20,500
            PN-011,Ballpoint pen (red),1.20,-5
            PN-010,Ballpoint pen (black),1.20,300
            ST-100,,2.00,40
            ST-101,Stapler,twelve,15
            ST-102,Staples (box of 1000),3.40,200
            """;

    record Product(String sku, String name, BigDecimal price, int stock) {
    }

    record Problem(int line, String message) {
    }

    private final Upload upload;
    private final Span summary = new Span();
    private final Grid<Product> ready = new Grid<>(Product.class, false);
    private final Grid<Problem> problems = new Grid<>(Problem.class, false);
    private final Button importButton = new Button();
    private final Grid<Product> catalogue = new Grid<>(Product.class, false);
    private final List<Product> catalogueItems = new ArrayList<>();
    private List<Product> pending = List.of();

    public ImportCsvView() {
        add(new H1("UC6 — Import data from a file"));
        add(new Paragraph("Upload a product list saved from a spreadsheet as "
                + "CSV, with the columns sku, name, price and stock. Every "
                + "row is checked before anything is imported."));
        byte[] sampleBytes = SAMPLE.getBytes(StandardCharsets.UTF_8);
        Anchor sample = new Anchor(
                DownloadHandler.fromInputStream(event -> new DownloadResponse(
                        new ByteArrayInputStream(sampleBytes), "products.csv",
                        "text/csv", sampleBytes.length)),
                "Download a sample file (with a few mistakes)");

        upload = new Upload(UploadHandler.inMemory(this::fileReceived));
        upload.setMaxFiles(1);
        upload.setMaxFileSize(5 * 1024 * 1024);
        upload.setAcceptedFileTypes(".csv", "text/csv");
        upload.setWidthFull();

        ready.setId("ready-rows");
        configure(ready);
        ready.setVisible(false);
        problems.setId("problems");
        problems.addColumn(Problem::line).setHeader("Line").setAutoWidth(true)
                .setFlexGrow(0);
        problems.addColumn(Problem::message).setHeader("Problem");
        problems.setAllRowsVisible(true);
        problems.setVisible(false);
        importButton.addThemeVariants(ButtonVariant.PRIMARY);
        importButton.setVisible(false);
        importButton.addClickListener(e -> importPending());

        catalogue.setId("catalogue");
        configure(catalogue);
        catalogue.setItems(catalogueItems);

        add(sample, upload, summary, problems, ready, importButton,
                new H2("Catalogue"), catalogue);
    }

    private static void configure(Grid<Product> grid) {
        grid.addColumn(Product::sku).setHeader("SKU").setAutoWidth(true);
        grid.addColumn(Product::name).setHeader("Name");
        grid.addColumn(Product::price).setHeader("Price").setAutoWidth(true);
        grid.addColumn(Product::stock).setHeader("Stock").setAutoWidth(true);
        grid.setAllRowsVisible(true);
    }

    private void fileReceived(UploadMetadata metadata, byte[] bytes) {
        List<CsvParser.Row> rows = CsvParser.parse(bytes);
        List<Product> products = new ArrayList<>();
        List<Problem> found = new ArrayList<>();
        if (rows.isEmpty()) {
            found.add(new Problem(1, "The file is empty."));
        } else {
            check(rows, products, found);
        }
        pending = List.copyOf(products);

        summary.setText(metadata.fileName() + ": " + products.size()
                + (products.size() == 1 ? " row" : " rows") + " ready, "
                + found.size()
                + (found.size() == 1 ? " problem." : " problems."));
        ready.setItems(pending);
        ready.setVisible(!pending.isEmpty());
        problems.setItems(found);
        problems.setVisible(!found.isEmpty());
        importButton.setText("Import " + pending.size()
                + (pending.size() == 1 ? " row" : " rows"));
        importButton.setVisible(!pending.isEmpty());
        upload.clearFileList();
    }

    private static void check(List<CsvParser.Row> rows, List<Product> products,
            List<Problem> found) {
        List<String> header = rows.get(0).fields().stream()
                .map(h -> h.trim().toLowerCase(Locale.ROOT)).toList();
        Map<String, Integer> index = IntStream.range(0, header.size()).boxed()
                .collect(Collectors.toMap(header::get, Function.identity(),
                        (first, second) -> first));
        List<String> missing = COLUMNS.stream()
                .filter(c -> !index.containsKey(c)).toList();
        if (!missing.isEmpty()) {
            found.add(new Problem(1, "Missing column(s): "
                    + String.join(", ", missing) + ". Nothing was imported."));
            return;
        }

        Set<String> skus = new HashSet<>();
        for (CsvParser.Row row : rows.subList(1, rows.size())) {
            Function<String, String> field = column -> {
                int i = index.getOrDefault(column, -1);
                return i >= 0 && i < row.fields().size()
                        ? row.fields().get(i).trim()
                        : "";
            };
            String sku = field.apply("sku");
            String name = field.apply("name");
            BigDecimal price = parsePrice(field.apply("price"));
            Integer stock = parseStock(field.apply("stock"));

            List<String> errors = new ArrayList<>();
            if (sku.isEmpty()) {
                errors.add("SKU is missing");
            } else if (!skus.add(sku)) {
                errors.add("SKU " + sku + " appears more than once");
            }
            if (name.isEmpty()) {
                errors.add("name is missing");
            }
            if (price == null) {
                errors.add("price \"" + field.apply("price")
                        + "\" is not a number of 0 or more");
            }
            if (stock == null) {
                errors.add("stock \"" + field.apply("stock")
                        + "\" is not a whole number of 0 or more");
            }
            if (errors.isEmpty() && price != null && stock != null) {
                products.add(new Product(sku, name, price, stock));
            } else {
                String message = String.join("; ", errors);
                found.add(new Problem(row.line(),
                        Character.toUpperCase(message.charAt(0))
                                + message.substring(1) + "."));
            }
        }
    }

    private static @Nullable BigDecimal parsePrice(String value) {
        try {
            BigDecimal price = new BigDecimal(value.replace(',', '.'));
            return price.signum() >= 0 ? price : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static @Nullable Integer parseStock(String value) {
        try {
            int stock = Integer.parseInt(value);
            return stock >= 0 ? stock : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void importPending() {
        catalogueItems.addAll(pending);
        catalogue.getDataProvider().refreshAll();
        summary.setText("Imported " + pending.size()
                + (pending.size() == 1 ? " product." : " products."));
        pending = List.of();
        ready.setVisible(false);
        problems.setVisible(false);
        importButton.setVisible(false);
    }
}
