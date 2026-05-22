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

    @Tool(description = "특정 도시의 현재 날씨를 조회합니다. 도시 이름을 입력하면 온도, 날씨, 습도 등의 정보를 반환합니다. 예: '서울', '부산', '뉴욕'")
    public String weatherInfo(String city) {
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("도시 이름을 입력해주세요.");
        }

        try {
            return callOpenMeteoAPI(city);
        } catch (Exception e) {
            log.error("날씨 조회 실패 - 도시: {}", city, e);
            return "날씨 조회 중 오류가 발생했습니다.";
        }
    }

    @Tool(description = "웹에서 특정 정보를 검색할 때 사용합니다. 뉴스, 정보, 시세 등을 실시간으로 조회할 수 있습니다. query는 검색어를 한국어로 전달하세요.")
    public String searchWeb(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }

        try {
            return callDuckDuckGoAPI(query);
        } catch (Exception e) {
            log.error("DuckDuckGo 검색 실패 - 검색어: {}", query, e);
            return "검색 중 오류가 발생했습니다.";
        }
    }

    private String callOpenMeteoAPI(String city) {
        try {
            String searchCity = convertKoreanCityToEnglish(city);
            String geocodingUrl = "https://geocoding-api.open-meteo.com/v1/search?name=" +
                java.net.URLEncoder.encode(searchCity, "UTF-8") +
                "&count=1&language=ko&format=json";

            log.debug("Open-Meteo Geocoding API 호출 - 도시: {}", city);

            ResponseEntity<String> geoResponse = restTemplate.getForEntity(geocodingUrl, String.class);
            String geoBody = geoResponse.getBody();

            if (geoBody == null || geoBody.isEmpty()) {
                return "도시를 찾을 수 없습니다: " + city;
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode geoJson = objectMapper.readTree(geoBody);
            JsonNode results = geoJson.get("results");

            if (results == null || !results.isArray() || results.size() == 0) {
                log.warn("Geocoding 결과 없음 - 도시: {}", city);
                return "도시를 찾을 수 없습니다: " + city;
            }

            JsonNode firstResult = results.get(0);
            double latitude = firstResult.get("latitude").asDouble();
            double longitude = firstResult.get("longitude").asDouble();
            String foundCity = firstResult.get("name").asText();

            String weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=" + latitude +
                "&longitude=" + longitude +
                "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m&temperature_unit=celsius&language=ko";

            log.debug("Open-Meteo Forecast API 호출 - 좌표: {}, {}", latitude, longitude);

            ResponseEntity<String> weatherResponse = restTemplate.getForEntity(weatherUrl, String.class);
            String weatherBody = weatherResponse.getBody();

            if (weatherBody == null || weatherBody.isEmpty()) {
                return "날씨 정보를 조회할 수 없습니다.";
            }

            JsonNode weatherJson = objectMapper.readTree(weatherBody);
            JsonNode current = weatherJson.get("current");

            if (current == null) {
                log.warn("날씨 정보 없음 - 도시: {}", city);
                return "날씨 정보를 조회할 수 없습니다.";
            }

            double temperature = current.get("temperature_2m").asDouble();
            int humidity = current.get("relative_humidity_2m").asInt();
            double windSpeed = current.get("wind_speed_10m").asDouble();
            int weatherCode = current.get("weather_code").asInt();
            String weatherDescription = getWeatherDescription(weatherCode);

            String result = String.format("%s의 현재 날씨: %.1f°C, %s (습도 %d%%, 풍속 %.1f m/s)",
                foundCity, temperature, weatherDescription, humidity, windSpeed);

            log.info("날씨 조회 완료 - 도시: {}", city);
            return result;

        } catch (Exception e) {
            log.error("Open-Meteo API 호출 중 오류 - 도시: {}", city, e);
            throw new RuntimeException("날씨 조회 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    private String convertKoreanCityToEnglish(String city) {
        return switch (city.replaceAll("\\s+", "").toLowerCase()) {
            case "서울", "서울특별시", "서울시" -> "Seoul";
            case "부산", "부산광역시", "부산시" -> "Busan";
            case "인천", "인천광역시", "인천시" -> "Incheon";
            case "대구", "대구광역시", "대구시" -> "Daegu";
            case "대전", "대전광역시", "대전시" -> "Daejeon";
            case "광주", "광주광역시", "광주시" -> "Gwangju";
            case "울산", "울산광역시", "울산시" -> "Ulsan";
            case "경기", "경기도" -> "Gyeonggi";
            case "강원", "강원도" -> "Gangwon";
            case "충북", "충청북도" -> "North Chungcheong";
            case "충남", "충청남도" -> "South Chungcheong";
            case "전북", "전라북도" -> "North Jeolla";
            case "전남", "전라남도" -> "South Jeolla";
            case "경북", "경상북도" -> "North Gyeongsang";
            case "경남", "경상남도" -> "South Gyeongsang";
            case "제주", "제주도", "제주특별자치도" -> "Jeju";
            default -> city;
        };
    }

    private String getWeatherDescription(int code) {
        return switch (code) {
            case 0 -> "맑음";
            case 1, 2 -> "구름 적음";
            case 3 -> "구름 많음";
            case 45, 48 -> "안개";
            case 51, 53, 55 -> "가벼운 비";
            case 61, 63, 65 -> "비";
            case 71, 73, 75 -> "눈";
            case 77 -> "눈입자";
            case 80, 81, 82 -> "소나기";
            case 85, 86 -> "눈소나기";
            case 95, 96, 99 -> "뇌우";
            default -> "기타";
        };
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