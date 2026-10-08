package com.example.home;

import java.util.List;
import java.util.stream.Stream;

import com.example.uc1.DownloadInvoiceView;
import com.example.uc2.OpenInNewTabView;
import com.example.uc3.PreviewInvoiceView;
import com.example.uc4.BillingRunView;
import com.example.uc5.DeliveryReceiptView;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.RouterLink;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = { HomeView.class, DownloadInvoiceView.class,
        OpenInNewTabView.class, PreviewInvoiceView.class, BillingRunView.class,
        DeliveryReceiptView.class })
class HomeViewTest extends SpringBrowserlessTest {

    private record UseCase(String tag, String title, String problem,
            String href) {
    }

    private static final List<UseCase> USE_CASES = List.of(
            new UseCase("UC1", "Download the invoice",
                    "Generating a PDF and letting the user download it", "uc1"),
            new UseCase("UC2", "Open it in a new tab",
                    "Opening a generated PDF in a new browser tab", "uc2"),
            new UseCase("UC3", "Check it before sending",
                    "Previewing the real document before it is sent", "uc3"),
            new UseCase("UC4", "The monthly billing run",
                    "Generating a month of invoices with progress", "uc4"),
            new UseCase("UC5", "Know that it arrived",
                    "Marking an invoice sent only once it has arrived", "uc5"));

    @Test
    void cardsListTheUseCasesInMenuOrder() {
        navigate(HomeView.class);

        assertEquals(USE_CASES, findInView(Card.class).all().stream()
                .map(HomeViewTest::useCaseOf).toList());
    }

    @Test
    void navItemsKeepTheShortNameAndShowTheProblemAsTooltip() {
        navigate(HomeView.class);

        assertEquals(
                USE_CASES.stream()
                        .map(useCase -> useCase.tag() + " — " + useCase.title()
                                + ": " + useCase.problem())
                        .toList(),
                find(SideNavItem.class).all().stream()
                        .filter(item -> item.getTooltip() != null
                                && item.getTooltip().getText() != null)
                        .map(item -> item.getLabel() + ": "
                                + item.getTooltip().getText())
                        .toList(),
                "the nav tooltip and the home card read the same text");
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
