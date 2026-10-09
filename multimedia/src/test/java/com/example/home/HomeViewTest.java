package com.example.home;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.example.uc1.PrivateRecordingView;
import com.example.uc2.SeekableStreamView;
import com.example.uc3.AdaptiveStreamingView;
import com.example.uc4.FormatFallbackView;
import com.example.uc5.BackgroundVideoView;
import com.example.uc6.PodcastPlaylistView;
import com.example.uc7.ChapterControlsView;
import com.example.uc8.ResumePlaybackView;
import com.example.uc9.SubtitlesView;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.sidenav.SideNavItem;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = { HomeView.class, PrivateRecordingView.class,
        SeekableStreamView.class, AdaptiveStreamingView.class,
        FormatFallbackView.class, BackgroundVideoView.class,
        PodcastPlaylistView.class, ChapterControlsView.class,
        ResumePlaybackView.class, SubtitlesView.class })
class HomeViewTest extends SpringBrowserlessTest {

    private record UseCase(String tag, String title, String problem) {
    }

    private record Group(String heading, List<UseCase> useCases) {
    }

    private static final UseCase UC1 = new UseCase("UC1", "Private recording",
            "Serving a video only its owner can watch");
    private static final UseCase UC2 = new UseCase("UC2", "Seekable streaming",
            "Letting users scrub through a long recording");
    private static final UseCase UC3 = new UseCase("UC3", "Adaptive streaming",
            "Switching video quality to match the connection");
    private static final UseCase UC4 = new UseCase("UC4", "Format fallback",
            "Offering a modern codec with a safe fallback");
    private static final UseCase UC5 = new UseCase("UC5", "Background video",
            "Playing a silent looping video behind a headline");
    private static final UseCase UC6 = new UseCase("UC6", "Podcast playlist",
            "Playing a list of episodes one after another");
    private static final UseCase UC7 = new UseCase("UC7", "Chapters & controls",
            "Controlling playback and chapters from the server");
    private static final UseCase UC8 = new UseCase("UC8", "Resume playback",
            "Resuming playback where the user left off");
    private static final UseCase UC9 = new UseCase("UC9", "Subtitles",
            "Showing subtitles in the viewer's language");

    private static final List<UseCase> USE_CASES = List.of(UC1, UC2, UC3, UC4,
            UC5, UC6, UC7, UC8, UC9);

    @Test
    void cardsAreGroupedByKindOfProblemInMenuOrder() {
        HomeView home = navigate(HomeView.class);

        List<Group> groups = home.getChildren()
                .filter(Section.class::isInstance)
                .map(section -> new Group(
                        find(H2.class).from(section).single().getText(),
                        find(Card.class).from(section).all().stream()
                                .map(HomeViewTest::useCaseOf).toList()))
                .toList();

        assertEquals(List.of(
                new Group("Delivering the media", List.of(UC1, UC2, UC3, UC4)),
                new Group("Playing it back",
                        List.of(UC5, UC6, UC7, UC8, UC9))),
                groups);
    }

    private static UseCase useCaseOf(Card card) {
        return new UseCase(card.getHeaderPrefix().getElement().getText(),
                card.getTitleAsText(),
                card.getChildren().filter(Paragraph.class::isInstance)
                        .map(p -> ((Paragraph) p).getText()).findFirst()
                        .orElse(null));
    }

    @Test
    void navItemsKeepTheShortNameAndShowTheProblemAsTooltip() {
        navigate(HomeView.class);

        Map<String, SideNavItem> items = find(SideNavItem.class).all().stream()
                .collect(Collectors.toMap(SideNavItem::getLabel,
                        Function.identity()));

        USE_CASES.forEach(useCase -> assertEquals(useCase.problem(),
                items.get(useCase.tag() + " — " + useCase.title()).getTooltip()
                        .getText(),
                "the nav tooltip and the home card read the same text"));
    }
}
