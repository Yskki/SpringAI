package com.example.ai.controller;

import com.example.ai.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    public Flux<String> chat(@RequestParam("prompt") String prompt,
                             @RequestParam("chatId") String chatId,
                             @RequestParam(value = "files",required = false)List<MultipartFile> files)
    {
        //1.保存会话id
        chatHistoryRepository.save("chat",chatId);
        if(files == null || files.isEmpty()){
            //2.如果没有附件，则是纯文本聊天
            return textChat(prompt,chatId);
        }else{
            //3.如果有附件，则是多模态聊天
            return multiModelChat(prompt,chatId,files);
        }
    }

    private Flux<String> multiModelChat(String prompt, String chatId, List<MultipartFile> files)
    {
        //1.解析多媒体
        List<Media> medias = new ArrayList<>();
        for(MultipartFile file : files)
        {
            //Objects.requireNonNull是进行非空处理
            String contentType = Objects.requireNonNull(file.getContentType());
            MimeType mimeType = MimeType.valueOf(contentType);
            Media media = new Media(mimeType,file.getResource());
            medias.add(media);
        }
        //2.请求模型
        return chatClient.prompt()
                //Media[]::new是方法引用，等价于 size -> new Media[size]
                .user(p -> p.text(prompt).media(medias.toArray(Media[] :: new)))
                .advisors(a -> a.param(CONVERSATION_ID,chatId)) //新增了用户id参数，防止不同用户串记忆
                .stream()//stream就是流式输出
                .content();
    }

    private Flux<String> textChat(String prompt, String chatId)
    {
        return chatClient.prompt()
                .user(prompt)
                .advisors(a -> a.param(CONVERSATION_ID,chatId)) //新增了用户id参数，防止不同用户串记忆
                .stream()//stream就是流式输出
                .content();
    }
}
