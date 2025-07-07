package com.example.ai.controller;

import com.example.ai.entity.vo.MessageVO;
import com.example.ai.repository.ChatHistoryRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.module.FindException;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/history")
public class ChatHistoryController
{
    private final ChatHistoryRepository chatHistoryRepository;

    private final ChatMemory chatMemory;

    @GetMapping("/{type}")
    public List<String> getChatIds(@PathVariable("type") String type) {
        return chatHistoryRepository.getChatIds(type);
    }

    @GetMapping("/{type}/{chatId}")
    public List<MessageVO> getChatHistory(@PathVariable("type") String type,@PathVariable("chatId") String chatId){
        List<Message> messages = chatMemory.get(chatId);
        if(messages == null){
            return List.of();
        }
        //m -> new MessageVO(m) 等价于MessageVO::new
        return messages.stream().map(m -> new MessageVO(m)).toList();
    }
}
