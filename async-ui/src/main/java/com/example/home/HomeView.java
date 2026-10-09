package com.example.home;

import java.util.List;

import com.example.common.BaseHomeView;
import com.example.uc1.MillionRowGridView;
import com.example.uc10.VirtualScrollingView;
import com.example.uc11.GridSearchLoadingView;
import com.example.uc2.ParallelDashboardView;
import com.example.uc3.LongRunningJobView;
import com.example.uc4.OrderDetailView;
import com.example.uc5.LatestResponseWinsView;
import com.example.uc6.DeferredSectionsView;
import com.example.uc7.ThrottledFeedView;
import com.example.uc8.OptimisticSaveView;
import com.example.uc9.SlowRequestFeedbackView;
import com.example.views.MainLayout;

import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Async UI & Performance Use Cases")
@Menu(order = 0, title = "Home")
public class HomeView extends BaseHomeView {

    public HomeView() {
        super("Async UI & Performance — use cases",
                "Each card below keeps a UI fast while the work behind it is "
                        + "slow: loading large data sets page by page, "
                        + "answering in the background through server push, "
                        + "building only what the user looks at, and sending "
                        + "the browser no more than it needs. API-GAPS.md "
                        + "records where Flow made that harder than it "
                        + "should be.");
        addMenuCards(List.of(MillionRowGridView.class,
                ParallelDashboardView.class, LongRunningJobView.class,
                OrderDetailView.class, LatestResponseWinsView.class,
                DeferredSectionsView.class, ThrottledFeedView.class,
                OptimisticSaveView.class, SlowRequestFeedbackView.class,
                VirtualScrollingView.class, GridSearchLoadingView.class));
    }
}
