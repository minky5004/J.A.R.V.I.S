package com.jarvis.service;

import com.jarvis.config.VoiceConfig;
import com.jarvis.dto.VoiceData;
import com.jarvis.entity.ConversationRole;
import com.jarvis.exception.InvalidFileException;
import com.jarvis.exception.VoiceProcessingException;
import com.jarvis.tool.JarvisTools;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceService {

    private final ChatClient chatClient;
    private final JarvisTools jarvisTools;
    private final RestTemplate restTemplate;
    private final ConversationService conversationService;

    @Value("${spring.ai.openai.api-key}")
    private String openAiApiKey;

    public VoiceData processVoice(MultipartFile file, String sessionId) {
        validateFile(file);

        String id = UUID.randomUUID().toString();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }
        long fileSize = file.getSize();
        log.info("음성 처리 시작 - ID: {}, sessionId: {}, 크기: {}bytes", id, sessionId, fileSize);

        // 1. STT 변환 + 언어 자동 감지
        TranscriptionResult transcription = extractTranscriptFromFile(file);
        String transcript = transcription.text();
        String language = transcription.language();
        log.info("감지된 언어: {}", language);

        // 사용자 메시지 저장
        conversationService.saveMessage(sessionId, transcript, ConversationRole.USER);

        // 2. ChatClient로 처리 (Tool Calling 포함)
        String aiResponse;
        try {
            aiResponse = chatClient.prompt()
                .system(buildSystemPrompt(language))
                .user(transcript)
                .tools(jarvisTools)
                .call()
                .content();
        } catch (Exception e) {
            log.error("ChatClient 처리 중 오류 발생 - ID: {}", id, e);
            throw new VoiceProcessingException("음성 처리 중 오류가 발생했습니다.", e);
        }

        // AI 응답 저장
        conversationService.saveMessage(sessionId, aiResponse, ConversationRole.ASSISTANT);

        // 3. AI 응답을 자동으로 음성으로 변환
        String response = convertToAudio(aiResponse);

        log.info("음성 처리 완료 - ID: {}, 언어: {}, sessionId: {}", id, language, sessionId);

        String intent = extractIntent(transcript);
        return VoiceData.builder()
            .id(id)
            .sessionId(sessionId)
            .transcript(transcript)
            .language(language)
            .intent(intent)
            .resultText(aiResponse)
            .result(response)
            .build();
    }

    private String extractIntent(String transcript) {
        if (transcript == null || transcript.isEmpty()) {
            return "음성 명령 처리";
        }
        return transcript.length() > 50 ? transcript.substring(0, 50) + "..." : transcript;
    }

    private TranscriptionResult extractTranscriptFromFile(MultipartFile file) {
        try {
            byte[] fileBytes = file.getBytes();
            String filename = file.getOriginalFilename();
            String fileId = UUID.randomUUID().toString().substring(0, 8);

            log.info("Whisper API 호출 시작 - 파일 ID: {}, 크기: {}bytes", fileId, fileBytes.length);

            // OpenAI Whisper API를 통한 음성 인식 + 언어 감지
            TranscriptionResult result = callWhisperAPI(fileBytes, filename);

            log.info("음성 인식 완료 - 텍스트 길이: {}글자, 언어: {}", result.text().length(), result.language());
            return result;

        } catch (Exception e) {
            log.error("Whisper API 호출 실패 - 크기: {}bytes", file.getSize(), e);
            throw new VoiceProcessingException("음성 변환에 실패했습니다.", e);
        }
    }

    private TranscriptionResult callWhisperAPI(byte[] fileBytes, String filename) {
        try {
            log.debug("Whisper API 호출 - 파일: {}, 크기: {}bytes", filename, fileBytes.length);

            String whisperApiUrl = "https://api.openai.com/v1/audio/transcriptions";

            // Multipart form-data 구성 (language 파라미터 제거 → Whisper 자동 감지)
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            });
            body.add("model", "whisper-1");
            body.add("response_format", "verbose_json");

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setBearerAuth(openAiApiKey);

            // HTTP 요청 생성
            HttpEntity<MultiValueMap<String, Object>> requestEntity =
                new HttpEntity<>(body, headers);

            log.info("Whisper API 요청 전송 (언어 자동 감지 모드)");

            // REST API 호출
            ResponseEntity<String> response = restTemplate.postForEntity(
                whisperApiUrl,
                requestEntity,
                String.class
            );

            // JSON 응답 파싱
            String responseBody = response.getBody();
            if (responseBody == null || responseBody.isEmpty()) {
                log.warn("Whisper API 응답이 비어있음");
                throw new VoiceProcessingException("음성 인식에 실패했습니다. 다시 시도해주세요.");
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(responseBody);
            JsonNode textNode = responseJson.get("text");

            if (textNode == null) {
                log.warn("Whisper API 응답에 'text' 필드 없음");
                throw new VoiceProcessingException("음성 인식에 실패했습니다. 다시 시도해주세요.");
            }

            String transcript = textNode.asText();

            if (transcript.trim().isEmpty()) {
                log.warn("Whisper API 응답이 비어있음");
                throw new VoiceProcessingException("음성 인식에 실패했습니다. 다시 시도해주세요.");
            }

            // language 필드 추출 (Whisper가 자동 감지한 언어, 없거나 빈 문자열이면 "ko" 기본값)
            JsonNode languageNode = responseJson.get("language");
            String language = (languageNode != null && !languageNode.asText().isBlank())
                ? languageNode.asText()
                : "ko";

            log.info("음성 인식 성공 - 텍스트 길이: {}글자, 감지 언어: {}", transcript.length(), language);
            return new TranscriptionResult(transcript, language);

        } catch (Exception e) {
            log.error("Whisper API 호출 중 오류 - 파일 크기: {}bytes", fileBytes.length, e);
            throw new VoiceProcessingException("음성 변환에 실패했습니다: " + e.getMessage(), e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("파일을 선택해주세요.");
        }

        validateFileSize(file);
        validateFileExtension(file);
        validateContentType(file);
    }

    private void validateFileSize(MultipartFile file) {
        if (file.getSize() > VoiceConfig.MAX_FILE_SIZE) {
            throw new InvalidFileException(
                String.format("파일이 너무 큽니다. 최대 %dMB까지 업로드 가능합니다.",
                    VoiceConfig.MAX_FILE_SIZE / (1024 * 1024))
            );
        }
    }

    private void validateFileExtension(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.contains(".")) {
            throw new InvalidFileException("유효한 파일명이 필요합니다.");
        }

        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        if (!VoiceConfig.ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidFileException(
                String.format("지원하지 않는 파일 형식입니다. 허용 형식: %s",
                    String.join(", ", VoiceConfig.ALLOWED_EXTENSIONS))
            );
        }
    }

    private void validateContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("audio/")) {
            throw new InvalidFileException("오디오 파일만 업로드 가능합니다.");
        }
    }

    public VoiceData processText(String text, String sessionId) {
        if (text == null || text.isBlank()) {
            throw new VoiceProcessingException("텍스트를 입력해주세요.");
        }

        String id = UUID.randomUUID().toString();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }
        String language = detectLanguageFromText(text);
        log.info("텍스트 처리 시작 - ID: {}, sessionId: {}, 길이: {}글자, 감지 언어: {}", id, sessionId, text.length(), language);

        // 사용자 메시지 저장
        conversationService.saveMessage(sessionId, text, ConversationRole.USER);

        // ChatClient로 처리 (Tool Calling 포함)
        String aiResponse;
        try {
            aiResponse = chatClient.prompt()
                .system(buildSystemPrompt(language))
                .user(text)
                .tools(jarvisTools)
                .call()
                .content();
        } catch (Exception e) {
            log.error("ChatClient 처리 중 오류 발생 - ID: {}", id, e);
            throw new VoiceProcessingException("텍스트 처리 중 오류가 발생했습니다.", e);
        }

        // AI 응답 저장
        conversationService.saveMessage(sessionId, aiResponse, ConversationRole.ASSISTANT);

        // AI 응답을 자동으로 음성으로 변환
        String response = convertToAudio(aiResponse);

        log.info("텍스트 처리 완료 - ID: {}, 언어: {}, sessionId: {}", id, language, sessionId);

        String intent = extractIntent(text);
        return VoiceData.builder()
            .id(id)
            .sessionId(sessionId)
            .transcript(text)
            .language(language)
            .intent(intent)
            .resultText(aiResponse)
            .result(response)
            .build();
    }

    /**
     * 감지된 언어 코드에 맞는 시스템 프롬프트를 생성합니다.
     */
    private String buildSystemPrompt(String language) {
        String responseLanguage = switch (language != null ? language : "ko") {
            case "en" -> "English";
            case "ja" -> "日本語";
            case "zh" -> "中文";
            default  -> "한국어";
        };

        return String.format("""
            You are a voice command AI assistant. Please respond in %s concisely and kindly.
            Use the following tools automatically based on the user's request:
            - Weather: use weatherInfo() tool (e.g., "Seoul weather", "서울 날씨")
            - News: use searchNews() tool (e.g., "Bitcoin news", "비트코인 뉴스")
            - Web search: use searchWeb() tool (e.g., "Samsung stock", "삼성 주가")
            - Calculation: use calculate() tool (e.g., "100 plus 50", "100 더하기 50")
            - Time: use convertTime() tool (e.g., "What time is it in New York?", "뉴욕은 몇 시?")
            - Unit conversion: use convertUnit() tool (e.g., "100 pounds to kg", "100 파운드는 몇 kg?")
            - Translation: use translate() tool (e.g., "translate 'hello' to Korean", "'hello'를 한국어로")
            Do not ask the user for choices — look up the information and answer directly.
            Keep your answer concise with only the key information.
            """, responseLanguage);
    }

    /**
     * 텍스트에서 문자 범위로 언어를 감지합니다. (processText 전용)
     * 한국어(ko), 일본어(ja), 중국어(zh), 영어(en) 감지 지원
     */
    private String detectLanguageFromText(String text) {
        long koreanChars = text.chars()
            .filter(c -> (c >= 0xAC00 && c <= 0xD7A3) || (c >= 0x1100 && c <= 0x11FF))
            .count();
        if (koreanChars > 0) {
            return "ko";
        }

        // 일본어 히라가나/가타카나 검사
        long japaneseChars = text.chars()
            .filter(c -> (c >= 0x3040 && c <= 0x309F) || (c >= 0x30A0 && c <= 0x30FF))
            .count();
        if (japaneseChars > 0) {
            return "ja";
        }

        // 중국어 CJK 한자 검사 (일본어 가나 없이 한자만 있는 경우)
        long cjkChars = text.chars()
            .filter(c -> (c >= 0x4E00 && c <= 0x9FFF))
            .count();
        if (cjkChars > 0) {
            return "zh";
        }

        return "en";
    }

    /**
     * Whisper API 응답을 담는 레코드 (텍스트 + 감지된 언어 코드)
     */
    private record TranscriptionResult(String text, String language) {}

    private String convertToAudio(String aiResponse) {
        try {
            if (aiResponse == null || aiResponse.isEmpty()) {
                log.warn("AI 응답이 비어있음");
                return aiResponse;
            }

            if (aiResponse.startsWith("data:audio/")) {
                log.debug("AI 응답이 이미 오디오 데이터 - 변환 스킵");
                return aiResponse;
            }

            log.debug("AI 응답을 음성으로 변환 중 - 길이: {}글자", aiResponse.length());
            String audioBase64 = jarvisTools.speakText(aiResponse);
            log.info("음성 변환 완료 - 오디오 크기: {}bytes", audioBase64.length());
            return audioBase64;

        } catch (Exception e) {
            int responseLength = (aiResponse != null) ? aiResponse.length() : 0;
            log.error("음성 변환 중 오류 발생 - 응답 길이: {}글자", responseLength, e);
            return aiResponse;
        }
    }

    /**
     * 음성 파일을 비동기로 처리하여 스트리밍 응답을 전송합니다.
     * STT 변환 후 ChatClient의 스트리밍 API로 토큰 단위로 응답합니다.
     */
    public void processVoiceStreamAsync(MultipartFile file, String sessionId, SseEmitter emitter) {
        try {
            validateFile(file);

            // async 블록 전에 MultipartFile 데이터 미리 읽기
            final byte[] fileBytes = file.getBytes();
            final String filename = file.getOriginalFilename();

            CompletableFuture.runAsync(() -> {
                try {
                    final String id = UUID.randomUUID().toString();
                    final String finalSessionId = (sessionId != null && !sessionId.isBlank())
                        ? sessionId
                        : UUID.randomUUID().toString();
                    final long fileSize = fileBytes.length;
                    log.info("음성 스트리밍 처리 시작 - ID: {}, sessionId: {}, 크기: {}bytes", id, finalSessionId, fileSize);

                    TranscriptionResult transcription = callWhisperAPI(fileBytes, filename);
                    final String transcript = transcription.text();
                    final String language = transcription.language();
                    log.info("감지된 언어: {}", language);

                    conversationService.saveMessage(finalSessionId, transcript, ConversationRole.USER);

                    final String systemPrompt = buildSystemPrompt(language);
                    final StringBuilder fullResponse = new StringBuilder();

                    chatClient.prompt()
                        .system(systemPrompt)
                        .user(transcript)
                        .tools(jarvisTools)
                        .stream()
                        .content()
                        .doOnNext(token -> {
                            try {
                                fullResponse.append(token);
                                emitter.send(SseEmitter.event()
                                    .id(id)
                                    .name("token")
                                    .data(token)
                                    .reconnectTime(1000));
                            } catch (IOException e) {
                                log.error("SSE 전송 중 오류 - ID: {}", id, e);
                                emitter.completeWithError(e);
                            }
                        })
                        .subscribe(
                            token -> {},
                            e -> {
                                log.error("음성 스트리밍 에러 - ID: {}", id, e);
                                emitter.completeWithError(e);
                            },
                            () -> {
                                try {
                                    conversationService.saveMessage(finalSessionId, fullResponse.toString(), ConversationRole.ASSISTANT);
                                    emitter.send(SseEmitter.event()
                                        .id(id)
                                        .name("complete")
                                        .data("스트리밍 완료"));
                                    emitter.complete();
                                    log.info("음성 스트리밍 처리 완료 - ID: {}, 응답 길이: {}글자", id, fullResponse.length());
                                } catch (IOException e) {
                                    log.error("스트리밍 완료 처리 중 오류", e);
                                    emitter.completeWithError(e);
                                }
                            }
                        );

                } catch (Exception e) {
                    log.error("음성 스트리밍 처리 중 오류", e);
                    emitter.completeWithError(e);
                }
            });
        } catch (Exception e) {
            log.error("음성 파일 읽기 실패", e);
            emitter.completeWithError(e);
        }
    }

    /**
     * 텍스트를 비동기로 처리하여 스트리밍 응답을 전송합니다.
     * 언어 감지 후 ChatClient의 스트리밍 API로 토큰 단위로 응답합니다.
     */
    public void processTextStreamAsync(String text, String sessionId, SseEmitter emitter) {
        try {
            if (text == null || text.isBlank()) {
                throw new VoiceProcessingException("텍스트를 입력해주세요.");
            }

            CompletableFuture.runAsync(() -> {
                try {
                    final String id = UUID.randomUUID().toString();
                    final String finalSessionId = (sessionId != null && !sessionId.isBlank())
                        ? sessionId
                        : UUID.randomUUID().toString();
                    final String language = detectLanguageFromText(text);
                    log.info("텍스트 스트리밍 처리 시작 - ID: {}, sessionId: {}, 길이: {}글자, 감지 언어: {}", id, finalSessionId, text.length(), language);

                    conversationService.saveMessage(finalSessionId, text, ConversationRole.USER);

                    final String systemPrompt = buildSystemPrompt(language);
                    final StringBuilder fullResponse = new StringBuilder();

                    chatClient.prompt()
                        .system(systemPrompt)
                        .user(text)
                        .tools(jarvisTools)
                        .stream()
                        .content()
                        .doOnNext(token -> {
                            try {
                                fullResponse.append(token);
                                emitter.send(SseEmitter.event()
                                    .id(id)
                                    .name("token")
                                    .data(token)
                                    .reconnectTime(1000));
                            } catch (IOException e) {
                                log.error("SSE 전송 중 오류 - ID: {}", id, e);
                                emitter.completeWithError(e);
                            }
                        })
                        .subscribe(
                            token -> {},
                            e -> {
                                log.error("텍스트 스트리밍 에러 - ID: {}", id, e);
                                emitter.completeWithError(e);
                            },
                            () -> {
                                try {
                                    conversationService.saveMessage(finalSessionId, fullResponse.toString(), ConversationRole.ASSISTANT);
                                    emitter.send(SseEmitter.event()
                                        .id(id)
                                        .name("complete")
                                        .data("스트리밍 완료"));
                                    emitter.complete();
                                    log.info("텍스트 스트리밍 처리 완료 - ID: {}, 응답 길이: {}글자", id, fullResponse.length());
                                } catch (IOException e) {
                                    log.error("스트리밍 완료 처리 중 오류", e);
                                    emitter.completeWithError(e);
                                }
                            }
                        );

                } catch (Exception e) {
                    log.error("텍스트 스트리밍 처리 중 오류", e);
                    emitter.completeWithError(e);
                }
            });
        } catch (Exception e) {
            log.error("텍스트 검증 실패", e);
            emitter.completeWithError(e);
        }
    }
}