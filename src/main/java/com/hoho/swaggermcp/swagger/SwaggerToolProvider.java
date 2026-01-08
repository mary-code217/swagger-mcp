package com.hoho.swaggermcp.swagger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoho.swaggermcp.http.HttpApiClient;
import com.hoho.swaggermcp.mcp.McpModels;
import com.hoho.swaggermcp.mcp.ToolProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Swagger 스펙을 기반으로 MCP Tool을 동적으로 생성하는 클래스
 *
 * 대규모 API를 위해 계층적 탐색 + 검색 방식을 지원합니다.
 *
 * 제공되는 Tool:
 * 1. list_api_categories - API 카테고리(태그) 목록 조회
 * 2. list_api_endpoints - 특정 카테고리의 엔드포인트 목록 조회
 * 3. search_api - 키워드로 API 검색
 * 4. call_api - operationId로 API 직접 호출
 */
public class SwaggerToolProvider implements ToolProvider {

    private static final Logger logger = LoggerFactory.getLogger(SwaggerToolProvider.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String specUrl;
    private final String baseUrlOverride;

    private final List<ApiEndpoint> endpoints = new ArrayList<>();
    private final Map<String, List<ApiEndpoint>> endpointsByTag = new LinkedHashMap<>();
    private HttpApiClient httpClient;
    private String actualBaseUrl;
    private String apiTitle;
    private String apiVersion;

    public SwaggerToolProvider(String specUrl, String baseUrlOverride) {
        this.specUrl = specUrl;
        this.baseUrlOverride = baseUrlOverride;
    }

    /**
     * 초기화: Swagger 스펙 파싱 및 Tool 생성
     */
    public void initialize() {
        logger.info("SwaggerToolProvider 초기화 시작");

        // Swagger 스펙 파싱
        SwaggerParser parser = new SwaggerParser(specUrl);
        endpoints.addAll(parser.parse());

        // API 정보 저장
        apiTitle = parser.getApiTitle();
        apiVersion = parser.getApiVersion();

        // Base URL 결정
        actualBaseUrl = baseUrlOverride != null ? baseUrlOverride : parser.getBaseUrl();
        logger.info("API Base URL: {}", actualBaseUrl);

        // HTTP 클라이언트 생성
        httpClient = new HttpApiClient(actualBaseUrl);

        // 태그별로 엔드포인트 그룹화
        for (ApiEndpoint endpoint : endpoints) {
            List<String> tags = endpoint.getTags();
            if (tags == null || tags.isEmpty()) {
                tags = Collections.singletonList("default");
            }
            for (String tag : tags) {
                endpointsByTag.computeIfAbsent(tag, k -> new ArrayList<>()).add(endpoint);
            }
        }

        logger.info("총 {}개의 엔드포인트, {}개의 카테고리 파싱 완료", endpoints.size(), endpointsByTag.size());
    }

    @Override
    public List<McpModels.Tool> getTools() {
        List<McpModels.Tool> tools = new ArrayList<>();

        // 1. list_api_categories
        tools.add(createListCategoriesToolDef());

        // 2. list_api_endpoints
        tools.add(createListEndpointsToolDef());

        // 3. search_api
        tools.add(createSearchApiToolDef());

        // 4. call_api
        tools.add(createCallApiToolDef());

        return tools;
    }

    private McpModels.Tool createListCategoriesToolDef() {
        String description = String.format(
            "List all API categories (tags) available in %s (v%s).\n" +
            "Returns category names with endpoint counts.\n" +
            "Use this first to explore the API structure.",
            apiTitle != null ? apiTitle : "this API",
            apiVersion != null ? apiVersion : "?"
        );

        return new McpModels.Tool(
            "list_api_categories",
            description,
            new McpModels.InputSchema(Collections.emptyMap(), null)
        );
    }

    private McpModels.Tool createListEndpointsToolDef() {
        Map<String, McpModels.PropertySchema> properties = new LinkedHashMap<>();
        properties.put("category", new McpModels.PropertySchema(
            "string",
            "Category (tag) name to list endpoints for. Use list_api_categories to get available categories."
        ));

        return new McpModels.Tool(
            "list_api_endpoints",
            "List all API endpoints in a specific category.\n" +
            "Returns operationId, method, path, and summary for each endpoint.\n" +
            "Use call_api with the operationId to invoke an endpoint.",
            new McpModels.InputSchema(properties, List.of("category"))
        );
    }

    private McpModels.Tool createSearchApiToolDef() {
        Map<String, McpModels.PropertySchema> properties = new LinkedHashMap<>();
        properties.put("keyword", new McpModels.PropertySchema(
            "string",
            "Keyword to search for in API paths, operationIds, summaries, and descriptions"
        ));
        properties.put("limit", new McpModels.PropertySchema(
            "integer",
            "Maximum number of results to return (default: 10, max: 50)"
        ));

        return new McpModels.Tool(
            "search_api",
            "Search for API endpoints by keyword.\n" +
            "Searches in paths, operationIds, summaries, and descriptions.\n" +
            "Returns matching endpoints with full details including parameters.",
            new McpModels.InputSchema(properties, List.of("keyword"))
        );
    }

    private McpModels.Tool createCallApiToolDef() {
        Map<String, McpModels.PropertySchema> properties = new LinkedHashMap<>();
        properties.put("operationId", new McpModels.PropertySchema(
            "string",
            "The operationId of the API to call. Get this from list_api_endpoints or search_api."
        ));
        properties.put("parameters", new McpModels.PropertySchema(
            "object",
            "Parameters for the API call as a JSON object. Include path, query, header, and body parameters as needed."
        ));

        return new McpModels.Tool(
            "call_api",
            "Call an API endpoint by its operationId.\n" +
            "First use list_api_endpoints or search_api to find the operationId and required parameters.\n" +
            "Pass parameters as a JSON object with parameter names as keys.",
            new McpModels.InputSchema(properties, List.of("operationId"))
        );
    }

    @Override
    public McpModels.CallToolResult callTool(String name, JsonNode arguments) {
        try {
            switch (name) {
                case "list_api_categories":
                    return handleListCategories();
                case "list_api_endpoints":
                    return handleListEndpoints(arguments);
                case "search_api":
                    return handleSearchApi(arguments);
                case "call_api":
                    return handleCallApi(arguments);
                default:
                    return McpModels.CallToolResult.error("Unknown tool: " + name);
            }
        } catch (Exception e) {
            logger.error("Tool 실행 오류: {}", name, e);
            return McpModels.CallToolResult.error("Error: " + e.getMessage());
        }
    }

    /**
     * 카테고리(태그) 목록 반환
     */
    private McpModels.CallToolResult handleListCategories() {
        StringBuilder sb = new StringBuilder();
        sb.append("# API Categories\n\n");
        sb.append(String.format("**%s** (v%s)\n\n",
            apiTitle != null ? apiTitle : "API",
            apiVersion != null ? apiVersion : "?"));
        sb.append(String.format("Total: %d categories, %d endpoints\n\n",
            endpointsByTag.size(), endpoints.size()));

        sb.append("| Category | Endpoints |\n");
        sb.append("|----------|----------|\n");

        for (Map.Entry<String, List<ApiEndpoint>> entry : endpointsByTag.entrySet()) {
            sb.append(String.format("| %s | %d |\n", entry.getKey(), entry.getValue().size()));
        }

        sb.append("\n*Use `list_api_endpoints` with a category name to see endpoints.*");

        return McpModels.CallToolResult.success(sb.toString());
    }

    /**
     * 특정 카테고리의 엔드포인트 목록 반환
     */
    private McpModels.CallToolResult handleListEndpoints(JsonNode arguments) {
        String category = getStringParam(arguments, "category");
        if (category == null || category.isEmpty()) {
            return McpModels.CallToolResult.error("'category' parameter is required");
        }

        List<ApiEndpoint> categoryEndpoints = endpointsByTag.get(category);
        if (categoryEndpoints == null) {
            // 대소문자 무시하고 찾기
            for (String key : endpointsByTag.keySet()) {
                if (key.equalsIgnoreCase(category)) {
                    categoryEndpoints = endpointsByTag.get(key);
                    category = key;
                    break;
                }
            }
        }

        if (categoryEndpoints == null) {
            return McpModels.CallToolResult.error(
                "Category not found: " + category + "\nAvailable categories: " +
                String.join(", ", endpointsByTag.keySet())
            );
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("# Endpoints in '%s'\n\n", category));
        sb.append(String.format("Total: %d endpoints\n\n", categoryEndpoints.size()));

        for (ApiEndpoint ep : categoryEndpoints) {
            sb.append(String.format("## %s\n", ep.getOperationId()));
            sb.append(String.format("- **Method:** %s\n", ep.getMethod()));
            sb.append(String.format("- **Path:** %s\n", ep.getPath()));
            if (ep.getSummary() != null && !ep.getSummary().isEmpty()) {
                sb.append(String.format("- **Summary:** %s\n", ep.getSummary()));
            }

            // 파라미터 요약
            if (ep.getParameters() != null && !ep.getParameters().isEmpty()) {
                List<String> required = ep.getParameters().stream()
                    .filter(ApiParameter::isRequired)
                    .map(ApiParameter::getName)
                    .collect(Collectors.toList());
                List<String> optional = ep.getParameters().stream()
                    .filter(p -> !p.isRequired())
                    .map(ApiParameter::getName)
                    .collect(Collectors.toList());

                if (!required.isEmpty()) {
                    sb.append(String.format("- **Required params:** %s\n", String.join(", ", required)));
                }
                if (!optional.isEmpty()) {
                    sb.append(String.format("- **Optional params:** %s\n", String.join(", ", optional)));
                }
            }
            sb.append("\n");
        }

        sb.append("*Use `call_api` with operationId and parameters to call an endpoint.*\n");
        sb.append("*Use `search_api` to get full parameter details for a specific endpoint.*");

        return McpModels.CallToolResult.success(sb.toString());
    }

    /**
     * API 검색
     */
    private McpModels.CallToolResult handleSearchApi(JsonNode arguments) {
        String keyword = getStringParam(arguments, "keyword");
        if (keyword == null || keyword.isEmpty()) {
            return McpModels.CallToolResult.error("'keyword' parameter is required");
        }

        int limit = getIntParam(arguments, "limit", 10);
        limit = Math.min(limit, 50);

        String lowerKeyword = keyword.toLowerCase();

        List<ApiEndpoint> matches = endpoints.stream()
            .filter(ep -> matchesKeyword(ep, lowerKeyword))
            .limit(limit)
            .collect(Collectors.toList());

        if (matches.isEmpty()) {
            return McpModels.CallToolResult.success(
                "No endpoints found matching '" + keyword + "'.\n" +
                "Try different keywords or use list_api_categories to browse."
            );
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("# Search Results for '%s'\n\n", keyword));
        sb.append(String.format("Found %d endpoints (showing max %d)\n\n", matches.size(), limit));

        for (ApiEndpoint ep : matches) {
            sb.append(formatEndpointDetails(ep));
            sb.append("\n---\n\n");
        }

        return McpModels.CallToolResult.success(sb.toString());
    }

    private boolean matchesKeyword(ApiEndpoint ep, String lowerKeyword) {
        if (ep.getOperationId().toLowerCase().contains(lowerKeyword)) return true;
        if (ep.getPath().toLowerCase().contains(lowerKeyword)) return true;
        if (ep.getSummary() != null && ep.getSummary().toLowerCase().contains(lowerKeyword)) return true;
        if (ep.getDescription() != null && ep.getDescription().toLowerCase().contains(lowerKeyword)) return true;
        if (ep.getTags() != null) {
            for (String tag : ep.getTags()) {
                if (tag.toLowerCase().contains(lowerKeyword)) return true;
            }
        }
        return false;
    }

    private String formatEndpointDetails(ApiEndpoint ep) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("## %s\n", ep.getOperationId()));
        sb.append(String.format("**%s** `%s`\n\n", ep.getMethod(), ep.getPath()));

