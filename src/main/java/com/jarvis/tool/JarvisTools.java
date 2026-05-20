package com.jarvis.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class JarvisTools {

    private final RestTemplate restTemplate;

    @Tool(description = "웹에서 특정 정보를 검색할 때 사용합니다. 뉴스, 정보, 시세 등을 실시간으로 조회할 수 있습니다. query는 검색어를 한국어로 전달하세요.")
    public String searchWeb(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }

        try {
            return callDuckDuckGoAPI(query);
        } catch (Exception e) {
            log.error("DuckDuckGo 검색 실패 - 검색어: {}", query, e);
            return "검색 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    private String callDuckDuckGoAPI(String query) {
        try {
            String apiUrl = "https://api.duckduckgo.com/?q=" +
                java.net.URLEncoder.encode(query, "UTF-8") +
                "&format=json";

            log.debug("DuckDuckGo API 호출 - 검색어: {}", query);

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, String.class);
            String responseBody = response.getBody();

            if (responseBody == null || responseBody.isEmpty()) {
                return "검색 결과가 없습니다: " + query;
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(responseBody);

            // Abstract 필드에서 요약 정보 추출
            JsonNode abstractNode = responseJson.get("AbstractText");
            if (abstractNode != null && !abstractNode.asText().isBlank()) {
                String summary = abstractNode.asText();
                log.info("검색 완료 - 검색어: {}, 결과 길이: {}", query, summary.length());
                return "검색 결과: " + summary;
            }

            // Related Topics에서 정보 추출
            JsonNode relatedTopics = responseJson.get("RelatedTopics");
            if (relatedTopics != null && relatedTopics.isArray() && relatedTopics.size() > 0) {
                JsonNode firstTopic = relatedTopics.get(0);
                JsonNode firstTopicText = firstTopic.get("Text");
                if (firstTopicText != null && !firstTopicText.asText().isBlank()) {
                    String result = firstTopicText.asText();
                    log.info("검색 완료 (관련 주제) - 검색어: {}", query);
                    return "검색 결과: " + result;
                }
            }

            log.warn("DuckDuckGo 응답에서 유용한 정보 없음 - 검색어: {}", query);
            return "검색 결과: '" + query + "'에 대한 정보를 조회했습니다.";

        } catch (Exception e) {
            log.error("DuckDuckGo API 호출 중 오류 - 검색어: {}", query, e);
            throw new RuntimeException("검색 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}