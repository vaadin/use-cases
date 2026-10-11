package com.example.usecase17;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Use Case 17: Custom PC Builder - Complex State Management at Scale
 *
 * Demonstrates handling many signals (~70) with complex interdependencies: - 12
 * component selection signals - 40+ computed signals (price, power,
 * compatibility, performance) - 15+ validation/compatibility checks - 8 UI
 * state signals
 *
 * Key Patterns: - Scale: 70 signals vs typical 10-20 in other use cases -
 * Multi-level computed signals with dependencies - Complex cross-component
 * validation - Conditional options (filtered by previous selections) - Multiple
 * aggregation types (price, power, performance)
 */
@Route(value = "use-case-17", layout = MainLayout.class)
@PageTitle("Use Case 17: Custom PC Builder")
@UseCaseDescription("Managing a large form with dozens of dependent, validated signals")
@Menu(order = 17, title = "UC 17: PC Builder (70 signals)")
@StyleSheet("usecase17.css")
@AnonymousAllowed
public class UseCase17View extends VerticalLayout {

    // ==================== Signal Declarations (~70 total) ====================

    // 1. Component Selection Signals (12)
    private final ValueSignal<CPU> cpuSignal = new ValueSignal<>(
            PCData.ALL_CPUS.get(PCData.ALL_CPUS.size() - 1)); // None
    private final ValueSignal<Motherboard> motherboardSignal = new ValueSignal<>(
            PCData.ALL_MOTHERBOARDS.get(PCData.ALL_MOTHERBOARDS.size() - 1)); // None
    private final ValueSignal<RAM> ramSignal = new ValueSignal<>(
            PCData.ALL_RAM.get(PCData.ALL_RAM.size() - 1)); // None
    private final ValueSignal<GPU> gpuSignal = new ValueSignal<>(
            PCData.ALL_GPUS.get(PCData.ALL_GPUS.size() - 1)); // None
    private final ValueSignal<Storage> storage1Signal = new ValueSignal<>(
            PCData.ALL_STORAGE.get(PCData.ALL_STORAGE.size() - 1)); // None
    private final ValueSignal<Storage> storage2Signal = new ValueSignal<>(
            PCData.ALL_STORAGE.get(PCData.ALL_STORAGE.size() - 1)); // None
    private final ValueSignal<Storage> storage3Signal = new ValueSignal<>(
            PCData.ALL_STORAGE.get(PCData.ALL_STORAGE.size() - 1)); // None
    private final ValueSignal<PSU> psuSignal = new ValueSignal<>(
            PCData.ALL_PSUS.get(PCData.ALL_PSUS.size() - 1)); // None
    private final ValueSignal<Case> caseSignal = new ValueSignal<>(
            PCData.ALL_CASES.get(PCData.ALL_CASES.size() - 1)); // None
    private final ValueSignal<Cooler> coolerSignal = new ValueSignal<>(
            PCData.ALL_COOLERS.get(PCData.ALL_COOLERS.size() - 1)); // None

    // 2. Computed Price Signals (10)
    private Signal<BigDecimal> cpuPriceSignal;
    private Signal<BigDecimal> motherboardPriceSignal;
    private Signal<BigDecimal> ramPriceSignal;
    private Signal<BigDecimal> gpuPriceSignal;
    private Signal<BigDecimal> storage1PriceSignal;
    private Signal<BigDecimal> storage2PriceSignal;
    private Signal<BigDecimal> storage3PriceSignal;
    private Signal<BigDecimal> psuPriceSignal;
    private Signal<BigDecimal> casePriceSignal;
    private Signal<BigDecimal> coolerPriceSignal;
    private Signal<BigDecimal> totalPriceSignal;

    // 3. Computed Power Signals (6)
    private Signal<Integer> cpuPowerSignal;
    private Signal<Integer> gpuPowerSignal;
    private Signal<Integer> totalPowerSignal;
    private Signal<Integer> recommendedPsuWattageSignal;
    private Signal<Boolean> psuSufficiencySignal;
    private Signal<Integer> powerMarginSignal;

