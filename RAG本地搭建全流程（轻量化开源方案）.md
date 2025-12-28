# RAG本地搭建全流程（轻量化开源方案）

本文档基于「轻量化开源组件」整理RAG本地搭建流程，核心组件选用LangChain（文档处理）+ BAAI/bge-small-zh（嵌入模型）+ Chroma（向量数据库）+ Qwen-7B（本地大模型），适配个人/中小团队，无需GPU，纯CPU即可运行，数据全程本地存储，保障隐私安全。

# 一、前期准备

## 1. 硬件配置要求（最低/推荐）

|硬件类型|最低配置|推荐配置|说明|
|---|---|---|---|
|CPU|双核|四核及以上|文档拆分、向量生成、模型推理均依赖CPU，多核可提升效率|
|内存|8GB|16GB及以上|核心瓶颈，16GB可流畅运行量化版大模型，避免程序崩溃|
|硬盘|10GB空闲（SSD）|50GB空闲（SSD）|需存储组件依赖、模型文件、文档及向量数据，SSD提升读写速度|
## 2. 软件环境准备

支持Windows、MacOS、Linux（推荐Ubuntu/CentOS），核心依赖如下：

- 操作系统：Windows 10及以上 / MacOS 12及以上 / Linux内核5.0及以上

- Python环境：3.8 ~ 3.11（推荐3.9，避免版本兼容问题）

- 辅助工具：Git（克隆开源项目/组件）、Anaconda（可选，管理虚拟环境，避免依赖冲突）

## 3. 核心组件清单（轻量化开源选型）

|组件类别|选用工具|核心作用|优势|
|---|---|---|---|
|文档处理框架|LangChain|文档加载、拆分、对接嵌入模型与向量数据库|生态成熟、教程丰富，支持多种文档格式|
|中文嵌入模型|BAAI/bge-small-zh（量化版）|将文本转化为向量|中文语义理解精准、轻量高效、纯CPU可运行、开源免费|
|向量数据库|Chroma|存储向量数据，支持相似度检索|本地嵌入式部署，无需单独启动服务，开箱即用|
|本地大模型|Qwen-7B-Chat（4bit量化版）|基于检索结果生成回答|中文问答效果好、量化后内存占用低（4GB左右）、开源免费|
# 二、分步实施流程

## 第一步：搭建Python虚拟环境（推荐，避免依赖冲突）

若未安装Anaconda，可直接用Python自带的venv；已安装Anaconda则按以下步骤操作：

```bash

# 1. 打开终端/命令提示符，创建虚拟环境（命名为rag-local，Python版本3.9）
conda create -n rag-local python=3.9

# 2. 激活虚拟环境（Windows）
conda activate rag-local

# 2. 激活虚拟环境（MacOS/Linux）
source activate rag-local
```

验证：激活后终端前缀显示「(rag-local)」，代表环境生效。

## 第二步：安装核心依赖库

在激活的虚拟环境中，执行以下命令批量安装依赖（复制粘贴即可）：

```bash

# 1. 安装LangChain核心及社区库（文档处理核心）
pip install langchain langchain-community

# 2. 安装文档解析依赖（支持PDF/Word/TXT等格式）
pip install pypdf python-docx python-dotenv

# 3. 安装嵌入模型依赖（sentence-transformers用于加载bge-small-zh）
pip install sentence-transformers

# 4. 安装轻量化向量数据库Chroma
pip install chromadb

# 5. 安装本地大模型运行依赖（transformers、torch、accelerate用于加载Qwen）
pip install transformers torch accelerate sentencepiece

# 6. 安装可选：可视化工具（用于查看向量数据库内容，可选）
pip install chroma-ui
```

验证：安装完成后无报错，可通过「pip list」查看上述包是否已成功安装。

## 第三步：下载本地大模型（Qwen-7B-Chat-4bit）

通过Hugging Face下载量化版模型（无需手动下载，代码运行时自动缓存，也可提前手动下载）：

### 方式1：代码运行时自动下载（推荐，简单便捷）

后续编写模型加载代码时，指定模型名称为「Qwen/Qwen-7B-Chat-4bit」，首次运行会自动下载模型文件（约5GB），缓存至本地用户目录（Windows：C:\Users\用户名\.cache\huggingface\hub；MacOS/Linux：~/.cache/huggingface/hub）。

### 方式2：手动下载（适合网络不稳定场景）

1. 访问Hugging Face官网：https://huggingface.co/Qwen/Qwen-7B-Chat-4bit

2. 点击「Files and versions」，下载所有文件至本地文件夹（如「./qwen-7b-chat-4bit」）

3. 后续代码中指定模型本地路径即可（避免重复下载）

## 第四步：准备本地文档（知识库原始素材）

1. 新建文件夹结构：在本地创建一个项目根目录（如「RAG-Local-Project」），并在其中新建「docs」文件夹，用于存放需要导入知识库的原始文档（PDF、Word、TXT等）；

