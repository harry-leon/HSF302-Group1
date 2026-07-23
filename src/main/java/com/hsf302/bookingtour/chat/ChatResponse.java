package com.hsf302.bookingtour.chat;

import java.util.List;

public record ChatResponse(String reply, List<String> sources) {
}