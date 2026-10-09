package com.example.usecase11;

import com.example.MissingAPI;
import com.example.MissingAPI.ComponentSize;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;

/**
 * Use Case 11: Responsive Layout with Container Size Signal
 *
 * Demonstrates responsive content inside a resizable container using a split
 * panel. The content adapts to the available width as you drag the splitter,
 * showing how components can be responsive within any container, not just the
 * window.
 *
 * Key Patterns: - ResizeObserver for container size tracking - Signal from
 * container resize events - Computed signals for breakpoint detection -
 * Responsive component visibility and layout within a fixed container - Split
 * panel with draggable divider
 */
@Route(value = "use-case-11", layout = MainLayout.class)
@PageTitle("Use Case 11: Responsive Layout")
@UseCaseDescription("Adapting a layout to its container size instead of the window")
@Menu(order = 11, title = "UC 11: Responsive Layout")
@StyleSheet("usecase11.css")
@AnonymousAllowed
public class UseCase11View extends VerticalLayout {

    private static final int SMALL_BREAKPOINT = 400;
    private static final int LARGE_BREAKPOINT = 700;

    private final Signal<ComponentSize> containerSizeSignal;
    private final Signal<Boolean> isSmall;
    private final Signal<Boolean> isMedium;
    private final Signal<Boolean> isLarge;

    private Div responsiveContent;

    public UseCase11View() {
        addClassName("usecase11-view");
        setSpacing(true);
        setPadding(true);
        setSizeFull();

        // Create responsive content container first so we can set up the size
        // signal before building other panels that depend on it
        responsiveContent = new Div();
        responsiveContent.addClassName("responsive-content");
        containerSizeSignal = MissingAPI.sizeSignal(responsiveContent);
        isSmall = containerSizeSignal
                .map(size -> size.width() < SMALL_BREAKPOINT);
        isMedium = containerSizeSignal
                .map(size -> size.width() >= SMALL_BREAKPOINT
                        && size.width() < LARGE_BREAKPOINT);
        isLarge = containerSizeSignal
                .map(size -> size.width() >= LARGE_BREAKPOINT);

        H2 title = new H2(
                "Use Case 11: Responsive Content in Resizable Container");

        Paragraph description = new Paragraph(
                "This use case demonstrates responsive content inside a resizable container. "
                        + "Drag the splitter left and right to resize the content area. "
                        + "The content adapts to the available width, showing different layouts at different sizes. "
                        + "This uses ResizeObserver to track container size changes.");

        // Create split layout
        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(60); // Start at 60% for the primary
                                             // content

        // Left side: Static info panel
        Div infoPanel = createInfoPanel();

        // Right side: Populate responsive content
        populateResponsiveContent(responsiveContent);

        splitLayout.addToPrimary(responsiveContent);
        splitLayout.addToSecondary(infoPanel);

        // Info box
        Div tipBox = new Div();
        tipBox.addClassName("tip-box");
        tipBox.add(new Paragraph(
                "💡 Drag the splitter to resize the content area. "
                        + "The responsive content will adapt to different widths, "
                        + "showing mobile layout (< 400px), tablet layout (400-700px), or desktop layout (≥ 700px). "
                        + "This demonstrates container queries with Signals using ResizeObserver."));

        add(title, description, tipBox, splitLayout);
    }

    private Div createInfoPanel() {
        Div panel = new Div();
        panel.addClassName("info-panel");

        H3 title = new H3("Info Panel");
        title.addClassName("info-panel-title");

        Paragraph info = new Paragraph(
                "This is a static side panel. The main content on the left adapts "
                        + "to its container size as you drag the splitter.");

        H3 statsTitle = new H3("Current Size");

        // Container size display
        Div sizeDisplay = new Div();
        sizeDisplay.addClassName("size-display");

        Paragraph widthPara = new Paragraph(
                () -> "Width: " + containerSizeSignal.get().width() + "px");
        widthPara.addClassName("size-line");

        Paragraph heightPara = new Paragraph(
                () -> "Height: " + containerSizeSignal.get().height() + "px");
        heightPara.addClassName("size-line");

        Paragraph breakpointPara = new Paragraph(() -> {
            if (isSmall.get())
                return "📱 Small (< 400px)";
            if (isMedium.get())
                return "💻 Medium (400-700px)";
            return "🖥️ Large (≥ 700px)";
        });
        breakpointPara.addClassName("breakpoint-line");

        sizeDisplay.add(widthPara, heightPara, breakpointPara);

        panel.add(title, info, statsTitle, sizeDisplay);
        return panel;
    }

    private void populateResponsiveContent(Div container) {
        // Small width content
        Div smallContent = createSection("📱 Small Width Layout",
                "This is the mobile view (width < 400px). Navigation is stacked vertically, "
                        + "and complex UI elements are simplified or hidden.",
                "small-layout");
        smallContent.bindVisible(isSmall);

        // Medium width content
        Div mediumContent = createSection("💻 Medium Width Layout",
                "This is the tablet view (400px ≤ width < 700px). Navigation can be horizontal, "
                        + "and more content is visible with a balanced layout.",
                "medium-layout");
        mediumContent.bindVisible(isMedium);

        // Large width content
        Div largeContent = createSection("🖥️ Large Width Layout",
                "This is the desktop view (width ≥ 700px). All features are visible, "
                        + "with multi-column layouts and detailed information.",
                "large-layout");
        largeContent.bindVisible(isLarge);

        // Responsive card grid
        H3 cardGridTitle = new H3("Responsive Card Grid");
        Component cardGrid = createResponsiveCardGrid();

        container.add(smallContent, mediumContent, largeContent, cardGridTitle,
                cardGrid);
    }

    private Div createSection(String title, String content,
            String variantClass) {
        Div section = new Div();
        section.addClassName("section");
        section.addClassName(variantClass);

        H3 sectionTitle = new H3(title);
        sectionTitle.addClassName("section-title");

        Paragraph sectionContent = new Paragraph(content);
        sectionContent.addClassName("section-content");

        section.add(sectionTitle, sectionContent);
        return section;
    }

    private Component createResponsiveCardGrid() {
        Div gridContainer = new Div();
        gridContainer.addClassName("card-grid");

        // Create sample cards
        for (int i = 1; i <= 6; i++) {
            Div card = new Div();
            card.addClassName("grid-card");

            H3 cardTitle = new H3("Card " + i);
            cardTitle.addClassName("grid-card-title");

            Paragraph cardContent = new Paragraph("Content item " + i);
            cardContent.addClassName("grid-card-content");

            card.add(cardTitle, cardContent);
            gridContainer.add(card);
        }

        // Toggle small layout class based on container size
        gridContainer.getClassNames().bind("is-small", isSmall);

        return gridContainer;
    }

}
