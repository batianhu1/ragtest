# RAG本地知识库系统

基于Java生态的RAG（检索增强生成）系统，使用Spring Boot + Tika + Sentence-BERT + Milvus-Lite + LlamaCpp实现。

## 技术栈

- **开发框架**: Spring Boot 3.x
- **文档处理**: Apache Tika + Tika-Parsers
- **文本拆分**: Lucene Core（Tokenizer组件）
- **中文嵌入模型**: Sentence-BERT + BAAI/bge-small-zh（ONNX格式）
- **向量数据库**: Milvus-Lite
- **本地大模型调用**: LlamaCpp-Java + Qwen-7B-Chat（GGUF格式，4bit量化）

## 硬件要求

- **CPU**: 六核及以上
- **内存**: 32GB及以上（推荐）
- **硬盘**: 100GB空闲（SSD）

## 快速开始

1. **准备模型文件**:
   - 下载嵌入模型 BAAI/bge-small-zh-onnx 至 `./models/bge-small-zh-onnx/`
   - 下载大模型 Qwen-7B-Chat-GGUF 至 `./models/qwen-7b-chat-gguf/`

2. **准备文档**:
   - 将需要构建知识库的文档（PDF、Word、TXT等）放入 `./docs/` 文件夹

3. **启动项目**:
   ```bash
   cd rag-local-demo
   mvn spring-boot:run
   ```

4. **使用API**:
   - 初始化知识库: `POST /api/rag/init`
   - 问答接口: `POST /api/rag/query`

5. **控制台交互**:
   - 项目启动后会自动初始化知识库，然后进入问答交互模式
   - 输入问题即可查询，输入`quit`退出