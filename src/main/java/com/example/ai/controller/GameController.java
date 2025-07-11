package com.example.ai.controller;

import com.example.ai.entity.vo.MessageVO;
import com.example.ai.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@RequiredArgsConstructor
@RestController
@RequestMapping("ai")
public class GameController
{
    private final ChatClient gameChatClient;

    //后边的produces是为了输出的不是乱码
    @RequestMapping(value = "/game",produces = "text/html;charset=utf-8")
    public Flux<String> chat(String prompt, String chatId)
    {
        //请求模型
        return gameChatClient.prompt()
                .user(prompt)
                .advisors(a -> a.param(CONVERSATION_ID,chatId)) //新增了用户id参数，防止不同用户串记忆
                .stream()//stream就是流式输出
                .content();
    }
}
