package com.example;

import java.util.List;
import java.util.stream.Stream;

import com.example.uc1.MaintenanceBannerView;
import com.example.uc2.SharedCartView;
import com.example.uc3.OrderExportView;
import com.example.uc4.PriceListEditorView;
import com.example.uc5.CustomerDetailsView;
import com.example.uc6.SeatSelectionView;
import com.example.uc7.ScopePlaygroundView;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Pre;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every use case shows how it is built: only the code that uses the scope it is
 * about, not whole files.
 */
@SpringBootTest
@ViewPackages(packages = "com.example")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class HowItIsBuiltTest extends SpringBrowserlessTest {

    static Stream<Arguments> useCases() {
        return Stream.of(
                Arguments.of(MaintenanceBannerView.class,
                        List.of("@Component", "SharedValueSignal")),
                Arguments.of(SharedCartView.class,
                        List.of("@VaadinSessionScope")),
                Arguments.of(OrderExportView.class,
                        List.of("@SessionScope", "@GetMapping")),
                Arguments.of(PriceListEditorView.class, List.of("@UIScope")),
                Arguments.of(CustomerDetailsView.class, List.of("@RouteScope",
                        "@RouteScopeOwner(CustomerEditorLayout.class)")),
                Arguments.of(SeatSelectionView.class,
                        List.of("@BrowserTabScope")),
                Arguments.of(ScopePlaygroundView.class,
                        List.of("@SessionScope", "@VaadinSessionScope",
                                "@BrowserTabScope", "@UIScope",
                                "@RouteScope")));
    }

    @ParameterizedTest
    @MethodSource("useCases")
    void showsOnlyTheCodeThatUsesTheScope(Class<? extends Component> view,
            List<String> scopeCode) {
        navigate(view);
        runPendingSignalsTasks();

        assertTrue(findInView(H2.class).all().stream()
                .anyMatch(h -> "How it is built".equals(h.getText())));
        String shown = String.join("\n", findInView(Pre.class).all().stream()
                .map(Pre::getText).toList());
        scopeCode.forEach(code -> assertTrue(shown.contains(code),
                () -> "Expected the shown code to contain " + code));
        // Excerpts, not whole files.
        assertFalse(shown.contains("package com.example"));
        assertFalse(shown.lines().anyMatch(l -> l.startsWith("import ")));
    }
}
