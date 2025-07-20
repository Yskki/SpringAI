package com.example.ai.repository;

import com.example.ai.entity.po.Msg;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class InMemoryChatHistoryRepository implements ChatHistoryRepository
{
    //这个可以看成一个小Demo，不是真实项目，因为没存数据库
    //我们这里会话历史没存到数据库中，暂时先存到内存中，因此可以简单的利用map实现
    private Map<String,List<String>> chatHistory;
    private final ObjectMapper objectMapper;
    private final ChatMemory chatMemory;
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

    @PostConstruct
    private void init() {
        // 1.初始化会话历史记录
        this.chatHistory = new HashMap<>();
        // 2.读取本地会话历史和会话记忆
        FileSystemResource historyResource = new FileSystemResource("chat-history.json");
        FileSystemResource memoryResource = new FileSystemResource("chat-memory.json");
        if (!historyResource.exists()) {
            return;
        }
        try {
            // 会话历史
            Map<String, List<String>> chatIds = this.objectMapper.readValue(historyResource.getInputStream(), new TypeReference<>() {
            });
            if (chatIds != null) {
                this.chatHistory = chatIds;
            }
            // 会话记忆
            Map<String, List<Msg>> memory = this.objectMapper.readValue(memoryResource.getInputStream(), new TypeReference<>() {
            });
            if (memory != null) {
                memory.forEach(this::convertMsgToMessage);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void convertMsgToMessage(String chatId, List<Msg> messages) {
        this.chatMemory.add(chatId, messages.stream().map(Msg::toMessage).toList());
    }

    @PreDestroy
    private void persistent() {
        String history = toJsonString(this.chatHistory);
        String memory = getMemoryJsonString();
        FileSystemResource historyResource = new FileSystemResource("chat-history.json");
        FileSystemResource memoryResource = new FileSystemResource("chat-memory.json");
        try (
                PrintWriter historyWriter = new PrintWriter(historyResource.getOutputStream(), true, StandardCharsets.UTF_8);
                PrintWriter memoryWriter = new PrintWriter(memoryResource.getOutputStream(), true, StandardCharsets.UTF_8)
        ) {
            historyWriter.write(history);
            memoryWriter.write(memory);
        } catch (IOException ex) {
            log.error("IOException occurred while saving vector store file.", ex);
            throw new RuntimeException(ex);
        } catch (SecurityException ex) {
            log.error("SecurityException occurred while saving vector store file.", ex);
            throw new RuntimeException(ex);
        } catch (NullPointerException ex) {
            log.error("NullPointerException occurred while saving vector store file.", ex);
            throw new RuntimeException(ex);
        }
    }

    private String getMemoryJsonString()
    {
        //这里之前是InMemoryChatMemory，但是现在没有这里之前是InMemoryChatMemory
//        Class<MessageWindowChatMemory> clazz = MessageWindowChatMemory.class;
//        try {
//            Field field = clazz.getDeclaredField("chatMemoryRepository");
//            field.setAccessible(true);
//            Map<String, List<Message>> memory = (Map<String, List<Message>>) field.get(chatMemory);
//            Map<String, List<Msg>> memoryToSave = new HashMap<>();
//            memory.forEach((chatId, messages) -> memoryToSave.put(chatId, messages.stream().map(Msg::new).toList()));
//            return toJsonString(memoryToSave);
//        } catch (NoSuchFieldException | IllegalAccessException e) {
//            throw new RuntimeException(e);
//        }
        //这里是让大模型写的代码，因为上边给的代码不对
        try {
            // 1. 获取MessageWindowChatMemory的Class对象
            Class<MessageWindowChatMemory> clazz = MessageWindowChatMemory.class;
            // 2. 获取名为"chatMemoryRepository"的私有字段对象
            Field field = clazz.getDeclaredField("chatMemoryRepository");
            // 3. 设置可访问（因为字段是private的）
            field.setAccessible(true);
            // 4. 关键操作：从chatMemory实例中提取chatMemoryRepository字段的值
            /*
                关于这句代码的理解：
                如果不使用反射，这段代码想做的等价操作：
                假设chatMemoryRepository是public的（实际不是）
                ChatMemoryRepository repository = ((MessageWindowChatMemory)chatMemory).chatMemoryRepository;
                但因为chatMemoryRepository是private的，所以必须用反射。
             */
            ChatMemoryRepository repository = (ChatMemoryRepository) field.get(chatMemory);

            // 使用 ChatMemoryRepository 的公共方法获取所有对话 ID
            List<String> conversationIds = repository.findConversationIds();
            Map<String, List<Msg>> memoryToSave = new HashMap<>();

            // 遍历每个对话 ID 获取消息
            for (String conversationId : conversationIds) {
                List<Message> messages = repository.findByConversationId(conversationId);
                memoryToSave.put(conversationId, messages.stream().map(Msg::new).toList());
            }

            return toJsonString(memoryToSave);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private String toJsonString(Object object) {
        ObjectWriter objectWriter = this.objectMapper.writerWithDefaultPrettyPrinter();
        try {
            return objectWriter.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing documentMap to JSON.", e);
        }
    }
}
