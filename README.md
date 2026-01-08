# SwaggerMCP

Claude Code에서 Swagger API를 바로 사용할 수 있게 해주는 MCP 서버입니다.

> **1000개 이상의 API도 문제없이!** 4개의 Tool로 효율적으로 탐색하고 호출할 수 있습니다.

---

## 빠른 시작

### 방법 1: CLI로 추가 (권장)

```bash
# Docker 사용 (Java 설치 불필요)
claude mcp add --transport stdio swagger-api -- docker run -i --rm ghcr.io/mary-code217/swagger-mcp:latest http://your-api/v3/api-docs

# 또는 JAR 직접 실행 (Java 17+ 필요)
claude mcp add --transport stdio swagger-api -- java -jar /path/to/swagger-mcp.jar http://your-api/v3/api-docs
```

### 방법 2: 설정 파일 직접 수정

`C:\Users\{사용자명}\.claude.json` 파일의 `mcpServers`에 추가:

```json
{
  "mcpServers": {
    "swagger-api": {
      "type": "stdio",
      "command": "docker",
      "args": [
        "run", "-i", "--rm",
        "ghcr.io/mary-code217/swagger-mcp:latest",
        "http://your-api-server/v3/api-docs"
      ]
    }
  }
}
```

### 설정 후

Claude Code를 재시작하면 바로 사용 가능!

---

## MCP 관리 명령어

```bash
# MCP 서버 목록 확인
claude mcp list

# MCP 서버 제거
claude mcp remove swagger-api

# MCP 서버 정보 확인
claude mcp get swagger-api
```

### Scope 옵션

```bash
# 현재 프로젝트만 (기본값)
claude mcp add --scope local ...

# 프로젝트 공유 (.mcp.json 생성, git으로 공유 가능)
claude mcp add --scope project ...

# 전역 (모든 프로젝트에서 사용)
claude mcp add --scope user ...
```

---

## 사용 방법

### API 둘러보기

```
"API 카테고리 보여줘"
```
→ 전체 API 카테고리 목록이 표시됩니다.

```
"user-controller 엔드포인트 보여줘"
```
→ 해당 카테고리의 API 목록이 표시됩니다.

### API 검색하기

```
"로그인 관련 API 찾아줘"
```
→ 'login' 키워드로 API를 검색합니다.

```
"사용자 생성 API 파라미터 알려줘"
```
→ 해당 API의 상세 정보와 필수 파라미터를 확인할 수 있습니다.

### API 호출하기

```
"getAllUsers API 호출해줘"
```
→ 실제 API를 호출하고 결과를 보여줍니다.

```
"createUser API로 새 사용자 만들어줘. 이름은 홍길동, 이메일은 hong@test.com"
```
→ 파라미터와 함께 API를 호출합니다.

---

## 제공되는 기능

| 명령 | 설명 |
|------|------|
| `list_api_categories` | 전체 API 카테고리 목록 조회 |
| `list_api_endpoints` | 특정 카테고리의 API 목록 조회 |
| `search_api` | 키워드로 API 검색 |
| `call_api` | API 직접 호출 |

---

## 설치 옵션

### 옵션 A: Docker (권장)

Java 설치 없이 Docker만 있으면 됩니다.

```bash
# CLI로 추가
claude mcp add --transport stdio swagger-api -- docker run -i --rm ghcr.io/mary-code217/swagger-mcp:latest http://your-api/v3/api-docs
```

또는 설정 파일:

```json
{
  "mcpServers": {
    "swagger-api": {
      "type": "stdio",
      "command": "docker",
      "args": [
        "run", "-i", "--rm",
        "ghcr.io/mary-code217/swagger-mcp:latest",
        "http://your-api-server/v3/api-docs"
      ]
    }
  }
}
```

### 옵션 B: JAR 직접 실행

Java 17 이상이 설치되어 있으면 JAR로 직접 실행할 수 있습니다.

```bash
# 1. JAR 다운로드
curl -L -o swagger-mcp.jar https://github.com/mary-code217/swagger-mcp/releases/download/v1.0.0/swagger-mcp.jar

# 2. MCP 추가
claude mcp add --scope user --transport stdio swagger-api -- java -jar ./swagger-mcp.jar http://your-api/v3/api-docs
```

또는 설정 파일:

```json
{
  "mcpServers": {
    "swagger-api": {
      "type": "stdio",
      "command": "java",
      "args": [
        "-jar",
        "C:\\path\\to\\swagger-mcp.jar",
        "http://your-api-server/v3/api-docs"
      ]
    }
  }
}
```

> Java 17 이상 필요

---

## 자주 묻는 질문

### Q: Tool이 안 보여요
1. Claude Code 재시작
2. `claude mcp list`로 서버가 추가되었는지 확인
3. Swagger URL이 접속 가능한지 확인

### Q: localhost API를 사용하고 싶어요
Docker 사용 시 `localhost` 대신 `host.docker.internal` 사용:
```
http://host.docker.internal:8080/v3/api-docs
```

### Q: API 호출이 실패해요
- API 서버가 실행 중인지 확인
- 네트워크 접근이 가능한지 확인
- 인증이 필요한 API인지 확인

---

## 지원하는 API 스펙

- OpenAPI 3.0 / 3.1
- Swagger 2.0

---

## 문의

문제가 있으면 [Issues](../../issues)에 등록해주세요.
