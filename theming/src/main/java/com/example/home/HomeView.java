package com.example.home;

import java.util.List;

import com.example.common.BaseHomeView;
import com.example.common.UseCaseDescription;
import com.example.uc1.BrandTokensView;
import com.example.uc2.DarkModeView;
import com.example.uc3.TenantThemeView;
import com.example.uc4.UserPreferencesView;
import com.example.uc5.DensityView;
import com.example.uc6.VariantsView;
import com.example.uc7.SingleInstanceView;
import com.example.uc8.AccessibleThemingView;
import com.example.uc9.ThemeSwitchView;
import com.example.views.MainLayout;

import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Theming Use Cases")
@UseCaseDescription("Making a Vaadin application look the way its users and owners need")
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Theming — use cases",
                "Each card below changes how an application looks: a brand "
                        + "from a few design tokens, dark mode, a look per "
                        + "customer and per user, compact screens, component "
                        + "variants and one-off styling, accessibility "
                        + "settings, and the choice between Aura and Lumo. "
                        + "Appearance changes apply to every view and tab. "
                        + "API-GAPS.md records where Flow made that harder "
                        + "than it should be.");
        addMenuCards(List.of(BrandTokensView.class, DarkModeView.class,
                TenantThemeView.class, UserPreferencesView.class,
                DensityView.class, VariantsView.class, SingleInstanceView.class,
                AccessibleThemingView.class, ThemeSwitchView.class));
    }
}
