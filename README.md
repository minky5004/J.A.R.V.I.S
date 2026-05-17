# J.A.R.V.I.S

> 음성 명령을 이해하고 의도를 파악하여 자동으로 기능을 실행하는 **AI 어시스턴트 백엔드 서비스**입니다.

---

## 1. 프로젝트 목적

자연어(한국어) 음성 입력을 처리하여 사용자의 의도를 정확히 파악하고, Tool Calling을 통해 날씨 조회, 일정 관리, 웹 검색 등의 기능을 자동으로 실행하는 지능형 AI 어시스턴트 시스템을 구현합니다.

**포트폴리오 목표**:
- Spring AI를 활용한 LLM Tool Calling 패턴 구현
- OpenAI API와 Whisper를 통한 음성-텍스트 처리 파이프라인
- 자연어 이해(NLU)를 바탕으로 한 의도 기반 기능 자동 실행

---

## 2. 처리 흐름

### 음성 → AI 응답 파이프라인

```
음성 파일 (wav/mp3)
    │
    ▼
STT (Whisper API)
    │
    ▼
텍스트 추출
    │
    ▼
Spring AI (GPT + Tool Calling)
    │
    ├── 의도 파악
    ├── Tool Calling 판단
    ├── 날씨 조회 (Open-Meteo API)
    ├── 일정 관리 (Google Calendar API)
    └── 웹 검색
    │
    ▼
응답 반환
```

---

## 3. 주요 기능

- ✅ 음성 파일 입력 → STT 변환 → 텍스트 추출
- ✅ 자연어 이해(NLU) → 사용자 의도 파악
- ✅ Tool Calling 기반 기능 실행
  - 날씨 정보 조회 (Open-Meteo API)
  - 일정 관리 (Google Calendar API)
  - 웹 검색
- ✅ 결과 텍스트 응답 반환

---

## 4. 기술 스택

- **Language**: Java 21
- **Framework**: Spring Boot 4.0.6
- **AI Model**: GPT (OpenAI) + Spring AI 1.0.0-M3
- **STT (Speech-to-Text)**: OpenAI Whisper API
- **Build**: Gradle

---

## 5. 로컬 실행 방법

### 사전 요구사항

- Java 21+
- Gradle
- OpenAI API Key

### 1단계 — 환경변수 설정

```bash
export OPENAI_API_KEY=your-openai-api-key
```

### 2단계 — 빌드 및 실행

```bash
./gradlew build
./gradlew bootRun
```

서버가 뜨면 `http://localhost:8080/api` 로 접근할 수 있습니다.

---

## 6. API 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| `POST` | `/api/voice/process` | 음성 파일 처리 및 AI 응답 반환 |

---

## 7. 프로젝트 가치

- **LLM 통합**: Spring AI를 활용한 OpenAI GPT 모델 연동
- **Tool Calling**: 자연어 기반 기능 자동 실행 패턴
- **음성 처리**: STT 파이프라인 구축
- **한국어 NLU**: 자연스러운 한국어 명령 처리
