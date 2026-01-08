package com.hoho.swaggermcp.mcp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * JSON-RPC 2.0 요청 객체
 *
 * Claude Code가 MCP 서버로 보내는 요청 형식입니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonRpcRequest {
    private String jsonrpc = "2.0";
    private JsonNode id;
    private String method;
    private JsonNode params;

    public JsonRpcRequest() {}

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public JsonNode getId() {
        return id;
    }

    public void setId(JsonNode id) {
        this.id = id;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public JsonNode getParams() {
        return params;
    }

    public void setParams(JsonNode params) {
        this.params = params;
    }
}
