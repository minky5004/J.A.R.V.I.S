package com.jarvis.tool;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JarvisTools {

    @Tool(description = "웹에서 특정 정보를 검색할 때 사용합니다. 뉴스, 정보, 시세 등을 실시간으로 조회할 수 있습니다. query는 검색어를 한국어로 전달하세요.")
    public String searchWeb(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }
        // 현재는 임시: 실제로는 DuckDuckGo나 Google Custom Search API를 사용
        return "검색 결과: '" + query + "'에 대한 정보를 조회했습니다.";
    }
}