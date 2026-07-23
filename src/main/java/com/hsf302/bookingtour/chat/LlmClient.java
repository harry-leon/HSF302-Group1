package com.hsf302.bookingtour.chat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Thin wrapper around the Anthropic Messages API. Only this class sees the
 * API key. Config defaults are inlined in @Value so application.properties
 * doesn't need to be touched - override via real environment variables
 * (ANTHROPIC_API_KEY, CHAT_LLM_MODEL, CHAT_LLM_ENABLED) when running.
 *
 * Builds its own RestClient rather than injecting a RestClient.Builder bean.
 * On this project's Spring Boot 4.1.0 setup, RestClientAutoConfiguration's
 * bean was not actually being registered at runtime (confirmed via a real
 * NoSuchBeanDefinitionException), even though spring-boot-autoconfigure is
 * on the classpath - likely a conditional-wiring quirk tied to the very new
 * "webmvc" starter naming in Boot 4.1. Building the client directly avoids
 * depending on that auto-configuration at all.
 */
@Component
public class LlmClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final boolean enabled;

    public LlmClient(@Value("${CHAT_LLM_ENDPOINT:https://api.anthropic.com/v1/messages}") String endpoint,
                     @Value("${ANTHROPIC_API_KEY:}") String apiKey,
                     @Value("${CHAT_LLM_MODEL:claude-sonnet-5}") String model,
                     @Value("${CHAT_LLM_ENABLED:false}") boolean enabled) {
        this.restClient = RestClient.builder().baseUrl(endpoint).build();
        this.apiKey = apiKey;
        this.model = model;
        this.enabled = enabled;
    }

    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    /**
     * Calls the LLM with a system prompt (the RAG guardrail + retrieved
     * context) and the running conversation. Returns the assistant's plain
     * text reply, or throws if the call fails - callers should catch and
     * fall back to a safe message.
     */
    @SuppressWarnings("unchecked")
    public String complete(String systemPrompt, List<Map<String, Object>> messages) {
        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", 500,
                "system", systemPrompt,
                "messages", messages
        );

        Map<String, Object> response = restClient.post()
                .uri("")
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .body(body)
                .retrieve()
                .body(Map.class);

        if (response == null || !(response.get("content") instanceof List<?> blocks)) {
            throw new IllegalStateException("Empty response from LLM provider");
        }

        StringBuilder text = new StringBuilder();
        for (Object block : blocks) {
            if (block instanceof Map<?, ?> map && "text".equals(map.get("type"))) {
                text.append(map.get("text"));
            }
        }
        if (text.isEmpty()) {
            throw new IllegalStateException("No text content in LLM response");
        }
        return text.toString();
    }
}