2. 放入文档：将需要构建知识库的文档（如RPA流程手册、业务SOP等）复制到「docs」文件夹中；

3. 注意事项：文档编码统一为UTF-8，避免中文乱码；优先选择纯文本类文档，复杂格式（如带大量图片、表格的PDF）可先转为TXT/Word简化处理。

## 第五步：编写核心代码（实现文档入库+问答功能）

在项目根目录新建「rag_local_demo.py」文件，复制以下代码（关键步骤已标注注释，可直接运行）：

```python

from langchain.document_loaders import DirectoryLoader, PyPDFLoader, Docx2txtLoader, TextLoader
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain.embeddings import HuggingFaceEmbeddings
from langchain.vectorstores import Chroma
from langchain.chains import RetrievalQA
from langchain.llms import HuggingFacePipeline
from transformers import AutoModelForCausalLM, AutoTokenizer, pipeline, BitsAndBytesConfig

# ---------------------- 1. 文档加载（加载docs文件夹下的所有文档）----------------------
def load_documents(doc_dir="./docs"):
    # 定义加载器：支持PDF、Word、TXT格式
    loaders = {
        'pdf': DirectoryLoader(doc_dir, glob="*.pdf", loader_cls=PyPDFLoader),
        'docx': DirectoryLoader(doc_dir, glob="*.docx", loader_cls=Docx2txtLoader),
        'txt': DirectoryLoader(doc_dir, glob="*.txt", loader_cls=TextLoader)
    }
    # 加载所有文档
    documents = []
    for loader in loaders.values():
        try:
            documents.extend(loader.load())
        except Exception as e:
            print(f"加载{loader}时出错：{e}")
    print(f"成功加载文档数量：{len(documents)}")
    return documents

# ---------------------- 2. 文档拆分（拆分为小片段，提升检索精度）----------------------
def split_documents(documents):
    # 递归字符拆分：按语义拆分，避免拆分完整句子
    text_splitter = RecursiveCharacterTextSplitter(
        chunk_size=800,          # 每个片段长度（可调整，500-1000字为宜）
        chunk_overlap=100,       # 片段重叠长度（保持上下文连贯）
        length_function=len,
        separators=["\n\n", "\n", "。", "，", " "]
    )
    splits = text_splitter.split_documents(documents)
    print(f"文档拆分后片段数量：{len(splits)}")
    return splits

# ---------------------- 3. 初始化嵌入模型（BAAI/bge-small-zh）----------------------
def init_embedding_model():
    embedding_model_name = "BAAI/bge-small-zh"
    # 配置嵌入模型：使用CPU，量化提升效率
    embedding_model = HuggingFaceEmbeddings(
        model_name=embedding_model_name,
        model_kwargs={'device': 'cpu'},  # 纯CPU运行
        encode_kwargs={'normalize_embeddings': True}  # 归一化向量，提升检索精度
    )
    return embedding_model

# ---------------------- 4. 初始化向量数据库（Chroma），将文档片段入库----------------------
def init_vector_db(splits, embedding_model):
    # 定义Chroma存储路径（本地目录，数据持久化）
    persist_directory = "./chroma_db"
    # 初始化向量数据库：将拆分后的片段转化为向量并存储
    vectordb = Chroma.from_documents(
        documents=splits,
        embedding=embedding_model,
        persist_directory=persist_directory
    )
    # 持久化数据库（避免每次运行重新生成）
    vectordb.persist()
    print(f"向量数据库初始化完成，存储路径：{persist_directory}")
    return vectordb

# ---------------------- 5. 初始化本地大模型（Qwen-7B-Chat-4bit）----------------------
def init_local_llm():
    model_name = "Qwen/Qwen-7B-Chat-4bit"  # 4bit量化版，内存占用低
    # 配置量化参数（4bit量化，适配CPU）
    bnb_config = BitsAndBytesConfig(
        load_in_4bit=True,
        bnb_4bit_use_double_quant=True,
        bnb_4bit_quant_type="nf4",
        bnb_4bit_compute_dtype=torch.bfloat16
    )
    # 加载tokenizer和model
    tokenizer = AutoTokenizer.from_pretrained(model_name, trust_remote_code=True)
    model = AutoModelForCausalLM.from_pretrained(
        model_name,
        quantization_config=bnb_config,
        device_map="auto",  # 自动分配设备（纯CPU）
        trust_remote_code=True
    )
    # 构建pipeline，用于对接LangChain
    pipe = pipeline(
        "text-generation",
        model=model,
        tokenizer=tokenizer,
        max_new_tokens=512,  # 生成回答的最大长度
        temperature=0.7,     # 随机性，0.7适中
        top_p=0.9,
        repetition_penalty=1.1  # 避免重复生成
    )
    # 封装为LangChain可用的LLM
    llm = HuggingFacePipeline(pipeline=pipe)
    print("本地大模型初始化完成")
    return llm

# ---------------------- 6. 构建RAG问答链（检索+生成）----------------------
def build_rag_chain(vectordb, llm):
    # 构建检索器：从向量数据库中检索相似片段（top_k=3，返回3个最相关片段）
    retriever = vectordb.as_retriever(search_kwargs={"k": 3})
    # 构建问答链：基于检索结果生成回答
    qa_chain = RetrievalQA.from_chain_type(
        llm=llm,
        chain_type="stuff",  # 简单直接：将检索到的片段拼接后输入模型
        retriever=retriever,
        return_source_documents=True  # 返回回答对应的参考文档，方便核验
    )
    return qa_chain

# ---------------------- 7. 核心运行函数----------------------
def main():
    # 步骤1-4：文档加载→拆分→嵌入→向量入库
    documents = load_documents()
    if not documents:
        print("未加载到任何文档，请检查docs文件夹是否有文档")
        return
    splits = split_documents(documents)
    embedding_model = init_embedding_model()
    vectordb = init_vector_db(splits, embedding_model)
    
    # 步骤5-6：初始化大模型→构建问答链
    llm = init_local_llm()
    qa_chain = build_rag_chain(vectordb, llm)
    
    # 交互问答
    print("\nRAG本地知识库已启动，输入问题即可查询（输入'quit'退出）：")
    while True:
        query = input("\n请输入问题：")
        if query.lower() == "quit":
            print("退出问答")
            break
        # 执行问答
        result = qa_chain({"query": query})
        # 输出结果
        print(f"\n回答：{result['result']}")
        # 输出参考来源
        print("\n参考来源：")
        for idx, doc in enumerate(result['source_documents'], 1):
            print(f"{idx}. 文档：{doc.metadata.get('source', '未知文档')}，页码：{doc.metadata.get('page', '未知')}")

if __name__ == "__main__":
    main()
```

