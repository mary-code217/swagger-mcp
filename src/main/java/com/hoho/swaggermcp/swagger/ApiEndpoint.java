package com.hoho.swaggermcp.swagger;

import java.util.List;

/**
 * API 엔드포인트 정보를 담는 클래스
 */
public class ApiEndpoint {
    private String operationId;
    private String method;
    private String path;
    private String summary;
    private String description;
    private List<ApiParameter> parameters;
    private List<String> tags;

    public ApiEndpoint() {}

    public ApiEndpoint(String operationId, String method, String path, String summary,
                       String description, List<ApiParameter> parameters, List<String> tags) {
        this.operationId = operationId;
        this.method = method;
        this.path = path;
        this.summary = summary;
        this.description = description;
        this.parameters = parameters;
        this.tags = tags;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ApiParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<ApiParameter> parameters) {
        this.parameters = parameters;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
