package com.example.uc3;

import java.util.List;

import com.example.AiTestConfiguration;
import com.example.FakeLlm;
import com.example.ai.ToolCallLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(AiTestConfiguration.class)
@ViewPackages(classes = HandbookView.class)
class HandbookViewTest extends SpringBrowserlessTest {

    @Autowired
    private FakeLlm llm;

    @BeforeEach
    void resetLlm() {
        llm.reset();
    }

    @Test
    void searchReturnsTheSectionsClosestInMeaningAndTheAnswerIsShown() {
        String[] searchResult = new String[1];
        llm.respond(request -> {
            searchResult[0] = FakeLlm.toolObject(request, HandbookTools.class)
                    .searchHandbook("work abroad in Spain");
            return "Up to 20 working days. Source: Remote work";
        });
        navigate(HandbookView.class);

        test(find(Button.class).withText("Can I work from Spain for a month?")
                .single()).click();
        runPendingSignalsTasks();

        assertTrue(searchResult[0].startsWith("## Remote work\n"),
                searchResult[0]);
        assertTrue(searchResult[0].contains("20 working days"),
                "the model gets the section text, not only its title");
        ToolCallLog.ToolCall call = find(ToolCallLog.class).single().getCalls()
                .getFirst();
        assertEquals("work abroad in Spain", call.input());
        assertTrue(call.output().startsWith("Remote work"), call.output());
        assertEquals("Up to 20 working days. Source: Remote work",
                find(MessageList.class).single().getItems().stream()
                        .map(MessageListItem::getText).toList().getLast());
    }

    @Test
    void handbookIsSplitIntoOneSectionPerHeading() {
        List<Handbook.Section> sections = Handbook.parse("""
                # Title

                ## Vacation

                25 days.

                ## Sick leave

                Tell your manager.
                """);

        assertEquals(List.of(new Handbook.Section("Vacation", "25 days."),
                new Handbook.Section("Sick leave", "Tell your manager.")),
                sections);
    }
}
