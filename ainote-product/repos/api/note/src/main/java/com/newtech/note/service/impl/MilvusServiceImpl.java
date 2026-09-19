package com.newtech.note.service.impl;

import com.newtech.note.service.MilvusService;
import io.milvus.client.MilvusClient;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.FieldData;
import io.milvus.grpc.MutationResult;
import io.milvus.grpc.ScalarField;
import io.milvus.param.ConnectParam;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.dml.UpsertParam;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MilvusServiceImpl implements MilvusService {
    private final String milvusHost;
    private final int milvusPort;
    private volatile MilvusClient milvusClient;
    private int inFlight;
    private boolean closing;
    private boolean closed;

    public MilvusServiceImpl(
            @Value("${milvus.host:}") String milvusHost,
            @Value("${milvus.port:19530}") int milvusPort) {
        this.milvusHost = milvusHost;
        this.milvusPort = milvusPort;
    }

    private synchronized ClientLease acquireClient() {
        if (closing || closed) {
            throw new IllegalStateException("Milvus client is already closed");
        }
        MilvusClient client = getMilvusClient();
        inFlight++;
        return new ClientLease(client);
    }

    /**
     * Returns the initialized client for subclasses and diagnostics. RPC paths must use
     * {@link #withClient(Function)} so shutdown can wait for the operation lease.
     */
    protected synchronized MilvusClient getMilvusClient() {
        if (closing || closed) {
            throw new IllegalStateException("Milvus client is already closed");
        }
        if (milvusClient == null) {
            if (milvusHost == null || milvusHost.isBlank()) {
                throw new IllegalStateException("Milvus host is not configured; set MILVUS_HOST or milvus.host before using Milvus");
            }
            milvusClient = createMilvusClient();
        }
        return milvusClient;
    }

    private <T> T withClient(Function<MilvusClient, T> operation) {
        ClientLease lease = acquireClient();
        try {
            return operation.apply(lease.client);
        } finally {
            lease.close();
        }
    }

    protected MilvusClient createMilvusClient() {
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(milvusHost)
                .withPort(milvusPort)
                .build();
        return new MilvusServiceClient(connectParam);
    }

    @PreDestroy
    public void close() {
        boolean interrupted = false;
        MilvusClient clientToClose;
        synchronized (this) {
            if (closed) {
                return;
            }
            if (closing) {
                while (!closed) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
                return;
            }
            closing = true;
            while (inFlight > 0) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
            clientToClose = milvusClient;
        }
        try {
            if (clientToClose != null) {
                clientToClose.close();
            }
        } finally {
            synchronized (this) {
                closed = true;
                notifyAll();
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private final class ClientLease implements AutoCloseable {
        private final MilvusClient client;
        private boolean released;

        private ClientLease(MilvusClient client) {
            this.client = client;
        }

        @Override
        public void close() {
            synchronized (MilvusServiceImpl.this) {
                if (released) {
                    return;
                }
                released = true;
                inFlight--;
                if (inFlight == 0) {
                    MilvusServiceImpl.this.notifyAll();
                }
            }
        }
    }

    /**
     * 异步插入数据到 Milvus
     *
     * @param collectionName 要插入数据的集合名称
     * @param uid            用户id列表
     * @param noteId         笔记id
     * @param note           笔记内容
     * @param vector         要插入的向量数据列表，每个向量是一个 List<Float>
     * @return 返回插入操作的响应
     */
    @Override
    public Mono<MutationResult> insertData(String collectionName, String uid, String noteId, String note, String noteThemeId, List<Float> vector) {
        // 创建插入请求的字段数据
        InsertParam.Field uidFieldData = new InsertParam.Field("uid", List.of(uid));
        InsertParam.Field noteFieldData = new InsertParam.Field("note", List.of(note));
        InsertParam.Field noteIdFieldData = new InsertParam.Field("note_id", List.of(noteId));
        InsertParam.Field vectorFieldData = new InsertParam.Field("note_emb", List.of(vector));
        //hard code for note_theme fixme
        InsertParam.Field noteThemeFieldData = new InsertParam.Field("theme_id", List.of(noteThemeId));

        InsertParam.Field timeFieldData = new InsertParam.Field("time", List.of(System.currentTimeMillis()));
        // 创建插入请求
        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(collectionName)  // 设置集合名称
                .withFields(List.of(uidFieldData, noteFieldData, noteIdFieldData, vectorFieldData, noteThemeFieldData, timeFieldData))  // 设置字段数据
                .build();
        // 调用插入操作并返回 Mono，确保异步执行
        return Mono.fromCallable(() -> withClient(client -> client.insert(insertParam))).map(R::getData);
    }

    public Mono<List<FieldData>> queryForAllFields(String collectionName, String queryExpr) {
        // 创建搜索参数
        SearchParam searchParam;
        try {
            searchParam = SearchParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withVectorFieldName("note_emb")
                    .withExpr(queryExpr) // 使用传入的表达式
                    .withTopK(10)
                    .withFloatVectors(List.of(List.of(0.0f, 0.0f, 0.0f)))
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return Mono.empty();
        }
        return Mono.fromCallable(() -> withClient(client -> client.search(searchParam)))
                .map(searchResult -> {
                    // 获取搜索结果中的 ID 列表（Top K 最相似项的 ID）
                    return searchResult.getData().getResults().getFieldsDataList();  // 收集到列表中;
                });
    }

    public Object getScalarFieldData(ScalarField scalarField) {
        return switch (scalarField.getDataCase()) {
            case BOOL_DATA -> scalarField.getBoolData();  // 获取 boolean 类型数据
            case INT_DATA -> scalarField.getIntData();  // 获取 int 类型数据
            case LONG_DATA -> scalarField.getLongData();  // 获取 long 类型数据
            case FLOAT_DATA -> scalarField.getFloatData();  // 获取 float 类型数据
            case DOUBLE_DATA -> scalarField.getDoubleData();  // 获取 double 类型数据
            case STRING_DATA -> scalarField.getStringData();  // 获取 string 类型数据
            case BYTES_DATA -> scalarField.getBytesData();  // 获取 byte[] 类型数据
            case ARRAY_DATA -> scalarField.getArrayData();  // 获取 array 类型数据
            case JSON_DATA -> scalarField.getJsonData();  // 获取 json 类型数据
            case GEOMETRY_DATA -> scalarField.getGeometryData();  // 获取 geometry 类型数据
            case DATA_NOT_SET -> null;  // 没有设置数据
        };
    }

    @Override
    public Mono<MutationResult> upsertData(String collectionName, String uid, String noteId, String note, String noteThemeId, List<Float> vector) {
        return queryForAllFields(collectionName, "note_id == '" + noteId + "'")
                .flatMap(fieldDataList -> {
                    if (CollectionUtils.isEmpty(fieldDataList)) {
                        return insertData(collectionName, uid, noteId, note, noteThemeId, vector);
                    }
                    List<InsertParam.Field> updatedFields = fieldDataList.stream()
                            .map(fieldData -> {
                                // 根据字段名称判断哪些字段需要更新
                                if ("note".equals(fieldData.getFieldName())) {
                                    return new InsertParam.Field("note", List.of(note));
                                } else if ("note_emb".equals(fieldData.getFieldName())) {
                                    return new InsertParam.Field("note_emb", List.of(vector));
                                } else if ("theme_id".equals(fieldData.getFieldName())) {
                                    return new InsertParam.Field("theme_id", List.of(noteThemeId));
                                } else if ("time".equals(fieldData.getFieldName())) {
                                    return new InsertParam.Field("time", List.of(System.currentTimeMillis()));
                                } else {
                                    ScalarField scalarField = fieldData.getScalars();
                                    Object fieldValue = getScalarFieldData(scalarField);
                                    return new InsertParam.Field(fieldData.getFieldName(), List.of(fieldValue));
                                }
                            })
                            .collect(Collectors.toList());
                    // 创建upsert参数，执行插入或更新操作
                    UpsertParam upsertParam = UpsertParam.newBuilder()
                            .withCollectionName(collectionName)
                            .withFields(updatedFields)  // 设置字段数据
                            .build();
                    // 执行插入操作
                    return Mono.fromCallable(() -> withClient(client -> client.insert(upsertParam))).map(R::getData);
                });
    }

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
        return Mono.fromCallable(() -> withClient(client -> client.search(searchParam)))
                .map(searchResult -> {
                    // 获取搜索结果中的 ID 列表（Top K 最相似项的 ID）
                    return searchResult.getData().getResults().getFieldsDataList().stream()
                            .filter(fieldData -> "noteId".equals(fieldData.getFieldName()))  // 过滤出 "noteId" 字段
                            .flatMap(fieldData -> fieldData.getScalars().getStringData().getDataList().stream())  // 获取 string_data
                            .collect(Collectors.toList());  // 收集到列表中;
                });
    }
}
