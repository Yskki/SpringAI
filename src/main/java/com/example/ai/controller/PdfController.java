package com.example.ai.controller;


import com.example.ai.entity.vo.Result;
import com.example.ai.repository.ChatHistoryRepository;
import com.example.ai.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

//用于实现文件的上传、下载
@Slf4j //日志
@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/pdf")
public class PdfController
{
    private final FileRepository fileRepository;
    private final VectorStore vectorStore;
    private final ChatClient pdfChatClient;//注意这里C一定要大写，因为Bean名称是pdfChatClient
    private final ChatHistoryRepository chatHistoryRepository;

    //后边的produces是为了输出的不是乱码
    @RequestMapping(value = "/chat",produces = "text/html;charset=utf-8")
    public Flux<String> chat(String prompt, String chatId)
    {
        //1.找到会话文件
        Resource file = fileRepository.getFile(chatId);
        if(!file.exists()){
            //文件不存在
            throw new RuntimeException("会话文件不存在！");
        }
        //2.保存会话id
        chatHistoryRepository.save("pdf",chatId);
        //3.请求模型
        return pdfChatClient.prompt()
                .user(prompt)
                .advisors(a -> a.param(CONVERSATION_ID,chatId)) //新增了用户id参数，防止不同用户串记忆
                //下边这个因为最新的file_name没有了，所以暂时先不配置这个
                //.advisors(a -> a.param(FILTER_EXPRESSION,"file_name == '" + file.getFilename() + "'"))
                .stream()//stream就是流式输出
                .content();
    }
    /*
        文件上传
     */
    @RequestMapping("/upload/{chatId}")
    public Result uploadPdf(@PathVariable String chatId, @RequestParam("file")MultipartFile file)
    {
        try {
            //1.检查文件是否是pdf格式的
            if(!Objects.equals(file.getContentType(),"application/pdf")){
                return Result.fail("只能上传PDF文件！");
            }
            //2.保存文件
            boolean success = fileRepository.save(chatId, file.getResource());
            if(!success){
                return Result.fail("上传文件失败！");
            }
            //3.写入向量库
            writeToVectorStore(file.getResource());
            return Result.ok();
        }catch (Exception e){
            log.error("Failed to upload PDF",e);
            return Result.fail("上传文件失败！");
        }
    }
    /*
        文件下载
     */
    @RequestMapping("/file/{chatId}")
    public ResponseEntity<Resource> download(@PathVariable("chatId") String charId)
    {
        //1.读取文件
        Resource resource = fileRepository.getFile(charId);
        if(!resource.exists()){
            return  ResponseEntity.notFound().build();
        }
        //2.文件名编码，写入响应头
        //编码是为了防止文件名中有特殊字符(包括中文)导致问题
        String filename = URLEncoder.encode(Objects.requireNonNull(resource.getFilename()), StandardCharsets.UTF_8);
        //3.返回文件
        return ResponseEntity.ok()
                //设置文件类型为通用的二进制流
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                //设置响应头告诉浏览器这是一个要下载的附件，而不是直接显示
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                //最后把文件内容发送给用户
                .body(resource);
    }

    private void writeToVectorStore(Resource resource)
    {
        //1.创建PDF的读取器
        PagePdfDocumentReader reader = new PagePdfDocumentReader(
                resource, // 文件源
                PdfDocumentReaderConfig.builder()
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.defaults())
                        .withPagesPerDocument(1) // 每1页PDF作为一个Document
                        .build()
        );
        // 2.读取PDF文档，拆分为Document
        List<Document> documents = reader.read();
        // 3.写入向量库
        vectorStore.add(documents);
    }
}
