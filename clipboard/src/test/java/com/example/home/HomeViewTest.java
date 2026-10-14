package com.example.home;

import java.util.List;
import java.util.stream.Stream;

import com.example.uc1.CopyStaticTextView;
import com.example.uc2.CopyComponentValueView;
import com.example.uc3.CopyRichContentView;
import com.example.uc4.CopyImageView;
import com.example.uc5.PasteSpreadsheetView;
import com.example.uc6.CopyFromContextMenuView;
import com.example.uc7.PasteFilesView;
import com.example.uc8.CopyFromGridView;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.RouterLink;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = { HomeView.class, CopyStaticTextView.class,
        CopyComponentValueView.class, CopyRichContentView.class,
        CopyImageView.class, PasteSpreadsheetView.class,
        CopyFromContextMenuView.class, PasteFilesView.class,
        CopyFromGridView.class })
class HomeViewTest extends SpringBrowserlessTest {

    private record UseCase(String tag, String title, String problem,
            String href) {
    }

    @Test
    void cardsListTheUseCasesInMenuOrder() {
        navigate(HomeView.class);

        assertEquals(List.of(new UseCase("UC1", "Copy static text",
                "Copying a fixed text with one click and confirming it", "uc1"),
                new UseCase("UC2", "Copy component value",
                        "Copying whatever a field holds at the moment of the click",
                        "uc2"),
                new UseCase("UC3", "Copy rich content",
                        "Copying formatted HTML with a plain-text fallback",
                        "uc3"),
                new UseCase("UC4", "Copy image",
                        "Copying an image so it can be pasted elsewhere",
                        "uc4"),
                new UseCase("UC5", "Paste a table",
                        "Pasting cells from a spreadsheet into a grid", "uc5"),
                new UseCase("UC6", "Context menu",
                        "Offering copy as a context-menu item", "uc6"),
                new UseCase("UC7", "Paste files",
                        "Receiving pasted images and files on the server",
                        "uc7"),
                new UseCase("UC8", "Copy from a grid",
                        "Putting a copy button next to every value in a grid",
                        "uc8")),
                findInView(Card.class).all().stream()
                        .map(HomeViewTest::useCaseOf).toList());
    }

    private static UseCase useCaseOf(Card card) {
        return new UseCase(card.getHeaderPrefix().getElement().getText(),
                card.getTitleAsText(),
                card.getChildren().filter(Paragraph.class::isInstance)
                        .map(p -> ((Paragraph) p).getText()).findFirst()
                        .orElse(null),
                Stream.of(card.getFooterComponents())
                        .filter(RouterLink.class::isInstance)
                        .map(link -> ((RouterLink) link).getHref()).findFirst()
                        .orElse(null));
    }
}
