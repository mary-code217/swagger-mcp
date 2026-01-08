package com.hoho.swaggermcp.swagger;

import com.hoho.swaggermcp.http.HttpApiClient;

import java.util.*;

/**
 * 단일 API 인스턴스를 나타내는 클래스
 *
 * 각 등록된 API별로 하나의 인스턴스가 생성됩니다.
 */
public class ApiInstance {

    private final String name;
    private final String specUrl;
    private final List<ApiEndpoint> endpoints = new ArrayList<>();
    private final Map<String, List<ApiEndpoint>> endpointsByTag = new LinkedHashMap<>();
    private HttpApiClient httpClient;
    private String baseUrl;
    private String apiTitle;
    private String apiVersion;

    public ApiInstance(String name, String specUrl) {
        this.name = name;
        this.specUrl = specUrl;
    }

    /**
     * API 초기화: Swagger 스펙 파싱
     */
    public void initialize() {
        SwaggerParser parser = new SwaggerParser(specUrl);
        endpoints.addAll(parser.parse());

        apiTitle = parser.getApiTitle();
        apiVersion = parser.getApiVersion();
        baseUrl = parser.getBaseUrl();

        httpClient = new HttpApiClient(baseUrl);

        // 태그별 그룹화
        for (ApiEndpoint endpoint : endpoints) {
            List<String> tags = endpoint.getTags();
            if (tags == null || tags.isEmpty()) {
                tags = Collections.singletonList("default");
            }
            for (String tag : tags) {
                endpointsByTag.computeIfAbsent(tag, k -> new ArrayList<>()).add(endpoint);
            }
        }
    }

    public String getName() {
        return name;
    }

    public String getSpecUrl() {
        return specUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getApiTitle() {
        return apiTitle;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public List<ApiEndpoint> getEndpoints() {
        return endpoints;
    }

    public Map<String, List<ApiEndpoint>> getEndpointsByTag() {
        return endpointsByTag;
    }

    public HttpApiClient getHttpClient() {
        return httpClient;
    }

    public int getEndpointCount() {
        return endpoints.size();
    }

    public int getCategoryCount() {
        return endpointsByTag.size();
    }
}
