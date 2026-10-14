package com.example.home;

import java.util.List;

import com.example.common.BaseHomeView;
import com.example.common.UseCaseDescription;
import com.example.uc1.AddressFormView;
import com.example.uc10.ServiceDeskView;
import com.example.uc11.PackagingView;
import com.example.uc2.MoneyFieldView;
import com.example.uc3.RatingView;
import com.example.uc4.SparklineView;
import com.example.uc5.SignatureView;
import com.example.uc6.QrCodeView;
import com.example.uc7.ColorPickerView;
import com.example.uc8.StylingView;
import com.example.uc9.TestingView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Custom Components Use Cases")
@UseCaseDescription("Building your own components and an application assembled from them")
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    private static final String API_GAPS_URL = "https://github.com/vaadin/use-cases/blob/main/custom-components/API-GAPS.md";

    public HomeView() {
        super("Custom components — use cases",
                "Each card below builds a component in one of the ways "
                        + "Flow supports: composing existing components, a "
                        + "custom field, a component with its own events, "
                        + "SVG from Java, a Lit web component, an npm web "
                        + "component and a React component. Then come their "
                        + "styling API, their tests, a service desk "
                        + "assembled from them, and how they would ship as "
                        + "an add-on.");
        add(new Paragraph(new Anchor(API_GAPS_URL, "API-GAPS.md"),
                new Text(" records where Flow made that harder than it "
                        + "should be.")));
        addGroup("Built in Java", VaadinIcon.CODE, Accent.BLUE,
                List.of(AddressFormView.class, MoneyFieldView.class,
                        RatingView.class, SparklineView.class));
        addGroup("Built on web components", VaadinIcon.PUZZLE_PIECE,
                Accent.PURPLE, List.of(SignatureView.class, QrCodeView.class,
                        ColorPickerView.class));
        addGroup("Around the components", VaadinIcon.TOOLS, Accent.ORANGE,
                List.of(StylingView.class, TestingView.class,
                        ServiceDeskView.class, PackagingView.class));
    }
}
