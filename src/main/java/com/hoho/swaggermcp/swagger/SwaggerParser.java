package com.hoho.swaggermcp.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * OpenAPI/Swagger 스펙을 파싱하여 API 정보를 추출하는 클래스
 */
public class SwaggerParser {

    private static final Logger logger = LoggerFactory.getLogger(SwaggerParser.class);

    private final String specUrl;
    private OpenAPI openAPI;
    private final List<ApiEndpoint> apiEndpoints = new ArrayList<>();

    public SwaggerParser(String specUrl) {
        this.specUrl = specUrl;
    }

    /**
     * Swagger 스펙 파싱 및 API 엔드포인트 추출
     */
    public List<ApiEndpoint> parse() {
        logger.info("Swagger 스펙 파싱 시작: {}", specUrl);

        openAPI = new OpenAPIV3Parser().read(specUrl);
        if (openAPI == null) {
            throw new IllegalStateException("Swagger 스펙을 파싱할 수 없습니다: " + specUrl);
        }

        if (openAPI.getInfo() != null) {
            logger.info("API 정보: {} v{}", openAPI.getInfo().getTitle(), openAPI.getInfo().getVersion());
        }

        if (openAPI.getPaths() != null) {
            openAPI.getPaths().forEach(this::extractEndpoints);
        }

        logger.info("총 {}개의 API 엔드포인트 발견", apiEndpoints.size());
        return apiEndpoints;
    }

    /**
     * PathItem에서 각 HTTP 메서드별 엔드포인트 추출
     */
    private void extractEndpoints(String path, PathItem pathItem) {
        if (pathItem.getGet() != null) addEndpoint("GET", path, pathItem.getGet());
        if (pathItem.getPost() != null) addEndpoint("POST", path, pathItem.getPost());
        if (pathItem.getPut() != null) addEndpoint("PUT", path, pathItem.getPut());
        if (pathItem.getDelete() != null) addEndpoint("DELETE", path, pathItem.getDelete());
        if (pathItem.getPatch() != null) addEndpoint("PATCH", path, pathItem.getPatch());
    }

    /**
     * Operation을 ApiEndpoint로 변환
     */
    private void addEndpoint(String method, String path, Operation operation) {
        String operationId = operation.getOperationId();
        if (operationId == null || operationId.isBlank()) {
            operationId = generateOperationId(method, path);
        }

        List<ApiParameter> parameters = new ArrayList<>();

        // Path, Query, Header 파라미터 추출
        if (operation.getParameters() != null) {
            for (Parameter param : operation.getParameters()) {
                parameters.add(extractParameter(param));
            }
        }

        // RequestBody 추출 (POST, PUT, PATCH)
        if (operation.getRequestBody() != null &&
                operation.getRequestBody().getContent() != null &&
                operation.getRequestBody().getContent().get("application/json") != null) {

            Schema<?> schema = operation.getRequestBody().getContent().get("application/json").getSchema();
            if (schema != null) {
                extractRequestBodyParams(schema, parameters);
            }
        }

        String summary = operation.getSummary() != null ? operation.getSummary() : "";
        String description = operation.getDescription() != null ? operation.getDescription() : summary;
        List<String> tags = operation.getTags() != null ? operation.getTags() : Collections.emptyList();

        ApiEndpoint endpoint = new ApiEndpoint(operationId, method, path, summary, description, parameters, tags);
        apiEndpoints.add(endpoint);

        logger.debug("엔드포인트 추가: {} {} -> {}", method, path, operationId);
    }

    /**
     * Parameter를 ApiParameter로 변환
     */
    private ApiParameter extractParameter(Parameter param) {
        String type = "string";
        if (param.getSchema() != null && param.getSchema().getType() != null) {
            type = param.getSchema().getType();
        }

        return new ApiParameter(
                param.getName(),
                param.getIn(),  // "path", "query", "header"
                type,
                param.getRequired() != null && param.getRequired(),
                param.getDescription() != null ? param.getDescription() : ""
        );
    }

    /**
     * RequestBody 스키마에서 파라미터 추출
     */
    @SuppressWarnings("unchecked")
    private void extractRequestBodyParams(Schema<?> schema, List<ApiParameter> parameters) {
        // $ref인 경우 참조 해결
        Schema<?> resolvedSchema = resolveSchema(schema);
        if (resolvedSchema == null) return;

        Map<String, Schema> properties = resolvedSchema.getProperties();
        if (properties == null) return;

        List<String> requiredFields = resolvedSchema.getRequired();
        if (requiredFields == null) requiredFields = Collections.emptyList();

        for (Map.Entry<String, Schema> entry : properties.entrySet()) {
            String name = entry.getKey();
            Schema propSchema = entry.getValue();

            String type = propSchema.getType() != null ? propSchema.getType() : "string";
            boolean required = requiredFields.contains(name);
            String description = propSchema.getDescription() != null ? propSchema.getDescription() : "";

            parameters.add(new ApiParameter(name, "body", type, required, description));
        }
    }

    /**
     * $ref 스키마 참조 해결
     */
    private Schema<?> resolveSchema(Schema<?> schema) {
        String ref = schema.get$ref();
        if (ref == null) return schema;

        // "#/components/schemas/Pet" -> "Pet"
        String schemaName = ref.substring(ref.lastIndexOf("/") + 1);

        if (openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null) {
            return openAPI.getComponents().getSchemas().get(schemaName);
        }

        return null;
    }

    /**
     * operationId가 없을 경우 자동 생성
     */
    private String generateOperationId(String method, String path) {
        String cleanPath = path
                .replace("/", "_")
                .replace("{", "")
                .replace("}", "")
                .replaceAll("^_+|_+$", "");

        return method.toLowerCase() + "_" + cleanPath;
    }

    /**
     * 서버 Base URL 가져오기
     */
    public String getBaseUrl() {
        if (openAPI != null && openAPI.getServers() != null && !openAPI.getServers().isEmpty()) {
            return openAPI.getServers().get(0).getUrl();
        }

        // 기본값: 스펙 URL에서 추출
        return specUrl
                .replaceAll("/v3/api-docs.*", "")
                .replaceAll("/swagger.*", "")
                .replaceAll("/openapi.*", "");
    }

    /**
     * API 제목 가져오기
     */
    public String getApiTitle() {
        if (openAPI != null && openAPI.getInfo() != null) {
            return openAPI.getInfo().getTitle();
        }
        return null;
    }

    /**
     * API 버전 가져오기
     */
    public String getApiVersion() {
        if (openAPI != null && openAPI.getInfo() != null) {
            return openAPI.getInfo().getVersion();
        }
        return null;
    }
}
