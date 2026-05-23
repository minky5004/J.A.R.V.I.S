package com.jarvis.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JarvisTools {

    private final RestTemplate restTemplate;

    @Value("${spring.ai.openai.api-key}")
    private String openaiApiKey;

    @Tool(description = "특정 도시의 현재 날씨를 조회합니다. 도시 이름을 입력하면 온도, 날씨, 습도 등의 정보를 반환합니다. 예: '서울', '부산', '뉴욕'")
    public String weatherInfo(String city) {
        try {
            if (city == null || city.isBlank()) {
                throw new IllegalArgumentException("도시 이름을 입력해주세요.");
            }
            return callOpenMeteoAPI(city);
        } catch (Exception e) {
            log.error("날씨 조회 실패 - 도시: {}", city, e);
            return "날씨 조회 중 오류가 발생했습니다.";
        }
    }

    @Tool(description = "간단한 수학 계산을 수행합니다. 덧셈, 뺄셈, 곱셈, 나눗셈 등. 예: '100 더하기 50', '1000 곱하기 2', '50 나누기 5'")
    public String calculate(String expression) {
        try {
            if (expression == null || expression.isBlank()) {
                throw new IllegalArgumentException("계산식을 입력해주세요.");
            }
            return performCalculation(expression);
        } catch (Exception e) {
            log.error("계산 실패 - 식: {}", expression, e);
            return "계산 중 오류가 발생했습니다.";
        }
    }

    @Tool(description = "특정 도시의 현재 시간을 조회합니다. 예: '뉴욕', '런던', '도쿄', '시드니'")
    public String convertTime(String city) {
        try {
            if (city == null || city.isBlank()) {
                throw new IllegalArgumentException("도시 이름을 입력해주세요.");
            }
            return getTimeInCity(city);
        } catch (Exception e) {
            log.error("시간 조회 실패 - 도시: {}", city, e);
            return "시간 조회 중 오류가 발생했습니다.";
        }
    }

    @Tool(description = "단위를 변환합니다. 무게(kg, lbs, g), 거리(m, km, mile, ft), 온도(°C, °F) 등. 예: '100 파운드를 킬로그램으로', '50 화씨를 섭씨로'")
    public String convertUnit(String value, String fromUnit, String toUnit) {
        try {
            if (value == null || fromUnit == null || toUnit == null) {
                throw new IllegalArgumentException("값과 단위를 입력해주세요.");
            }
            return performUnitConversion(value, fromUnit, toUnit);
        } catch (Exception e) {
            log.error("단위 변환 실패 - 값: {}, {}->{}", value, fromUnit, toUnit, e);
            return "단위 변환 중 오류가 발생했습니다.";
        }
    }

    @Tool(description = "텍스트를 다른 언어로 번역합니다. 한국어↔영어, 일본어, 중국어 등 다양한 언어 지원. sourceLanguage 생략 시 한국어로 가정. 예: '안녕하세요'를 '영어'로, 'hello'를 '한국어'로")
    public String translate(String text, String targetLanguage, String sourceLanguage) {
        try {
            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException("번역할 텍스트를 입력해주세요.");
            }
            String actualSourceLanguage = (sourceLanguage == null || sourceLanguage.isBlank()) ? "한국어" : sourceLanguage;
            return callMyMemoryAPI(text, targetLanguage, actualSourceLanguage);
        } catch (Exception e) {
            log.error("번역 실패 - 텍스트: {}, 원본 언어: {}, 대상 언어: {}", text, sourceLanguage, targetLanguage, e);
            return "번역 중 오류가 발생했습니다.";
        }
    }

    @Tool(description = "뉴스 검색어로 웹을 검색하고 관련 요약 정보를 제공합니다. query는 검색어를 한국어로 전달하세요. 예: '비트코인 뉴스', '날씨 속보'")
    public String searchNews(String query) {
        try {
            if (query == null || query.isBlank()) {
                throw new IllegalArgumentException("검색어를 입력해주세요.");
            }
            return callDuckDuckGoAPI(query + " 뉴스");
        } catch (Exception e) {
            log.error("뉴스 검색 실패 - 검색어: {}", query, e);
            return "뉴스 검색 중 오류가 발생했습니다.";
        }
    }

    @Tool(description = "웹에서 특정 정보를 검색할 때 사용합니다. 정보, 시세 등을 실시간으로 조회할 수 있습니다. query는 검색어를 한국어로 전달하세요.")
    public String searchWeb(String query) {
        try {
            if (query == null || query.isBlank()) {
                throw new IllegalArgumentException("검색어를 입력해주세요.");
            }
            return callDuckDuckGoAPI(query);
        } catch (Exception e) {
            log.error("DuckDuckGo 검색 실패 - 검색어: {}", query, e);
            return "검색 중 오류가 발생했습니다.";
        }
    }

    @Tool(description = "텍스트를 음성으로 변환합니다. 긴 텍스트도 음성으로 들을 수 있습니다.")
    public String speakText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("음성으로 변환할 텍스트를 입력해주세요.");
        }

        try {
            return callOpenAITTSAPI(text);
        } catch (Exception e) {
            log.error("TTS 변환 실패 - 텍스트 길이: {}글자", text.length(), e);
            throw new RuntimeException("음성 변환 중 오류가 발생했습니다.", e);
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

    private String callMyMemoryAPI(String text, String targetLanguage, String sourceLanguage) {
        try {
            String fromLang = convertLanguageNameToCode(sourceLanguage);
            String toLang = convertLanguageNameToCode(targetLanguage);

            if (fromLang == null) {
                throw new RuntimeException("지원하지 않는 원본 언어입니다: " + sourceLanguage);
            }
            if (toLang == null) {
                throw new RuntimeException("지원하지 않는 대상 언어입니다: " + targetLanguage);
            }

            String apiUrl = "https://api.mymemory.translated.net/get?q=" +
                java.net.URLEncoder.encode(text, "UTF-8") +
                "&langpair=" + fromLang + "|" + toLang;

            log.debug("MyMemory API 호출 - 텍스트: {}, 원본 언어: {}, 대상 언어: {}", text, sourceLanguage, targetLanguage);

            ResponseEntity<String> response = restTemplate.getForEntity(apiUrl, String.class);
            String responseBody = response.getBody();

            if (responseBody == null || responseBody.isEmpty()) {
                log.warn("MyMemory API 응답 없음");
                return "번역을 할 수 없습니다.";
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(responseBody);
            JsonNode responseData = responseJson.get("responseData");

            if (responseData == null) {
                log.warn("MyMemory API 응답 형식 오류");
                return "번역을 할 수 없습니다.";
            }

            String translatedText = responseData.get("translatedText").asText();

            if (translatedText == null || translatedText.isBlank()) {
                log.warn("번역 결과 없음");
                return "번역을 할 수 없습니다.";
            }

            log.info("번역 완료 - 원본 언어: {}, 대상 언어: {}", sourceLanguage, targetLanguage);
            return translatedText;

        } catch (Exception e) {
            log.error("MyMemory API 호출 중 오류 - 원본 언어: {}, 대상 언어: {}", sourceLanguage, targetLanguage, e);
            throw new RuntimeException("번역 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    private String performCalculation(String expression) {
        try {
            String normalized = expression
                .replaceAll("더하기|\\+", "+")
                .replaceAll("빼기|마이너스|\\-", "-")
                .replaceAll("곱하기|곱|\\*|×", "*")
                .replaceAll("나누기|÷|/", "/")
                .replaceAll("\\s+", "");

            double result = evaluateExpression(normalized);
            String resultStr = (result == (long) result) ?
                String.format("%d", (long) result) :
                String.format("%.2f", result);

            log.info("계산 완료 - 식: {}, 결과: {}", expression, resultStr);
            return resultStr;
        } catch (Exception e) {
            log.error("계산 실패 - 식: {}", expression, e);
            throw new RuntimeException("계산할 수 없습니다: " + expression);
        }
    }

    private double evaluateExpression(String expr) {
        try {
            return evaluateAddSub(expr);
        } catch (Exception e) {
            throw new RuntimeException("Invalid expression: " + expr);
        }
    }

    private double evaluateAddSub(String expr) {
        int lastAddSubIndex = -1;
        for (int i = expr.length() - 1; i > 0; i--) {
            char ch = expr.charAt(i);
            if ((ch == '+' || ch == '-') && Character.isDigit(expr.charAt(i - 1))) {
                lastAddSubIndex = i;
                break;
            }
        }

        if (lastAddSubIndex == -1) {
            return evaluateMulDiv(expr);
        }

        String left = expr.substring(0, lastAddSubIndex);
        char op = expr.charAt(lastAddSubIndex);
        String right = expr.substring(lastAddSubIndex + 1);

        double leftVal = evaluateAddSub(left);
        double rightVal = evaluateMulDiv(right);

        return op == '+' ? leftVal + rightVal : leftVal - rightVal;
    }

    private double evaluateMulDiv(String expr) {
        String[] mulDiv = expr.split("(?=[*/])|(?<=[*/])");
        double result = parseNumber(mulDiv[0].trim());

        for (int i = 1; i < mulDiv.length; i += 2) {
            String op = mulDiv[i].trim();
            double nextNum = parseNumber(mulDiv[i + 1].trim());
            if ("*".equals(op)) {
                result *= nextNum;
            } else if ("/".equals(op)) {
                if (nextNum == 0) {
                    throw new RuntimeException("0으로 나눌 수 없습니다.");
                }
                result /= nextNum;
            }
        }

        return result;
    }

    private double parseNumber(String str) {
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid number: " + str);
        }
    }

    private String getTimeInCity(String city) {
        java.time.ZoneId zoneId = switch (city.toLowerCase().replaceAll("\\s+", "")) {
            case "뉴욕", "newyork", "newyorkusa" -> java.time.ZoneId.of("America/New_York");
            case "런던", "london" -> java.time.ZoneId.of("Europe/London");
            case "파리", "paris" -> java.time.ZoneId.of("Europe/Paris");
            case "도쿄", "tokyo" -> java.time.ZoneId.of("Asia/Tokyo");
            case "서울", "seoul" -> java.time.ZoneId.of("Asia/Seoul");
            case "상하이", "shanghai" -> java.time.ZoneId.of("Asia/Shanghai");
            case "싱가포르", "singapore" -> java.time.ZoneId.of("Asia/Singapore");
            case "시드니", "sydney" -> java.time.ZoneId.of("Australia/Sydney");
            case "더빈", "dubai" -> java.time.ZoneId.of("Asia/Dubai");
            case "방콕", "bangkok" -> java.time.ZoneId.of("Asia/Bangkok");
            case "홍콩", "hongkong" -> java.time.ZoneId.of("Asia/Hong_Kong");
            case "뭄바이", "mumbai" -> java.time.ZoneId.of("Asia/Kolkata");
            case "모스크바", "moscow" -> java.time.ZoneId.of("Europe/Moscow");
            case "라스베이거스", "losangeles", "la" -> java.time.ZoneId.of("America/Los_Angeles");
            default -> null;
        };

        if (zoneId == null) {
            return city + "의 시간대를 찾을 수 없습니다. (뉴욕, 런던, 도쿄, 시드니 등)";
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now(zoneId);
        String timeStr = now.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss (E)").withLocale(java.util.Locale.KOREAN));
        log.info("시간 조회 완료 - 도시: {}", city);
        return String.format("%s의 현재 시간: %s", city, timeStr);
    }

    private String performUnitConversion(String value, String fromUnit, String toUnit) {
        try {
            double numValue = Double.parseDouble(value.replaceAll("[^0-9.-]", ""));

            String normalized_from = normalizeUnit(fromUnit);
            String normalized_to = normalizeUnit(toUnit);

            double result = convertUnits(numValue, normalized_from, normalized_to);
            String resultStr = (result == (long) result) ?
                String.format("%d", (long) result) :
                String.format("%.2f", result);

            log.info("단위 변환 완료 - {} {} = {} {}", value, fromUnit, resultStr, toUnit);
            return String.format("%.2f %s = %s %s", numValue, fromUnit, resultStr, toUnit);
        } catch (Exception e) {
            log.error("단위 변환 실패 - 값: {}, {} -> {}", value, fromUnit, toUnit, e);
            throw new RuntimeException("단위 변환에 실패했습니다: " + e.getMessage());
        }
    }

    private String normalizeUnit(String unit) {
        String normalized = unit.toLowerCase().replaceAll("\\s+", "");
        return switch (normalized) {
            case "kg", "킬로그램", "킬로" -> "kg";
            case "g", "그램" -> "g";
            case "lb", "lbs", "파운드", "pound" -> "lbs";
            case "oz", "온스", "ounce" -> "oz";
            case "m", "미터" -> "m";
            case "cm", "센티미터" -> "cm";
            case "km", "킬로미터" -> "km";
            case "mile", "마일" -> "mile";
            case "ft", "feet", "푸트", "피트" -> "ft";
            case "c", "celsius", "섭씨", "도" -> "C";
            case "f", "fahrenheit", "화씨" -> "F";
            default -> normalized;
        };
    }

    private double convertUnits(double value, String from, String to) {
        if (from.equalsIgnoreCase(to)) {
            return value;
        }

        double result = switch (from + "->" + to) {
            case "kg->lbs" -> value * 2.20462;
            case "lbs->kg" -> value / 2.20462;
            case "g->oz" -> value / 28.3495;
            case "oz->g" -> value * 28.3495;
            case "m->ft" -> value * 3.28084;
            case "ft->m" -> value / 3.28084;
            case "km->mile" -> value / 1.60934;
            case "mile->km" -> value * 1.60934;
            case "C->F" -> value * 9/5 + 32;
            case "F->C" -> (value - 32) * 5/9;
            default -> Double.NaN;
        };

        if (Double.isNaN(result)) {
            throw new RuntimeException("지원하지 않는 단위 변환입니다: " + from + " -> " + to);
        }
        return result;
    }

    private String convertLanguageNameToCode(String language) {
        if (language == null || language.isBlank()) {
            return null;
        }

        String code = switch (language.toLowerCase().replaceAll("\\s+", "")) {
            case "한국어", "korean", "ko" -> "ko";
            case "영어", "english", "en" -> "en";
            case "일본어", "japanese", "ja" -> "ja";
            case "중국어", "chinese", "zh" -> "zh";
            case "스페인어", "spanish", "es" -> "es";
            case "프랑스어", "french", "fr" -> "fr";
            case "독일어", "german", "de" -> "de";
            case "러시아어", "russian", "ru" -> "ru";
            case "아랍어", "arabic", "ar" -> "ar";
            case "포르투갈어", "portuguese", "pt" -> "pt";
            default -> null;
        };

        return code;
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

    private String callOpenAITTSAPI(String text) {
        if (openaiApiKey == null || openaiApiKey.isBlank()) {
            throw new RuntimeException("OpenAI API 키가 설정되지 않았습니다.");
        }

        try {
            String apiUrl = "https://api.openai.com/v1/audio/speech";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "tts-1");
            requestBody.put("input", text);
            requestBody.put("voice", "alloy");
            requestBody.put("response_format", "mp3");

            log.debug("OpenAI TTS API 호출 - 텍스트 길이: {}", text.length());

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + openaiApiKey);
            headers.set("Content-Type", "application/json");

            ObjectMapper objectMapper = new ObjectMapper();
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, byte[].class);
            byte[] audioData = response.getBody();

            if (audioData == null || audioData.length == 0) {
                log.warn("OpenAI TTS 응답이 비어있음");
                return "음성 변환에 실패했습니다.";
            }

            String base64Audio = Base64.getEncoder().encodeToString(audioData);
            log.info("TTS 변환 완료 - 텍스트 길이: {}, 오디오 크기: {} bytes", text.length(), audioData.length);
            return "data:audio/mp3;base64," + base64Audio;

        } catch (Exception e) {
            log.error("OpenAI TTS API 호출 중 오류 - 텍스트 길이: {}글자", text.length(), e);
            throw new RuntimeException("음성 변환 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}