package com.hoho.swaggermcp.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * JSON-RPC 2.0 응답 객체
 *
 * MCP 서버가 Claude Code로 보내는 응답 형식입니다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonRpcResponse {
    private String jsonrpc = "2.0";
    private JsonNode id;
    private Object result;
    private JsonRpcError error;

    public JsonRpcResponse() {}

    public static JsonRpcResponse success(JsonNode id, Object result) {
        JsonRpcResponse response = new JsonRpcResponse();
        response.setId(id);
        response.setResult(result);
        return response;
    }

    public static JsonRpcResponse error(JsonNode id, int code, String message) {
        JsonRpcResponse response = new JsonRpcResponse();
        response.setId(id);
        response.setError(new JsonRpcError(code, message));
        return response;
    }

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

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public JsonRpcError getError() {
        return error;
    }

    public void setError(JsonRpcError error) {
        this.error = error;
    }
}
