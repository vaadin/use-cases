package com.example.views;

import java.util.List;

import com.example.common.BaseHomeView;
import com.example.muc01.MUC01View;
import com.example.muc02.MUC02View;
import com.example.muc03.MUC03View;
import com.example.muc04.MUC04View;
import com.example.muc06.MUC06View;
import com.example.muc07.MUC07View;
import com.example.muc08.MUC08View;
import com.example.usecase01.UseCase01View;
import com.example.usecase02.UseCase02View;
import com.example.usecase03.UseCase03View;
import com.example.usecase04.UseCase04View;
import com.example.usecase05.UseCase05View;
import com.example.usecase06.UseCase06View;
import com.example.usecase08.UseCase08View;
import com.example.usecase09.UseCase09View;
import com.example.usecase10.UseCase10View;
import com.example.usecase11.UseCase11View;
import com.example.usecase13.UseCase13View;
import com.example.usecase14.UseCase14View;
import com.example.usecase15.UseCase15View;
import com.example.usecase16.UseCase16View;
import com.example.usecase17.UseCase17View;
import com.example.usecase18.UseCase18View;
import com.example.usecase19.UseCase19View;
import com.example.usecase20.UseCase20View;
import com.example.usecase21.UseCase21View;
import com.example.usecase22.UseCase22View;
import com.example.usecase23.UseCase23View;
import com.example.usecase24.UseCase24View;
import com.example.usecase25.UseCase25View;
import com.example.usecase26.UseCase26View;
import com.example.usecase27.UseCase27View;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Signal API Use Cases - Home")
@Menu(order = 0, title = "Home")
@AnonymousAllowed
public class HomeView extends BaseHomeView {

    private static final String DOCS_URL = "https://vaadin.com/docs/latest/flow/ui-state";

    public HomeView() {
        super("Signals — use cases",
                "Signals are reactive state holders: bind a component to one "
                        + "and the UI updates whenever its value changes, with "
                        + "no listeners to register or clean up. Each card below "
                        + "is one problem a developer solves with signals, from "
                        + "a single enabled button to many users sharing state.");
        add(new Paragraph(new Text("Logging in is optional; the multi-user "
                + "use cases are easiest to try in two browsers. For the API "
                + "itself, see the "),
                new Anchor(DOCS_URL, "UI state management documentation"),
                new Text(".")));
        addGroup("Reactive UI basics", VaadinIcon.BOLT, Accent.BLUE,
                List.of(UseCase01View.class, UseCase02View.class,
                        UseCase03View.class, UseCase10View.class,
                        UseCase11View.class, UseCase26View.class));
        addGroup("Forms and derived state", VaadinIcon.EDIT, Accent.GREEN,
                List.of(UseCase05View.class, UseCase08View.class,
                        UseCase09View.class, UseCase17View.class,
                        UseCase22View.class));
        addGroup("Lists and live data", VaadinIcon.LIST, Accent.ORANGE,
                List.of(UseCase04View.class, UseCase06View.class,
                        UseCase23View.class, UseCase24View.class,
                        UseCase25View.class));
        addGroup("Async loading and AI", VaadinIcon.HOURGLASS, Accent.PURPLE,
                List.of(UseCase14View.class, UseCase15View.class,
                        UseCase18View.class, UseCase19View.class));
        addGroup("Session, URL and app-wide state", VaadinIcon.COGS, Accent.RED,
                List.of(UseCase13View.class, UseCase16View.class,
                        UseCase20View.class, UseCase21View.class,
                        UseCase27View.class));
        addGroup("Multi-user collaboration", VaadinIcon.USERS, Accent.YELLOW,
                List.of(MUC01View.class, MUC02View.class, MUC03View.class,
                        MUC04View.class, MUC06View.class, MUC07View.class,
                        MUC08View.class));
    }
}
