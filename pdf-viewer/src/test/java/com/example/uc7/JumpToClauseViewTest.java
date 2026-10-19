package com.example.uc7;

import com.example.documents.SampleDocuments;
import com.example.pdfviewer.PdfViewer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = JumpToClauseView.class)
class JumpToClauseViewTest extends SpringBrowserlessTest {

    @Test
    void eachQuestionOpensThePageOfItsAnswer() {
        navigate(JumpToClauseView.class);
        PdfViewer viewer = findInView(PdfViewer.class).single();
        ClauseFinder finder = new ClauseFinder(
                SampleDocuments.serviceAgreement().bytes());

        for (JumpToClauseView.Question question : JumpToClauseView.CHECKLIST) {
            test(findInView(Button.class).withText(question.text()).single())
                    .click();
            int page = finder.pageOf(question.phrase()).orElseThrow();
            assertEquals(page, viewer.getPage(), question.text());
            assertEquals("Look for \"" + question.phrase() + "\" on page "
                    + page + ".", result());
        }
    }

    @Test
    void theAnswersAreSpreadOverTheContract() {
        ClauseFinder finder = new ClauseFinder(
                SampleDocuments.serviceAgreement().bytes());
        long pages = JumpToClauseView.CHECKLIST.stream().mapToInt(
                question -> finder.pageOf(question.phrase()).orElseThrow())
                .distinct().count();
        // Otherwise the view would only ever show one page.
        assertTrue(pages >= 3, pages + " distinct pages");
    }

    private String result() {
        return findInView(Paragraph.class).withClassName("uc7-result").single()
                .getText();
    }
}
