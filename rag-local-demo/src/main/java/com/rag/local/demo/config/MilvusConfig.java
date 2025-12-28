package com.rag.local.demo.config;

import io.milvus.client.MilvusClient;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Configuration
public class MilvusConfig {
    @Resource
    private ModelConfig modelConfig;

    @Bean
    public MilvusClient milvusClient() {
        // 初始化Milvus-Lite客户端（本地嵌入式部署）
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withUri("file:" + modelConfig.getMilvusPath()) // 本地存储路径
                .build();
        return new MilvusServiceClient(connectParam);
    }
}