    // 4. Compatibility Check Signals (15)
    private Signal<Boolean> cpuSocketMatchSignal;
    private Signal<Boolean> ramTypeMatchSignal;
    private Signal<Boolean> ramSpeedSupportedSignal;
    private Signal<Boolean> gpuFitsCaseSignal;
    private Signal<Boolean> coolerFitsCaseSignal;
    private Signal<Boolean> motherboardFitsCaseSignal;
    private Signal<Boolean> m2SlotsAvailableSignal;
    private Signal<Boolean> sataSlotsAvailableSignal;
    private Signal<Boolean> psuFitsCaseSignal;
    private Signal<Boolean> cpuCoolerCompatibleSignal;
    private Signal<Boolean> ramCapacitySupportedSignal;
    private Signal<Boolean> coolerTdpSufficientSignal;
    private Signal<Boolean> allCompatibleSignal;
    private Signal<Boolean> hasCriticalIssuesSignal;
    private Signal<Integer> compatibilityCheckCountSignal;

    // 5. Performance Estimate Signals (8)
    private Signal<Integer> cpuScoreSignal;
    private Signal<Integer> gpuScoreSignal;
    private Signal<Integer> storageSpeedSignal;
    private Signal<Integer> overallScoreSignal;
    private Signal<Integer> gamingScoreSignal;
    private Signal<Integer> productivityScoreSignal;
    private Signal<String> bottleneckSignal;
    private Signal<String> performanceRatingSignal;

    // 6. Validation & Warning Signals (5)
    private Signal<List<String>> missingComponentsSignal;
    private Signal<Integer> warningCountSignal;
    private Signal<Boolean> canBuildSignal;
    private Signal<List<String>> warningMessagesSignal;
    private Signal<Boolean> isValidConfigurationSignal;

    // 7. UI State Signals (4)
    private final ValueSignal<Boolean> showCompatibilityDetailsSignal = new ValueSignal<>(
            true);
    private final ValueSignal<Boolean> showPerformanceDetailsSignal = new ValueSignal<>(
            true);

    public UseCase17View() {
        addClassName("usecase17-view");
        setSpacing(true);
        setPadding(true);
        setWidthFull();

        // Initialize all computed signals
        initializePriceSignals();
        initializePowerSignals();
        initializeCompatibilitySignals();
        initializePerformanceSignals();
        initializeValidationSignals();

        // Build UI
        buildLayout();
    }

    // ==================== Signal Initialization ====================

    private void initializePriceSignals() {
        cpuPriceSignal = cpuSignal
                .map(cpu -> cpu != null ? cpu.getPrice() : BigDecimal.ZERO);
        motherboardPriceSignal = motherboardSignal
                .map(mb -> mb != null ? mb.getPrice() : BigDecimal.ZERO);
        ramPriceSignal = ramSignal
                .map(ram -> ram != null ? ram.getPrice() : BigDecimal.ZERO);
        gpuPriceSignal = gpuSignal
                .map(gpu -> gpu != null ? gpu.getPrice() : BigDecimal.ZERO);
        storage1PriceSignal = storage1Signal
                .map(s -> s != null ? s.getPrice() : BigDecimal.ZERO);
        storage2PriceSignal = storage2Signal
                .map(s -> s != null ? s.getPrice() : BigDecimal.ZERO);
        storage3PriceSignal = storage3Signal
                .map(s -> s != null ? s.getPrice() : BigDecimal.ZERO);
        psuPriceSignal = psuSignal
                .map(psu -> psu != null ? psu.getPrice() : BigDecimal.ZERO);
        casePriceSignal = caseSignal
                .map(c -> c != null ? c.getPrice() : BigDecimal.ZERO);
        coolerPriceSignal = coolerSignal.map(
                cooler -> cooler != null ? cooler.getPrice() : BigDecimal.ZERO);

        totalPriceSignal = Signal.computed(() -> cpuPriceSignal.get()
                .add(motherboardPriceSignal.get()).add(ramPriceSignal.get())
                .add(gpuPriceSignal.get()).add(storage1PriceSignal.get())
                .add(storage2PriceSignal.get()).add(storage3PriceSignal.get())
                .add(psuPriceSignal.get()).add(casePriceSignal.get())
                .add(coolerPriceSignal.get()));
    }

