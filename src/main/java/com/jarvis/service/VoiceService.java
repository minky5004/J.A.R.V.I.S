package com.jarvis.service;

import com.jarvis.config.VoiceConfig;
import com.jarvis.dto.VoiceData;
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

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceService {

    private final ChatClient chatClient;
    private final JarvisTools jarvisTools;
    private final RestTemplate restTemplate;

    @Value("${spring.ai.openai.api-key}")
    private String openAiApiKey;

    public VoiceData processVoice(MultipartFile file) {
        validateFile(file);

        String id = UUID.randomUUID().toString();
        long fileSize = file.getSize();
        log.info("음성 처리 시작 - ID: {}, 크기: {}bytes", id, fileSize);

        // 1. STT 변환 (현재는 임시)
        String transcript = extractTranscriptFromFile(file);

        // 2. ChatClient로 처리 (Tool Calling 포함)
        String aiResponse;
        try {
            response = chatClient.prompt()
                .system("""
                    당신은 음성 명령 AI 어시스턴트입니다. 한국어로 간결하고 친절하게 응답하세요.
                    사용자 요청에 따라 다음 Tool을 자동으로 활용하세요:
                    - 날씨: weatherInfo() Tool 사용 (예: "서울 날씨", "부산 기온")
                    - 뉴스: searchNews() Tool 사용 (예: "비트코인 뉴스", "날씨 속보")
                    - 웹 검색: searchWeb() Tool 사용 (예: "삼성 주가", "환율 정보")
                    - 계산: calculate() Tool 사용 (예: "100 더하기 50", "1000 곱하기 2")
                    - 시간 조회: convertTime() Tool 사용 (예: "뉴욕은 지금 몇 시?", "런던 시간")
                    - 단위 변환: convertUnit() Tool 사용 (예: "100 파운드는 몇 kg?", "5 마일은 km")
                    - 번역: translate() Tool 사용 (예: "'hello'를 한국어로", "'안녕하세요'를 영어로", "'bonjour'를 프랑스어에서 한국어로")
                    사용자에게 선택지를 묻지 말고, 요청에 맞는 정보를 바로 조회해서 답변하세요.
                    답변은 핵심 정보만 간단하게 전달하세요.
                    """)
                .user(transcript)
                .tools(jarvisTools)
                .call()
                .content();
        } catch (Exception e) {
            log.error("ChatClient 처리 중 오류 발생 - ID: {}", id, e);
            throw new VoiceProcessingException("음성 처리 중 오류가 발생했습니다.", e);
        }

        // 3. AI 응답을 자동으로 음성으로 변환
        String response = convertToAudio(aiResponse);

        log.info("음성 처리 완료 - ID: {}", id);

        String intent = extractIntent(transcript);
        return VoiceData.builder()
            .id(id)
            .transcript(transcript)
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

    private String extractTranscriptFromFile(MultipartFile file) {
        try {
            byte[] fileBytes = file.getBytes();
            String filename = file.getOriginalFilename();
            String fileId = UUID.randomUUID().toString().substring(0, 8);

            log.info("Whisper API 호출 시작 - 파일 ID: {}, 크기: {}bytes", fileId, fileBytes.length);

            // OpenAI Whisper API를 통한 음성 인식
            String transcript = callWhisperAPI(fileBytes, filename);

            log.info("음성 인식 완료 - 텍스트 길이: {}글자", transcript.length());
            return transcript;

        } catch (Exception e) {
            log.error("Whisper API 호출 실패 - 크기: {}bytes", file.getSize(), e);
            throw new VoiceProcessingException("음성 변환에 실패했습니다.", e);
        }
    }

    private String callWhisperAPI(byte[] fileBytes, String filename) {
        try {
            log.debug("Whisper API 호출 - 파일: {}, 크기: {}bytes", filename, fileBytes.length);

            String whisperApiUrl = "https://api.openai.com/v1/audio/transcriptions";

            // Multipart form-data 구성
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            });
            body.add("model", "whisper-1");
            body.add("language", "ko");

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setBearerAuth(openAiApiKey);

            // HTTP 요청 생성
            HttpEntity<MultiValueMap<String, Object>> requestEntity =
                new HttpEntity<>(body, headers);

            log.info("Whisper API 요청 전송");

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

            log.info("음성 인식 성공 - 텍스트 길이: {}글자", transcript.length());
            return transcript;

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

    public VoiceData processText(String text) {
        if (text == null || text.isBlank()) {
            throw new VoiceProcessingException("텍스트를 입력해주세요.");
        }

        String id = UUID.randomUUID().toString();
        log.info("텍스트 처리 시작 - ID: {}, 길이: {}글자", id, text.length());

        // ChatClient로 처리 (Tool Calling 포함)
        String aiResponse;
        try {
            aiResponse = chatClient.prompt()
                .system("당신은 음성 명령 AI 어시스턴트입니다. 한국어로 친절하게 응답하세요.")
                .user(text)
                .tools(jarvisTools)
                .call()
                .content();
        } catch (Exception e) {
            log.error("ChatClient 처리 중 오류 발생 - ID: {}", id, e);
            throw new VoiceProcessingException("텍스트 처리 중 오류가 발생했습니다.", e);
        }

        // AI 응답을 자동으로 음성으로 변환
        String response = convertToAudio(aiResponse);

        log.info("텍스트 처리 완료 - ID: {}", id);

        String intent = extractIntent(text);
        return VoiceData.builder()
            .id(id)
            .transcript(text)
            .intent(intent)
            .resultText(aiResponse)
            .result(response)
            .build();
    }

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
}