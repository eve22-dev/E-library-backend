package com.e_library.modules.user.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.e_library.modules.user.service.GeminiChatService;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*") // Libera o acesso para o front-end
public class ChatController {

    @Autowired
    private GeminiChatService geminiChatService;

    @PostMapping
    public String conversar(@RequestBody String mensagem) {
        return geminiChatService.callGemini(mensagem);
    }
}