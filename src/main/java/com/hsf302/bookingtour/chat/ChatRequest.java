package com.hsf302.bookingtour.chat;

import java.util.List;

public record ChatRequest(String message, List<ChatTurn> history) {
    public List<ChatTurn> historyOrEmpty() {
        return history == null ? List.of() : history;
    }
}