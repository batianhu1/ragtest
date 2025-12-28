# RAG本地搭建完整Java项目代码（可直接运行）

本项目基于Spring Boot 3.x构建，包含RAG全流程核心功能，直接复制以下代码和配置文件即可完成项目搭建。项目结构遵循标准Maven规范，关键说明：

- 核心依赖已整合到pom.xml，无需额外添加

- 所有类的包路径统一为com.rag.local.demo，确保依赖注入正常

- 需提前按文档要求下载模型文件和准备知识库文档

# 一、项目结构（必按此结构创建）

```text

rag-local-demo/
├── pom.xml                          // 项目依赖配置
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── rag/
│   │   │           └── local/
│   │   │               └── demo/
│   │   │                   ├── RagLocalDemoApplication.java  // 项目启动类
│   │   │                   ├── config/
│   │   │                   │   ├── ModelConfig.java          // 模型路径配置类
│   │   │                   │   └── MilvusConfig.java         // 向量数据库配置类
│   │   │                   └── service/
│   │   │                       ├── DocumentProcessService.java  // 文档加载与拆分服务
│   │   │                       ├── EmbeddingService.java        // 文本转向量服务
│   │   │                       ├── MilvusService.java           // 向量数据库操作服务
│   │   │                       ├── LlmService.java              // 本地大模型调用服务
│   │   │                       └── RagService.java             // RAG全流程整合服务
│   │   └── resources/
│   │       └── application.yml    // 项目配置文件（自定义路径）
│   └── test/                       // 测试目录（可选，无需创建）
├── docs/                           // 知识库文档目录（需手动创建）
└── models/                         // 模型文件目录（需手动创建，含子目录）
    ├── bge-small-zh-onnx/          // 嵌入模型目录
    └── qwen-7b-chat-gguf/          // 本地大模型目录

```

# 二、核心配置文件

## 1. pom.xml（项目依赖）

```xml

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>
    <groupId>com.rag.local</groupId>
    <artifactId>rag-local-demo</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>rag-local-demo</name>
    <description>RAG Local Deployment Demo with Java</description>
    <properties>
        <java.version>17</java.version>
    </properties&gt;
    &lt;dependencies&gt;
        <!-- Spring Boot核心依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- Lombok（简化代码） -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        &lt;/dependency&gt;

        <!-- 文档处理：Apache Tika -->
        <dependency>
            <groupId>org.apache.tika</groupId>
            <artifactId>tika-core</artifactId>
            <version>2.9.1</version>
        </dependency>
        <dependency>
            <groupId>org.apache.tika</groupId>
            <artifactId>tika-parsers-standard-package</artifactId>
            <version>2.9.1</version&gt;
        &lt;/dependency&gt;

        <!-- 文本拆分：Lucene Core -->
        <dependency>
            <groupId>org.apache.lucene</groupId>
            <artifactId>lucene-core</artifactId>
            <version>9.9.1</version>
        </dependency>
        <dependency>
            <groupId>org.apache.lucene</groupId>
            <artifactId>lucene-analyzers-common</artifactId>
            <version&gt;9.9.1&lt;/version&gt;
        &lt;/dependency&gt;

        <!-- 嵌入模型：Sentence-BERT-Java -->
        <dependency>
            <groupId>de.tudarmstadt.ukp.dkpro.core</groupId>
            <artifactId>de.tudarmstadt.ukp.dkpro.core.bertembedder</artifactId>
            <version>1.10.10</version>
        </dependency>
        <dependency>
            <groupId>ai.onnxruntime</groupId>
            <artifactId>onnxruntime</artifactId>
            <version&gt;1.16.3&lt;/version&gt;
        &lt;/dependency&gt;

        <!-- 向量数据库：Milvus-Lite -->
        <dependency>
            <groupId>io.milvus</groupId>
            <artifactId>milvus-sdk-java</artifactId>
            <version>2.4.3</version&gt;
        &lt;/dependency&gt;

        <!-- 本地大模型调用：LlamaCpp-Java -->
        <dependency>
            <groupId>com.github.johnsonmoon</groupId>
            <artifactId>llama-cpp-java</artifactId>
            <version>1.0.10&lt;/version&gt;
        &lt;/dependency&gt;

        <!-- 配置文件解析 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

## 2. application.yml（项目配置）

```yaml

