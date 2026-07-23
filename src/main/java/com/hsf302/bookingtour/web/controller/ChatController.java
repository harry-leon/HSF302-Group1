package com.hsf302.bookingtour.web.controller;

import com.hsf302.bookingtour.catalog.TourKnowledgeBase;
import com.hsf302.bookingtour.chat.ChatRequest;
import com.hsf302.bookingtour.chat.ChatResponse;
import com.hsf302.bookingtour.chat.ChatService;
import com.hsf302.bookingtour.chat.ChatTurn;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/ai-chat")
public class ChatController {

    private final ChatService chatService;
    private final TourKnowledgeBase knowledgeBase;

    public ChatController(ChatService chatService, TourKnowledgeBase knowledgeBase) {
        this.chatService = chatService;
        this.knowledgeBase = knowledgeBase;
    }

    @GetMapping
    public String page(Model model) {
        model.addAttribute("messages", List.of(
                new ChatTurn("assistant", "Hi! Ask me about tours in this app - I only answer from the app's own tour list.")
        ));
        model.addAttribute("knowledgeBase", knowledgeBase.all().stream()
                .map(t -> t.name() + " - " + t.price() + " USD")
                .toList());
        return "chat";
    }

    @PostMapping(value = "/messages", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ChatResponse sendMessage(@RequestBody ChatRequest request) {
        return chatService.ask(request.message(), request.historyOrEmpty());
    }
}