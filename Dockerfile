# Stage 1: Builder - Gradle로 애플리케이션 빌드
FROM gradle:8.8-jdk21-alpine AS builder

WORKDIR /app

# Gradle 캐시 최적화: 의존성 파일 먼저 복사
COPY gradle/ gradle/
COPY gradlew gradlew
COPY build.gradle build.gradle
COPY settings.gradle settings.gradle

# 의존성 다운로드 (캐시 레이어)
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY src/ src/

# 애플리케이션 빌드
RUN ./gradlew clean build -x test --no-daemon

# Stage 2: Runtime - 경량 실행 환경
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 빌더 스테이지에서 생성된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 헬스체크
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 포트 노출
EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]