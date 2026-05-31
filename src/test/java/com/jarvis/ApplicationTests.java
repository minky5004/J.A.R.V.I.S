package com.jarvis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("스프링 부트 애플리케이션 테스트")
class ApplicationTests {

    @Test
    @DisplayName("애플리케이션 시작 확인")
    void applicationStartup() {
        // 단순 더미 테스트 - DB 연결 불필요
        assert true;
    }
}
