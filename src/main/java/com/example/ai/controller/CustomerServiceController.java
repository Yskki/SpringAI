package com.example.ai.controller;

import com.example.ai.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/ai")
public class CustomerServiceController
{
    private final ChatClient serviceChatClient;
    private final ChatHistoryRepository chatHistoryRepository;
    //后边的produces是为了输出的不是乱码
    @RequestMapping(value = "/service",produces = "text/html;charset=utf-8")
    public Flux<String> service(String prompt,String chatId)
    {
        //1.保存会话id
        chatHistoryRepository.save("service",chatId);
        //2.请求模型
        return serviceChatClient.prompt()
                .user(prompt)
                .advisors(a -> a.param(CONVERSATION_ID,chatId)) //新增了用户id参数，防止不同用户串记忆
                .stream()//stream就是流式输出
                .content();
    }
}
