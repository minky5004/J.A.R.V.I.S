# J.A.R.V.I.S - AI 어시스턴트 백엔드 서비스

음성 명령을 이해하고 의도를 파악하여 자동으로 기능을 실행하는 AI 어시스턴트 백엔드 서비스입니다.  
**Spring AI + OpenAI GPT-4o-mini + Whisper**를 통해 자연어(한국어) 기반 의도 파악 및 Tool Calling을 구현했습니다.

## 🎯 주요 기능

### Phase 8-3: 고급 기능 (완료)
- ✅ **대화 이력 관리** - PostgreSQL + JPA 기반 세션별 메시지 저장 및 30일 자동 정리
- ✅ **스트리밍 응답** - SSE 기반 token-by-token 실시간 응답
- ✅ **응답 캐싱** - Redis 하이브리드 캐싱 (세션별 + 전역 공유)
- ✅ **자동 언어 감지** - Whisper API를 통한 다국어 자동 감지
- ✅ **레이트 리미팅** - Token Bucket 알고리즘 기반 분당 10회 제한

### 지원하는 Tool (8개)

| # | Tool | API | 설명 |
|----|------|-----|------|
| 1 | `weatherInfo()` | Open-Meteo | 현재 날씨 및 예보 조회 |
| 2 | `searchNews()` | DuckDuckGo | 뉴스 검색 |
| 3 | `searchWeb()` | DuckDuckGo | 웹 검색 |
| 4 | `translate()` | MyMemory | 텍스트 번역 |
| 5 | `calculate()` | 내장 | 수식 계산 |
| 6 | `convertTime()` | 내장 | 시간 변환 |
| 7 | `convertUnit()` | 내장 | 단위 변환 |
| 8 | `speakText()` | OpenAI TTS | 텍스트 음성 변환 |

## 🛠️ 기술 스택

| 구분 | 기술 |
|------|------|
| **Language** | Java 21 |
| **Framework** | Spring Boot 4.0.6 |
| **AI Model** | GPT-4o-mini (OpenAI) + Spring AI 2.0.0-M4 |
| **STT** | OpenAI Whisper API |
| **TTS** | OpenAI Text-to-Speech API |
| **Cache** | Redis 7-Alpine |
| **Database** | PostgreSQL |
| **Build** | Gradle |

## 📦 설치 방법

### 사전 요구사항
- Docker & Docker Compose
- 또는 Java 21 + PostgreSQL + Redis

### 1. 저장소 클론
```bash
git clone https://github.com/minky5004/J.A.R.V.I.S.git
cd J.A.R.V.I.S
```

### 2. 환경변수 설정
`.env` 파일 생성 또는 `application.yml`에서 설정:

```bash
# OpenAI API
OPENAI_API_KEY=your-openai-api-key

# Database (PostgreSQL)
# 주의: URL과 USERNAME은 application.yml에 하드코딩됨
# jdbc:postgresql://localhost:5432/jarvis
# username: jarvis
DB_PASSWORD=your-db-password

# Redis
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379
```

### 3-A. Docker Compose로 실행 (권장)
```bash
docker-compose up -d
```

### 3-B. 로컬 환경에서 실행
```bash
# 데이터베이스 및 캐시 준비
docker-compose up -d postgres redis

# 애플리케이션 실행
./gradlew bootRun
```

## 🚀 실행 방법

### 애플리케이션 시작
```bash
docker-compose up -d
```

### 헬스 체크
```bash
curl http://localhost:8080/health
```

### 로그 확인
```bash
docker-compose logs -f jarvis-app
```

## 📡 API 사용 예시

### 1. 음성 처리 (Voice)
**POST** `/api/voice/process`

요청:
```bash
curl -X POST http://localhost:8080/api/voice/process \
  -H "Content-Type: multipart/form-data" \
  -F "file=@audio.wav" \
  -F "sessionId=user-123"
```

응답:
```json
{
  "success": true,
  "message": "음성 처리 완료",
  "data": {
    "id": "response-uuid",
    "sessionId": "user-123",
    "transcript": "서울 날씨 알려줘",
    "language": "ko",
    "intent": "weather_info",
    "resultText": "서울의 현재 날씨는 맑고 기온은 25도입니다.",
    "result": {}
  }
}
```

### 2. 텍스트 처리 (Voice to Text)
**POST** `/api/voice/process-text`

요청:
```bash
curl -X POST http://localhost:8080/api/voice/process-text \
  -H "Content-Type: application/json" \
  -d '{
    "text": "서울 날씨 알려줘",
    "sessionId": "user-123"
  }'
```

### 3. 스트리밍 응답 (SSE)
**POST** `/api/voice/process-stream`

요청:
```bash
curl -X POST http://localhost:8080/api/voice/process-stream \
  -H "Content-Type: multipart/form-data" \
  -F "file=@audio.wav" \
  -F "sessionId=user-123"
```

응답 (Server-Sent Events):
```json
data: {"chunk":"서울의"}
data: {"chunk":"현재"}
data: {"chunk":"날씨는"}
...
```

### 4. 대화 이력 조회
**GET** `/api/conversation/{sessionId}`

```bash
curl http://localhost:8080/api/conversation/user-123
```

응답:
```json
{
  "sessionId": "user-123",
  "conversations": [
    {
      "id": "conv-id",
      "message": "서울 날씨 알려줘",
      "role": "USER",
      "createdAt": "2026-05-29T10:30:00Z"
    },
    {
      "id": "conv-id",
      "message": "서울의 현재 날씨는 맑습니다.",
      "role": "AI",
      "createdAt": "2026-05-29T10:30:01Z"
    }
  ]
}
```

## ⚙️ 환경 설정

### application.yml 예시

**개발 환경 (dev)**
```yaml
spring:
  application:
    name: jarvis
  profiles:
    active: dev
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      model: gpt-4o-mini
  datasource:
    url: jdbc:postgresql://localhost:5432/jarvis
    username: jarvis
    password: ${DB_PASSWORD}
  redis:
    host: localhost
    port: 6379
```

**운영 환경 (prod)**
```yaml
spring:
  profiles:
    active: prod
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
  datasource:
    url: ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  redis:
    host: ${REDIS_HOST}
    port: ${REDIS_PORT}
```

## 🔒 보안 고려사항

### 레이트 리미팅
- 사용자별 분당 최대 10회 요청 제한 (Token Bucket 알고리즘)
- 429 Too Many Requests 응답

### API 키 보안
- 환경변수로만 관리 (.env 파일 .gitignore에 포함)
- 절대 코드에 하드코딩하지 않기

## 📊 데이터베이스 스키마

### conversations 테이블
```sql
CREATE TABLE conversations (
  id BIGSERIAL PRIMARY KEY,
  session_id VARCHAR(255) NOT NULL,
  message TEXT NOT NULL,
  role VARCHAR(50) NOT NULL,
  is_deleted BOOLEAN DEFAULT false,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_conversation_query ON conversations(session_id, is_deleted, created_at DESC);
CREATE INDEX idx_session_id ON conversations(session_id);
```

## 🐳 Docker 관련

### 이미지 빌드
```bash
docker build -t jarvis-app:latest .
```

### 컨테이너 실행
```bash
docker run -d \
  --name jarvis \
  -p 8080:8080 \
  -e OPENAI_API_KEY=your-key \
  jarvis-app:latest
```

## 📝 라이선스

MIT License

## 🤝 기여

버그 리포트 및 기능 요청은 GitHub Issues를 통해 제출해주세요.

## 📧 연락처

- Email: minky5004@gmail.com
- GitHub: [@minky5004](https://github.com/minky5004)
