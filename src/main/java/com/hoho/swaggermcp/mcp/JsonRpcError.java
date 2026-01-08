package com.hoho.swaggermcp.mcp;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * JSON-RPC 2.0 에러 객체
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonRpcError {
    private int code;
    private String message;
    private Object data;

    // 표준 에러 코드
    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;

    public JsonRpcError() {}

    public JsonRpcError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public JsonRpcError(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