    private void initializePowerSignals() {
        cpuPowerSignal = cpuSignal.map(cpu -> cpu != null ? cpu.getTdp() : 0);
        gpuPowerSignal = gpuSignal
                .map(gpu -> gpu != null ? gpu.getPowerConsumption() : 0);

        totalPowerSignal = Signal
                .computed(() -> cpuPowerSignal.get() + gpuPowerSignal.get() + 50 // Motherboard,
                                                                                 // RAM,
                                                                                 // storage,
                                                                                 // fans
                                                                                 // (estimated)
                );

        recommendedPsuWattageSignal = totalPowerSignal
                .map(power -> (int) (power * 1.3) // 30% headroom
                );

        psuSufficiencySignal = Signal.computed(() -> {
            PSU psu = psuSignal.get();
            if (psu == null)
                return false;
            return psu.getWattage() >= totalPowerSignal.get();
        });

        powerMarginSignal = Signal.computed(() -> {
            PSU psu = psuSignal.get();
            if (psu == null)
                return 0;
            return psu.getWattage() - totalPowerSignal.get();
        });
    }

    private void initializeCompatibilitySignals() {
        cpuSocketMatchSignal = Signal.computed(() -> {
            CPU cpu = cpuSignal.get();
            Motherboard mobo = motherboardSignal.get();
            if (cpu == null || mobo == null || "None".equals(cpu.getName())
                    || "None".equals(mobo.getName()))
                return true;
            return cpu.getSocket().equals(mobo.getSocket());
        });

        ramTypeMatchSignal = Signal.computed(() -> {
            RAM ram = ramSignal.get();
            Motherboard mobo = motherboardSignal.get();
            if (ram == null || mobo == null)
                return true;
            return ram.getType().equals(mobo.getRamType());
        });

        ramSpeedSupportedSignal = Signal.computed(() -> {
            RAM ram = ramSignal.get();
            Motherboard mobo = motherboardSignal.get();
            if (ram == null || mobo == null)
                return true;
            return ram.getSpeed() <= mobo.getMaxRamSpeed();
        });

        ramCapacitySupportedSignal = Signal.computed(() -> {
            RAM ram = ramSignal.get();
            Motherboard mobo = motherboardSignal.get();
            if (ram == null || mobo == null)
                return true;
            return ram.getCapacity() <= mobo.getMaxRamCapacity();
        });

        gpuFitsCaseSignal = Signal.computed(() -> {
            GPU gpu = gpuSignal.get();
            Case pc = caseSignal.get();
            if (gpu == null || pc == null)
                return true;
            return gpu.getLengthMm() <= pc.getGpuClearanceMm();
        });

        coolerFitsCaseSignal = Signal.computed(() -> {
            Cooler cooler = coolerSignal.get();
            Case pc = caseSignal.get();
            if (cooler == null || pc == null)
                return true;
            return cooler.getHeightMm() <= pc.getCpuCoolerClearanceMm();
        });

        motherboardFitsCaseSignal = Signal.computed(() -> {
            Motherboard mobo = motherboardSignal.get();
            Case pc = caseSignal.get();
            if (mobo == null || pc == null)
                return true;
            return mobo.getFormFactor().equals(pc.getFormFactor())
                    || pc.getFormFactor().equals("ATX"); // ATX cases fit all
        });

        m2SlotsAvailableSignal = Signal.computed(() -> {
            Motherboard mobo = motherboardSignal.get();
            Storage stor1 = storage1Signal.get();
            if (mobo == null)
                return true;
            int m2Count = 0;
            if (stor1 != null && stor1.getType().contains("NVMe"))
                m2Count++;
            return m2Count <= mobo.getM2Slots();
        });

        sataSlotsAvailableSignal = Signal.computed(() -> {
            Motherboard mobo = motherboardSignal.get();
            if (mobo == null)
                return true;
            Storage stor2 = storage2Signal.get();
            Storage stor3 = storage3Signal.get();
            int sataCount = 0;
            if (stor2 != null && stor2.getType().contains("SATA"))
                sataCount++;
            if (stor3 != null && stor3.getType().contains("SATA"))
                sataCount++;
            return sataCount <= mobo.getSataSlots();
        });

        psuFitsCaseSignal = Signal.computed(() -> {
            PSU psu = psuSignal.get();
            Case pc = caseSignal.get();
            if (psu == null || pc == null)
                return true;
            return psu.getFormFactor().equals(pc.getPsuFormFactor());
        });

        cpuCoolerCompatibleSignal = Signal.computed(() -> {
            CPU cpu = cpuSignal.get();
            Cooler cooler = coolerSignal.get();
            if (cpu == null || cooler == null)
                return true;
            return Arrays.asList(cooler.getCompatibleSockets())
                    .contains(cpu.getSocket());
        });

        coolerTdpSufficientSignal = Signal.computed(() -> {
            CPU cpu = cpuSignal.get();
            Cooler cooler = coolerSignal.get();
            if (cpu == null || cooler == null)
                return true;
            return cooler.getMaxTdp() >= cpu.getTdp();
        });

        allCompatibleSignal = Signal.computed(() -> cpuSocketMatchSignal.get()
                && ramTypeMatchSignal.get() && ramSpeedSupportedSignal.get()
                && ramCapacitySupportedSignal.get() && gpuFitsCaseSignal.get()
                && coolerFitsCaseSignal.get() && motherboardFitsCaseSignal.get()
                && m2SlotsAvailableSignal.get()
                && sataSlotsAvailableSignal.get() && psuFitsCaseSignal.get()
                && cpuCoolerCompatibleSignal.get()
                && coolerTdpSufficientSignal.get()
                && psuSufficiencySignal.get());

        hasCriticalIssuesSignal = allCompatibleSignal
                .map(compatible -> !compatible);

        compatibilityCheckCountSignal = Signal.computed(() -> {
            int passed = 0;
            if (cpuSocketMatchSignal.get())
                passed++;
            if (ramTypeMatchSignal.get())
                passed++;
            if (ramSpeedSupportedSignal.get())
                passed++;
            if (gpuFitsCaseSignal.get())
                passed++;
            if (coolerFitsCaseSignal.get())
                passed++;
            if (motherboardFitsCaseSignal.get())
                passed++;
            if (m2SlotsAvailableSignal.get())
                passed++;
            if (sataSlotsAvailableSignal.get())
                passed++;
            if (psuFitsCaseSignal.get())
                passed++;
            if (cpuCoolerCompatibleSignal.get())
                passed++;
            if (coolerTdpSufficientSignal.get())
                passed++;
            if (ramCapacitySupportedSignal.get())
                passed++;
            if (psuSufficiencySignal.get())
                passed++;
            return passed;
        });
    }

