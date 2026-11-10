package com.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import reactor.core.publisher.Flux;
import tools.jackson.databind.json.JsonMapper;

import com.vaadin.flow.component.ai.provider.LLMProvider;
import com.vaadin.flow.component.ai.provider.LLMProvider.LLMRequest;
import com.vaadin.flow.component.ai.provider.LLMProvider.ToolSpec;

/**
 * A scripted stand-in for the language model. A test sets what the "model" does
 * with a request — usually call one of the request's tools the way a real model
 * would, then answer — and can inspect the requests it got.
 */
public class FakeLlm {

    private final List<LLMRequest> requests = new ArrayList<>();
    private Function<LLMRequest, String> responder = request -> "OK";

    public void respond(Function<LLMRequest, String> responder) {
        this.responder = responder;
    }

    public List<LLMRequest> requests() {
        return requests;
    }

    public void reset() {
        requests.clear();
        responder = request -> "OK";
    }

    public LLMProvider newProvider() {
        return request -> {
            requests.add(request);
            return Flux.just(responder.apply(request));
        };
    }

    /**
     * Returns the request's {@code @Tool} object of the given type.
     */
    public static <T> T toolObject(LLMRequest request, Class<T> type) {
        return Arrays.stream(request.tools()).filter(type::isInstance)
                .map(type::cast).findFirst().orElseThrow();
    }

    /**
     * Calls a tool contributed by a controller with JSON arguments, as the
     * model would.
     */
    public static String callTool(LLMRequest request, String name,
            String jsonArguments) {
        ToolSpec tool = request.explicitTools().stream()
                .filter(t -> t.getName().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("No tool " + name));
        return tool.execute(JsonMapper.shared().readTree(jsonArguments));
    }
}
