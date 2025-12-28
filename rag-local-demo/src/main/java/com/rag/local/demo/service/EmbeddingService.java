package com.rag.local.demo.service;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import de.tudarmstadt.ukp.dkpro.core.bertembedder.BertEmbedder;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class EmbeddingService {
    @Resource
    private ModelConfig modelConfig;
    private AnalysisEngine bertEmbedder;
    private OrtEnvironment ortEnv;

    // 初始化嵌入模型
    @PostConstruct
    public void init() throws Exception {
        ortEnv = OrtEnvironment.getEnvironment();
        // 配置BertEmbedder，加载本地ONNX格式的bge-small-zh模型
        bertEmbedder = AnalysisEngineFactory.createEngine(
                BertEmbedder.class,
                BertEmbedder.PARAM_MODEL_NAME_OR_PATH, modelConfig.getEmbeddingModelPath(),
                BertEmbedder.PARAM_LAYER_INDEX, -1, // 使用最后一层输出
                BertEmbedder.PARAM_USE_CASED_TOKENIZATION, false,
                BertEmbedder.PARAM_DEVICE, "CPU" // 纯CPU运行
        );
        System.out.println("嵌入模型初始化完成");
    }

    // 文本转向量：生成384维向量（bge-small-zh默认维度）
    public float[] textToEmbedding(String text) throws Exception {
        JCas jCas = JCasFactory.createJCas();
        jCas.setDocumentText(text);
        try {
            bertEmbedder.process(jCas);
        } catch (AnalysisEngineProcessException e) {
            System.err.println("文本嵌入转化失败：" + e.getMessage());
            return new float[0];
        }
        // 计算句子级向量：取所有token向量的平均值
        List<float[]> tokenEmbeddings = new ArrayList<>();
        for (Sentence sentence : JCasUtil.select(jCas, Sentence.class)) {
            for (Token token : JCasUtil.selectCovered(Token.class, sentence)) {
                float[] embedding = token.getEmbedding();
                if (embedding != null && embedding.length > 0) {
                    tokenEmbeddings.add(embedding);
                }
            }
        }
        if (tokenEmbeddings.isEmpty()) {
            return new float[0];
        }
        // 计算平均向量
        float[] avgEmbedding = new float[tokenEmbeddings.get(0).length];
        for (float[] embedding : tokenEmbeddings) {
            for (int i = 0; i < avgEmbedding.length; i++) {
                avgEmbedding[i] += embedding[i];
            }
        }
        for (int i = 0; i < avgEmbedding.length; i++) {
            avgEmbedding[i] /= tokenEmbeddings.size();
        }
        return avgEmbedding;
    }
}