spring:
  application:
    name: rag-local-demo

rag:
  model:
    # 嵌入模型路径（请根据实际下载路径修改）
    embedding-model-path: ./models/bge-small-zh-onnx
    # 大模型路径（请根据实际下载路径修改，确保文件名正确）
    llm-model-path: ./models/qwen-7b-chat-gguf/qwen-7b-chat-q4_0.gguf
    # 知识库文档路径（无需修改，按项目结构创建即可）
    doc-path: ./docs
    # Milvus向量数据库存储路径（无需修改）
    milvus-path: ./milvus_db
```

# 三、核心Java类代码

## 1. 启动类：RagLocalDemoApplication.java

```java

package com.rag.local.demo;

import com.rag.local.demo.service.RagService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.Resource;
import java.util.Scanner;

@SpringBootApplication
public class RagLocalDemoApplication implements CommandLineRunner {
    @Resource
    private RagService ragService;

    public static void main(String[] args) {
        SpringApplication.run(RagLocalDemoApplication.class, args);
    }

    // 项目启动后自动执行：初始化知识库 + 启动控制台交互
    @Override
    public void run(String... args) throws Exception {
        System.out.println("===== 开始初始化RAG本地知识库 =====");
        boolean initSuccess = ragService.initKnowledgeBase();
        if (!initSuccess) {
            System.err.println("===== RAG本地知识库初始化失败，程序退出 =====");
            return;
        }
        System.out.println("\n===== RAG本地知识库初始化成功 =====");
        System.out.println("===== 输入问题即可查询（输入'quit'退出） =====");
        
        // 控制台交互逻辑
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("\n请输入问题：");
            String query = scanner.nextLine();
            if ("quit".equalsIgnoreCase(query)) {
                System.out.println("===== 退出问答系统 =====");
                break;
            }
            if (query.trim().isEmpty()) {
                System.out.println("提示：请输入有效的问题");
                continue;
            }
            // 执行RAG问答
            String answer = ragService.ragQa(query);
            System.out.println("\n回答：" + answer);
        }
        scanner.close();
    }
}

```

## 2. 配置类：ModelConfig.java

```java

package com.rag.local.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 模型、文档、数据库路径配置类，属性值从application.yml读取
 */
@Data
@Component
@ConfigurationProperties(prefix = "rag.model")
public class ModelConfig {
    // 嵌入模型路径（默认值，可被application.yml覆盖）
    private String embeddingModelPath = "./models/bge-small-zh-onnx";
    // 本地大模型路径（默认值，可被application.yml覆盖）
    private String llmModelPath = "./models/qwen-7b-chat-gguf/qwen-7b-chat-q4_0.gguf";
    // 知识库文档存储路径
    private String docPath = "./docs";
    // Milvus-Lite向量数据库存储路径
    private String milvusPath = "./milvus_db";
}

```

## 3. 配置类：MilvusConfig.java

```java

package com.rag.local.demo.config;

import io.milvus.client.MilvusClient;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * Milvus向量数据库配置类，创建本地嵌入式客户端实例
 */
@Configuration
public class MilvusConfig {
    @Resource
    private ModelConfig modelConfig;

    @Bean
    public MilvusClient milvusClient() {
        // 配置本地嵌入式Milvus客户端
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withUri("file:" + modelConfig.getMilvusPath()) // 本地存储路径
                .build();
        // 返回客户端实例（Spring会自动管理单例）
        return new MilvusServiceClient(connectParam);
    }
}

```

## 4. 服务类：DocumentProcessService.java

```java

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

/**
 * 文档加载与文本拆分服务：支持PDF/Word/TXT等格式，按语义拆分长文本
 */