    private void initializePerformanceSignals() {
        cpuScoreSignal = cpuSignal.map(cpu -> cpu != null ? cpu.getScore() : 0);
        gpuScoreSignal = gpuSignal.map(gpu -> gpu != null ? gpu.getScore() : 0);
        storageSpeedSignal = storage1Signal
                .map(s -> s != null ? s.getSpeedMBps() : 0);

        overallScoreSignal = Signal
                .computed(() -> (int) (cpuScoreSignal.get() * 0.3
                        + gpuScoreSignal.get() * 0.6
                        + storageSpeedSignal.get() / 100.0 * 0.1));

        gamingScoreSignal = Signal
                .computed(() -> (int) (cpuScoreSignal.get() * 0.2
                        + gpuScoreSignal.get() * 0.8));

        productivityScoreSignal = Signal
                .computed(() -> (int) (cpuScoreSignal.get() * 0.6
                        + gpuScoreSignal.get() * 0.3
                        + storageSpeedSignal.get() / 100.0 * 0.1));

        bottleneckSignal = Signal.computed(() -> {
            int cpuScore = cpuScoreSignal.get();
            int gpuScore = gpuScoreSignal.get();
            if (cpuScore == 0 || gpuScore == 0)
                return "N/A";
            if (cpuScore < gpuScore * 0.6)
                return "CPU";
            if (gpuScore < cpuScore * 0.6)
                return "GPU";
            return "Balanced";
        });

        performanceRatingSignal = overallScoreSignal.map(score -> {
            if (score >= 85)
                return "Enthusiast";
            if (score >= 70)
                return "High-End";
            if (score >= 50)
                return "Mid-Range";
            if (score >= 30)
                return "Entry-Level";
            return "Budget";
        });
    }

