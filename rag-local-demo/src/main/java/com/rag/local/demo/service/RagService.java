package com.rag.local.demo.service;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class RagService {
    @Resource
    private DocumentProcessService documentProcessService;
    @Resource
    private EmbeddingService embeddingService;
    @Resource
    private MilvusService milvusService;
    @Resource
    private LlmService llmService;

    // 知识库初始化：文档加载→拆分→嵌入→向量入库
    public boolean initKnowledgeBase() {
        try {
            // 1. 加载文档
            List<String> documents = documentProcessService.loadDocuments();
            if (documents.isEmpty()) {
                System.err.println("未加载到任何文档，请检查docs文件夹");
                return false;
            }
            // 2. 拆分文本
            List<String> allChunks = new ArrayList<>();
            for (String doc : documents) {
                allChunks.addAll(documentProcessService.splitText(doc));
            }
            System.out.println("文档拆分后片段数量：" + allChunks.size());
            if (allChunks.isEmpty()) {
                System.err.println("文本拆分后无有效片段");
                return false;
            }
            // 3. 文本转向量
            List<float[]> allEmbeddings = new ArrayList<>();
            for (String chunk : allChunks) {
                float[] embedding = embeddingService.textToEmbedding(chunk);
                if (embedding.length > 0) {
                    allEmbeddings.add(embedding);
                }
            }
            if (allEmbeddings.isEmpty()) {
                System.err.println("向量生成失败");
                return false;
            }
            // 4. 向量入库
            return milvusService.insertEmbeddings(allChunks, allEmbeddings);
        } catch (Exception e) {
            System.err.println("知识库初始化失败：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // RAG问答：问题→向量转化→检索相似片段→生成回答
    public String ragQa(String query) {
        try {
            // 1. 问题转向量
            float[] queryEmbedding = embeddingService.textToEmbedding(query);
            if (queryEmbedding.length == 0) {
                return "问题向量转化失败，请重新输入问题";
            }
            // 2. 检索相似片段
            List<String> similarChunks = milvusService.searchSimilarChunks(queryEmbedding);
            if (similarChunks.isEmpty()) {
                return "未检索到相关文档片段，请尝试其他问题";
            }
            // 3. 生成回答
            return llmService.generateAnswer(query, similarChunks);
        } catch (Exception e) {
            System.err.println("问答失败：" + e.getMessage());
            return "问答过程中发生错误：" + e.getMessage();
        }
    }
}