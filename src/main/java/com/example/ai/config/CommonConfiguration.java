package com.example.ai.config;

import com.example.ai.constants.SystemConstants;
import com.example.ai.tools.CourseTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonConfiguration
{
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory
                .builder()
                //.maxMessages(10) // 保留最近10条消息
                .build();
    }

    //ChatClient是一个通用的客户端，不管对接任何的大模型，都使用这个客户端
    @Bean
    public ChatClient chatClient(OpenAiChatModel model,ChatMemory chatMemory)
    {
        return ChatClient
                .builder(model)
                //单独给这个模块配置这个多模态模型，其他模块模型不变
                .defaultOptions(ChatOptions.builder().model("qwen-omni-turbo").build())
                //可以在配置客户端的时候设置system提示词
                .defaultSystem("你是一个热心可爱的智能助手小团团")
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),//配置日志Advisor
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

    @Bean
    public ChatClient gameChatClient(OpenAiChatModel model,ChatMemory chatMemory)
    {
        return ChatClient
                .builder(model)
                //可以在配置客户端的时候设置system提示词
                .defaultSystem(SystemConstants.GAME_SYSTEM_PROMPT)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),//配置日志Advisor
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

    @Bean
    public ChatClient serviceChatClient(OpenAiChatModel model, ChatMemory chatMemory, CourseTools courseTools)
    {
        return ChatClient
                .builder(model)
                //可以在配置客户端的时候设置system提示词
                .defaultSystem(SystemConstants.SERVICE_SYSTEM_PROMPT)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),//配置日志Advisor
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .defaultTools(courseTools) //配置tools
                .build();
    }

    @Bean
    public ChatClient pdfChatClient(OpenAiChatModel model, ChatMemory chatMemory, VectorStore vectorStore)
    {
        return ChatClient
                .builder(model)
                //可以在配置客户端的时候设置system提示词
                .defaultSystem("请根据上下文回答问题，遇到上下文没有的问题，不要随意编造。")
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),//配置日志Advisor
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder()
                                        .similarityThreshold(0.7)
                                        .topK(2)
                                        .build()).build()
                )
                .build();
    }
}
