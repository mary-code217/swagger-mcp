package com.hoho.swaggermcp.mcp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * MCP 프로토콜에서 사용하는 데이터 모델들
 */
public class McpModels {

    // ==================== 초기화 관련 ====================

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InitializeParams {
        private String protocolVersion;
        private ClientCapabilities capabilities;
        private ClientInfo clientInfo;

        public String getProtocolVersion() { return protocolVersion; }
        public void setProtocolVersion(String protocolVersion) { this.protocolVersion = protocolVersion; }
        public ClientCapabilities getCapabilities() { return capabilities; }
        public void setCapabilities(ClientCapabilities capabilities) { this.capabilities = capabilities; }
        public ClientInfo getClientInfo() { return clientInfo; }
        public void setClientInfo(ClientInfo clientInfo) { this.clientInfo = clientInfo; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ClientCapabilities {
        private JsonNode roots;
        private JsonNode sampling;

        public JsonNode getRoots() { return roots; }
        public void setRoots(JsonNode roots) { this.roots = roots; }
        public JsonNode getSampling() { return sampling; }
        public void setSampling(JsonNode sampling) { this.sampling = sampling; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ClientInfo {
        private String name;
        private String version;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class InitializeResult {
        private String protocolVersion = "2024-11-05";
        private ServerCapabilities capabilities = new ServerCapabilities();
        private ServerInfo serverInfo = new ServerInfo();

        public String getProtocolVersion() { return protocolVersion; }
        public void setProtocolVersion(String protocolVersion) { this.protocolVersion = protocolVersion; }
        public ServerCapabilities getCapabilities() { return capabilities; }
        public void setCapabilities(ServerCapabilities capabilities) { this.capabilities = capabilities; }
        public ServerInfo getServerInfo() { return serverInfo; }
        public void setServerInfo(ServerInfo serverInfo) { this.serverInfo = serverInfo; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ServerCapabilities {
        private ToolsCapability tools = new ToolsCapability();

        public ToolsCapability getTools() { return tools; }
        public void setTools(ToolsCapability tools) { this.tools = tools; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ToolsCapability {
        private Boolean listChanged = true;

        public Boolean getListChanged() { return listChanged; }
        public void setListChanged(Boolean listChanged) { this.listChanged = listChanged; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ServerInfo {
        private String name = "SwaggerMCP";
        private String version = "1.0.0";

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
    }

    // ==================== Tool 관련 ====================

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Tool {
        private String name;
        private String description;
        private InputSchema inputSchema;

        public Tool() {}

        public Tool(String name, String description, InputSchema inputSchema) {
            this.name = name;
            this.description = description;
            this.inputSchema = inputSchema;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public InputSchema getInputSchema() { return inputSchema; }
        public void setInputSchema(InputSchema inputSchema) { this.inputSchema = inputSchema; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class InputSchema {
        private String type = "object";
        private Map<String, PropertySchema> properties;
        private List<String> required;

        public InputSchema() {}

        public InputSchema(Map<String, PropertySchema> properties, List<String> required) {
            this.properties = properties;
            this.required = required;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Map<String, PropertySchema> getProperties() { return properties; }
        public void setProperties(Map<String, PropertySchema> properties) { this.properties = properties; }
        public List<String> getRequired() { return required; }
        public void setRequired(List<String> required) { this.required = required; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PropertySchema {
        private String type;
        private String description;
        private List<String> enumValues;

        public PropertySchema() {}

        public PropertySchema(String type, String description) {
            this.type = type;
            this.description = description;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<String> getEnumValues() { return enumValues; }
        public void setEnumValues(List<String> enumValues) { this.enumValues = enumValues; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ListToolsResult {
        private List<Tool> tools;

        public ListToolsResult() {}

        public ListToolsResult(List<Tool> tools) {
            this.tools = tools;
        }

        public List<Tool> getTools() { return tools; }
        public void setTools(List<Tool> tools) { this.tools = tools; }
    }

    // ==================== Tool 호출 관련 ====================

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CallToolParams {
        private String name;
        private JsonNode arguments;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public JsonNode getArguments() { return arguments; }
        public void setArguments(JsonNode arguments) { this.arguments = arguments; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CallToolResult {
        private List<ToolContent> content;
        private Boolean isError;

        public CallToolResult() {}

        public CallToolResult(List<ToolContent> content, boolean isError) {
            this.content = content;
            this.isError = isError ? true : null;  // false면 null로 (생략)
        }

        public static CallToolResult success(String text) {
            return new CallToolResult(List.of(new ToolContent(text)), false);
        }

        public static CallToolResult error(String text) {
            return new CallToolResult(List.of(new ToolContent(text)), true);
        }

        public List<ToolContent> getContent() { return content; }
        public void setContent(List<ToolContent> content) { this.content = content; }
        public Boolean getIsError() { return isError; }
        public void setIsError(Boolean isError) { this.isError = isError; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ToolContent {
        private String type = "text";
        private String text;

        public ToolContent() {}

        public ToolContent(String text) {
            this.text = text;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }
}
