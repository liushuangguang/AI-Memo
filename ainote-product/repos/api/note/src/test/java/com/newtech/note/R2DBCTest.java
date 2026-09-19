package com.newtech.note;

import com.newtech.note.entity.dto.NoteAnalysis;
import com.newtech.note.entity.dto.NoteDevice;
import com.newtech.note.entity.vo.NoteAnalysisDeviceVo;
import com.newtech.note.repositories.NoteAnalysisDeviceVoRepository;
import com.newtech.note.repositories.NoteAnalysisRepository;
import com.newtech.note.repositories.NoteDeviceRepository;
import io.asyncer.r2dbc.mysql.MySqlConnectionConfiguration;
import io.asyncer.r2dbc.mysql.MySqlConnectionFactory;
import io.asyncer.r2dbc.mysql.api.MySqlConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Example;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.newtech.note.util.PinyinUtil.chineseToPinyin;


@SpringBootTest
public class R2DBCTest {


    @Autowired  // join查询不好做； 单表查询用
    R2dbcEntityTemplate r2dbcEntityTemplate; //CRUD API； 更多API操作示例： https://docs.spring.io/spring-data/relational/reference/r2dbc/entity-persistence.html


    @Autowired  //贴近底层，join操作好做； 复杂查询好用
    DatabaseClient databaseClient; //数据库客户端

    @Autowired
    NoteDeviceRepository noteDeviceRepository;

    @Autowired
    NoteAnalysisRepository noteAnalysisRepository;

    @Autowired
    NoteAnalysisDeviceVoRepository noteAnalysisDeviceVoRepository;

    @Autowired
    R2dbcCustomConversions r2dbcCustomConversions;


    //@Test
    void oneToN() throws IOException {


        // 1~6
        // 1：false 2：false 3:false 4: true 8:true 5:false 6:false 7:false 8:true 9:false 10:false
        // [1,2,3]
        // [4,8]
        // [5,6,7]
        // [8]
        // [9,10]
        // bufferUntilChanged：
        Flux.just(1, 2, 3, 4, 8, 5, 6, 7, 8, 9, 10)
                .bufferUntilChanged(integer -> integer % 4 == 0)
                .subscribe(list -> System.out.println("list = " + list));
        ; //自带分组

        Flux<NoteDevice> flux = databaseClient.sql("select a.id aid,a.device_name,b.* from note_device a  " +
                        "left join note_analysis b on a.device_id = b.device_id " +
                        "order by aid asc")//这里一定要排序！！！
                .fetch()
                .all()
                // 如果下一个判定值比起上一个发生了变化就开一个新buffer保存，如果没有变化就保存到原buffer中
                .bufferUntilChanged(rowMap -> Long.parseLong(rowMap.get("aid").toString()))
                .map(list -> {
                    NoteDevice noteDevice = new NoteDevice();
                    Map<String, Object> map = list.get(0);
                    noteDevice.setId(Long.parseLong(map.get("aid").toString()));
                    noteDevice.setDeviceId(map.get("device_id").toString());
                    noteDevice.setDeviceName(map.get("device_name").toString());
                    //查到的所有图书
                    List<NoteAnalysis> noteAnalyses = list.stream()
                            .map(ele -> {
                                NoteAnalysis noteAnalysis = new NoteAnalysis();
                                noteAnalysis.setId(Long.parseLong(ele.get("id").toString()));
                                noteAnalysis.setDeviceId(ele.get("device_id").toString());
                                noteAnalysis.setRawNote(ele.get("raw_note").toString());
                                noteAnalysis.setTitle(ele.get("title").toString());
                                noteAnalysis.setNoteAnalysisContent(ele.get("note_analysis_content").toString());
                                ZonedDateTime zonedCreatedAt = ZonedDateTime.parse(ele.get("created_at").toString(), DateTimeFormatter.ISO_DATE_TIME);
                                noteAnalysis.setCreatedAt(zonedCreatedAt.toLocalDateTime());
                                ZonedDateTime zonedUpdatedAt = ZonedDateTime.parse(ele.get("updated_at").toString(), DateTimeFormatter.ISO_DATE_TIME);
                                noteAnalysis.setUpdatedAt(zonedUpdatedAt.toLocalDateTime());
                                return noteAnalysis;
                            })
                            .toList();
                    noteDevice.setNoteAnalyses(noteAnalyses);
                    return noteDevice;
                });//Long 数字缓存 -127 - 127；// 对象比较需要自己写好equals方法
        flux.subscribe(noteDevice -> System.out.println("noteDevice = " + noteDevice));
        System.in.read();
    }


