package com.e_library.modules.user.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.e_library.modules.user.service.GeminiChatService; // Importando o seu serviço

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private GeminiChatService chatService;

    @PostMapping
    public String conversarComIA(@RequestBody String mensagem) {
        return chatService.enviarMensagem(mensagem);
    }
}