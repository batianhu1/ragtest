# RAG本地搭建全流程（Java框架方案）

本文档基于「Java生态轻量化开源组件」整理RAG本地搭建流程，核心技术栈选用Spring Boot（开发框架）+ Tika（文档处理）+ Sentence-BERT-Java（嵌入模型）+ Milvus-Lite（向量数据库）+ LlamaCpp-Java（本地大模型调用），适配Java开发者/企业级Java技术栈团队，无需GPU，纯CPU即可运行，数据全程本地存储，保障隐私安全。

# 一、前期准备

## 1. 硬件配置要求（最低/推荐）

|硬件类型|最低配置|推荐配置|说明|
|---|---|---|---|
|CPU|四核|六核及以上|Java程序运行、文档解析、模型推理均依赖CPU，多核可显著提升并发处理效率|
|内存|16GB|32GB及以上|核心瓶颈，Java虚拟机（JVM）占用部分内存，16GB可支撑轻量模型运行，32GB可保障流畅性|
|硬盘|20GB空闲（SSD）|100GB空闲（SSD）|需存储项目依赖、JDK、模型文件、文档及向量数据，SSD可大幅提升文件读写和检索速度|
## 2. 软件环境准备

支持Windows、MacOS、Linux（推荐Ubuntu/CentOS），核心依赖如下：

- 操作系统：Windows 10及以上 / MacOS 12及以上 / Linux内核5.0及以上

- JDK：11及以上（推荐JDK 17，兼容性更好，支持更多新特性）

- 构建工具：Maven 3.6+ 或 Gradle 7.0+（推荐Maven，生态更成熟）

- 开发工具：IntelliJ IDEA（推荐）、Eclipse（需安装Spring Boot插件）

- 辅助工具：Git（克隆开源项目/组件）、Postman（接口测试，可选）

## 3. 核心组件清单（Java生态轻量化选型）

|组件类别|选用工具|核心作用|优势|
|---|---|---|---|
|开发框架|Spring Boot 3.x|快速搭建项目骨架，提供依赖管理、自动配置、Web服务等基础能力|Java生态主流框架，开发效率高，社区活跃，问题解决方案丰富|
|文档处理|Apache Tika + Tika-Parrsers|解析PDF、Word、TXT等多种格式文档，提取纯文本内容|开源免费，支持格式广泛，集成简单，可通过扩展插件支持更多格式|
|文本拆分|Lucene Core（Tokenizer组件）|将长文本按语义、字符数拆分为小片段（Chunk）|轻量级，拆分策略灵活，支持中文分词扩展，与Java生态兼容性好|
|中文嵌入模型|Sentence-BERT-Java + BAAI/bge-small-zh（ONNX格式）|将文本转化为向量，支撑语义检索|Sentence-BERT-Java是Java原生的嵌入模型调用库，BAAI/bge-small-zh中文效果好，ONNX格式适配CPU高效运行|
|向量数据库|Milvus-Lite|存储向量数据，支持相似度检索|本地嵌入式部署，无需单独启动服务，支持Java SDK，检索性能优异，支持TB级数据|
|本地大模型调用|LlamaCpp-Java + Qwen-7B-Chat（GGUF格式，4bit量化）|基于检索结果生成回答|LlamaCpp-Java是Java原生的Llama系列模型调用库，支持CPU推理，Qwen-7B-Chat中文问答效果好，量化版内存占用低|
# 二、分步实施流程

## 第一步：搭建Spring Boot项目骨架

### 方式1：通过Spring Initializr快速生成（推荐）

1. 访问Spring Initializr官网：https://start.spring.io/

2. 配置项目信息：
        

    - Project：Maven Project

    - Language：Java

    - Spring Boot：3.2.x（稳定版）

    - Group/Artifact：自定义（如Group：com.rag.local，Artifact：rag-local-demo）

    - Package name：com.rag.local.demo

    - Java Version：17

3. 添加核心依赖：
        

    - Spring Web（用于搭建简单Web交互界面，可选）

    - Lombok（简化实体类代码，可选）

    - Spring Boot DevTools（开发热部署，可选）

4. 点击「Generate」下载项目压缩包，解压后用IntelliJ IDEA打开。

### 方式2：手动创建Maven项目并配置依赖

创建普通Maven项目后，在pom.xml中添加Spring Boot核心依赖（参考Spring官方文档最新版本）。

### 关键配置：pom.xml核心依赖补充

在生成的pom.xml中添加RAG所需的组件依赖，完整依赖如下（版本可根据实际情况调整）：