    //@Test
    void noteDevice() throws IOException {
        noteDeviceRepository.findById(1L)
                .subscribe(noteDevice -> System.out.println("noteDevice = " + noteDevice));
        System.in.read();
    }

    //@Test
    void noteAnalysis() throws IOException {
        noteAnalysisRepository.findAll()
                .subscribe(noteAnalysis -> System.out.println("noteAnalysis = " + noteAnalysis));

        NoteAnalysis probe = new NoteAnalysis();
        probe.setId(3L);
        probe.setTitle("Analysis 3");
        Example<NoteAnalysis> example = Example.of(probe);

        noteAnalysisRepository.findAll(example)
                .flatMap(noteAnalysis -> {
                    NoteDevice deviceProbe = new NoteDevice();
                    deviceProbe.setDeviceId(noteAnalysis.getDeviceId());
                    Example<NoteDevice> deviceExample = Example.of(deviceProbe);
                    return noteDeviceRepository.findOne(deviceExample)
                            .map(noteDevice -> {
                                NoteAnalysisDeviceVo analysisDeviceVo = new NoteAnalysisDeviceVo();
                                analysisDeviceVo.setId(noteAnalysis.getId());
                                analysisDeviceVo.setNoteAnalysisContent(noteAnalysis.getNoteAnalysisContent());
                                analysisDeviceVo.setRawNote(noteAnalysis.getRawNote());
                                analysisDeviceVo.setTitle(noteAnalysis.getTitle());
                                analysisDeviceVo.setCreatedAt(noteAnalysis.getCreatedAt());
                                analysisDeviceVo.setUpdatedAt(noteAnalysis.getUpdatedAt());
                                analysisDeviceVo.setNoteDevice(noteDevice);
                                return analysisDeviceVo;
                            });
                })
                .subscribe(noteAnalysisDeviceVo -> System.out.println("noteAnalysisDeviceVo = " + noteAnalysisDeviceVo));


        //第一种方式:  自定义转换器封装
        noteAnalysisDeviceVoRepository.findNoteAnalysisById(1L)
                .subscribe(noteAnalysisDeviceVo -> System.out.println("noteAnalysisDeviceVo = " + noteAnalysisDeviceVo));


        //自定义转换器  Converter<Row, NoteAnalysisDeviceVo> ： 把数据库的row转成 NoteAnalysisDeviceVo； 所有NoteAnalysisDeviceVo的结果封装都用这个
        //工作时机： Spring Data 发现方法签名只要是返回 NoteAnalysisDeviceVo 就自动使用自定义转换器。 利用自定义转换器进行工作

        //对以前的CRUD产生影响; 错误：Column device_name 'device_name' does not exist
        //解决办法：
        //  1）、新VO+新的Repository+自定义类型转化器
        //  2）、自定义类型转化器 多写判断。兼容更多表类型
        System.out.println("noteAnalysisRepository.findById(1L).block() = "
                + noteAnalysisRepository.findById(1L).block());


        System.out.println("================");

        System.out.println("noteAnalysisDeviceVoRepository.findNoteAnalysisById(1L).block() = " + noteAnalysisDeviceVoRepository.findNoteAnalysisById(1L)
                .block());
        //第二种方式
        databaseClient.sql("select b.*,t.device_id, t.id tid,t.device_name from note_analysis b " +
                        "LEFT JOIN note_device t on b.device_id = t.device_id " +
                        "WHERE b.id = ?")
                .bind(0, 1L)
                .fetch()
                .all()
                .map(row -> {
                    NoteAnalysisDeviceVo analysisDeviceVo = new NoteAnalysisDeviceVo();
                    String id = row.get("id").toString();
                    String noteId = row.get("note_id").toString();
                    String noteAnalysisContent = row.get("note_analysis_content").toString();
                    String rawNote = row.get("raw_note").toString();
                    String title = row.get("title").toString();
                    analysisDeviceVo.setId(Long.parseLong(id));
                    analysisDeviceVo.setNoteId(noteId);
                    analysisDeviceVo.setNoteAnalysisContent(noteAnalysisContent);
                    analysisDeviceVo.setRawNote(rawNote);
                    analysisDeviceVo.setTitle(title);
                    ZonedDateTime zonedCreatedAt = ZonedDateTime.parse(row.get("created_at").toString(), DateTimeFormatter.ISO_DATE_TIME);
                    // 将 ZonedDateTime 转换为 LocalDateTime（如果你只需要日期时间部分）
                    analysisDeviceVo.setCreatedAt(zonedCreatedAt.toLocalDateTime());
                    ZonedDateTime zonedUpdatedAt = ZonedDateTime.parse(row.get("updated_at").toString(), DateTimeFormatter.ISO_DATE_TIME);
                    analysisDeviceVo.setUpdatedAt(zonedUpdatedAt.toLocalDateTime());
                    NoteDevice noteDevice = new NoteDevice();
                    noteDevice.setId(Long.parseLong(row.get("tid").toString()));
                    noteDevice.setDeviceId(row.get("device_id").toString());
                    noteDevice.setDeviceName(row.get("device_name").toString());
                    analysisDeviceVo.setNoteDevice(noteDevice);
                    return analysisDeviceVo;
                })
                .subscribe(analysisDeviceVo -> System.out.println("analysisDeviceVo = " + analysisDeviceVo));

        // buffer api: 实现一对N；

        //两种办法：
        //1、一次查询出来，封装好
        //2、两次查询

        // 1-N： 一个作者；可以查询到很多图书
        System.in.read();
    }

