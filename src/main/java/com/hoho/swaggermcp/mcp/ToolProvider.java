package com.hoho.swaggermcp.mcp;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Tool 제공자 인터페이스
 *
 * Swagger 파서가 이 인터페이스를 구현하여 동적으로 Tool을 생성합니다.
 */
public interface ToolProvider {

    /**
     * 사용 가능한 모든 Tool 목록 반환
     */
    List<McpModels.Tool> getTools();

    /**
     * Tool 호출 실행
     *
     * @param name Tool 이름 (operationId)
     * @param arguments 호출 인자
     * @return 호출 결과
     */
    McpModels.CallToolResult callTool(String name, JsonNode arguments);
}
