package com.example.ai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfiguration implements WebMvcConfigurer
{
    @Override
    public void addCorsMappings(CorsRegistry registry)
    {
        registry.addMapping("/**")
                .allowedOrigins("*") //允许所有的域
                .allowedMethods("GET","POST","PUT","DELETE","OPTIONS") //允许的方法
                .allowedHeaders("*");//允许所有请求头
    }
}
