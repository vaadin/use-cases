package com.example.uc4;

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocsMcpClientTest {

    @Test
    void onlyTheConfiguredToolsAreOfferedToTheModel() {
        List<McpSchema.Tool> serverTools = List.of(tool("search_docs"),
                tool("get_full_document"), tool("delete_everything"));

        List<DocsMcpClient.DocsTool> offered = DocsMcpClient.offered(
                serverTools, Set.of("search_docs", "get_full_document"));

        assertEquals(List.of("search_docs", "get_full_document"),
                offered.stream().map(DocsMcpClient.DocsTool::name).toList());
        assertTrue(
                offered.getFirst().parametersSchema().contains("\"question\""),
                "the schema is passed on as JSON");
    }

    @Test
    void toolErrorsAreMarkedSoTheModelCanTellThemFromAnswers() {
        McpSchema.CallToolResult answer = McpSchema.CallToolResult.builder()
                .addContent(new McpSchema.TextContent("part 1"))
                .addContent(new McpSchema.TextContent("part 2")).build();
        McpSchema.CallToolResult error = McpSchema.CallToolResult.builder()
                .addContent(new McpSchema.TextContent("unknown version"))
                .isError(true).build();

        assertEquals("part 1\npart 2", DocsMcpClient.text(answer));
        assertEquals("Error: unknown version", DocsMcpClient.text(error));
    }

    private static McpSchema.Tool tool(String name) {
        return McpSchema.Tool.builder().name(name).description("Tool " + name)
                .inputSchema(Map.of("type", "object", "properties",
                        Map.of("question", Map.of("type", "string"))))
                .build();
    }
}
