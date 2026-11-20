package com.example.uc3;

import java.util.List;

import com.example.ai.AiProvider;
import com.example.ai.AiProviders;
import com.example.ai.AssistantPanel;
import com.example.ai.LlmProviderFactory;
import com.example.ai.ProviderNotice;
import com.example.ai.ToolCallLog;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.ai.orchestrator.AIOrchestrator;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;

/**
 * UC3 — Employee handbook.
 * <p>
 * Employees ask HR questions ("Can I work from Spain for a month?") and get an
 * answer from the company's own handbook instead of searching through it. The
 * handbook is indexed by meaning in a vector store ({@link Handbook}); an
 * {@link AIOrchestrator} gives the model a search tool over it
 * ({@link HandbookTools}) and tells it to answer only from what the search
 * returns, naming the sections it used. Questions the handbook does not cover
 * get "the handbook does not say" rather than a made-up policy.
 */
@Route(value = "uc3", layout = MainLayout.class)
@UseCaseDescription("Answering employees' questions from an internal handbook, with the sections the answer came from")
@Menu(order = 3, title = "UC3 — Employee handbook")
public class HandbookView extends VerticalLayout {

    static final String SYSTEM_PROMPT = """
            You answer employees' questions about Acme Corp's policies. \
            Always search the employee handbook first, and answer only from \
            the sections the search returns. End every answer with the titles \
            of the sections you used, as "Source: <title>". If the sections do \
            not answer the question, say that the handbook does not cover it \
            and suggest asking HR, without listing sources. Keep answers \
            short.""";

    static final List<String> SUGGESTIONS = List.of(
            "Can I work from Spain for a month?",
            "Can I expense a new office chair?",
            "My kid has the flu. Can I stay home?",
            "What is the CEO's salary?");

    public HandbookView(Handbook handbook, LlmProviderFactory llmProviders,
            AiProviders providers) {
        add(new H1("UC3 — Employee handbook"));
        add(new Paragraph("Ask a question about working at Acme Corp. The "
                + "assistant searches the employee handbook by meaning, not "
                + "by exact words, and answers only from the sections it "
                + "finds. Try a question the handbook does not cover, too: "
                + "the assistant says so instead of inventing a policy."));

        ProviderNotice notice = ProviderNotice.forMissing(providers,
                AiProvider.OPENAI);
        if (notice != null) {
            add(notice);
        }

        ToolCallLog log = new ToolCallLog("Handbook searches",
                "No searches yet. Each search shows what the assistant "
                        + "looked for and which sections it found.");
        AssistantPanel panel = new AssistantPanel(log);

        AIOrchestrator orchestrator = AIOrchestrator
                .builder(llmProviders.create(), SYSTEM_PROMPT)
                .withMessageList(panel.messages()).withInput(panel.input())
                .withTools(new HandbookTools(handbook, log))
                .withAssistantName("HR assistant").build();

        panel.addSuggestions(SUGGESTIONS, orchestrator);

        panel.setReady(notice == null);

        add(panel);

        Div text = new Div();
        handbook.sections().forEach(section -> text.add(new H3(section.title()),
                new Paragraph(section.text())));
        add(new Details("The handbook the assistant searches", text));
    }
}
