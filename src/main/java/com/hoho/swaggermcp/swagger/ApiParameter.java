package com.hoho.swaggermcp.swagger;

/**
 * API 파라미터 정보를 담는 클래스
 */
public class ApiParameter {
    private String name;
    private String location;  // "path", "query", "header", "body"
    private String type;
    private boolean required;
    private String description;

    public ApiParameter() {}

    public ApiParameter(String name, String location, String type, boolean required, String description) {
        this.name = name;
        this.location = location;
        this.type = type;
        this.required = required;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
