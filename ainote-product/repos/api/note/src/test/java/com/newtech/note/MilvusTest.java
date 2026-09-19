package com.newtech.note;

import io.milvus.client.MilvusClient;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.MutationResult;
import io.milvus.grpc.SearchResults;
import io.milvus.param.ConnectParam;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

//@SpringBootTest
public class MilvusTest {

    public final MilvusClient milvusClient;  // 使用 @MockBean 来模拟 MilvusClient

    public MilvusTest() {
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost("117.72.124.43")
                .withPort(19530)
                .build();
        this.milvusClient = new MilvusServiceClient(connectParam);
    }

    /**
     * 异步插入数据到 Milvus
     *
     * @param collectionName 要插入数据的集合名称
     * @param ids            数据的 ID 列表
     * @param vectors        要插入的向量数据列表，每个向量是一个 List<Float>
     * @return 返回插入操作的响应
     */
    public Mono<MutationResult> insertData(String collectionName, List<Long> ids, List<List<Float>> vectors) {
        // 创建插入请求的字段数据
        //InsertParam.Field idFieldData = new InsertParam.Field("id", ids);
        // 创建字段：向量字段
        InsertParam.Field vectorFieldData = new InsertParam.Field("note_emb", vectors);


        /*// 创建行数据（可以根据需求来构造）
        // 使用 JsonObject 来封装每一行的数据
        JsonObject row1 = new JsonObject();
        row1.addProperty("id", 1);
        row1.add("vector_field", new JsonObject()); // 示例向量字段

        JsonObject row2 = new JsonObject();
        row2.addProperty("id", 2);
        row2.add("vector_field", new JsonObject()); // 示例向量字段*/

        // 创建插入请求
        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(collectionName)  // 设置集合名称
                .withFields(List.of(vectorFieldData))  // 设置字段数据
                //.withRows(List.of(row1, row2))  // 设置行数据
                .build();

        // 调用插入操作并返回 Mono，确保异步执行
        return Mono.fromCallable(() -> milvusClient.insert(insertParam)).map(R::getData);
    }

    public Mono<MutationResult> insertData(String collectionName, String uid, String note, List<Float> vector, Long time) {
        // 创建插入请求的字段数据
        InsertParam.Field uidFieldData = new InsertParam.Field("uid", List.of(uid));
        InsertParam.Field noteFieldData = new InsertParam.Field("note", List.of(note));
        InsertParam.Field vectorFieldData = new InsertParam.Field("note_emb", List.of(vector));
        InsertParam.Field timeFieldData = new InsertParam.Field("time", List.of(time));

        // 创建插入请求
        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(collectionName)  // 设置集合名称
                .withFields(List.of(uidFieldData, noteFieldData, vectorFieldData, timeFieldData))  // 设置字段数据
                .build();

        // 调用插入操作并返回 Mono，确保异步执行
        return Mono.fromCallable(() -> milvusClient.insert(insertParam)).map(R::getData);
    }


    // 异步查询数据
   /* public Mono<SearchResults> searchData(String collectionName, List<List<Float>> queryVectors, int topK) {
        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(collectionName) // 设置集合名称
                .withVectorFieldName("note_emb") // 设置向量字段名称
                .withMetricType(MetricType.L2) // 设置度量类型为 L2（欧式距离）
                .withTopK(topK) // 设置返回结果的前 K 个
                .withFloatVectors(queryVectors) // 设置查询向量
                .build(); // 调用 build() 方法构建 SearchParam
        // 执行查询并返回 Mono
        return Mono.fromCallable(() -> milvusClient.search(searchParam)).map(R::getData);
    }*/

    public Mono<List<String>> searchData(String collectionName, List<Float> queryVector, int topK) {
        // 创建搜索请求的查询字段数据
        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(collectionName)  // 设置集合名称
                .withVectorFieldName("note_emb") // 设置向量字段名称
                .withMetricType(MetricType.COSINE) // 设置度量类型为 L2（欧式距离）
                .withTopK(topK) // 设置返回结果的前 K 个
                .withOutFields(List.of("note"))
                .withFloatVectors(List.of(queryVector)) // 设置查询向量       // 设置向量字段名，这里是 "note_emb"
                .build();


        // 调用 Milvus 的搜索接口并返回 Mono，确保异步执行
        return Mono.fromCallable(() -> milvusClient.search(searchParam))
                .map(searchResult -> {
                    // 获取搜索结果中的 ID 列表（Top K 最相似项的 ID）
                    return searchResult.getData().getResults().getFieldsDataList().stream()
                            .filter(fieldData -> "note".equals(fieldData.getFieldName()))  // 过滤出 "note" 字段
                            .flatMap(fieldData -> fieldData.getScalars().getStringData().getDataList().stream())  // 获取 string_data
                            .collect(Collectors.toList());  // 收集到列表中;
                });
    }


    //@Test
    public void testSearchData() {
        int topK = 2;
        // 准备模拟数据
        List<String> resultIds = searchData("notes", generateRandomVector(1024), topK).block();
        System.out.println("Top " + topK + " similar notes: " + resultIds);
    }

    //@Test
    public void testInsertData() {
        // 准备模拟数据
        String uid = "user1";
        String note = "Weekend hiking to watch the sunrise.";
        List<Float> vectors = generateRandomVector(1024);
        Long time = System.currentTimeMillis();  // 使用当前时间戳

        // 执行单条数据插入
        MutationResult result = insertData("notes", uid, note, vectors, time).block();
        System.out.println(result);
    }

    // 生成一个具有指定维度的随机向量
    private static List<Float> generateRandomVector(int dimension) {
        Random random = new Random();
        List<Float> vector = new ArrayList<>();
        for (int i = 0; i < dimension; i++) {
            // 随机生成一个浮动数值，这里假设是[0.0f, 1.0f]范围内的浮动数值
            vector.add(random.nextFloat());
        }
        return vector;
    }


}

