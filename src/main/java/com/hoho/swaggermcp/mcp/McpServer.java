package com.hoho.swaggermcp.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 * MCP 서버 핵심 클래스
 *
 * stdin/stdout을 통해 Claude Code와 JSON-RPC 통신을 수행합니다.
 *
 * 동작 흐름:
 * 1. Claude Code가 stdin으로 JSON-RPC 요청 전송
 * 2. McpServer가 요청을 파싱하고 처리
 * 3. stdout으로 JSON-RPC 응답 반환
 */
public class McpServer {

    private static final Logger logger = LoggerFactory.getLogger(McpServer.class);

    private final ToolProvider toolProvider;
    private final ObjectMapper objectMapper;
    private final BufferedReader reader;
    private final PrintWriter writer;

    public McpServer(ToolProvider toolProvider) {
        this.toolProvider = toolProvider;
        this.objectMapper = new ObjectMapper();
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        this.writer = new PrintWriter(System.out, true);
    }

    /**
     * MCP 서버 메인 루프 시작
     */
    public void start() {
        logger.info("SwaggerMCP 서버 시작...");

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;

                logger.debug("수신: {}", line);
                JsonRpcResponse response = processRequest(line);

                if (response != null) {
                    String responseJson = objectMapper.writeValueAsString(response);
                    logger.debug("송신: {}", responseJson);
                    writer.println(responseJson);
                    writer.flush();
                    System.out.flush();
                }
            }
        } catch (Exception e) {
            logger.error("서버 실행 중 오류 발생", e);
        }

        logger.info("SwaggerMCP 서버 종료");
    }

    /**
     * JSON-RPC 요청 처리
     */
    private JsonRpcResponse processRequest(String line) {
        JsonRpcRequest request;
        try {
            request = objectMapper.readValue(line, JsonRpcRequest.class);
        } catch (Exception e) {
            logger.error("JSON 파싱 오류", e);
            return JsonRpcResponse.error(null, JsonRpcError.PARSE_ERROR, "JSON 파싱 오류: " + e.getMessage());
        }

        logger.info("메서드 호출: {}", request.getMethod());

        try {
            Object result = handleMethod(request);

            if (result == null) {
                return null;  // 알림 메시지는 응답 없음
            }

            return JsonRpcResponse.success(request.getId(), result);
        } catch (Exception e) {
            logger.error("메서드 처리 중 오류: {}", request.getMethod(), e);
            return JsonRpcResponse.error(request.getId(), JsonRpcError.INTERNAL_ERROR, e.getMessage());
        }
    }

    /**
     * 메서드별 처리
     */
    private Object handleMethod(JsonRpcRequest request) throws Exception {
        String method = request.getMethod();

        switch (method) {
            case "initialize":
                return handleInitialize();

            case "initialized":
            case "notifications/initialized":
                // 알림이므로 응답 없음
                logger.debug("알림 수신: {}", method);
                return null;

            case "tools/list":
                return handleListTools();

            case "tools/call":
                return handleCallTool(request.getParams());

            case "ping":
                return "pong";

            default:
                // 알 수 없는 메서드도 알림일 수 있으므로 id가 없으면 무시
                if (request.getId() == null) {
                    logger.debug("알 수 없는 알림 무시: {}", method);
                    return null;
                }
                logger.warn("알 수 없는 메서드: {}", method);
                throw new IllegalArgumentException("Unknown method: " + method);
        }
    }

    /**
     * initialize 메서드 처리
     */
    private McpModels.InitializeResult handleInitialize() {
        logger.info("클라이언트 초기화 요청");
        return new McpModels.InitializeResult();
    }

    /**
     * tools/list 메서드 처리
     */
    private McpModels.ListToolsResult handleListTools() {
        logger.info("도구 목록 요청");
        return new McpModels.ListToolsResult(toolProvider.getTools());
    }

    /**
     * tools/call 메서드 처리
     */
    private McpModels.CallToolResult handleCallTool(JsonNode params) throws Exception {
        if (params == null) {
            throw new IllegalArgumentException("params가 필요합니다");
        }

        McpModels.CallToolParams callParams = objectMapper.treeToValue(params, McpModels.CallToolParams.class);
        logger.info("도구 호출: {}", callParams.getName());

        return toolProvider.callTool(callParams.getName(), callParams.getArguments());
    }
}
