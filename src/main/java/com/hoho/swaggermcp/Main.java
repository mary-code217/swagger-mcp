package com.hoho.swaggermcp;

import com.hoho.swaggermcp.mcp.McpServer;
import com.hoho.swaggermcp.swagger.SwaggerToolProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SwaggerMCP 메인 진입점
 *
 * 사용법:
 *   java -jar swaggerMCP.jar <swagger-spec-url> [base-url]
 *
 * 예시:
 *   java -jar swaggerMCP.jar http://localhost:8080/v3/api-docs
 *   java -jar swaggerMCP.jar http://localhost:8080/v3/api-docs http://localhost:8080
 *
 * 환경변수로도 설정 가능:
 *   SWAGGER_SPEC_URL: Swagger 스펙 URL
 *   API_BASE_URL: API Base URL (선택)
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        // 설정 로드
        String specUrl = getConfig(args, 0, "SWAGGER_SPEC_URL");
        String baseUrl = getConfig(args, 1, "API_BASE_URL");

        if (specUrl == null) {
            printUsage();
            System.exit(1);
            return;
        }

        logger.info("SwaggerMCP 시작");
        logger.info("Swagger 스펙 URL: {}", specUrl);
        if (baseUrl != null) {
            logger.info("API Base URL: {}", baseUrl);
        }

        try {
            // Swagger Tool Provider 초기화
            SwaggerToolProvider toolProvider = new SwaggerToolProvider(specUrl, baseUrl);
            toolProvider.initialize();

            // MCP 서버 시작
            McpServer server = new McpServer(toolProvider);
            server.start();

        } catch (Exception e) {
            logger.error("SwaggerMCP 실행 중 오류 발생", e);
            System.err.println("오류: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * 설정 값 가져오기 (커맨드라인 인자 > 환경변수)
     */
    private static String getConfig(String[] args, int index, String envName) {
        if (args.length > index && args[index] != null && !args[index].isBlank()) {
            return args[index];
        }
        return System.getenv(envName);
    }

    /**
     * 사용법 출력
     */
    private static void printUsage() {
        System.err.println("사용법: java -jar swaggerMCP.jar <swagger-spec-url> [base-url]");
        System.err.println();
        System.err.println("예시:");
        System.err.println("  java -jar swaggerMCP.jar http://localhost:8080/v3/api-docs");
        System.err.println("  java -jar swaggerMCP.jar https://petstore3.swagger.io/api/v3/openapi.json");
        System.err.println();
        System.err.println("환경변수:");
        System.err.println("  SWAGGER_SPEC_URL: Swagger 스펙 URL");
        System.err.println("  API_BASE_URL: API Base URL (선택)");
    }
}
