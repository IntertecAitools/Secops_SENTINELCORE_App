package com.sentinelcore.backend.controller;

import com.sentinelcore.backend.dto.ApiResponse;
import com.sentinelcore.backend.dto.ChatRequestDTO;
import com.sentinelcore.backend.dto.ChatResponseDTO;
import com.sentinelcore.backend.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/query")
    public ResponseEntity<ApiResponse<ChatResponseDTO>> processChatQuery(
            @RequestBody ChatRequestDTO request,
            Authentication authentication) {

        String username = authentication != null ? authentication.getName() : "User";
        ChatResponseDTO response = chatbotService.processQuery(request, username);
        return ResponseEntity.ok(ApiResponse.success("Query processed successfully", response));
    }
}
