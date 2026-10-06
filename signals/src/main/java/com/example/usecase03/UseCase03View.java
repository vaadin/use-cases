package com.example.usecase03;

import com.example.MissingAPI;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.slider.DecimalSlider;
import com.vaadin.flow.component.slider.IntegerSlider;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

@Route(value = "use-case-03", layout = MainLayout.class)
@PageTitle("Use Case 3: Interactive SVG Shape Editor")
@Menu(order = 3, title = "UC 3: Interactive SVG Shape Editor")
@StyleSheet("usecase03.css")
@AnonymousAllowed
public class UseCase03View extends VerticalLayout {

    // Rectangle signals (green) - top left position
    private final ValueSignal<Integer> rectXSignal = new ValueSignal<>(0);
    private final ValueSignal<Integer> rectYSignal = new ValueSignal<>(0);
    private final ValueSignal<Integer> rectWidthSignal = new ValueSignal<>(0);
    private final ValueSignal<Integer> rectHeightSignal = new ValueSignal<>(0);
    private final ValueSignal<Integer> rectCornerRadiusSignal = new ValueSignal<>(
            0);
    private final ValueSignal<String> rectFillSignal = new ValueSignal<>(
            "#fff");
    private final ValueSignal<String> rectStrokeSignal = new ValueSignal<>(
            "#fff");
    private final ValueSignal<Integer> rectStrokeWidthSignal = new ValueSignal<>(
            0);
    private final ValueSignal<Double> rectOpacitySignal = new ValueSignal<>(
            0.0);
    private final ValueSignal<Integer> rectRotationSignal = new ValueSignal<>(
            0);

    // Star signals (orange) - below rectangle
    private final ValueSignal<Integer> starPointsSignal = new ValueSignal<>(0);
    private final ValueSignal<Integer> starSizeSignal = new ValueSignal<>(0);
    private final ValueSignal<Integer> starCxSignal = new ValueSignal<>(0);
    private final ValueSignal<Integer> starCySignal = new ValueSignal<>(0);
    private final ValueSignal<Integer> starRotationSignal = new ValueSignal<>(
            0);
    private final ValueSignal<String> starFillSignal = new ValueSignal<>(
            "#fff");
    private final ValueSignal<String> starStrokeSignal = new ValueSignal<>(
            "#fff");
    private final ValueSignal<Integer> starStrokeWidthSignal = new ValueSignal<>(
            0);
    private final ValueSignal<Double> starOpacitySignal = new ValueSignal<>(
            0.0);

    // Selected shape tracking
    private final ValueSignal<Integer> selectedShapeSignal = new ValueSignal<>(
            0);

    private Element rectElement = new Element("rect");
    private Element starElement = new Element("polygon");

    public UseCase03View() {
        resetAll();

        addClassName("usecase03-view");
        setSpacing(true);
        setPadding(true);
        setWidthFull();

        H2 title = new H2("Use Case 3: Interactive SVG Shape Editor");
        title.addClassName("view-title");

        Paragraph description = new Paragraph(
                "Click on a shape to select it, or use the tabs below. "
                        + "Demonstrates extensive bindAttribute() usage with SVG elements. "
                        + "Each shape is controlled by multiple signals that bind to SVG attributes like position, size, colors, and transforms. "
                        + "(19 writable + 4 computed = 23 signals total).");
        description.addClassName("view-description");

        // Main content: controls on left, canvas on right
        HorizontalLayout mainContent = new HorizontalLayout();
        mainContent.setWidthFull();
        mainContent.setSpacing(true);

        // Left panel: shape selector and controls
        VerticalLayout leftPanel = createLeftPanel();
        leftPanel.setWidth("280px");
        leftPanel.addClassName("left-panel");

        // Right panel: SVG canvas
        Div svgContainer = createSvgCanvas();
        svgContainer.addClassName("svg-container");

        mainContent.add(leftPanel, svgContainer);

        add(title, description, mainContent);
    }

    private VerticalLayout createLeftPanel() {
        VerticalLayout panel = new VerticalLayout();
        panel.setSpacing(true);
        panel.setPadding(false);

        // Shape selector tabs
        Tab rectangleTab = new Tab("\uD83D\uDFE9 Rectangle");
        Tab starTab = new Tab("⭐\uFE0F Star");

        var tabsheet = new TabSheet();
        tabsheet.setWidthFull();
        tabsheet.add(rectangleTab, createRectangleControls());
        tabsheet.add(starTab, createStarControls());
        MissingAPI.tabsSyncSelectedIndex(tabsheet, selectedShapeSignal,
                selectedShapeSignal::set);

        // Reset button
        Button resetButton = new Button("Reset to Defaults", e -> resetAll());
        resetButton.setWidthFull();

        panel.add(tabsheet, resetButton);
        return panel;
    }