## 第六步：运行代码，启动RAG本地知识库

1. 确保「docs」文件夹中已放入文档，且虚拟环境已激活；

2. 进入项目根目录（终端执行「cd 项目根目录路径」，如「cd D:\RAG-Local-Project」）；

3. 运行代码：
        `python rag_local_demo.py`

4. 首次运行说明：
        

    - 会自动下载「BAAI/bge-small-zh」嵌入模型和「Qwen-7B-Chat-4bit」大模型，网络不稳定时可能耗时较长，建议提前手动下载；

    - 模型下载完成后，会自动执行文档加载、拆分、向量入库，最后启动问答交互；

    - 后续再次运行时，会直接加载本地已缓存的模型和向量数据库，无需重复下载/生成。

5. 测试问答：输入问题（如「RPA财务发票流程如何设计」），等待片刻（纯CPU推理约10-30秒/次），即可得到基于本地文档的回答及参考来源。

# 三、验证与优化

## 1. 验证要点

- 文档加载验证：运行日志中显示「成功加载文档数量：X」，X为docs文件夹中文档数，无报错即正常；

- 向量入库验证：项目根目录生成「chroma_db」文件夹，且包含多个文件，代表向量数据已持久化；

- 问答效果验证：提问需基于本地文档内容，查看回答是否准确，参考来源是否对应正确文档/页码。

## 2. 常见优化方向

- 调整文档拆分参数：若回答不精准，可修改「chunk_size」（如改为600/1000）和「chunk_overlap」（如改为80/120）；

- 优化检索数量：调整「search_kwargs={"k": 3}」中的k值（如改为2/4），k值越大参考信息越多，但可能引入冗余；

- 更换嵌入模型：若中文语义理解不足，可替换为「BAAI/bge-large-zh」（效果更好，但更耗内存）；

- 提升模型推理速度：若CPU性能较弱，可更换为更小的模型（如Qwen-1.8B-Chat-4bit），推理速度更快。

# 四、注意事项

- 依赖冲突解决：若安装依赖时出现「version conflict」，可通过「pip install 包名==具体版本」指定稳定版本（如「pip install torch==2.0.1」）；

- 内存不足处理：若运行时提示「Out of memory」，可关闭其他占用内存的程序，或更换更小的模型（如Qwen-1.8B）；

- 模型下载问题：若自动下载失败，可通过Hugging Face手动下载，再修改代码中模型路径为本地路径；

- 数据安全：所有文档、模型、向量数据均存储在本地，无数据上传风险，可放心用于私有文档（如企业内部RPA手册、财务数据等）。

# 五、后续扩展（可选）

- 支持更多文档格式：添加PPT、Excel加载器（LangChain支持python-pptx、pandas加载相关格式）；

- 添加可视化界面：使用Gradio/Streamlit搭建简单的Web界面，实现浏览器端问答；

- 多轮对话功能：修改代码为「ConversationalRetrievalChain」，支持上下文关联问答（如追问「这个流程的报错怎么解决」）。
> （注：文档部分内容可能由 AI 生成）