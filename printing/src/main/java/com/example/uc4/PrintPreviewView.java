package com.example.uc4;

import java.util.List;

import com.example.MissingAPI;
import com.example.data.Orders;
import com.example.print.OrderDocument;
import com.example.print.PageSetup;
import com.example.print.Paper;
import com.example.views.MainLayout;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC4 — Paper setup and a preview of it.
 * <p>
 * The user chooses paper size, orientation and margins, sees the document
 * inside a sheet of exactly those dimensions, and prints it with the same
 * settings applied to the real page box.
 * <p>
 * The page box is CSS's {@code @page} at-rule, and that is the problem:
 * {@code @page} is a document-level rule, so it cannot be set through
 * {@code Element#getStyle()}, a theme variant or a class name — the only way to
 * change it at runtime is to write a {@code <style>} element into the head by
 * hand, which is what {@link MissingAPI#setPageRule(UI, String)
 * MissingAPI.setPageRule} does. Nor is the setting a contract: the print dialog
 * lets the user override paper and margins afterwards, and the browser never
 * reports what they picked, so the preview is the application's best guess
 * rather than a promise.
 */
@Route(value = "uc4", layout = MainLayout.class)
@PageTitle("UC4 — Paper setup and preview")
@Menu(order = 4, title = "UC4 — Paper setup and preview")
@StyleSheet("uc4.css")
public class PrintPreviewView extends VerticalLayout {

    private static final List<Integer> MARGINS = List.of(0, 10, 15, 25);

    private final Div sheet = new Div();
    private final Pre pageRule = new Pre();

    private final Select<Paper> paper = new Select<>();
    private final Checkbox landscape = new Checkbox("Landscape");
    private final Select<Integer> margin = new Select<>();

    public PrintPreviewView() {
        Div intro = new Div();
        intro.addClassName("no-print");
        intro.add(new H1("UC4 — Paper setup and preview"));
        intro.add(new Paragraph(
                "The sheet below is drawn at the chosen paper size, in "
                        + "millimetres, with the chosen margins — so what it "
                        + "shows is what the printer has room for. Printing "
                        + "writes the matching @page rule into the document "
                        + "first; the print dialog may still override it."));

        paper.setLabel("Paper");
        paper.setItems(Paper.values());
        paper.setItemLabelGenerator(Paper::label);
        paper.setValue(PageSetup.DEFAULT.paper());
        paper.setId("paper-select");
        paper.addValueChangeListener(event -> updatePreview());

        landscape.setValue(PageSetup.DEFAULT.landscape());
        landscape.setId("landscape-checkbox");
        landscape.addValueChangeListener(event -> updatePreview());

        margin.setLabel("Margin");
        margin.setItems(MARGINS);
        margin.setItemLabelGenerator(millimetres -> millimetres + " mm");
        margin.setValue(PageSetup.DEFAULT.marginMm());
        margin.setId("margin-select");
        margin.addValueChangeListener(event -> updatePreview());

        Button print = new Button("Print",
                event -> print(event.getSource().getUI().orElseThrow()));
        print.addThemeVariants(ButtonVariant.PRIMARY);
        print.setId("print-button");

        HorizontalLayout controls = new HorizontalLayout(paper, landscape,
                margin, print);
        controls.addClassName("no-print");
        controls.setAlignItems(Alignment.END);

        pageRule.setId("page-rule");
        pageRule.addClassNames("no-print", "page-rule");

        sheet.addClassNames("sheet", "printable");
        sheet.setId("preview-sheet");
        sheet.add(new OrderDocument(Orders.sampleOrder()));

        Div preview = new Div(sheet);
        preview.addClassName("preview-area");

        add(intro, controls, pageRule, preview);
        updatePreview();
    }

    /**
     * The paper setup the controls currently describe.
     *
     * @return the current setup
     */
    public PageSetup currentSetup() {
        return new PageSetup(paper.getValue(),
                Boolean.TRUE.equals(landscape.getValue()), margin.getValue());
    }

    private void updatePreview() {
        PageSetup setup = currentSetup();
        sheet.getStyle().set("width", setup.sheetWidthMm() + "mm")
                .set("min-height", setup.sheetHeightMm() + "mm")
                .set("padding", setup.marginMm() + "mm");
        pageRule.setText(setup.toPageRule());
        getUI().ifPresent(ui -> MissingAPI.setPageRule(ui, setup.toPageRule()));
    }

    private void print(UI ui) {
        MissingAPI.setPageRule(ui, currentSetup().toPageRule());
        MissingAPI.print(ui);
    }
}