    private Div createSvgCanvas() {
        Div container = new Div();
        // .svg-container class is added on the wrapper by the caller

        Element svg = new Element("svg");
        svg.setAttribute("viewBox", "0 0 500 500");
        svg.setAttribute("width", "100%");
        svg.setAttribute("height", "500");
        svg.setAttribute("style", "max-width: 100%;");
        svg.getClassList().add("svg-canvas");

        // Add background grid for reference
        Element defs = new Element("defs");
        Element pattern = new Element("pattern");
        pattern.setAttribute("id", "grid");
        pattern.setAttribute("width", "20");
        pattern.setAttribute("height", "20");
        pattern.setAttribute("patternUnits", "userSpaceOnUse");

        Element gridPath = new Element("path");
        gridPath.setAttribute("d", "M 20 0 L 0 0 0 20");
        gridPath.setAttribute("fill", "none");
        gridPath.setAttribute("stroke", "#e0e0e0");
        gridPath.setAttribute("stroke-width", "0.5");

        pattern.appendChild(gridPath);
        defs.appendChild(pattern);
        svg.appendChild(defs);

        Element gridRect = new Element("rect");
        gridRect.setAttribute("width", "100%");
        gridRect.setAttribute("height", "100%");
        gridRect.setAttribute("fill", "url(#grid)");
        svg.appendChild(gridRect);

        // Create shapes
        rectElement = createRectangleElement();
        starElement = createStarElement();

        svg.appendChild(rectElement);
        svg.appendChild(starElement);

        // Add click handlers to select shapes
        rectElement.addEventListener("click", e -> {
            selectedShapeSignal.set(0);
        });

        starElement.addEventListener("click", e -> {
            selectedShapeSignal.set(1);
        });

        // Add cursor pointer style for shapes
        rectElement.getClassList().add("clickable-shape");
        starElement.getClassList().add("clickable-shape");

        container.getElement().appendChild(svg);
        return container;
    }

    private Element createRectangleElement() {
        Element rect = new Element("rect");

        // Bind basic attributes
        rect.bindAttribute("x", rectXSignal.map(String::valueOf));
        rect.bindAttribute("y", rectYSignal.map(String::valueOf));
        rect.bindAttribute("width", rectWidthSignal.map(String::valueOf));
        rect.bindAttribute("height", rectHeightSignal.map(String::valueOf));
        rect.bindAttribute("rx", rectCornerRadiusSignal.map(String::valueOf));
        rect.bindAttribute("fill", rectFillSignal);
        rect.bindAttribute("stroke", rectStrokeSignal);

        // Stroke width increases when selected
        rect.bindAttribute("stroke-width", () -> {
            int baseWidth = rectStrokeWidthSignal.get();
            boolean isSelected = selectedShapeSignal.get() == 0;
            return String.valueOf(isSelected ? baseWidth + 2 : baseWidth);
        });

        rect.bindAttribute("opacity", rectOpacitySignal.map(String::valueOf));

        // Computed transform attribute (rotate around center)
        rect.bindAttribute("transform", Signal.computed(() -> {
            int x = rectXSignal.get();
            int y = rectYSignal.get();
            int w = rectWidthSignal.get();
            int h = rectHeightSignal.get();
            int centerX = x + w / 2;
            int centerY = y + h / 2;
            int rotation = rectRotationSignal.get();
            return String.format("rotate(%d %d %d)", rotation, centerX,
                    centerY);
        }));

        rect.setAttribute("filter", "drop-shadow(2px 2px 4px rgba(0,0,0,0.2))");

        return rect;
    }

    private Element createStarElement() {
        Element polygon = new Element("polygon");

        // Computed points attribute (complex calculation) - centered at origin
        polygon.bindAttribute("points", Signal.computed(() -> {
            int n = starPointsSignal.get();
            int size = starSizeSignal.get();
            return generateStarPoints(n, size, 0, 0); // Generate at origin
        }));

        // Bind styling attributes
        polygon.bindAttribute("fill", starFillSignal);
        polygon.bindAttribute("stroke", starStrokeSignal);

        // Stroke width increases when selected
        polygon.bindAttribute("stroke-width", () -> {
            int baseWidth = starStrokeWidthSignal.get();
            boolean isSelected = selectedShapeSignal.get() == 1;
            return String.valueOf(isSelected ? baseWidth + 2 : baseWidth);
        });

        polygon.bindAttribute("opacity",
                starOpacitySignal.map(String::valueOf));

        // Computed transform: translate to position, then rotate
        polygon.bindAttribute("transform", Signal.computed(() -> {
            int rotation = starRotationSignal.get();
            int cx = starCxSignal.get();
            int cy = starCySignal.get();
            // Since points are centered at (0,0), translate to position then
            // rotate
            return String.format("translate(%d %d) rotate(%d)", cx, cy,
                    rotation);
        }));

        polygon.setAttribute("filter",
                "drop-shadow(2px 2px 4px rgba(0,0,0,0.2))");

        return polygon;
    }

