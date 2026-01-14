package com.hoho.swaggermcp;

import com.hoho.swaggermcp.mcp.McpServer;
import com.hoho.swaggermcp.swagger.SwaggerToolProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SwaggerMCP 메인 진입점
 *
 * 사용법:
 *   # 단일 API (하위호환)
 *   java -jar swaggerMCP.jar http://localhost:8080/v3/api-docs
 *
 *   # 멀티 API
 *   java -jar swaggerMCP.jar --api 로컬=http://localhost:8080/v3/api-docs --api 개발=http://dev-server/v3/api-docs
 *
 *   # API with Authorization header
 *   java -jar swaggerMCP.jar --api myapi=http://localhost:8080/v3/api-docs --auth myapi="Bearer your-token"
 *
 * Docker 예시:
 *   docker run -i --rm ghcr.io/mary-code217/swagger-mcp:latest --api 로컬=http://host.docker.internal:8080/v3/api-docs
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        // UTF-8 인코딩 강제 설정 (Windows 한글 지원)
        try {
            System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
        } catch (Exception e) {
            // 무시 - 인코딩 설정 실패해도 계속 진행
        }

        Map<String, String> apis = parseApiArgs(args);
        Map<String, String> authHeaders = parseAuthArgs(args);

        if (apis.isEmpty()) {
            printUsage();
            System.exit(1);
            return;
        }

        logger.info("SwaggerMCP 시작");
        logger.info("등록된 API: {}개", apis.size());
        apis.forEach((name, url) -> {
            String authInfo = authHeaders.containsKey(name) ? " (인증 설정됨)" : "";
            logger.info("  - {}: {}{}", name, url, authInfo);
        });

        try {
            // Swagger Tool Provider 초기화
            SwaggerToolProvider toolProvider = new SwaggerToolProvider(apis, authHeaders);
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
     * 커맨드라인 인자 파싱 - API URL
     *
     * 지원 형식:
     * 1. 단일 URL (하위호환): http://localhost:8080/v3/api-docs
     * 2. 멀티 API: --api 이름=URL --api 이름2=URL2
     */
    private static Map<String, String> parseApiArgs(String[] args) {
        Map<String, String> apis = new LinkedHashMap<>();

        if (args.length == 0) {
            // 환경변수에서 읽기
            String envUrl = System.getenv("SWAGGER_SPEC_URL");
            if (envUrl != null && !envUrl.isBlank()) {
                apis.put("default", envUrl);
            }
            return apis;
        }

        // --api 형식 파싱
        for (int i = 0; i < args.length; i++) {
            if ("--api".equals(args[i]) && i + 1 < args.length) {
                String apiArg = args[i + 1];
                int eqIndex = apiArg.indexOf('=');
                if (eqIndex > 0) {
                    String name = apiArg.substring(0, eqIndex).trim();
                    String url = apiArg.substring(eqIndex + 1).trim();
                    apis.put(name, url);
                }
                i++; // skip next arg
            } else if (!args[i].startsWith("--") && args[i].startsWith("http")) {
                // 하위호환: 단일 URL
                apis.put("default", args[i]);
            }
        }

        return apis;
    }

    /**
     * 커맨드라인 인자 파싱 - Authorization 헤더
     *
     * 지원 형식:
     * --auth API이름="Bearer token" 또는 --auth API이름="Basic base64"
     *
     * 환경변수: SWAGGER_AUTH_HEADER (단일 API용)
     */
    private static Map<String, String> parseAuthArgs(String[] args) {
        Map<String, String> authHeaders = new LinkedHashMap<>();

        // 환경변수에서 읽기 (단일 API용)
        String envAuth = System.getenv("SWAGGER_AUTH_HEADER");
        if (envAuth != null && !envAuth.isBlank()) {
            authHeaders.put("default", envAuth);
        }

        // --auth 형식 파싱
        for (int i = 0; i < args.length; i++) {
            if ("--auth".equals(args[i]) && i + 1 < args.length) {
                String authArg = args[i + 1];
                int eqIndex = authArg.indexOf('=');
                if (eqIndex > 0) {
                    String name = authArg.substring(0, eqIndex).trim();
                    String header = authArg.substring(eqIndex + 1).trim();
                    // 따옴표 제거
                    if ((header.startsWith("\"") && header.endsWith("\"")) ||
                        (header.startsWith("'") && header.endsWith("'"))) {
                        header = header.substring(1, header.length() - 1);
                    }
                    authHeaders.put(name, header);
                }
                i++; // skip next arg
            }
        }

        return authHeaders;
    }

    /**
     * 사용법 출력
     */
    private static void printUsage() {
        System.err.println("SwaggerMCP - Claude Code에서 Swagger API를 사용할 수 있게 해주는 MCP 서버");
        System.err.println();
        System.err.println("사용법:");
        System.err.println("  # 단일 API");
        System.err.println("  java -jar swagger-mcp.jar http://localhost:8080/v3/api-docs");
        System.err.println();
        System.err.println("  # 멀티 API");
        System.err.println("  java -jar swagger-mcp.jar --api 로컬=http://localhost:8080/v3/api-docs --api 개발=http://dev-server/v3/api-docs");
        System.err.println();
        System.err.println("  # API with Authorization (Bearer Token)");
        System.err.println("  java -jar swagger-mcp.jar --api myapi=http://localhost:8080/v3/api-docs --auth myapi=\"Bearer your-token\"");
        System.err.println();
        System.err.println("  # API with Authorization (Basic Auth)");
        System.err.println("  java -jar swagger-mcp.jar --api myapi=http://localhost:8080/v3/api-docs --auth myapi=\"Basic base64-encoded\"");
        System.err.println();
        System.err.println("Docker 예시:");
        System.err.println("  docker run -i --rm ghcr.io/mary-code217/swagger-mcp:latest --api 로컬=http://host.docker.internal:8080/v3/api-docs --auth 로컬=\"Bearer token\"");
        System.err.println();
        System.err.println("환경변수:");
        System.err.println("  SWAGGER_SPEC_URL: 단일 API URL (하위호환)");
        System.err.println("  SWAGGER_AUTH_HEADER: 단일 API용 Authorization 헤더 값 (예: \"Bearer xxx\")");
    }
}