        if (ep.getSummary() != null && !ep.getSummary().isEmpty()) {
            sb.append(ep.getSummary()).append("\n\n");
        }

        if (ep.getDescription() != null && !ep.getDescription().isEmpty()
            && !ep.getDescription().equals(ep.getSummary())) {
            sb.append(ep.getDescription()).append("\n\n");
        }

        if (ep.getTags() != null && !ep.getTags().isEmpty()) {
            sb.append("**Tags:** ").append(String.join(", ", ep.getTags())).append("\n\n");
        }

        if (ep.getParameters() != null && !ep.getParameters().isEmpty()) {
            sb.append("**Parameters:**\n\n");
            sb.append("| Name | Location | Type | Required | Description |\n");
            sb.append("|------|----------|------|----------|-------------|\n");

            for (ApiParameter param : ep.getParameters()) {
                sb.append(String.format("| %s | %s | %s | %s | %s |\n",
                    param.getName(),
                    param.getLocation(),
                    param.getType(),
                    param.isRequired() ? "Yes" : "No",
                    param.getDescription() != null ? param.getDescription() : ""
                ));
            }
        } else {
            sb.append("**Parameters:** None\n");
        }

        return sb.toString();
    }

    /**
     * API 호출
     */
    private McpModels.CallToolResult handleCallApi(JsonNode arguments) {
        String operationId = getStringParam(arguments, "operationId");
        if (operationId == null || operationId.isEmpty()) {
            return McpModels.CallToolResult.error("'operationId' parameter is required");
        }

        // 엔드포인트 찾기
        ApiEndpoint endpoint = endpoints.stream()
            .filter(e -> e.getOperationId().equals(operationId))
            .findFirst()
            .orElse(null);

        if (endpoint == null) {
            // 대소문자 무시하고 찾기
            endpoint = endpoints.stream()
                .filter(e -> e.getOperationId().equalsIgnoreCase(operationId))
                .findFirst()
                .orElse(null);
        }

        if (endpoint == null) {
            return McpModels.CallToolResult.error(
                "Endpoint not found: " + operationId + "\n" +
                "Use search_api or list_api_endpoints to find valid operationIds."
            );
        }

        try {
            JsonNode params = arguments != null ? arguments.get("parameters") : null;

            // 파라미터 분류
            Map<String, String> pathParams = new HashMap<>();
            Map<String, String> queryParams = new HashMap<>();
            Map<String, String> headerParams = new HashMap<>();
            Map<String, Object> bodyParams = new LinkedHashMap<>();

            for (ApiParameter param : endpoint.getParameters()) {
                JsonNode value = params != null ? params.get(param.getName()) : null;
                if (value != null && !value.isNull()) {
                    String stringValue = value.isTextual() ? value.asText() : value.toString();

                    switch (param.getLocation()) {
                        case "path":
                            pathParams.put(param.getName(), stringValue);
                            break;
                        case "query":
                            queryParams.put(param.getName(), stringValue);
                            break;
                        case "header":
                            headerParams.put(param.getName(), stringValue);
                            break;
                        case "body":
                            bodyParams.put(param.getName(), extractValue(value));
                            break;
                    }
                }
            }

            // 필수 파라미터 검증
            List<String> missingRequired = new ArrayList<>();
            for (ApiParameter param : endpoint.getParameters()) {
                if (param.isRequired()) {
                    JsonNode value = params != null ? params.get(param.getName()) : null;
                    if (value == null || value.isNull()) {
                        missingRequired.add(param.getName() + " (" + param.getLocation() + ")");
                    }
                }
            }

            if (!missingRequired.isEmpty()) {
                return McpModels.CallToolResult.error(
                    "Missing required parameters: " + String.join(", ", missingRequired) + "\n\n" +
                    "Use search_api with operationId '" + operationId + "' to see all parameter details."
                );
            }

            // Path 파라미터 치환
            String resolvedPath = endpoint.getPath();
            for (Map.Entry<String, String> entry : pathParams.entrySet()) {
                resolvedPath = resolvedPath.replace("{" + entry.getKey() + "}", entry.getValue());
            }

            // API 호출
            String response = httpClient.request(
                endpoint.getMethod(),
                resolvedPath,
                queryParams,
                headerParams,
                bodyParams.isEmpty() ? null : bodyParams
            );

            logger.info("API 호출 성공: {} {}", endpoint.getMethod(), resolvedPath);
            return McpModels.CallToolResult.success(response);

        } catch (Exception e) {
            logger.error("API 호출 실패: {} {}", endpoint.getMethod(), endpoint.getPath(), e);
            return McpModels.CallToolResult.error("API call failed: " + e.getMessage());
        }
    }

    private String getStringParam(JsonNode args, String name) {
        if (args == null) return null;
        JsonNode node = args.get(name);
        return node != null && !node.isNull() ? node.asText() : null;
    }

    private int getIntParam(JsonNode args, String name, int defaultValue) {
        if (args == null) return defaultValue;
        JsonNode node = args.get(name);
        return node != null && node.isInt() ? node.asInt() : defaultValue;
    }

    private Object extractValue(JsonNode node) {
        if (node.isTextual()) return node.asText();
        if (node.isInt()) return node.asInt();
        if (node.isLong()) return node.asLong();
        if (node.isDouble()) return node.asDouble();
        if (node.isBoolean()) return node.asBoolean();
        if (node.isArray() || node.isObject()) {
            try {
                return objectMapper.treeToValue(node, Object.class);
            } catch (Exception e) {
                return node.toString();
            }
        }
        return node.toString();
    }
}
