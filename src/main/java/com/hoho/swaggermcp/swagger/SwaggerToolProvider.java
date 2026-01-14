package com.hoho.swaggermcp.swagger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoho.swaggermcp.mcp.McpModels;
import com.hoho.swaggermcp.mcp.ToolProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Swagger 스펙을 기반으로 MCP Tool을 동적으로 생성하는 클래스
 *
 * 멀티 API를 지원합니다.
 *
 * 제공되는 Tool:
 * 1. list_registered_apis - 등록된 API 목록 조회
 * 2. list_api_categories - API 카테고리(태그) 목록 조회
 * 3. list_api_endpoints - 특정 카테고리의 엔드포인트 목록 조회
 * 4. search_api - 키워드로 API 검색
 * 5. call_api - operationId로 API 직접 호출
 */
public class SwaggerToolProvider implements ToolProvider {

    private static final Logger logger = LoggerFactory.getLogger(SwaggerToolProvider.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, String> apiConfigs;
    private final Map<String, String> authHeaders;
    private final Map<String, ApiInstance> apiInstances = new LinkedHashMap<>();

    public SwaggerToolProvider(Map<String, String> apiConfigs) {
        this(apiConfigs, new LinkedHashMap<>());
    }

    public SwaggerToolProvider(Map<String, String> apiConfigs, Map<String, String> authHeaders) {
        this.apiConfigs = apiConfigs;
        this.authHeaders = authHeaders != null ? authHeaders : new LinkedHashMap<>();
    }

    /**
     * 초기화: 모든 API의 Swagger 스펙 파싱
     */
    public void initialize() {
        logger.info("SwaggerToolProvider 초기화 시작");

        for (Map.Entry<String, String> entry : apiConfigs.entrySet()) {
            String name = entry.getKey();
            String url = entry.getValue();
            String authHeader = authHeaders.get(name);

            try {
                logger.info("API 초기화 중: {} ({}){}", name, url, authHeader != null ? " [인증 설정됨]" : "");
                ApiInstance instance = new ApiInstance(name, url, authHeader);
                instance.initialize();
                apiInstances.put(name, instance);
                logger.info("API 초기화 완료: {} - {}개 엔드포인트, {}개 카테고리",
                    name, instance.getEndpointCount(), instance.getCategoryCount());
            } catch (Exception e) {
                logger.error("API 초기화 실패: {} - {}", name, e.getMessage());
            }
        }

        logger.info("총 {}개의 API 초기화 완료", apiInstances.size());
    }

    @Override
    public List<McpModels.Tool> getTools() {
        List<McpModels.Tool> tools = new ArrayList<>();

        // 1. list_registered_apis (멀티 API일 때만 추가)
        if (apiInstances.size() > 1) {
            tools.add(createListRegisteredApisToolDef());
        }

        // 2. list_api_categories
        tools.add(createListCategoriesToolDef());

        // 3. list_api_endpoints
        tools.add(createListEndpointsToolDef());

        // 4. search_api
        tools.add(createSearchApiToolDef());

        // 5. call_api
        tools.add(createCallApiToolDef());

        return tools;
    }

    private McpModels.Tool createListRegisteredApisToolDef() {
        return new McpModels.Tool(
            "list_registered_apis",
            "List all registered API servers.\n" +
            "Use this to see available APIs when multiple APIs are configured.\n" +
            "Returns API names, URLs, and endpoint counts.",
            new McpModels.InputSchema(Collections.emptyMap(), null)
        );
    }

    private McpModels.Tool createListCategoriesToolDef() {
        Map<String, McpModels.PropertySchema> properties = new LinkedHashMap<>();

        if (apiInstances.size() > 1) {
            properties.put("api", new McpModels.PropertySchema(
                "string",
                "API name to query. Use list_registered_apis to see available APIs."
            ));
        }

        String description;
        if (apiInstances.size() == 1) {
            ApiInstance api = apiInstances.values().iterator().next();
            description = String.format(
                "List all API categories (tags) available in %s (v%s).\n" +
                "Returns category names with endpoint counts.\n" +
                "Use this first to explore the API structure.",
                api.getApiTitle() != null ? api.getApiTitle() : "this API",
                api.getApiVersion() != null ? api.getApiVersion() : "?"
            );
        } else {
            description = "List all API categories (tags) for a specific API.\n" +
                "Returns category names with endpoint counts.\n" +
                "Specify 'api' parameter to choose which API to query.";
        }

        return new McpModels.Tool(
            "list_api_categories",
            description,
            new McpModels.InputSchema(properties, null)
        );
    }

    private McpModels.Tool createListEndpointsToolDef() {
        Map<String, McpModels.PropertySchema> properties = new LinkedHashMap<>();

        if (apiInstances.size() > 1) {
            properties.put("api", new McpModels.PropertySchema(
                "string",
                "API name to query. Use list_registered_apis to see available APIs."
            ));
        }
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

        if (apiInstances.size() > 1) {
            properties.put("api", new McpModels.PropertySchema(
                "string",
                "API name to search in. Use list_registered_apis to see available APIs."
            ));
        }
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

        if (apiInstances.size() > 1) {
            properties.put("api", new McpModels.PropertySchema(
                "string",
                "API name to call. Use list_registered_apis to see available APIs."
            ));
        }
        properties.put("operationId", new McpModels.PropertySchema(
            "string",
            "The operationId of the API to call. Get this from list_api_endpoints or search_api."
        ));
        properties.put("parameters", new McpModels.PropertySchema(
            "object",
            "Parameters for the API call as a JSON object. Include path, query, and body parameters as needed."
        ));
        properties.put("headers", new McpModels.PropertySchema(
            "object",
            "Custom HTTP headers to include in the request. Use this for Authorization tokens, e.g., {\"Authorization\": \"Bearer xxx\"}"
        ));

        return new McpModels.Tool(
            "call_api",
            "Call an API endpoint by its operationId.\n" +
            "First use list_api_endpoints or search_api to find the operationId and required parameters.\n" +
            "Pass parameters as a JSON object with parameter names as keys.\n" +
            "Use 'headers' parameter to pass custom headers like Authorization tokens.",
            new McpModels.InputSchema(properties, List.of("operationId"))
        );
    }

    @Override
    public McpModels.CallToolResult callTool(String name, JsonNode arguments) {
        try {
            switch (name) {
                case "list_registered_apis":
                    return handleListRegisteredApis();
                case "list_api_categories":
                    return handleListCategories(arguments);
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
     * API 인스턴스 가져오기
     */
    private ApiInstance getApiInstance(JsonNode arguments) {
        if (apiInstances.size() == 1) {
            return apiInstances.values().iterator().next();
        }

        String apiName = getStringParam(arguments, "api");
        if (apiName == null || apiName.isEmpty()) {
            return null; // 선택 필요
        }

        // 대소문자 무시 검색
        for (Map.Entry<String, ApiInstance> entry : apiInstances.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(apiName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * API 선택 필요 메시지 생성
     */
    private McpModels.CallToolResult requireApiSelection() {
        StringBuilder sb = new StringBuilder();
        sb.append("# API 선택 필요\n\n");
        sb.append("등록된 API가 여러 개입니다. 'api' 파라미터로 지정해주세요:\n\n");
        sb.append("| 이름 | URL | 엔드포인트 |\n");
        sb.append("|------|-----|------------|\n");

        for (ApiInstance api : apiInstances.values()) {
            sb.append(String.format("| %s | %s | %d개 |\n",
                api.getName(), api.getSpecUrl(), api.getEndpointCount()));
        }

        sb.append("\n예: \"로컬 API 카테고리 보여줘\" 또는 api 파라미터에 API 이름 지정");

        return McpModels.CallToolResult.success(sb.toString());
    }

    /**
     * 등록된 API 목록 반환
     */
    private McpModels.CallToolResult handleListRegisteredApis() {
        StringBuilder sb = new StringBuilder();
        sb.append("# 등록된 API 목록\n\n");
        sb.append(String.format("총 %d개의 API가 등록되어 있습니다.\n\n", apiInstances.size()));

        sb.append("| 이름 | 제목 | 버전 | 카테고리 | 엔드포인트 |\n");
        sb.append("|------|------|------|----------|------------|\n");

        for (ApiInstance api : apiInstances.values()) {
            sb.append(String.format("| %s | %s | %s | %d개 | %d개 |\n",
                api.getName(),
                api.getApiTitle() != null ? api.getApiTitle() : "-",
                api.getApiVersion() != null ? api.getApiVersion() : "-",
                api.getCategoryCount(),
                api.getEndpointCount()
            ));
        }

        sb.append("\n*사용법: \"[API이름] API 카테고리 보여줘\" 또는 api 파라미터 지정*");

        return McpModels.CallToolResult.success(sb.toString());
    }

    /**
     * 카테고리(태그) 목록 반환
     */
    private McpModels.CallToolResult handleListCategories(JsonNode arguments) {
        ApiInstance api = getApiInstance(arguments);
        if (api == null && apiInstances.size() > 1) {
            return requireApiSelection();
        }
        if (api == null) {
            return McpModels.CallToolResult.error("등록된 API가 없습니다.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("# API Categories - %s\n\n", api.getName()));
        sb.append(String.format("**%s** (v%s)\n\n",
            api.getApiTitle() != null ? api.getApiTitle() : "API",
            api.getApiVersion() != null ? api.getApiVersion() : "?"));
        sb.append(String.format("Total: %d categories, %d endpoints\n\n",
            api.getCategoryCount(), api.getEndpointCount()));

        sb.append("| Category | Endpoints |\n");
        sb.append("|----------|----------|\n");

        for (Map.Entry<String, List<ApiEndpoint>> entry : api.getEndpointsByTag().entrySet()) {
            sb.append(String.format("| %s | %d |\n", entry.getKey(), entry.getValue().size()));
        }

        sb.append("\n*Use `list_api_endpoints` with a category name to see endpoints.*");

        return McpModels.CallToolResult.success(sb.toString());
    }

    /**
     * 특정 카테고리의 엔드포인트 목록 반환
     */
    private McpModels.CallToolResult handleListEndpoints(JsonNode arguments) {
        ApiInstance api = getApiInstance(arguments);
        if (api == null && apiInstances.size() > 1) {
            return requireApiSelection();
        }
        if (api == null) {
            return McpModels.CallToolResult.error("등록된 API가 없습니다.");
        }

        String category = getStringParam(arguments, "category");
        if (category == null || category.isEmpty()) {
            return McpModels.CallToolResult.error("'category' parameter is required");
        }

        List<ApiEndpoint> categoryEndpoints = api.getEndpointsByTag().get(category);
        if (categoryEndpoints == null) {
            // 대소문자 무시하고 찾기
            for (String key : api.getEndpointsByTag().keySet()) {
                if (key.equalsIgnoreCase(category)) {
                    categoryEndpoints = api.getEndpointsByTag().get(key);
                    category = key;
                    break;
                }
            }
        }

        if (categoryEndpoints == null) {
            return McpModels.CallToolResult.error(
                "Category not found: " + category + "\nAvailable categories: " +
                String.join(", ", api.getEndpointsByTag().keySet())
            );
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("# Endpoints in '%s' (%s)\n\n", category, api.getName()));
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
        ApiInstance api = getApiInstance(arguments);
        if (api == null && apiInstances.size() > 1) {
            return requireApiSelection();
        }
        if (api == null) {
            return McpModels.CallToolResult.error("등록된 API가 없습니다.");
        }

        String keyword = getStringParam(arguments, "keyword");
        if (keyword == null || keyword.isEmpty()) {
            return McpModels.CallToolResult.error("'keyword' parameter is required");
        }

        int limit = getIntParam(arguments, "limit", 10);
        limit = Math.min(limit, 50);

        String lowerKeyword = keyword.toLowerCase();

        List<ApiEndpoint> matches = api.getEndpoints().stream()
            .filter(ep -> matchesKeyword(ep, lowerKeyword))
            .limit(limit)
            .collect(Collectors.toList());

        if (matches.isEmpty()) {
            return McpModels.CallToolResult.success(
                "No endpoints found matching '" + keyword + "' in " + api.getName() + ".\n" +
                "Try different keywords or use list_api_categories to browse."
            );
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("# Search Results for '%s' in %s\n\n", keyword, api.getName()));
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
        ApiInstance api = getApiInstance(arguments);
        if (api == null && apiInstances.size() > 1) {
            return requireApiSelection();
        }
        if (api == null) {
            return McpModels.CallToolResult.error("등록된 API가 없습니다.");
        }

        String operationId = getStringParam(arguments, "operationId");
        if (operationId == null || operationId.isEmpty()) {
            return McpModels.CallToolResult.error("'operationId' parameter is required");
        }

        // 엔드포인트 찾기
        ApiEndpoint endpoint = api.getEndpoints().stream()
            .filter(e -> e.getOperationId().equals(operationId))
            .findFirst()
            .orElse(null);

        if (endpoint == null) {
            // 대소문자 무시하고 찾기
            endpoint = api.getEndpoints().stream()
                .filter(e -> e.getOperationId().equalsIgnoreCase(operationId))
                .findFirst()
                .orElse(null);
        }

        if (endpoint == null) {
            return McpModels.CallToolResult.error(
                "Endpoint not found: " + operationId + " in " + api.getName() + "\n" +
                "Use search_api or list_api_endpoints to find valid operationIds."
            );
        }

        try {
            JsonNode params = arguments != null ? arguments.get("parameters") : null;
            JsonNode customHeaders = arguments != null ? arguments.get("headers") : null;

            // 파라미터 분류
            Map<String, String> pathParams = new HashMap<>();
            Map<String, String> queryParams = new HashMap<>();
            Map<String, String> headerParams = new HashMap<>();
            Map<String, Object> bodyParams = new LinkedHashMap<>();

            // 커스텀 헤더 추가 (Claude가 직접 전달한 헤더)
            if (customHeaders != null && customHeaders.isObject()) {
                customHeaders.fields().forEachRemaining(field -> {
                    String headerValue = field.getValue().isTextual()
                        ? field.getValue().asText()
                        : field.getValue().toString();
                    headerParams.put(field.getKey(), headerValue);
                    logger.debug("커스텀 헤더 추가: {} = {}", field.getKey(),
                        field.getKey().equalsIgnoreCase("Authorization") ? "[MASKED]" : headerValue);
                });
            }

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

            // Authorization 헤더 자동 주입 (설정된 경우)
            if (api.getAuthHeader() != null && !api.getAuthHeader().isEmpty()) {
                // 사용자가 명시적으로 Authorization 헤더를 지정하지 않은 경우에만 추가
                if (!headerParams.containsKey("Authorization")) {
                    headerParams.put("Authorization", api.getAuthHeader());
                    logger.debug("Authorization 헤더 자동 주입: {}", api.getName());
                }
            }

            // API 호출
            String response = api.getHttpClient().request(
                endpoint.getMethod(),
                resolvedPath,
                queryParams,
                headerParams,
                bodyParams.isEmpty() ? null : bodyParams
            );

            logger.info("API 호출 성공: {} {} {}", api.getName(), endpoint.getMethod(), resolvedPath);
            return McpModels.CallToolResult.success(response);

        } catch (Exception e) {
            logger.error("API 호출 실패: {} {} {}", api.getName(), endpoint.getMethod(), endpoint.getPath(), e);
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
