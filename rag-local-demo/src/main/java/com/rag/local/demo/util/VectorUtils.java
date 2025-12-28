package com.rag.local.demo.util;

import java.util.Arrays;

public class VectorUtils {
    
    /**
     * 计算两个向量的余弦相似度
     */
    public static double cosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("向量维度不一致");
        }
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        
        normA = Math.sqrt(normA);
        normB = Math.sqrt(normB);
        
        if (normA == 0 || normB == 0) {
            return 0.0;
        }
        
        return dotProduct / (normA * normB);
    }
    
    /**
     * 计算两个向量的欧几里得距离
     */
    public static double euclideanDistance(float[] vectorA, float[] vectorB) {
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("向量维度不一致");
        }
        
        double sum = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            sum += Math.pow(vectorA[i] - vectorB[i], 2);
        }
        
        return Math.sqrt(sum);
    }
    
    /**
     * 向量归一化
     */
    public static float[] normalize(float[] vector) {
        double norm = 0.0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        
        if (norm == 0) {
            return vector.clone();
        }
        
        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = (float) (vector[i] / norm);
        }
        
        return normalized;
    }
}