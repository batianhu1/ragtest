package com.rag.local.demo.service;

import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentProcessService {
    @Resource
    private ModelConfig modelConfig;
    private final Tika tika = new Tika();

    // 文档加载：读取docs文件夹下所有文档，提取纯文本
    public List<String> loadDocuments() throws IOException {
        List<String> documentTexts = new ArrayList<>();
        File docDir = new File(modelConfig.getDocPath());
        File[] files = docDir.listFiles((dir, name) -> 
                name.endsWith(".pdf") || name.endsWith(".docx") || name.endsWith(".txt"));
        if (files == null) {
            return documentTexts;
        }
        for (File file : files) {
            try (FileInputStream fis = new FileInputStream(file)) {
                // 用Tika提取文本，自动识别文档格式
                String text = tika.parseToString(fis);
                // 去除多余空格和空行
                text = text.replaceAll("\\s+", " ").trim();
                if (!text.isEmpty()) {
                    documentTexts.add(text);
                }
            } catch (Exception e) {
                System.err.println("加载文档" + file.getName() + "失败：" + e.getMessage());
            }
        }
        System.out.println("成功加载文档数量：" + documentTexts.size());
        return documentTexts;
    }

    // 文本拆分：将长文本拆分为800字左右的片段，重叠100字
    public List<String> splitText(String text) {
        List<String> chunks = new ArrayList<>();
        int chunkSize = 800;
        int overlapSize = 100;
        if (text.length() <= chunkSize) {
            chunks.add(text);
            return chunks;
        }
        // 使用SmartChineseAnalyzer进行中文分词，避免拆分完整语义
        try (SmartChineseAnalyzer analyzer = new SmartChineseAnalyzer();
             org.apache.lucene.analysis.TokenStream tokenStream = analyzer.tokenStream(null, new StringReader(text))) {
            OffsetAttribute offsetAttr = tokenStream.addAttribute(OffsetAttribute.class);
            tokenStream.reset();
            List<int[]> tokenOffsets = new ArrayList<>();
            while (tokenStream.incrementToken()) {
                tokenOffsets.add(new int[]{offsetAttr.startOffset(), offsetAttr.endOffset()});
            }
            tokenStream.end();
            // 按字符数拆分，基于分词结果调整拆分位置，避免拆分单词
            int start = 0;
            while (start < text.length()) {
                int end = Math.min(start + chunkSize, text.length());
                // 找到最近的分词结束位置
                for (int[] offset : tokenOffsets) {
                    if (offset[1] > end) {
                        end = offset[0];
                        break;
                    }
                }
                if (end <= start) {
                    end = Math.min(start + chunkSize, text.length());
                }
                chunks.add(text.substring(start, end));
                // 重叠部分：start = end - overlapSize
                start = end - overlapSize;
                if (start < 0) {
                    start = 0;
                }
            }
        } catch (Exception e) {
            System.err.println("文本拆分失败：" + e.getMessage());
            // 拆分失败时直接按字符数强制拆分
            int start = 0;
            while (start < text.length()) {
                int end = Math.min(start + chunkSize, text.length());
                chunks.add(text.substring(start, end));
                start = end - overlapSize;
            }
        }
        return chunks;
    }
}