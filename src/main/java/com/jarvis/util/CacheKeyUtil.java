package com.jarvis.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public class CacheKeyUtil {

    private static final String HASH_ALGORITHM = "SHA-256";

    public static String generateVoiceResponseKey(String sessionId, String textOrFileHash) {
        Objects.requireNonNull(sessionId, "sessionId는 null일 수 없습니다");
        Objects.requireNonNull(textOrFileHash, "textOrFileHash는 null일 수 없습니다");
        return String.format("voiceResponse:%s:%s", sessionId, textOrFileHash);
    }

    public static String generateVoiceResponseStreamKey(String sessionId, String textOrFileHash) {
        Objects.requireNonNull(sessionId, "sessionId는 null일 수 없습니다");
        Objects.requireNonNull(textOrFileHash, "textOrFileHash는 null일 수 없습니다");
        return String.format("voiceResponseStream:%s:%s", sessionId, textOrFileHash);
    }

    public static String generateConversationHistoryKey(String sessionId, int page, int size) {
        Objects.requireNonNull(sessionId, "sessionId는 null일 수 없습니다");
        return String.format("conversationHistory:%s:page:%d:size:%d", sessionId, page, size);
    }

    public static String generateConversationHistoryRecentKey(String sessionId) {
        Objects.requireNonNull(sessionId, "sessionId는 null일 수 없습니다");
        return String.format("conversationHistoryRecent:%s", sessionId);
    }

    public static String hashText(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("해시 생성 실패", e);
        }
    }

    public static String hashBytes(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(bytes);
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("해시 생성 실패", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