    private void initializeValidationSignals() {
        missingComponentsSignal = Signal.computed(() -> {
            List<String> missing = new java.util.ArrayList<>();
            if (cpuSignal.get() == null)
                missing.add("CPU");
            if (motherboardSignal.get() == null)
                missing.add("Motherboard");
            if (ramSignal.get() == null)
                missing.add("RAM");
            if (gpuSignal.get() == null)
                missing.add("GPU");
            if (storage1Signal.get() == null)
                missing.add("Primary Storage");
            if (psuSignal.get() == null)
                missing.add("Power Supply");
            if (caseSignal.get() == null)
                missing.add("Case");
            if (coolerSignal.get() == null)
                missing.add("CPU Cooler");
            return missing;
        });

        warningMessagesSignal = Signal.computed(() -> {
            List<String> warnings = new java.util.ArrayList<>();
            if (!cpuSocketMatchSignal.get())
                warnings.add("⚠ CPU socket doesn't match motherboard");
            if (!ramTypeMatchSignal.get())
                warnings.add("⚠ RAM type doesn't match motherboard");
            if (!ramSpeedSupportedSignal.get())
                warnings.add("⚠ RAM speed not supported by motherboard");
            if (!gpuFitsCaseSignal.get())
                warnings.add("⚠ GPU too long for case");
            if (!coolerFitsCaseSignal.get())
                warnings.add("⚠ CPU cooler too tall for case");
            if (!psuSufficiencySignal.get() && psuSignal.get() != null)
                warnings.add("⚠ PSU wattage insufficient");
            if (!cpuCoolerCompatibleSignal.get())
                warnings.add("⚠ Cooler not compatible with CPU socket");
            if (!coolerTdpSufficientSignal.get())
                warnings.add("⚠ Cooler TDP rating insufficient for CPU");
            return warnings;
        });

        warningCountSignal = warningMessagesSignal.map(List::size);

        canBuildSignal = Signal
                .computed(() -> missingComponentsSignal.get().isEmpty()
                        && allCompatibleSignal.get());

        isValidConfigurationSignal = canBuildSignal;
    }

    // ==================== UI Layout ====================

    private void buildLayout() {
        H2 title = new H2("Use Case 17: Custom PC Builder (70 Signals)");

        Paragraph description = new Paragraph(
                "This use case demonstrates handling complex state at scale with ~70 interdependent signals. "
                        + "Select components to build a custom PC. Watch how compatibility checks, price calculations, "
                        + "power requirements, and performance estimates all update reactively as you make selections.");

        // Signal count display
        Div signalCountBox = new Div();
        signalCountBox.addClassName("signal-count-box");
        signalCountBox.add(new Span(
                "📊 Active Signals: 12 component selections + 40+ computed values + 15 compatibility checks + 8 performance metrics = ~70 total signals"));

        // Main content: 3-column layout
        HorizontalLayout mainLayout = new HorizontalLayout();
        mainLayout.setWidthFull();
        mainLayout.setSpacing(true);

        // Left column: Component selection
        VerticalLayout selectionColumn = buildSelectionColumn();
        selectionColumn.setWidth("400px");

        // Center column: Build summary
        VerticalLayout summaryColumn = buildSummaryColumn();
        summaryColumn.setWidth("300px");

        // Right column: Statistics
        VerticalLayout statsColumn = buildStatsColumn();
        statsColumn.setWidth("350px");

        mainLayout.add(selectionColumn, summaryColumn, statsColumn);

        // Bottom: Compatibility checks
        Div compatibilitySection = buildCompatibilitySection();

        // Actions
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);

