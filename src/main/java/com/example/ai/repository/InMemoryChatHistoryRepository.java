package com.example.ai.repository;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class InMemoryChatHistoryRepository implements ChatHistoryRepository
{
    //这个可以看成一个小Demo，不是真实项目，因为没存数据库
    //我们这里会话历史没存到数据库中，暂时先存到内存中，因此可以简单的利用map实现
    private final Map<String,List<String>> chatHistory = new HashMap<>();
    @Override
    public void save(String type, String chatId)
    {
        //List<String> chatIds = chatHistory.getOrDefault(type,new ArrayList<>());
        List<String> chatIds = chatHistory.computeIfAbsent(type,k -> new ArrayList<>());
        if(chatIds.contains(chatId)) {
            return;
        }
        chatIds.add(chatId);
    }

    @Override
    public List<String> getChatIds(String type) {
        List<String> chatIds = chatHistory.getOrDefault(type, new ArrayList<>());
        return chatIds;
    }
}