```xml

&lt;dependencies&gt;
    <!-- Spring Boot核心依赖 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope&gt;test&lt;/scope&gt;
    &lt;/dependency&gt;
    
    <!-- Lombok（简化代码） -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    
<!-- 文档处理：Apache Tika -->
    <dependency>
        <groupId>org.apache.tika</groupId>
        <artifactId>tika-core</artifactId>
        <version>2.9.1</version>
    </dependency>
    <dependency>
        <groupId>org.apache.tika</groupId>
        <artifactId>tika-parsers-standard-package</artifactId>
        <version>2.9.1</version>
    </dependency&gt;
    
    <!-- 文本拆分：Lucene Core -->
    <dependency>
        <groupId>org.apache.lucene</groupId>
        <artifactId>lucene-core</artifactId>
        <version>9.9.1</version>
    </dependency>
    <dependency>
        <groupId>org.apache.lucene</groupId>
        <artifactId>lucene-analyzers-common</artifactId>
        <version>9.9.1</version>
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
        <version>1.16.3&lt;/version&gt;
    &lt;/dependency&gt;
    
    <!-- 向量数据库：Milvus-Lite -->
    <dependency>
        <groupId>io.milvus</groupId>
        <artifactId>milvus-sdk-java</artifactId>
        <version>2.4.3</version>
    </dependency>
    
    <!-- 本地大模型调用：LlamaCpp-Java -->
    <dependency>
        <groupId>com.github.johnsonmoon</groupId>
        <artifactId>llama-cpp-java</artifactId>
        <version>1.0.10&lt;/version&gt;
    &lt;/dependency&gt;
    
    <!-- 配置文件解析：用于读取模型路径等配置 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

## 第二步：下载核心模型文件（嵌入模型+大模型）

### 1. 嵌入模型：BAAI/bge-small-zh（ONNX格式）

1. 访问Hugging Face官网下载ONNX格式模型：https://huggingface.co/BAAI/bge-small-zh-onnx

2. 点击「Files and versions」，下载所有文件至本地文件夹（如「./models/bge-small-zh-onnx」）

3. 核心文件说明：model.onnx（模型权重文件）、config.json（模型配置文件）、tokenizer.json（分词器配置）

### 2. 本地大模型：Qwen-7B-Chat（GGUF格式，4bit量化）

1. 访问Hugging Face官网下载GGUF量化版模型：https://huggingface.co/Qwen/Qwen-7B-Chat-GGUF

2. 选择4bit量化版本（如「qwen-7b-chat-q4_0.gguf」），下载至本地文件夹（如「./models/qwen-7b-chat-gguf」）

3. 说明：GGUF格式是LlamaCpp支持的标准格式，4bit量化版内存占用低（约5GB），适合CPU运行

## 第三步：准备本地文档（知识库原始素材）

1. 在项目根目录创建「docs」文件夹，用于存放需要导入知识库的原始文档（PDF、Word、TXT等）；

2. 将需要构建知识库的文档（如RPA流程手册、业务SOP等）复制到「docs」文件夹中；

3. 注意事项：文档编码统一为UTF-8，避免中文乱码；复杂格式（如带大量图片、表格的PDF）可先转为TXT/Word简化处理。

## 第四步：编写核心功能代码

按「文档处理→文本拆分→嵌入转化→向量入库→大模型调用→问答交互」的流程编写代码，核心包结构建议：

```text

com.rag.local.demo
├── config          // 配置类（模型配置、Milvus配置等）
├── service         // 核心服务类（文档处理、嵌入转化、检索、问答等）
├── util            // 工具类（文件读取、路径处理等）
├── model           // 实体类（文档片段、问答请求/响应等）
└── RagLocalDemoApplication.java  // 启动类
```

### 1. 配置类：ModelConfig（模型路径配置）

```java

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

```

### 2. 配置类：MilvusConfig（向量数据库配置）

```java

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

```

### 3. 服务类：DocumentProcessService（文档加载与拆分）

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

@Service
public class DocumentProcessService {
    @Resource
    private ModelConfig modelConfig;
    private final Tika tika = new Tika();

    // 文档加载：读取docs文件夹下所有文档，提取纯文本
    public List<String> loadDocuments() throws IOException {
        List<String&gt; documentTexts = new ArrayList<>();
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
        List&lt;String&gt; chunks = new ArrayList<>();
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
            List<int[]&gt; tokenOffsets = new ArrayList<>();
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

```

### 4. 服务类：EmbeddingService（嵌入模型调用，文本转向量）

