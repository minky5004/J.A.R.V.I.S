package com.jarvis.exception;

import com.jarvis.dto.VoiceProcessResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<VoiceProcessResponse> handleInvalidFileException(InvalidFileException e) {
        log.warn("파일 검증 실패: {}", e.getMessage());
        VoiceProcessResponse response = VoiceProcessResponse.builder()
            .success(false)
            .message(e.getMessage())
            .data(null)
            .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(VoiceProcessingException.class)
    public ResponseEntity<VoiceProcessResponse> handleVoiceProcessingException(VoiceProcessingException e) {
        log.error("음성 처리 오류: {}", e.getMessage(), e);
        VoiceProcessResponse response = VoiceProcessResponse.builder()
            .success(false)
            .message("음성 처리 중 오류가 발생했습니다.")
            .data(null)
            .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<VoiceProcessResponse> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("파일 크기 초과: {}", e.getMessage());
        VoiceProcessResponse response = VoiceProcessResponse.builder()
            .success(false)
            .message("파일이 너무 큽니다. 최대 25MB까지 업로드 가능합니다.")
            .data(null)
            .build();
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<VoiceProcessResponse> handleGenericException(Exception e) {
        log.error("예상치 못한 오류: {}", e.getMessage(), e);
        VoiceProcessResponse response = VoiceProcessResponse.builder()
            .success(false)
            .message("서버 오류가 발생했습니다.")
            .data(null)
            .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}