package com.hoho.swaggermcp.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HTTP API 클라이언트
 *
 * MCP Tool 호출 시 실제 API 서버로 요청을 전송합니다.
 */
public class HttpApiClient {

    private static final Logger logger = LoggerFactory.getLogger(HttpApiClient.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final String baseUrl;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public HttpApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * HTTP 요청 수행
     */
    public String request(String method, String path, Map<String, String> queryParams,
                          Map<String, String> headers, Map<String, Object> body) throws IOException {

        String url = buildUrl(path, queryParams);
        logger.info("HTTP 요청: {} {}", method, url);

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .header("Accept", "application/json");

        // 헤더 추가
        if (headers != null) {
            headers.forEach(requestBuilder::header);
        }

        // HTTP 메서드별 처리
        RequestBody requestBody = null;
        if (body != null && !body.isEmpty()) {
            String jsonBody = objectMapper.writeValueAsString(body);
            logger.debug("요청 바디: {}", jsonBody);
            requestBody = RequestBody.create(jsonBody, JSON);
        }

        switch (method.toUpperCase()) {
            case "GET":
                requestBuilder.get();
                break;
            case "POST":
                requestBuilder.post(requestBody != null ? requestBody : RequestBody.create("", null));
                break;
            case "PUT":
                requestBuilder.put(requestBody != null ? requestBody : RequestBody.create("", null));
                break;
            case "DELETE":
                if (requestBody != null) {
                    requestBuilder.delete(requestBody);
                } else {
                    requestBuilder.delete();
                }
                break;
            case "PATCH":
                requestBuilder.patch(requestBody != null ? requestBody : RequestBody.create("", null));
                break;
            default:
                throw new IllegalArgumentException("지원하지 않는 HTTP 메서드: " + method);
        }

        try (Response response = client.newCall(requestBuilder.build()).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            logger.info("HTTP 응답: {} {}", response.code(), response.message());
            logger.debug("응답 바디: {}", responseBody);

            return formatResponse(response.code(), response.message(), responseBody);
        }
    }

    /**
     * URL 빌드 (Base URL + Path + Query Parameters)
     */
    private String buildUrl(String path, Map<String, String> queryParams) {
        StringBuilder urlBuilder = new StringBuilder(baseUrl);

        if (!path.startsWith("/")) {
            urlBuilder.append("/");
        }
        urlBuilder.append(path);

        if (queryParams != null && !queryParams.isEmpty()) {
            urlBuilder.append("?");
            boolean first = true;
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                if (!first) urlBuilder.append("&");
                urlBuilder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
                first = false;
            }
        }

        return urlBuilder.toString();
    }

    /**
     * 응답 포맷팅
     */
    private String formatResponse(int statusCode, String statusMessage, String body) {
        StringBuilder result = new StringBuilder();
        result.append("=== HTTP 응답 ===\n");
        result.append("상태: ").append(statusCode).append(" ").append(statusMessage).append("\n\n");

        if (body != null && !body.isBlank()) {
            // JSON 포맷팅 시도
            try {
                JsonNode jsonNode = objectMapper.readTree(body);
                String prettyJson = objectMapper.writeValueAsString(jsonNode);
                result.append("응답 데이터:\n").append(prettyJson);
            } catch (Exception e) {
                // JSON이 아니면 그대로 출력
                result.append("응답 데이터:\n").append(body);
            }
        } else {
            result.append("(응답 바디 없음)");
        }

        return result.toString();
    }

    /**
     * 클라이언트 종료
     */
    public void close() {
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
    }
}
