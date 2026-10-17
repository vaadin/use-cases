package com.example.uc4;

import java.util.List;
import java.util.Map;

import com.example.ai.ToolCallLog;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import com.vaadin.flow.component.ai.orchestrator.AIController;
import com.vaadin.flow.component.ai.provider.LLMProvider.ToolSpec;

/**
 * Offers the tools of an MCP server to an {@code AIOrchestrator}. The
 * orchestrator has no MCP support of its own (API-GAPS.md #1), but a controller
 * can contribute any tool as a {@link ToolSpec}: a name, a description, a JSON
 * schema and a method that runs it. That is exactly what an MCP server
 * describes for each of its tools, so the adapter only forwards calls.
 */
public class McpToolsController implements AIController {

    private static final JsonMapper JSON = JsonMapper.shared();

    private final DocsMcpClient client;
    private final List<ToolSpec> tools;
    private final ToolCallLog log;

    /**
     * @param connection
     *            the server's tools; the orchestrator validates a controller's
     *            tools as soon as it is added to the builder, so they have to
     *            be known up front (API-GAPS.md #2)
     */
    public McpToolsController(DocsMcpClient client,
            DocsMcpClient.Connection connection, ToolCallLog log) {
        this.client = client;
        this.log = log;
        this.tools = connection.tools().stream().map(this::toToolSpec).toList();
    }

    @Override
    public List<ToolSpec> getTools() {
        return tools;
    }

    private ToolSpec toToolSpec(DocsMcpClient.DocsTool tool) {
        return new ToolSpec() {
            @Override
            public String getName() {
                return tool.name();
            }

            @Override
            public String getDescription() {
                return tool.description();
            }

            @Override
            public String getParametersSchema() {
                return tool.parametersSchema();
            }

            @Override
            public String execute(JsonNode arguments) {
                Map<String, Object> args = JSON.convertValue(arguments,
                        new TypeReference<Map<String, Object>>() {
                        });
                String result = client.call(tool.name(), args);
                log.record(tool.name(), arguments.toString(), result);
                return result;
            }
        };
    }
}