```java

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
        List&lt;float[]&gt; tokenEmbeddings = new ArrayList<>();
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

```

### 5. 服务类：MilvusService（向量数据库操作：创建集合、插入数据、检索）

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

@Service
public class MilvusService {
    @Resource
    private MilvusClient milvusClient;
    // 集合名称（相当于数据库表名）
    private static final String COLLECTION_NAME = "rag_local_collection";
    // 向量维度（bge-small-zh为384维）
    private static final int DIMENSION = 384;

    // 初始化：创建集合（若不存在）并创建索引
    @PostConstruct
    public void initCollection() {
        // 检查集合是否存在
        HasCollectionParam hasCollectionParam = HasCollectionParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .build();
        R<Boolean> hasCollectionResp = milvusClient.hasCollection(hasCollectionParam);
        if (hasCollectionResp.getData()) {
            System.out.println("Milvus集合" + COLLECTION_NAME + "已存在");
            return;
        }
        // 创建集合
        CreateCollectionParam createCollectionParam = CreateCollectionParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .withDimension(DIMENSION)
                .withMetricType(MetricType.L2) // 距离计算方式：L2（欧氏距离）
                .withShardsNum(1) // 分片数（本地部署1个即可）
                .build();
        R<Boolean> createResp = milvusClient.createCollection(createCollectionParam);
        if (createResp.getStatus() != R.Status.Success.getCode()) {
            System.err.println("创建Milvus集合失败：" + createResp.getMessage());
            return;
        }
        // 创建索引（提升检索效率）
        CreateIndexParam createIndexParam = CreateIndexParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .withIndexType(IndexType.FLAT) // 扁平索引，适合小规模数据（本地部署）
                .withMetricType(MetricType.L2)
                .build();
        R<Boolean> createIndexResp = milvusClient.createIndex(createIndexParam);
        if (createIndexResp.getStatus() == R.Status.Success.getCode()) {
            System.out.println("Milvus集合" + COLLECTION_NAME + "创建并初始化索引成功");
        } else {
            System.err.println("创建Milvus索引失败：" + createIndexResp.getMessage());
        }
    }

    // 插入向量数据（文档片段+向量）
    public boolean insertEmbeddings(List<String> chunks, List<float[]> embeddings) {
        if (chunks.size() != embeddings.size()) {
            System.err.println("文档片段数量与向量数量不匹配");
            return false;
        }
        List&lt;InsertParam.Field&gt; fields = new ArrayList<>();
        // 文档片段字段
        List<String> chunkList = new ArrayList<>(chunks);
        fields.add(new InsertParam.Field("chunk", chunkList));
        // 向量字段
        fields.add(new InsertParam.Field("vector", embeddings));
        // 构建插入参数
        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .withFields(fields)
                .build();
        // 执行插入
        R<InsertResponse> insertResp = milvusClient.insert(insertParam);
        if (insertResp.getStatus() == R.Status.Success.getCode()) {
            System.out.println("成功插入" + insertResp.getData().getIds().size() + "条向量数据");
            // 刷新集合，确保数据可检索
            milvusClient.flush(FlushParam.newBuilder().withCollectionNames(List.of(COLLECTION_NAME)).build());
            return true;
        } else {
            System.err.println("插入向量数据失败：" + insertResp.getMessage());
            return false;
        }
    }

    // 向量检索：根据问题向量，检索Top3最相似的文档片段
    public List<String> searchSimilarChunks(float[] queryEmbedding) {
        List&lt;String&gt; similarChunks = new ArrayList<>();
        // 构建检索参数
        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(COLLECTION_NAME)
                .withQueryVectors(List.of(queryEmbedding))
                .withTopK(3) // 返回3个最相似的片段
                .withMetricType(MetricType.L2)
                .withOutputFields(List.of("chunk")) // 返回文档片段内容
                .build();
        // 执行检索
        R<SearchResponse> searchResp = milvusClient.search(searchParam);
        if (searchResp.getStatus() != R.Status.Success.getCode()) {
            System.err.println("向量检索失败：" + searchResp.getMessage());
            return similarChunks;
        }
        // 提取检索结果
        SearchResponse.DataWrapper dataWrapper = searchResp.getData().getResults();
        for (int i = 0; i < dataWrapper.getFieldData("chunk").size(); i++) {
            String chunk = (String) dataWrapper.getFieldData("chunk").get(i);
            similarChunks.add(chunk);
        }
        return similarChunks;
    }
}

```

### 6. 服务类：LlmService（本地大模型调用，生成回答）

```java

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