    //简单查询： 人家直接提供好接口
    //复杂条件查询：
    //    1、QBE API
    //    2、自定义方法
    //    3、自定义SQL

    //@Test
    void deviceRepository() throws IOException {
        noteDeviceRepository.findAll()
                .subscribe(noteDevice -> System.out.println("noteDevice = " + noteDevice));

        //statement
        // [SELECT id, device_id, device_name FROM note_device WHERE id IN (?, ?)
        // AND (device_name LIKE ?)]
        //方法起名
        noteDeviceRepository.findAllByIdInAndDeviceNameLike(
                Arrays.asList(1L, 2L),
                "device%"
        ).subscribe(noteDevice -> System.out.println("noteDevice = " + noteDevice));


        //自定义@Query注解
        noteDeviceRepository.findCustomized()
                .subscribe(noteDevice -> System.out.println("noteDevice = " + noteDevice));

        System.in.read();
    }


    //@Test
    void databaseClient() throws IOException {
        // 底层操作
        databaseClient
                .sql("select * from note_device where id = ?")
                .bind(0, 2L)
                .fetch()
                .all()
                .map(map -> {  //map == bean  属性=值
                    System.out.println("map = " + map);
                    String id = map.get("id").toString();
                    String deviceId = map.get("device_id").toString();
                    String deviceName = map.get("device_name").toString();
                    // 使用 DateTimeFormatter 解析日期时间字符串
                    ZonedDateTime zonedCreatedAt = ZonedDateTime.parse(map.get("created_at").toString(), DateTimeFormatter.ISO_DATE_TIME);
                    // 将 ZonedDateTime 转换为 LocalDateTime（如果你只需要日期时间部分）
                    LocalDateTime createdAt = zonedCreatedAt.toLocalDateTime();
                    ZonedDateTime zonedUpdatedAt = ZonedDateTime.parse(map.get("updated_at").toString(), DateTimeFormatter.ISO_DATE_TIME);
                    LocalDateTime updatedAt = zonedUpdatedAt.toLocalDateTime();
                    return new NoteDevice(Long.parseLong(id), deviceId, deviceName, createdAt, updatedAt, List.of());
                })
                .subscribe(noteDevice -> System.out.println("noteDevice = " + noteDevice));
        System.in.read();
    }

    public static void main(String[] args) {
        String chineseText = "你好（世界）！ 中国";
        String pinyinText = chineseToPinyin(chineseText);
        System.out.println(pinyinText); // 输出结果：ni hao shi jie !
    }


