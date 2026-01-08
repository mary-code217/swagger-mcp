FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="hoho"
LABEL description="Swagger MCP Server for Claude Code"

WORKDIR /app

COPY build/libs/swaggerMCP-1.0-SNAPSHOT-all.jar /app/swagger-mcp.jar

# Swagger spec URL을 환경변수로 받음
ENV SWAGGER_URL=""
ENV BASE_URL=""

ENTRYPOINT ["java", "-jar", "/app/swagger-mcp.jar"]
CMD ["${SWAGGER_URL}", "${BASE_URL}"]