    private Div createRectangleControls() {
        Div section = new Div();
        section.setWidthFull();

        VerticalLayout fields = new VerticalLayout();
        fields.setSpacing(false);
        fields.setPadding(false);
        fields.setWidthFull();

        // Position section
        Span positionLabel = new Span("Position");
        positionLabel.addClassName("section-label");

        IntegerSlider xSlider = new IntegerSlider("X", 0, 500);
        bindSliderToInteger(xSlider, rectXSignal);
        xSlider.setWidthFull();

        IntegerSlider ySlider = new IntegerSlider("Y", 0, 500);
        bindSliderToInteger(ySlider, rectYSignal);
        ySlider.setWidthFull();

        // Size section
        Span sizeLabel = new Span("Size");
        sizeLabel.addClassName("section-label");
        sizeLabel.addClassName("section-label-with-margin");

        IntegerSlider widthSlider = new IntegerSlider("Width", 50, 250);
        bindSliderToInteger(widthSlider, rectWidthSignal);
        widthSlider.setWidthFull();

        IntegerSlider heightSlider = new IntegerSlider("Height", 30, 150);
        bindSliderToInteger(heightSlider, rectHeightSignal);
        heightSlider.setWidthFull();

        IntegerSlider cornerRadiusSlider = new IntegerSlider("Corner Radius", 0,
                50);
        bindSliderToInteger(cornerRadiusSlider, rectCornerRadiusSignal);
        cornerRadiusSlider.setWidthFull();

        // Appearance section
        Span appearanceLabel = new Span("Appearance");
        appearanceLabel.addClassName("section-label");
        appearanceLabel.addClassName("section-label-with-margin");

        ComboBox<String> fillColorField = createColorPicker("Fill");
        fillColorField.bindValue(rectFillSignal, rectFillSignal::set);
        fillColorField.setWidthFull();

        ComboBox<String> strokeColorField = createColorPicker("Stroke");
        strokeColorField.bindValue(rectStrokeSignal, rectStrokeSignal::set);
        strokeColorField.setWidthFull();

        DecimalSlider opacitySlider = new DecimalSlider("Opacity", 0.0, 1.0);
        opacitySlider.setStep(0.1);
        opacitySlider.bindValue(rectOpacitySignal, rectOpacitySignal::set);
        opacitySlider.setWidthFull();

        // Transform section
        Span transformLabel = new Span("Transform");
        transformLabel.addClassName("section-label");
        transformLabel.addClassName("section-label-with-margin");

        IntegerSlider rotationSlider = new IntegerSlider("Rotation", 0, 360);
        bindSliderToInteger(rotationSlider, rectRotationSignal);
        rotationSlider.setWidthFull();

        fields.add(positionLabel, xSlider, ySlider, sizeLabel, widthSlider,
                heightSlider, cornerRadiusSlider, appearanceLabel,
                fillColorField, strokeColorField, opacitySlider, transformLabel,
                rotationSlider);

        section.add(fields);
        return section;
    }

    private Div createStarControls() {
        Div section = new Div();
        section.setWidthFull();

        VerticalLayout fields = new VerticalLayout();
        fields.setSpacing(false);
        fields.setPadding(false);
        fields.setWidthFull();

        // Position section
        Span positionLabel = new Span("Position");
        positionLabel.addClassName("section-label");

        IntegerSlider cxSlider = new IntegerSlider("Center X", 0, 500);
        bindSliderToInteger(cxSlider, starCxSignal);
        cxSlider.setWidthFull();

        IntegerSlider cySlider = new IntegerSlider("Center Y", 0, 500);
        bindSliderToInteger(cySlider, starCySignal);
        cySlider.setWidthFull();

        // Shape section
        Span shapeLabel = new Span("Shape");
        shapeLabel.addClassName("section-label");
        shapeLabel.addClassName("section-label-with-margin");

        IntegerSlider pointsSlider = new IntegerSlider("Points", 3, 10);
        bindSliderToInteger(pointsSlider, starPointsSignal);
        pointsSlider.setWidthFull();

        IntegerSlider sizeSlider = new IntegerSlider("Size", 30, 80);
        bindSliderToInteger(sizeSlider, starSizeSignal);
        sizeSlider.setWidthFull();

        // Appearance section
        Span appearanceLabel = new Span("Appearance");
        appearanceLabel.addClassName("section-label");
        appearanceLabel.addClassName("section-label-with-margin");

        ComboBox<String> fillColorField = createColorPicker("Fill");
        fillColorField.bindValue(starFillSignal, starFillSignal::set);
        fillColorField.setWidthFull();

        ComboBox<String> strokeColorField = createColorPicker("Stroke");
        strokeColorField.bindValue(starStrokeSignal, starStrokeSignal::set);
        strokeColorField.setWidthFull();

        DecimalSlider opacitySlider = new DecimalSlider("Opacity", 0.0, 1.0);
        opacitySlider.setStep(0.1);
        opacitySlider.bindValue(starOpacitySignal, starOpacitySignal::set);
        opacitySlider.setWidthFull();

        // Transform section
        Span transformLabel = new Span("Transform");
        transformLabel.addClassName("section-label");
        transformLabel.addClassName("section-label-with-margin");

        IntegerSlider rotationSlider = new IntegerSlider("Rotation", 0, 360);
        bindSliderToInteger(rotationSlider, starRotationSignal);
        rotationSlider.setWidthFull();

        fields.add(positionLabel, cxSlider, cySlider, shapeLabel, pointsSlider,
                sizeSlider, appearanceLabel, fillColorField, strokeColorField,
                opacitySlider, transformLabel, rotationSlider);

        section.add(fields);
        return section;
    }