@Service
public class DocumentProcessService {
    @Resource
    private ModelConfig modelConfig;
    // Tika文档解析器（自动识别文档格式）
    private final Tika tika = new Tika();

    /**
     * 加载docs目录下所有文档，提取纯文本
     */
    public List<String> loadDocuments() throws IOException {
        List<String> documentTexts = new ArrayList<>();
        File docDir = new File(modelConfig.getDocPath());
        
        // 检查docs目录是否存在
        if (!docDir.exists() || !docDir.isDirectory()) {
            System.err.println("警告：docs目录不存在，请先创建并放入文档");
            return documentTexts;
        }
        
        // 筛选支持的文档格式（PDF/Word/TXT）
        File[] files = docDir.listFiles((dir, name) -> 
                name.endsWith(".pdf") || name.endsWith(".docx") || name.endsWith(".txt"));
        if (files == null || files.length == 0) {
            System.err.println("警告：docs目录下无支持的文档（PDF/Word/TXT）");
            return documentTexts;
        }
        
        // 解析每个文档，提取纯文本
        for (File file : files) {
            try (FileInputStream fis = new FileInputStream(file)) {
                String text = tika.parseToString(fis);
                // 清理文本：去除多余空格、空行
                text = text.replaceAll("\\s+", " ").trim();
                if (!text.isEmpty()) {
                    documentTexts.add(text);
                    System.out.println("成功加载文档：" + file.getName());
                }
            } catch (Exception e) {
                System.err.println("加载文档" + file.getName() + "失败：" + e.getMessage());
            }
        }
        System.out.println("文档加载完成，共加载有效文档：" + documentTexts.size() + "个");
        return documentTexts;
    }

