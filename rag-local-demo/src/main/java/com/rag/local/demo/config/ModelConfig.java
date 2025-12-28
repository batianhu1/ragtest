package com.rag.local.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "rag.model")
public class ModelConfig {
    // 嵌入模型路径
    private String embeddingModelPath = "./models/bge-small-zh-onnx";
    // 大模型路径
    private String llmModelPath = "./models/qwen-7b-chat-gguf/qwen-7b-chat-q4_0.gguf";
    // 文档存储路径
    private String docPath = "./docs";
    // Milvus-Lite存储路径
    private String milvusPath = "./milvus_db";
}