package com.example.home;

import java.util.List;

import com.example.common.BaseHomeView;
import com.example.common.UseCaseDescription;
import com.example.uc1.MillionRowGridView;
import com.example.uc10.VirtualScrollingView;
import com.example.uc11.GridSearchLoadingView;
import com.example.uc12.StructuredSkeletonView;
import com.example.uc13.BlurHashPreviewView;
import com.example.uc2.ParallelDashboardView;
import com.example.uc3.LongRunningJobView;
import com.example.uc4.OrderDetailView;
import com.example.uc5.LatestResponseWinsView;
import com.example.uc6.DeferredSectionsView;
import com.example.uc7.ThrottledFeedView;
import com.example.uc8.OptimisticSaveView;
import com.example.uc9.SlowRequestFeedbackView;
import com.example.views.MainLayout;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Async UI & Performance Use Cases")
@UseCaseDescription("Keeping a Vaadin UI fast while the work behind it is slow")
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
        addGroup("Large data sets", VaadinIcon.TABLE, Accent.BLUE,
                List.of(MillionRowGridView.class, VirtualScrollingView.class,
                        GridSearchLoadingView.class));
        addGroup("Answering in the background", VaadinIcon.HOURGLASS,
                Accent.GREEN, List.of(ParallelDashboardView.class,
                        LongRunningJobView.class, OrderDetailView.class));
        addGroup("Building only what the user sees", VaadinIcon.EYE,
                Accent.ORANGE,
                List.of(DeferredSectionsView.class,
                        StructuredSkeletonView.class,
                        BlurHashPreviewView.class));
        addGroup("Fewer, smarter round-trips", VaadinIcon.BOLT, Accent.PURPLE,
                List.of(LatestResponseWinsView.class, ThrottledFeedView.class,
                        OptimisticSaveView.class,
                        SlowRequestFeedbackView.class));
    }
}