        Button resetButton = new Button("Reset Build", event -> resetBuild());
        resetButton.addThemeName("tertiary");

        Button addToCartButton = new Button("Add to Cart");
        addToCartButton.addThemeVariants(ButtonVariant.PRIMARY);
        addToCartButton.bindEnabled(canBuildSignal);

        Signal<String> cartButtonText = Signal.computed(() -> {
            if (canBuildSignal.get()) {
                return "Add to Cart ($" + totalPriceSignal.get().setScale(0,
                        RoundingMode.HALF_UP) + ")";
            }
            return "Complete Build First";
        });
        addToCartButton.bindText(cartButtonText);

        actions.add(resetButton, addToCartButton);

        add(title, description, signalCountBox, mainLayout,
                compatibilitySection, actions);
    }

    private VerticalLayout buildSelectionColumn() {
        VerticalLayout column = new VerticalLayout();
        column.setSpacing(false);
        column.setPadding(false);

        H3 header = new H3("Component Selection");
        header.addClassName("column-header");

        column.add(header);
        column.add(createComponentSelector("CPU", PCData.ALL_CPUS, cpuSignal));
        column.add(createComponentSelector("Motherboard",
                PCData.ALL_MOTHERBOARDS, motherboardSignal));
        column.add(createComponentSelector("RAM", PCData.ALL_RAM, ramSignal));
        column.add(createComponentSelector("GPU", PCData.ALL_GPUS, gpuSignal));
        column.add(createComponentSelector("Primary Storage",
                PCData.ALL_STORAGE, storage1Signal));
        column.add(createComponentSelector("Secondary Storage",
                PCData.ALL_STORAGE, storage2Signal));
        column.add(createComponentSelector("PSU", PCData.ALL_PSUS, psuSignal));
        column.add(
                createComponentSelector("Case", PCData.ALL_CASES, caseSignal));
        column.add(createComponentSelector("CPU Cooler", PCData.ALL_COOLERS,
                coolerSignal));

        return column;
    }

    private <T extends Component> ComboBox<T> createComponentSelector(
            String label, List<T> items, ValueSignal<T> signal) {
        ComboBox<T> combo = new ComboBox<>(label);
        combo.setItems(items);
        combo.setItemLabelGenerator(Component::getName);
        combo.setWidthFull();
        combo.bindValue(signal, signal::set);
        return combo;
    }

    private VerticalLayout buildSummaryColumn() {
        VerticalLayout column = new VerticalLayout();
        column.setSpacing(false);
        column.setPadding(false);

        H3 header = new H3("Build Summary");
        header.addClassName("column-header");

        Div summary = new Div();
        summary.addClassName("summary-box");

        ListSignal<Component> selectedComponentsSignal = new ListSignal<>();

        Signal.effect(summary, () -> {
            selectedComponentsSignal.clear();
            if (cpuSignal.get() != null)
                selectedComponentsSignal.insertLast(cpuSignal.get());
            if (motherboardSignal.get() != null)
                selectedComponentsSignal.insertLast(motherboardSignal.get());
            if (ramSignal.get() != null)
                selectedComponentsSignal.insertLast(ramSignal.get());
            if (gpuSignal.get() != null)
                selectedComponentsSignal.insertLast(gpuSignal.get());
            if (storage1Signal.get() != null)
                selectedComponentsSignal.insertLast(storage1Signal.get());
            if (storage2Signal.get() != null && storage2Signal.get().getPrice()
                    .compareTo(BigDecimal.ZERO) > 0)
                selectedComponentsSignal.insertLast(storage2Signal.get());
            if (psuSignal.get() != null)
                selectedComponentsSignal.insertLast(psuSignal.get());
            if (caseSignal.get() != null)
                selectedComponentsSignal.insertLast(caseSignal.get());
            if (coolerSignal.get() != null)
                selectedComponentsSignal.insertLast(coolerSignal.get());
        });

        summary.bindChildren(selectedComponentsSignal,
                this::createComponentSummaryItem);

        column.add(header, summary);
        return column;
    }

    private VerticalLayout buildStatsColumn() {
        VerticalLayout column = new VerticalLayout();
        column.setSpacing(true);
        column.setPadding(false);

        H3 header = new H3("Statistics");
        header.addClassName("column-header");

        // Price box
        Div priceBox = createStatBox("Total Price",
                totalPriceSignal
                        .map(p -> "$" + p.setScale(0, RoundingMode.HALF_UP)),
                "price-box");

        // Power box
        Div powerBox = new Div();
        powerBox.addClassName("stat-box");
        powerBox.addClassName("power-box");

        Span powerLabel = new Span("Power Consumption");
        powerLabel.addClassName("stat-label");

        Span powerValue = new Span(totalPowerSignal.map(p -> p + "W total"));
        powerValue.addClassName("power-value");

        Signal<String> psuStatusText = Signal.computed(() -> {
            PSU psu = psuSignal.get();
            if (psu == null)
                return "No PSU selected";
            boolean sufficient = psuSufficiencySignal.get();
            int margin = powerMarginSignal.get();
            return psu.getWattage() + "W PSU: "
                    + (sufficient ? "✓ OK (+" + margin + "W)"
                            : "⚠ Insufficient");
        });
        Span psuStatus = new Span(psuStatusText);
        psuStatus.addClassName("power-value");

        powerBox.add(powerLabel, powerValue, psuStatus);

        // Compatibility box (color depends on compatibility signal)
        Div compatBox = createStatBox("Compatibility",
                compatibilityCheckCountSignal.map(
                        count -> count + "/13 checks passing"),
                "compat-box");
        compatBox.getClassNames().bind("is-ok", allCompatibleSignal);

        // Performance box
        Div perfBox = new Div();
        perfBox.addClassName("stat-box");
        perfBox.addClassName("perf-box");

        Span perfLabel = new Span("Performance");
        perfLabel.addClassName("stat-label");

        Span perfRating = new Span(performanceRatingSignal);
        perfRating.addClassName("perf-rating");

        Span perfGaming = new Span(
                gamingScoreSignal.map(s -> "Gaming: " + s + "/100"));
        perfGaming.addClassName("perf-detail");

        Span perfBottleneck = new Span(
                bottleneckSignal.map(b -> "Bottleneck: " + b));
        perfBottleneck.addClassName("perf-detail");

        perfBox.add(perfLabel, perfRating, perfGaming, perfBottleneck);

        column.add(header, priceBox, powerBox, compatBox, perfBox);
        return column;
    }

    private Div createStatBox(String label, Signal<String> valueSignal,
            String variantClass) {
        Div box = new Div();
        box.addClassName("stat-box");
        box.addClassName(variantClass);

        Span labelSpan = new Span(label);
        labelSpan.addClassName("stat-label");

        Span valueSpan = new Span(valueSignal);
        valueSpan.addClassName("stat-value");

        box.add(labelSpan, valueSpan);
        return box;
    }

    private Div buildCompatibilitySection() {
        Div section = new Div();
        section.addClassName("compatibility-section");

        H3 header = new H3("Compatibility Checks");
        header.addClassName("column-header");

        Div checksContainer = new Div();

        ListSignal<String> compatibilityStatusSignal = new ListSignal<>();

        Signal.effect(checksContainer, () -> {
            compatibilityStatusSignal.clear();
            compatibilityStatusSignal
                    .insertLast(formatCheck("CPU socket matches motherboard",
                            cpuSocketMatchSignal.get()));
            compatibilityStatusSignal.insertLast(formatCheck(
                    "RAM type matches motherboard", ramTypeMatchSignal.get()));
            compatibilityStatusSignal.insertLast(formatCheck(
                    "RAM speed supported", ramSpeedSupportedSignal.get()));
            compatibilityStatusSignal.insertLast(
                    formatCheck("GPU fits in case", gpuFitsCaseSignal.get()));
            compatibilityStatusSignal.insertLast(formatCheck(
                    "CPU cooler fits in case", coolerFitsCaseSignal.get()));
            compatibilityStatusSignal
                    .insertLast(formatCheck("Motherboard fits in case",
                            motherboardFitsCaseSignal.get()));
            compatibilityStatusSignal.insertLast(formatCheck(
                    "M.2 slots available", m2SlotsAvailableSignal.get()));
            compatibilityStatusSignal.insertLast(formatCheck(
                    "SATA ports available", sataSlotsAvailableSignal.get()));
            compatibilityStatusSignal.insertLast(
                    formatCheck("PSU fits in case", psuFitsCaseSignal.get()));
            compatibilityStatusSignal
                    .insertLast(formatCheck("Cooler compatible with CPU",
                            cpuCoolerCompatibleSignal.get()));
            compatibilityStatusSignal.insertLast(formatCheck(
                    "Cooler TDP sufficient", coolerTdpSufficientSignal.get()));
            compatibilityStatusSignal
                    .insertLast(formatCheck("RAM capacity supported",
                            ramCapacitySupportedSignal.get()));
            compatibilityStatusSignal.insertLast(formatCheck(
                    "PSU wattage sufficient", psuSufficiencySignal.get()));
        });

        checksContainer.bindChildren(compatibilityStatusSignal,
                this::createCompatibilityCheckDiv);

        section.add(header, checksContainer);
        return section;
    }

    private Div createComponentSummaryItem(ValueSignal<Component> compSignal) {
        Div item = new Div();
        item.addClassName("component-summary-item");

        Span name = new Span(() -> compSignal.get().getName());
        name.addClassName("component-summary-name");

        Span price = new Span(() -> "$" + compSignal.get().getPrice()
                .setScale(0, RoundingMode.HALF_UP));
        price.addClassName("component-summary-price");

        item.add(name, price);
        return item;
    }

    private Div createCompatibilityCheckDiv(ValueSignal<String> statusSignal) {
        Div checkDiv = new Div();
        checkDiv.addClassName("compatibility-check-row");
        checkDiv.getElement().bindProperty("innerHTML", statusSignal, null);
        return checkDiv;
    }

    private String formatCheck(String label, boolean passes) {
        String icon = passes ? "✓" : "✗";
        String color = passes ? "var(--aura-green)" : "var(--aura-red)";
        return "<span style='color: " + color + "; font-weight: bold;'>" + icon
                + "</span> " + label;
    }

    private void resetBuild() {
        cpuSignal.set(PCData.ALL_CPUS.get(PCData.ALL_CPUS.size() - 1)); // None
        motherboardSignal.set(PCData.ALL_MOTHERBOARDS
                .get(PCData.ALL_MOTHERBOARDS.size() - 1)); // None
        ramSignal.set(PCData.ALL_RAM.get(PCData.ALL_RAM.size() - 1)); // None
        gpuSignal.set(PCData.ALL_GPUS.get(PCData.ALL_GPUS.size() - 1)); // None
        storage1Signal
                .set(PCData.ALL_STORAGE.get(PCData.ALL_STORAGE.size() - 1)); // None
        storage2Signal
                .set(PCData.ALL_STORAGE.get(PCData.ALL_STORAGE.size() - 1)); // None
        storage3Signal
                .set(PCData.ALL_STORAGE.get(PCData.ALL_STORAGE.size() - 1)); // None
        psuSignal.set(PCData.ALL_PSUS.get(PCData.ALL_PSUS.size() - 1)); // None
        caseSignal.set(PCData.ALL_CASES.get(PCData.ALL_CASES.size() - 1)); // None
        coolerSignal.set(PCData.ALL_COOLERS.get(PCData.ALL_COOLERS.size() - 1)); // None
    }
}
