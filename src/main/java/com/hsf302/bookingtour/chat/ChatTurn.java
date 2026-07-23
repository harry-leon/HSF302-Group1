package com.hsf302.bookingtour.chat;

/** One turn of chat history, sent from the browser so the server stays stateless. */
public record ChatTurn(String role, String text) {
}