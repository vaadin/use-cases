package com.example.uc11;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.example.common.UseCaseDescription;
import com.example.components.AddressForm;
import com.example.components.ColorPicker;
import com.example.components.MoneyField;
import com.example.components.QrCode;
import com.example.components.RatingStars;
import com.example.components.SignaturePad;
import com.example.components.Sparkline;
import com.example.views.MainLayout;
import com.example.views.Sample;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC11 — Packaging as an add-on.
 * <p>
 * The components in {@code com.example.components} are laid out the way an
 * add-on jar is: Java classes, their stylesheets in {@code META-INF/resources},
 * their TypeScript in {@code META-INF/resources/frontend}, and npm dependencies
 * declared with {@code @NpmPackage} on the classes that need them. Moved into a
 * jar of their own, they would work in another application unchanged: Vaadin
 * copies the frontend files out of the jar, adds the npm packages and bundles
 * them when that application builds.
 * <p>
 * The view lists what each component brings, read from its annotations and
 * checked against the classpath.
 */
@Route(value = "uc11", layout = MainLayout.class)
@PageTitle("UC11 — Packaging as an add-on")
@UseCaseDescription("Laying out components so they can ship as a reusable add-on jar")
@Menu(order = 11, title = "UC11 — Packaging as an add-on")
public class PackagingView extends VerticalLayout {

    static final List<Class<? extends Component>> COMPONENTS = List.of(
            AddressForm.class, MoneyField.class, RatingStars.class,
            Sparkline.class, SignaturePad.class, QrCode.class,
            ColorPicker.class);

    static final String LAYOUT = """
            my-components.jar
            ├── com/example/components/*.class
            └── META-INF/resources/
                ├── rating-stars.css          (@StyleSheet)
                ├── sparkline.css             (@StyleSheet)
                └── frontend/
                    ├── signature-pad.ts      (@JsModule, Lit)
                    └── color-picker.tsx      (@JsModule, React)
            npm, declared in the classes:
                webcomponent-qr-code, react-colorful  (@NpmPackage)""";

    private final UnorderedList inventory = new UnorderedList();

    public PackagingView() {
        add(new H1("UC11 — Packaging as an add-on"));
        add(new Paragraph("What each component brings with it, and where it "
                + "lives in the jar."));

        COMPONENTS.forEach(type -> inventory.add(new ListItem(describe(type))));
        add(new Sample("Add-on layout", new Pre(LAYOUT)),
                new Sample("Components", inventory));
    }

    /**
     * One line per component: its own stylesheets, modules and npm packages
     * (not the ones it inherits from the Vaadin component it extends), each
     * marked missing if the resource is not on the classpath.
     */
    static String describe(Class<? extends Component> type) {
        Stream<String> sheets = Arrays
                .stream(type.getDeclaredAnnotationsByType(StyleSheet.class))
                .map(sheet -> sheet.value()
                        + present("META-INF/resources/" + sheet.value()));
        Stream<String> modules = Arrays
                .stream(type.getDeclaredAnnotationsByType(JsModule.class))
                .map(module -> module.value() + (module.value().startsWith("./")
                        ? present("META-INF/resources/frontend/"
                                + module.value().substring(2))
                        : " (npm)"));
        Stream<String> packages = Arrays
                .stream(type.getDeclaredAnnotationsByType(NpmPackage.class))
                .map(npm -> "npm " + npm.value() + "@" + npm.version());
        List<String> parts = Stream.of(sheets, modules, packages)
                .flatMap(s -> s).toList();
        return type.getSimpleName() + ": "
                + (parts.isEmpty() ? "Java only" : String.join(", ", parts));
    }

    private static String present(String resource) {
        return PackagingView.class.getClassLoader()
                .getResource(resource) == null ? " (MISSING)" : "";
    }

    // Package-private test seam.
    List<String> inventory() {
        return inventory.getChildren().map(item -> item.getElement().getText())
                .toList();
    }
}
