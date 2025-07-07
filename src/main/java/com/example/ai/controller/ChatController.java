package com.example.ai.controller;

import com.example.ai.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/ai")
public class ChatController
{
    private final ChatClient chatClient;
    
    private final ChatHistoryRepository chatHistoryRepository;

    //后边的produces是为了输出的不是乱码
    @RequestMapping(value = "/chat",produces = "text/html;charset=utf-8")
    public Flux<String> chat(String prompt,String chatId)
    {
        //1.保存会话id
        chatHistoryRepository.save("chat",chatId);
        //2.请求模型
        return chatClient.prompt()
                .user(prompt)
                .advisors(a -> a.param(CONVERSATION_ID,chatId)) //新增了用户id参数，防止不同用户串记忆
                .stream()//stream就是流式输出
                .content();
    }
}