```

### 7. 核心服务类：RagService（整合全流程，提供问答功能）

```java

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
            List&lt;String&gt; allChunks = new ArrayList<>();
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

```

### 8. 启动类：RagLocalDemoApplication（整合启动，提供交互入口）

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

    // 项目启动后执行：初始化知识库并启动交互问答
    @Override
    public void run(String... args) throws Exception {
        System.out.println("开始初始化RAG本地知识库...");
        boolean initSuccess = ragService.initKnowledgeBase();
        if (!initSuccess) {
            System.err.println("RAG本地知识库初始化失败，程序退出");
            return;
        }
        System.out.println("\nRAG本地知识库初始化成功，输入问题即可查询（输入'quit'退出）：");
        // 控制台交互
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("\n请输入问题：");
            String query = scanner.nextLine();
            if ("quit".equalsIgnoreCase(query)) {
                System.out.println("退出问答");
                break;
            }
            if (query.trim().isEmpty()) {
                System.out.println("请输入有效的问题");
                continue;
            }
            // 执行问答
            String answer = ragService.ragQa(query);
            System.out.println("\n回答：" + answer);
        }
        scanner.close();
    }
}

```

## 第五步：配置application.yml（可选，自定义模型路径等）

在src/main/resources目录下创建application.yml文件，可自定义模型路径、文档路径等配置（覆盖ModelConfig的默认值）：

```yaml

spring:
  application:
    name: rag-local-demo

rag:
  model:
    # 嵌入模型路径（根据实际下载路径修改）
    embedding-model-path: ./models/bge-small-zh-onnx
    # 大模型路径（根据实际下载路径修改）
    llm-model-path: ./models/qwen-7b-chat-gguf/qwen-7b-chat-q4_0.gguf
    # 文档路径
    doc-path: ./docs
    # Milvus存储路径
    milvus-path: ./milvus_db
```

## 第六步：运行项目，启动RAG本地知识库

1. 确保「docs」文件夹中已放入文档，模型文件已下载至指定路径；

2. 在IntelliJ IDEA中，找到RagLocalDemoApplication类，右键点击「Run RagLocalDemoApplication」；

3. 首次运行说明：
       

    - 项目启动后会自动初始化知识库（文档加载→拆分→嵌入→向量入库）；

    - 初始化完成后，控制台会提示输入问题，输入问题即可进行问答交互；

    - 纯CPU推理速度较慢（约15-30秒/次回答），耐心等待即可。

4. 测试问答：输入问题（如「RPA财务发票流程如何设计」），即可得到基于本地文档的回答。

# 三、验证与优化

## 1. 验证要点

- 知识库初始化验证：控制台输出「成功加载文档数量：X」「文档拆分后片段数量：Y」「成功插入Z条向量数据」，无报错即正常；

- 向量数据库验证：项目根目录生成「milvus_db」文件夹，代表Milvus-Lite数据已持久化；

- 问答效果验证：提问需基于本地文档内容，查看回答是否准确，是否符合参考文档逻辑。

## 2. 常见优化方向

- 调整文本拆分参数：若回答不精准，可修改chunkSize（如改为600/1000）和overlapSize（如改为80/120）；

- 优化检索数量：修改MilvusService中searchSimilarChunks方法的withTopK参数（如改为2/4）；

- 提升推理速度：更换更小的模型（如Qwen-1.8B-Chat-GGUF），或增加CPU核心数；

- 添加缓存机制：对频繁查询的问题向量和检索结果进行缓存，减少重复计算。

# 四、注意事项

- JVM内存配置：若运行时出现内存溢出（OOM），可在IDEA中调整JVM参数（Run/Debug Configurations → VM options），增加堆内存（如-Xms4g -Xmx16g）；

- 依赖冲突解决：若出现依赖冲突，可在pom.xml中排除冲突的依赖（如通过<exclusions>标签）；

- 模型路径问题：确保application.yml中配置的模型路径与实际下载路径一致，路径分隔符使用「/」或「\\」；

- 数据安全：所有文档、模型、向量数据均存储在本地，无数据上传风险，可放心用于私有文档。

# 五、后续扩展（可选）

- 搭建Web界面：使用Spring Boot + Thymeleaf/Vue搭建简单Web界面，实现浏览器端问答；

- 支持更多文档格式：扩展Tika的解析插件，支持PPT、Excel等格式；

- 多轮对话功能：添加对话上下文管理，支持连续追问（如「这个流程的报错怎么解决」）；

- 日志与监控：集成Logback日志框架，添加运行状态监控，方便问题排查。
> （注：文档部分内容可能由 AI 生成）