    //@Test
    void fullTextSearch() throws IOException {
        // 底层操作
        databaseClient
                .sql("SELECT * FROM note_analysis WHERE MATCH(hash_tags) AGAINST ('zhong1guo2 ni3hao3shi4jie4' IN BOOLEAN MODE)")
                .fetch()
                .all()
                .map(map -> {  //map == bean  属性=值
                    System.out.println("map = " + map);
                    String id = map.get("id").toString();
                    String deviceId = map.get("device_id").toString();
                    String deviceName = map.getOrDefault("device_name", "").toString();
                    // 使用 DateTimeFormatter 解析日期时间字符串
                    ZonedDateTime zonedCreatedAt = ZonedDateTime.parse(map.get("created_at").toString(), DateTimeFormatter.ISO_DATE_TIME);
                    // 将 ZonedDateTime 转换为 LocalDateTime（如果你只需要日期时间部分）
                    LocalDateTime createdAt = zonedCreatedAt.toLocalDateTime();
                    ZonedDateTime zonedUpdatedAt = ZonedDateTime.parse(map.get("updated_at").toString(), DateTimeFormatter.ISO_DATE_TIME);
                    LocalDateTime updatedAt = zonedUpdatedAt.toLocalDateTime();
                    return new NoteDevice(Long.parseLong(id), deviceId, deviceName, createdAt, updatedAt, List.of());
                })
                .subscribe(noteDevice -> System.out.println("noteDevice = " + noteDevice));
        System.in.read();
    }


    //@Test
    void r2dbcEntityTemplate() throws IOException {
        // Query By Criteria: QBC
        //1、Criteria构造查询条件  where id=1 and name=张三
        Criteria criteria = Criteria
                .empty()
                .and("id").is(1L)
                .and("device_name").is("Device 1");
        //2、封装为 Query 对象
        Query query = Query.query(criteria);
        r2dbcEntityTemplate
                .select(query, NoteDevice.class)
                .subscribe(noteDevice -> System.out.println("noteDevice = " + noteDevice));
        System.in.read();
    }

    // 有了r2dbc-spi的支持，我们的应用在数据库中天然支持高并发，高吞吐量。
    //@Test
    void test() throws InterruptedException {
        MySqlConnectionConfiguration configuration = MySqlConnectionConfiguration.builder()
                .host("localhost")
                .username("root")
                .password("my-secret-pw")
                .database("note")
                .build();
        MySqlConnectionFactory connectionFactory = MySqlConnectionFactory.from(configuration);
        //阻塞方式创建连接
        MySqlConnection conn = connectionFactory.create().block();
        String str = conn.createStatement("SELECT * FROM note_analysis where id = 1").execute().blockFirst().map(readable -> {
            String title = readable.get("title", String.class);
            System.out.println(title);
            return title;
        }).blockFirst();
        System.out.println(str);


        Mono.from(connectionFactory.create())
                .flatMapMany(connection ->
                        connection.createStatement("SELECT * FROM note_analysis where id = ?")
                                .bind(0, 2L)
                                .execute())
                .flatMap(result -> result.map(readable -> {
                    long id = readable.get("id", Long.class);
                    String title = readable.get("title", String.class);
                    String deviceId = readable.get("device_id", String.class);
                    String rawNote = readable.get("raw_note", String.class);
                    String noteAnalysisContent = readable.get("note_analysis_content", String.class);
                    String hashTags = readable.get("hash_tags", String.class);
                    String tagString = readable.get("tags", String.class);
                    List<String> tags = List.of();
                    if (tagString != null) {
                        tags = Arrays.stream(tagString.split("\\s+")).collect(Collectors.toList());
                    }
                    LocalDateTime createdAt = readable.get("created_at", LocalDateTime.class);
                    LocalDateTime updatedAt = readable.get("updated_at", LocalDateTime.class);
                    LocalDateTime deletedAt = readable.get("deleted_at", LocalDateTime.class);
                    NoteAnalysis noteAnalysis = new NoteAnalysis();
                    noteAnalysis.setId(id);
                    noteAnalysis.setRawNote(rawNote);
                    noteAnalysis.setDeleted(false);
                    noteAnalysis.setHashTags(hashTags);
                    noteAnalysis.setTitle(title);
                    noteAnalysis.setDeviceId(deviceId);
                    noteAnalysis.setCreatedAt(createdAt);
                    noteAnalysis.setUpdatedAt(updatedAt);
                    noteAnalysis.setDeletedAt(deletedAt);
                    noteAnalysis.setHitTags(List.of());
                    noteAnalysis.setTags(tagString);
                    noteAnalysis.setNoteAnalysisContent(noteAnalysisContent);
                    noteAnalysis.setTagList(tags);
                    return noteAnalysis;
                }))
                .doOnNext(System.out::println)
                .subscribe();

        Thread.sleep(100000);
    }
}
