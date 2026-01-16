# SwaggerMCP

Claude Code에서 Swagger API를 바로 사용할 수 있게 해주는 MCP 서버입니다.

---

## 빠른 시작

### 1. MCP 서버 등록

```bash
claude mcp add --scope user --transport stdio swagger-api -- docker run -i --rm ghcr.io/mary-code217/swagger-mcp:latest
```

### 2. 설정 파일에서 API 추가 (권장)

`C:\Users\{사용자명}\.claude.json` 파일을 직접 수정하는 것이 가장 편리합니다:

```json
{
  "mcpServers": {
    "swagger-api": {
      "type": "stdio",
      "command": "docker",
      "args": [
        "run", "-i", "--rm",
        "ghcr.io/mary-code217/swagger-mcp:latest",
        "--api", "로컬=http://host.docker.internal:8080/v3/api-docs",
        "--api", "개발=http://dev-server/v3/api-docs"
      ]
    }
  }
}
```

> **💡 Tip:** API 추가/삭제, 토큰 설정 등 모든 설정 변경은 `.claude.json` 파일을 직접 수정하는 것이 가장 빠르고 편합니다.

### 3. Claude Code 재시작

설정 저장 후 Claude Code를 재시작하면 바로 사용 가능!

---

## 인증 설정 (Authorization 헤더)

### 방법 1: 설정 파일에 토큰 저장 (권장)

`.claude.json`의 args에 `--auth` 옵션 추가:

```json
{
  "mcpServers": {
    "swagger-api": {
      "type": "stdio",
      "command": "docker",
      "args": [
        "run", "-i", "--rm",
        "ghcr.io/mary-code217/swagger-mcp:latest",
        "--api", "myapi=http://host.docker.internal:8080/v3/api-docs",
        "--auth", "myapi=Bearer your-jwt-token"
      ]
    }
  }
}
```

> **💡 Tip:** 토큰이 만료되면 `.claude.json` 파일의 `--auth` 값만 수정하고 Claude Code 재시작

### 방법 2: Claude에게 직접 토큰 전달

설정 없이 대화 중에 토큰을 전달할 수도 있습니다:
```
"이 토큰으로 profile API 호출해줘: Bearer eyJhbGci..."
```

### 지원하는 인증 형식

| 형식 | 예시 |
|------|------|
| Bearer Token | `--auth myapi="Bearer eyJhbGciOiJ..."` |
| Basic Auth | `--auth myapi="Basic dXNlcjpwYXNz"` |
| API Key | `--auth myapi="ApiKey your-api-key"` |

---

## 사용 방법

### 단일 API 모드

```
"API 카테고리 보여줘"
"user-controller 엔드포인트 보여줘"
"로그인 관련 API 찾아줘"
"getAllUsers API 호출해줘"
```

### 멀티 API 모드

```
"등록된 API 목록 보여줘"
"로컬 API 카테고리 보여줘"
"개발서버에서 user 관련 API 검색해줘"
"운영 API의 getUser 호출해줘"
```

> 멀티 API 모드에서 API를 지정하지 않으면 등록된 API 목록을 보여줍니다.

---

## 제공되는 기능

| 기능 | 설명 |
|------|------|
| `list_registered_apis` | 등록된 API 서버 목록 (멀티 API 모드) |
| `list_api_categories` | API 카테고리(태그) 목록 조회 |
| `list_api_endpoints` | 특정 카테고리의 API 목록 조회 |
| `search_api` | 키워드로 API 검색 |
| `call_api` | API 직접 호출 |

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

---

## 자주 묻는 질문

### Q: localhost API를 사용하고 싶어요

Docker에서 호스트의 localhost에 접근하려면 `host.docker.internal` 사용:

```
http://host.docker.internal:8080/v3/api-docs
```

### Q: Tool이 안 보여요

1. Claude Code 재시작
2. `claude mcp list`로 서버가 추가되었는지 확인
3. Docker가 실행 중인지 확인
4. Swagger URL이 접속 가능한지 확인

### Q: API 호출이 실패해요

- API 서버가 실행 중인지 확인
- 네트워크 접근이 가능한지 확인
- 인증이 필요한 API라면 위의 [인증 설정](#인증-설정-authorization-헤더) 참고

### Q: API를 추가/수정하고 싶어요

`.claude.json` 파일의 args를 직접 수정 후 Claude Code 재시작:

```json
"args": [
  "run", "-i", "--rm",
  "ghcr.io/mary-code217/swagger-mcp:latest",
  "--api", "로컬=http://host.docker.internal:8080/v3/api-docs",
  "--api", "신규=http://new-server/v3/api-docs"
]
```

### Q: 새 버전으로 업데이트하고 싶어요

Docker 이미지는 자동으로 업데이트되지 않습니다. 새 버전이 나오면 직접 pull 해주세요:

```bash
docker pull ghcr.io/mary-code217/swagger-mcp:latest
```

pull 후 Claude Code를 재시작하면 새 버전이 적용됩니다.

---

## 지원하는 API 스펙

- OpenAPI 3.0 / 3.1
- Swagger 2.0

---

## 요구 사항

- Docker
- Claude Code

---

## 문의

문제가 있으면 [Issues](../../issues)에 등록해주세요.
