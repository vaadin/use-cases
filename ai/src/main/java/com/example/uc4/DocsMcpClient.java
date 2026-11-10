package com.example.uc4;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Connects the app to the MCP server that serves the product documentation, and
 * calls its tools. MCP (Model Context Protocol) is an open protocol for
 * offering tools and documents to an AI model; any documentation, wiki or
 * ticket system with an MCP server can be plugged in by changing
 * {@code app.docs-mcp.url}.
 * <p>
 * One connection is shared by all users, opened on first use: the default
 * server, Vaadin's public documentation server, keeps no per-user state.
 */
@Component
public class DocsMcpClient implements DisposableBean {

    /**
     * What the server offers, as the app sees it after connecting.
     */
    public record Connection(String serverName, String serverVersion,
            int toolsOnServer, List<DocsTool> tools) {
    }

    /**
     * A tool of the MCP server: its name, its description for the model and the
     * JSON schema of its arguments.
     */
    public record DocsTool(String name, String description,
            String parametersSchema) {
    }

    private static final JsonMapper JSON = JsonMapper.shared();

    private final String url;
    private final Set<String> offeredTools;
    private @Nullable McpSyncClient client;
    private @Nullable Connection connection;

    public DocsMcpClient(@Value("${app.docs-mcp.url}") String url,
            @Value("${app.docs-mcp.tools}") Set<String> offeredTools) {
        this.url = url;
        this.offeredTools = offeredTools;
    }

    public String url() {
        return url;
    }

    /**
     * Connects on the first call and returns what the server offers: of its
     * tools, only the ones listed in {@code app.docs-mcp.tools}, so the model
     * gets a short, focused tool list.
     */
    public synchronized Connection connect() {
        if (connection == null) {
            // The transport takes the server's base URL and the endpoint path
            // separately, and defaults the path to /mcp.
            URI uri = URI.create(url);
            McpSyncClient newClient = McpClient
                    .sync(HttpClientStreamableHttpTransport
                            .builder(uri.resolve("/").toString())
                            .endpoint(uri.getRawPath()).build())
                    .clientInfo(
                            new McpSchema.Implementation("ai-use-cases", "1.0"))
                    .requestTimeout(Duration.ofSeconds(30)).build();
            try {
                McpSchema.InitializeResult info = newClient.initialize();
                List<McpSchema.Tool> allTools = newClient.listTools().tools();
                connection = new Connection(info.serverInfo().name(),
                        info.serverInfo().version(), allTools.size(),
                        offered(allTools, offeredTools));
                client = newClient;
            } catch (RuntimeException e) {
                newClient.close();
                throw e;
            }
        }
        return connection;
    }

    /**
     * Returns the tools the model may use: those of the server's tools that are
     * listed in {@code offeredTools}.
     */
    static List<DocsTool> offered(List<McpSchema.Tool> serverTools,
            Set<String> offeredTools) {
        return serverTools.stream()
                .filter(tool -> offeredTools.contains(tool.name()))
                .map(tool -> new DocsTool(tool.name(), tool.description(),
                        JSON.writeValueAsString(tool.inputSchema())))
                .toList();
    }

    /**
     * Returns the text of a tool result, marked as an error when the server
     * reported one, so the model can tell the two apart.
     */
    static String text(McpSchema.CallToolResult result) {
        String text = result.content().stream()
                .filter(McpSchema.TextContent.class::isInstance)
                .map(content -> ((McpSchema.TextContent) content).text())
                .collect(Collectors.joining("\n"));
        return Boolean.TRUE.equals(result.isError()) ? "Error: " + text : text;
    }

    /**
     * Calls a tool on the server and returns the text it answered with.
     */
    public String call(String tool, Map<String, Object> arguments) {
        McpSyncClient connected;
        synchronized (this) {
            connect();
            connected = Objects.requireNonNull(client);
        }
        try {
            return text(connected
                    .callTool(new McpSchema.CallToolRequest(tool, arguments)));
        } catch (RuntimeException e) {
            // The connection is shared and kept for the app's lifetime; a
            // server restart or an expired session would otherwise break
            // every later call. Drop it, so the next call connects again.
            disconnect(connected);
            throw e;
        }
    }

    private synchronized void disconnect(McpSyncClient failed) {
        if (client == failed) {
            client = null;
            connection = null;
            failed.close();
        }
    }

    @Override
    public synchronized void destroy() {
        if (client != null) {
            client.closeGracefully();
        }
    }
}
