package com.hsf302.bookingtour.chat;

import com.hsf302.bookingtour.catalog.Tour;
import com.hsf302.bookingtour.catalog.TourKnowledgeBase;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            You are the in-app tour assistant for this booking demo.
            Always answer in Vietnamese, regardless of what language the
            question is asked in.
            Only answer using the CONTEXT below, which lists tours currently
            available in the app's own catalog, including each tour's detail
            page URL. Do not use outside knowledge, and do not invent tours,
            prices, links, or details that are not in CONTEXT. When a user
            asks about a specific tour, include its detail page URL in your
            reply so they can click through.
            If the question cannot be answered from CONTEXT, reply exactly
            (in Vietnamese):
            "Xin loi, toi khong co thong tin nay trong ung dung - vui long kiem tra danh sach tour hoac lien he ho tro."
            Keep answers short (2-3 sentences).

            CONTEXT:
            %s
            """;

    private final TourKnowledgeBase knowledgeBase;
    private final LlmClient llmClient;

    public ChatService(TourKnowledgeBase knowledgeBase, LlmClient llmClient) {
        this.knowledgeBase = knowledgeBase;
        this.llmClient = llmClient;
    }

    public ChatResponse ask(String userMessage, List<ChatTurn> history) {
        List<Tour> context = knowledgeBase.search(userMessage);
        List<String> sourceLabels = context.stream()
                .map(t -> t.name() + " (" + t.location() + ")")
                .toList();

        if (!llmClient.isConfigured()) {
            return new ChatResponse(noKeyFallback(context), sourceLabels);
        }

        String systemPrompt = SYSTEM_PROMPT_TEMPLATE.formatted(renderContext(context));
        try {
            List<Map<String, Object>> messages = new ArrayList<>();
            for (ChatTurn turn : history) {
                messages.add(Map.of("role", normalizeRole(turn.role()), "content", turn.text()));
            }
            messages.add(Map.of("role", "user", "content", userMessage));

            String reply = llmClient.complete(systemPrompt, messages);
            return new ChatResponse(reply, sourceLabels);
        } catch (RuntimeException ex) {
            return new ChatResponse(
                    "Tro ly khong ket noi duoc voi AI luc nay. Vui long thu lai sau it phut.",
                    sourceLabels);
        }
    }

    /**
     * Fallback used when no LLM is configured (no ANTHROPIC_API_KEY set).
     * When retrieval narrowed to exactly one tour, synthesize a real-looking
     * Vietnamese sentence straight from the catalog data - no LLM needed for
     * this case, since there's only one possible answer.
     */
    private String noKeyFallback(List<Tour> context) {
        if (context.size() == 1) {
            Tour t = context.get(0);
            return "%s la mot tour den %s, gia $%d/nguoi, danh gia %.1f sao. %s Xem chi tiet tai: /tour/detail/%s"
                    .formatted(t.name(), t.location(), t.price(), t.rating(), t.summary(), t.id());
        }
        String listing = context.stream()
                .map(t -> "%s - $%d/nguoi (%s)".formatted(t.name(), t.price(), t.location()))
                .collect(Collectors.joining(", "));
        return "Tro ly AI chua duoc cau hinh (chua thiet lap ANTHROPIC_API_KEY) - du lieu tour phu hop: "
                + listing;
    }

    private String normalizeRole(String role) {
        return "user".equalsIgnoreCase(role) ? "user" : "assistant";
    }

    private String renderContext(List<Tour> tours) {
        return tours.stream()
                .map(t -> "- %s | %s | $%d/person | rating %.1f | %s | detail page: /tour/detail/%s".formatted(
                        t.name(), t.location(), t.price(), t.rating(), t.summary(), t.id()))
                .collect(Collectors.joining("\n"));
    }
}