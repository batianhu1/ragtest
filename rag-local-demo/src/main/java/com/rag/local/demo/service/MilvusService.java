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
        List<InsertParam.Field> fields = new ArrayList<>();
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
        List<String> similarChunks = new ArrayList<>();
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