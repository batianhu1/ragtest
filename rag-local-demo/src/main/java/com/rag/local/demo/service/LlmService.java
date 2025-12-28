package com.rag.local.demo.service;

import com.github.johnsonmoon.llama.cpp.java.LlamaCpp;
import com.github.johnsonmoon.llama.cpp.java.config.InferenceConfig;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Service
public class LlmService {
    @Resource
    private ModelConfig modelConfig;
    private LlamaCpp llamaCpp;

    // 初始化LlamaCpp，加载本地大模型
    @PostConstruct
    public void init() {
        try {
            // 加载GGUF格式模型
            llamaCpp = LlamaCpp.loadModel(modelConfig.getLlmModelPath());
            System.out.println("本地大模型初始化完成");
        } catch (Exception e) {
            System.err.println("加载本地大模型失败：" + e.getMessage());
            throw new RuntimeException("大模型初始化失败", e);
        }
    }

    // 生成回答：结合问题和检索到的文档片段
    public String generateAnswer(String query, List<String> similarChunks) {
        if (llamaCpp == null) {
            return "本地大模型未初始化成功，无法生成回答";
        }
        // 构建提示词：将问题和检索到的片段拼接
        StringBuilder prompt = new StringBuilder();
        prompt.append("请基于以下参考资料，回答用户问题。参考资料：");
        for (int i = 0; i < similarChunks.size(); i++) {
            prompt.append("\n").append(i + 1).append(". ").append(similarChunks.get(i));
        }
        prompt.append("\n用户问题：").append(query);
        prompt.append("\n回答要求：简洁准确，基于参考资料，不要编造信息。");

        // 配置推理参数
        InferenceConfig config = InferenceConfig.builder()
                .nThreads(Runtime.getRuntime().availableProcessors()) // 使用所有可用CPU核心
                .nMaxTokens(512) // 生成回答的最大长度
                .temperature(0.7f) // 随机性，0.7适中
                .topP(0.9f)
                .repeatPenalty(1.1f) // 避免重复生成
                .build();

        // 执行推理，生成回答
        try {
            return llamaCpp.generate(prompt.toString(), config);
        } catch (Exception e) {
            System.err.println("生成回答失败：" + e.getMessage());
            return "生成回答时发生错误：" + e.getMessage();
        }
    }
}