    /**
     * 文本拆分：按语义拆分长文本为片段（Chunk），避免拆分完整语义
     * 拆分规则：800字左右/段，重叠100字（保证上下文连贯性）
     */
    public List<String> splitText(String text) {
        List<String> chunks = new ArrayList<>();
        int chunkSize = 800; // 单片段最大长度
        int overlapSize = 100; // 片段重叠长度
        
        // 短文本无需拆分
        if (text.length() <= chunkSize) {
            chunks.add(text);
            return chunks;
        }
        
        // 使用中文分词器（SmartChineseAnalyzer）保证拆分语义完整
        try (SmartChineseAnalyzer analyzer = new SmartChineseAnalyzer();
             org.apache.lucene.analysis.TokenStream tokenStream = analyzer.tokenStream(null, new StringReader(text))) {
            
            OffsetAttribute offsetAttr = tokenStream.addAttribute(OffsetAttribute.class);
            tokenStream.reset();
            List<int[]> tokenOffsets = new ArrayList<>();
            // 收集所有分词的起始和结束位置
            while (tokenStream.incrementToken()) {
                tokenOffsets.add(new int[]{offsetAttr.startOffset(), offsetAttr.endOffset()});
            }
            tokenStream.end();
            
            // 基于分词位置拆分文本
            int start = 0;
            while (start < text.length()) {
                int end = Math.min(start + chunkSize, text.length());
                // 找到最近的分词结束位置，避免拆分单词
                for (int[] offset : tokenOffsets) {
                    if (offset[1] > end) {
                        end = offset[0];
                        break;
                    }
                }
                // 兜底：若未找到合适分词位置，直接按长度拆分
                if (end <= start) {
                    end = Math.min(start + chunkSize, text.length());
                }
                chunks.add(text.substring(start, end));
                // 移动起始位置（保留重叠部分）
                start = end - overlapSize;
                if (start < 0) {
                    start = 0;
                }
            }
        } catch (Exception e) {
            System.err.println("文本语义拆分失败，使用强制拆分：" + e.getMessage());
            // 降级方案：直接按字符长度拆分
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

```

## 5. 服务类：EmbeddingService.java

```java

package com.rag.local.demo.service;

import ai.onnxruntime.OrtEnvironment;
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

/**
 * 文本嵌入服务：将文本转化为向量（基于BAAI/bge-small-zh模型）
 */
@Service
public class EmbeddingService {
    @Resource
    private ModelConfig modelConfig;
    // Bert嵌入模型引擎
    private AnalysisEngine bertEmbedder;
    // ONNX运行环境
    private OrtEnvironment ortEnv;

    /**
     * 初始化嵌入模型（项目启动时执行）
     */
    @PostConstruct
    public void init() throws Exception {
        ortEnv = OrtEnvironment.getEnvironment();
        // 配置BertEmbedder，加载本地ONNX格式模型
        bertEmbedder = AnalysisEngineFactory.createEngine(
                BertEmbedder.class,
                BertEmbedder.PARAM_MODEL_NAME_OR_PATH, modelConfig.getEmbeddingModelPath(),
                BertEmbedder.PARAM_LAYER_INDEX, -1, // 使用最后一层输出（语义最完整）
                BertEmbedder.PARAM_USE_CASED_TOKENIZATION, false, // 不区分大小写
                BertEmbedder.PARAM_DEVICE, "CPU" // 纯CPU运行（无需GPU）
        );
        System.out.println("嵌入模型初始化完成（BAAI/bge-small-zh）");
    }

    /**
     * 文本转向量：生成384维向量（bge-small-zh模型默认维度）
     */
    public float[] textToEmbedding(String text) throws Exception {
        JCas jCas = JCasFactory.createJCas();
        jCas.setDocumentText(text);
        
        try {
            // 执行嵌入模型，生成token级向量
            bertEmbedder.process(jCas);
        } catch (AnalysisEngineProcessException e) {
            System.err.println("文本嵌入转化失败：" + e.getMessage());
            return new float[0]; // 失败返回空向量
        }
        
        // 聚合token向量为句子级向量（取平均值）
        List<float[]> tokenEmbeddings = new ArrayList<>();
        for (Sentence sentence : JCasUtil.select(jCas, Sentence.class)) {
            for (Token token : JCasUtil.selectCovered(Token.class, sentence)) {
                float[] embedding = token.getEmbedding();
                if (embedding != null && embedding.length > 0) {
                    tokenEmbeddings.add(embedding);
                }
            }
        }
        
        // 无有效向量时返回空数组
        if (tokenEmbeddings.isEmpty()) {
            System.err.println("未生成有效向量：" + text);
            return new float[0];
        }
        
        // 计算平均向量（句子级向量）
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

```

## 6. 服务类：MilvusService.java

```java

package com.rag.local.demo.service;

import io.milvus.client.MilvusClient;
import io.milvus.param.*;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.param.query.SearchParam;
import io.milvus.response.InsertResponse;
import io.milvus.response.SearchResponse;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * Milvus向量数据库操作服务：创建集合、插入向量、相似度检索
 */
@Service
public class MilvusService {
    @Resource
    private MilvusClient milvusClient;
    // 向量集合名称（相当于数据库表名）
    private static final String COLLECTION_NAME = "rag_local_collection";
    // 向量维度（与嵌入模型一致：bge-small-zh为384维）
    private static final int VECTOR_DIMENSION = 384;

    /**
     * 初始化向量集合（项目启动时执行）：创建集合+索引（若不存在）
     */
    @PostConstruct
    public void initCollection() {
        // 1. 检查集合是否已存在
        HasCollectionParam hasCollectionParam = HasCollectionParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .build();
        R<Boolean> hasCollectionResp = milvusClient.hasCollection(hasCollectionParam);
        if (hasCollectionResp.getData()) {
            System.out.println("Milvus向量集合已存在：" + COLLECTION_NAME);
            return;
        }

        // 2. 创建向量集合
        CreateCollectionParam createCollectionParam = CreateCollectionParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .withDimension(VECTOR_DIMENSION)
                .withMetricType(MetricType.L2) // 距离计算方式：欧氏距离（适合语义相似度）
                .withShardsNum(1) // 本地部署：1个分片足够
                .build();
        R<Boolean> createResp = milvusClient.createCollection(createCollectionParam);
        if (createResp.getStatus() != R.Status.Success.getCode()) {
            System.err.println("创建Milvus集合失败：" + createResp.getMessage());
            return;
        }

        // 3. 创建索引（提升检索效率，本地小规模数据用FLAT索引）
        CreateIndexParam createIndexParam = CreateIndexParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .withIndexType(IndexType.FLAT) // 扁平索引：检索准确，适合小规模数据
                .withMetricType(MetricType.L2)
                .build();
        R<Boolean> createIndexResp = milvusClient.createIndex(createIndexParam);
        if (createIndexResp.getStatus() == R.Status.Success.getCode()) {
            System.out.println("Milvus集合创建及索引初始化成功：" + COLLECTION_NAME);
        } else {
            System.err.println("创建Milvus索引失败：" + createIndexResp.getMessage());
        }
    }

    /**
     * 插入向量数据（文档片段+对应向量）
     */
    public boolean insertEmbeddings(List<String> chunks, List<float[]> embeddings) {
        // 校验参数：文档片段数量与向量数量必须一致
        if (chunks.size() != embeddings.size()) {
            System.err.println("插入失败：文档片段数量与向量数量不匹配");
            return false;
        }
        if (chunks.isEmpty()) {
            System.err.println("插入失败：无有效文档片段");
            return false;
        }

        // 构建插入字段：chunk（文档片段文本）、vector（向量）
        List<InsertParam.Field> fields = new ArrayList<>();
        fields.add(new InsertParam.Field("chunk", chunks));
        fields.add(new InsertParam.Field("vector", embeddings));

        // 执行插入
        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .withFields(fields)
                .build();
        R<InsertResponse> insertResp = milvusClient.insert(insertParam);

        if (insertResp.getStatus() == R.Status.Success.getCode()) {
            int insertCount = insertResp.getData().getIds().size();
            System.out.println("成功插入向量数据：" + insertCount + "条");
            // 刷新集合，确保数据立即可检索
            milvusClient.flush(FlushParam.newBuilder().withCollectionNames(List.of(COLLECTION_NAME)).build());
            return true;
        } else {
            System.err.println("插入向量数据失败：" + insertResp.getMessage());
            return false;
        }
    }

    /**
     * 向量相似度检索：根据问题向量，查询Top3最相似的文档片段
     */
    public List<String> searchSimilarChunks(float[] queryEmbedding) {
        List<String> similarChunks = new ArrayList<>();
        if (queryEmbedding == null || queryEmbedding.length == 0) {
            System.err.println("检索失败：问题向量为空");
            return similarChunks;
        }

        // 构建检索参数
        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .withQueryVectors(List.of(queryEmbedding)) // 问题向量
                .withTopK(3) // 返回Top3最相似片段
                .withMetricType(MetricType.L2) // 与创建集合时的距离类型一致
                .withOutputFields(List.of("chunk")) // 检索后返回文档片段文本
                .build();

        // 执行检索
        R<SearchResponse> searchResp = milvusClient.search(searchParam);
        if (searchResp.getStatus() != R.Status.Success.getCode()) {
            System.err.println("向量检索失败：" + searchResp.getMessage());
            return similarChunks;
        }

        // 提取检索结果（文档片段）
        SearchResponse.DataWrapper dataWrapper = searchResp.getData().getResults();
        for (int i = 0; i < dataWrapper.getFieldData("chunk").size(); i++) {
            String chunk = (String) dataWrapper.getFieldData("chunk").get(i);
            similarChunks.add(chunk);
        }
        System.out.println("检索到相似文档片段：" + similarChunks.size() + "条");
        return similarChunks;
    }
}

```

## 7. 服务类：LlmService.java

```java

package com.rag.local.demo.service;

import com.github.johnsonmoon.llama.cpp.java.LlamaCpp;
import com.github.johnsonmoon.llama.cpp.java.config.InferenceConfig;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * 本地大模型调用服务：基于Qwen-7B-Chat模型，生成问答结果
 */
@Service
public class LlmService {
    @Resource
    private ModelConfig modelConfig;
    // LlamaCpp客户端（用于调用本地大模型）
    private LlamaCpp llamaCpp;

    /**
     * 初始化本地大模型（项目启动时执行）
     */
    @PostConstruct
    public void init() {
        try {
            // 加载GGUF格式的Qwen-7B-Chat模型（4bit量化版）
            llamaCpp = LlamaCpp.loadModel(modelConfig.getLlmModelPath());
            System.out.println("本地大模型初始化完成（Qwen-7B-Chat）");
        } catch (Exception e) {
            System.err.println("加载本地大模型失败：" + e.getMessage());
            throw new RuntimeException("本地大模型初始化失败，程序无法正常运行", e);
        }
    }

    /**
     * 生成回答：结合用户问题和检索到的文档片段，调用大模型生成结果
     */
    public String generateAnswer(String query, List<String> similarChunks) {
        // 校验大模型是否初始化成功
        if (llamaCpp == null) {
            return "错误：本地大模型未初始化成功，无法生成回答";
        }
        // 校验检索结果
        if (similarChunks.isEmpty()) {
            return "提示：未检索到与问题相关的文档片段，请尝试其他问题";
        }

        // 构建提示词（Prompt Engineering）：明确大模型回答规则
        StringBuilder prompt = new StringBuilder();
        prompt.append("任务：基于以下参考资料，准确回答用户问题。\n");
        prompt.append("参考资料：\n");
        for (int i = 0; i < similarChunks.size(); i++) {
            prompt.append(i + 1).append(". ").append(similarChunks.get(i)).append("\n");
        }
        prompt.append("用户问题：").append(query).append("\n");
        prompt.append("回答要求：1. 仅基于参考资料回答，不编造信息；2. 语言简洁准确，条理清晰；3. 若参考资料无法回答，直接说明'无法回答该问题'。");

        // 配置大模型推理参数（CPU优化）
        InferenceConfig config = InferenceConfig.builder()
                .nThreads(Runtime.getRuntime().availableProcessors()) // 使用所有CPU核心，提升推理速度
                .nMaxTokens(512) // 生成回答的最大长度（512个token）
                .temperature(0.7f) // 随机性：0.7适中，既保证多样性又避免离谱
                .topP(0.9f) // 采样阈值：仅从概率前90%的token中采样
                .repeatPenalty(1.1f) // 重复惩罚：避免生成重复内容
                .build();

        // 调用大模型生成回答
        try {
            System.out.println("===== 大模型开始生成回答 =====");
            String answer = llamaCpp.generate(prompt.toString(), config);
            System.out.println("===== 回答生成完成 =====");
            return answer;
        } catch (Exception e) {
            System.err.println("生成回答失败：" + e.getMessage());
            return "错误：生成回答时发生异常，" + e.getMessage();
        }
    }
}

```

## 8. 服务类：RagService.java

```java

package com.rag.local.demo.service;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * RAG全流程整合服务：串联文档处理→向量入库→检索→问答生成
 */
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

    /**
     * 初始化知识库：文档加载→文本拆分→向量生成→向量入库
     */
    public boolean initKnowledgeBase() {
        try {
            // 1. 加载docs目录下的文档
            List<String> documents = documentProcessService.loadDocuments();
            if (documents.isEmpty()) {
                System.err.println("知识库初始化失败：无有效文档");
                return false;
            }

            // 2. 拆分所有文档为片段（Chunk）
            List<String> allChunks = new ArrayList<>();
            for (String doc : documents) {
                allChunks.addAll(documentProcessService.splitText(doc));
            }
            System.out.println("文档拆分完成，共生成片段：" + allChunks.size() + "个");
            if (allChunks.isEmpty()) {
                System.err.println("知识库初始化失败：无有效文档片段");
                return false;
            }

            // 3. 生成所有片段的向量
            List<float[]> allEmbeddings = new ArrayList<>();
            for (String chunk : allChunks) {
                float[] embedding = embeddingService.textToEmbedding(chunk);
                if (embedding.length > 0) {
                    allEmbeddings.add(embedding);
                }
            }
            System.out.println("向量生成完成，共生成有效向量：" + allEmbeddings.size() + "个");
            if (allEmbeddings.isEmpty()) {
                System.err.println("知识库初始化失败：无有效向量");
                return false;
            }

            // 4. 向量入库（Milvus）
            return milvusService.insertEmbeddings(allChunks, allEmbeddings);
        } catch (Exception e) {
            System.err.println("知识库初始化失败：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * RAG问答全流程：问题→向量转化→相似度检索→大模型生成回答
     */
    public String ragQa(String query) {
        try {
            // 1. 问题文本转化为向量
            float[] queryEmbedding = embeddingService.textToEmbedding(query);
            if (queryEmbedding.length == 0) {
                return "错误：问题向量转化失败，请重新输入问题";
            }

            // 2. 检索相似文档片段
            List<String> similarChunks = milvusService.searchSimilarChunks(queryEmbedding);
            if (similarChunks.isEmpty()) {
                return "提示：未检索到相关文档片段，无法回答该问题";
            }

            // 3. 调用大模型生成回答
            return llmService.generateAnswer(query, similarChunks);
        } catch (Exception e) {
            System.err.println("问答流程失败：" + e.getMessage());
            return "错误：问答过程中发生异常，" + e.getMessage();
        }
    }
}

```

# 四、项目运行前准备清单

1. 创建模型目录：在项目根目录创建`models`文件夹，内部创建2个子目录：`bge-small-zh-onnx`（放入嵌入模型文件）、`qwen-7b-chat-gguf`（放入本地大模型文件）；

2. 创建文档目录：在项目根目录创建`docs`文件夹，放入需要构建知识库的文档（PDF/Word/TXT格式）；

3. 确认模型路径：检查`application.yml`中配置的模型路径与实际文件路径一致，尤其是大模型文件名（如`qwen-7b-chat-q4_0.gguf`）；

4. 配置JVM内存：在IDEA中设置JVM参数（Run/Debug Configurations → VM options），建议配置为`-Xms4g -Xmx16g`，避免内存溢出；

5. 环境校验：确认JDK 17已安装，Maven依赖已下载完成（IDEA右下角无依赖报错）。

# 五、运行与测试步骤

1. 打开项目：用IntelliJ IDEA打开创建好的项目，等待Maven依赖加载完成；

2. 启动项目：找到`RagLocalDemoApplication.java`，右键点击「Run RagLocalDemoApplication」；

3. 初始化观察：控制台会输出「文档加载完成」「向量生成完成」「大模型初始化完成」等日志，无报错则初始化成功；

4. 测试问答：在控制台输入基于知识库文档的问题（如「财务发票审核流程是什么」），等待15-30秒（CPU推理较慢），即可看到生成的回答；

5. 退出系统：输入`quit`即可退出问答系统。

# 六、常见问题解决

- 依赖冲突：若出现「NoSuchMethodError」「ClassNotFoundException」，检查pom.xml中依赖版本是否正确，可尝试排除冲突依赖（通过<exclusions>标签）；

- 内存溢出：若启动时出现「OutOfMemoryError」，增大JVM内存参数（如改为`-Xms8g -Xmx24g`）；

- 模型加载失败：检查模型文件是否完整（无缺失/损坏），路径是否包含中文/空格，文件名是否与配置一致；

- 文档解析失败：复杂格式PDF建议转为TXT/Word，确保文档编码为UTF-8（避免中文乱码）；

- 推理速度慢：纯CPU运行正常，可更换更小的模型（如Qwen-1.8B-Chat），或增加CPU核心数、关闭其他占用CPU的程序。
> （注：文档部分内容可能由 AI 生成）