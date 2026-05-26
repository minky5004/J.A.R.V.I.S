package com.jarvis.entity;

public enum ConversationRole {
    USER("user"),
    ASSISTANT("assistant");

    private final String value;

    ConversationRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ConversationRole fromString(String value) {
        for (ConversationRole role : ConversationRole.values()) {
            if (role.value.equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + value);
    }
}
