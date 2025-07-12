package com.example.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
//不加这个包扫描，mapper不生效。为啥？
/*
@MapperScan 的作用是告诉 MyBatisPlus 在哪里扫描 Mapper 接口，并自动为其生成代理实现类。
如果不加 @MapperScan，MyBatisPlus 就不知道哪些接口需要被动态代理，
导致 Mapper 无法注入，最终报 NoSuchBeanDefinitionException 错误。
之前项目中没加是因为在每个mapper接口上都添加了@mapper注解
*/
@MapperScan("com.example.ai.mapper")
@SpringBootApplication
public class SpringAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiApplication.class, args);
    }

}