    /**
     * Create a color picker ComboBox with 10 predefined colors.
     */
    private ComboBox<String> createColorPicker(String label) {
        ComboBox<String> combo = new ComboBox<>(label);
        combo.setItems("#3b82f6", // Blue
                "#ef4444", // Red
                "#10b981", // Green
                "#f59e0b", // Orange
                "#8b5cf6", // Purple
                "#ec4899", // Pink
                "#14b8a6", // Teal
                "#f97316", // Deep Orange
                "#6366f1", // Indigo
                "#84cc16" // Lime
        );

        // Custom renderer to show color swatch
        combo.setRenderer(new ComponentRenderer<>(color -> {
            HorizontalLayout layout = new HorizontalLayout();
            layout.setAlignItems(HorizontalLayout.Alignment.CENTER);
            layout.setSpacing(true);

            Div swatch = new Div();
            // ComboBox renders items into a popover outside the view, so
            // nested CSS would not match — keep these styles inline.
            swatch.getStyle().set("width", "20px").set("height", "20px")
                    .set("background-color", color)
                    .set("border",
                            "1px solid color-mix(in srgb, var(--vaadin-text-color) 20%, transparent)")
                    .set("border-radius", "4px");

            Span text = new Span(color);
            text.getStyle().set("font-size", "var(--aura-font-size-s)");

            layout.add(swatch, text);
            return layout;
        }));

        return combo;
    }

    /**
     * Generate SVG polygon points for a star shape.
     *
     * @param numPoints
     *            Number of points on the star (3-10)
     * @param size
     *            Outer radius of the star
     * @param cx
     *            Center X coordinate
     * @param cy
     *            Center Y coordinate
     * @return SVG points attribute string
     */
    private String generateStarPoints(int numPoints, int size, int cx, int cy) {
        StringBuilder points = new StringBuilder();
        double angleStep = Math.PI / numPoints;
        int outerRadius = size;
        int innerRadius = size / 2;

        for (int i = 0; i < numPoints * 2; i++) {
            double angle = i * angleStep - Math.PI / 2;
            int radius = (i % 2 == 0) ? outerRadius : innerRadius;
            int x = cx + (int) (radius * Math.cos(angle));
            int y = cy + (int) (radius * Math.sin(angle));
            if (i > 0)
                points.append(" ");
            points.append(x).append(",").append(y);
        }

        return points.toString();
    }

    private void resetAll() {
        // Reset rectangle
        rectXSignal.set(100);
        rectYSignal.set(50);
        rectWidthSignal.set(150);
        rectHeightSignal.set(80);
        rectCornerRadiusSignal.set(10);
        rectFillSignal.set("#10b981");
        rectStrokeSignal.set("#059669");
        rectStrokeWidthSignal.set(2);
        rectOpacitySignal.set(1.0);
        rectRotationSignal.set(0);

        // Reset star
        starPointsSignal.set(5);
        starSizeSignal.set(50);
        starCxSignal.set(175);
        starCySignal.set(300);
        starRotationSignal.set(0);
        starFillSignal.set("#f59e0b");
        starStrokeSignal.set("#d97706");
        starStrokeWidthSignal.set(2);
        starOpacitySignal.set(1.0);
    }

    private void bindSliderToInteger(IntegerSlider slider,
            ValueSignal<Integer> integerSignal) {
        slider.bindValue(integerSignal, integerSignal::set);
    }

}
