package com.newtech.note.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import com.newtech.note.client.BailianClient;
import com.newtech.note.client.entity.bailian.BailianResponse;
import com.newtech.note.client.entity.bailian.BailianResponseData;
import com.newtech.note.entity.dto.NoteAnalysis;
import com.newtech.note.entity.filter.NoteAnalysisFilter;
import com.newtech.note.entity.request.CreateNoteAnalysisRequest;
import com.newtech.note.entity.request.SearchNoteRequest;
import com.newtech.note.entity.request.UpdateNoteAnalysisRequest;
import com.newtech.note.repositories.NoteAnalysisRepository;
import com.newtech.note.service.FileUploadService;
import com.newtech.note.service.NoteAnalysisHistoryService;
import com.newtech.note.service.NoteService;
import com.newtech.note.util.SnowflakeIdGenerator;
import io.micrometer.common.util.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NoteServiceImpl implements NoteService {
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Logger logger = LogManager.getLogger(NoteServiceImpl.class);

    private final NoteAnalysisRepository noteAnalysisRepository;

    private final NoteAnalysisHistoryService noteAnalysisHistoryService;

    private final SnowflakeIdGenerator snowflakeIdGenerator;

    private final FileUploadService fileUploadService;

    private final ObjectMapper objectMapper;

    public NoteServiceImpl(NoteAnalysisRepository noteAnalysisRepository,
                           NoteAnalysisHistoryService noteAnalysisHistoryService, FileUploadService fileUploadService, ObjectMapper objectMapper) {
        this.noteAnalysisRepository = noteAnalysisRepository;
        this.noteAnalysisHistoryService = noteAnalysisHistoryService;
        this.fileUploadService = fileUploadService;
        this.objectMapper = objectMapper;
        this.snowflakeIdGenerator = SnowflakeIdGenerator.getInstance();
    }

    private record ImageNoteDigest(String title, String content) {

    }

    private static String trimString(String content) {
        return content.replaceAll("\\\\n", "").replaceAll("\\\\", "").trim();
    }

    private static ImageNoteDigest extractImageNoteDigest(String note) {
        if (note.substring(0, 20).startsWith("【备忘录标题】")) {
            // 寻找标题部分的起始位置和结束位置
            String titleStart = "【备忘录标题】";
            String bodyStart = "【优化版正文】";
            // 获取标题
            int titleStartIndex = note.indexOf(titleStart) + titleStart.length();
            int bodyStartIndex = note.indexOf(bodyStart);
            String title = note.substring(titleStartIndex, bodyStartIndex);
            // 获取正文
            int bodyContentStartIndex = bodyStartIndex + bodyStart.length();
            String body = note.substring(bodyContentStartIndex, note.length() - 1);
            return new ImageNoteDigest(trimString(title), trimString(body));
        }
        int firstHashIndex = note.indexOf("###");
        int secondHashIndex = note.indexOf("###", firstHashIndex + 3);
        if (firstHashIndex == -1 || secondHashIndex == -1) {
            return new ImageNoteDigest("", note);
        }
        String memoSection = note.substring(firstHashIndex, secondHashIndex).trim();
        // 替换“备忘录标题”
        String title = memoSection.replaceAll("###", "").replace("\\n", "").replaceAll("\\\\", "").replaceAll("备忘录标题", "").trim();
        return new ImageNoteDigest(title, note);
    }

    private static NoteAnalysisFilter buildFilter(String deviceId, SearchNoteRequest request) {
        NoteAnalysisFilter noteAnalysisFilter = request.buildFilter(deviceId);
        if (StringUtils.isNotBlank(request.getKeyword())) {
            List<Term> termList = HanLP.segment(request.getKeyword());
            String str = termList.stream().map(term -> term.word).filter(StringUtils::isNotBlank).map(s -> s + "0").collect(Collectors.joining(" "));
            noteAnalysisFilter.setKeyword(str);
        }
        return noteAnalysisFilter;
    }

    private static void setNoteContentText(String title, String content, NoteAnalysis exists) {
        String title1 = StringUtils.isBlank(title) ? title : "";
        String content1 = StringUtils.isBlank(content) ? content : "";
        List<Term> termList = HanLP.segment(title1 + "\n" + content1);
        String str = termList.stream().map(term -> term.word).filter(StringUtils::isNotBlank).map(s -> s + "0").collect(Collectors.joining(" "));
        exists.setNoteContentText(str);
    }

    public static void main(String[] args) throws Exception {

        String str = "{\n" +
                "\t\"output\": {\n" +
                "\t\t\"finish_reason\": \"stop\",\n" +
                "\t\t\"session_id\": \"377022e44afa4863b0e10f7586a47b2b\",\n" +
                "\t\t\"text\": \"{\\\"wechat_img_ai_stream\\\":{\\\"to_do_list\\\":[{\\\"task\\\":\\\"和刘双广约定T\\\",\\\"time\\\":\\\"2024-10-16 19:00:00\\\"}],\\\"note_list\\\":[\\\"刘家俊（匿名）的背景信息：华东交通大学软件工程与机械电子工程双学士，精通Java和Spring，擅长微服务架构与高并发系统开发。在杭州刻睿科技有限公司负责Gig术问题并提升系统性能。\\\"],\\\"chat_history\\\":[{\\\"sender\\\":\\\"刘双广\\\",\\\"message\\\":\\\"刘家俊（匿名），华东交通大学软件工程与机械电子工程双学士，精通Java和Spring全家桶，擅长微服务架构与高并发系统的开发。在杭州技术创新。此前在电子发票和营销自动化平台方面也有丰富经验，善于解决复杂技术问题并提升系统性能。\\\"},{\\\"sender\\\":\\\"我\\\",\\\"message\\\":\\\"哪天我们约下时间过下ppt呢\\\"},{\\\"sender\\\":\\\"刘双广\\\",\\\"message\\\":\\\"刚开始\\\"sender\\\":\\\"刘双广\\\",\\\"message\\\":\\\"优势/市场分析预测还没思路呢\\\"},{\\\"sender\\\":\\\"刘双广\\\",\\\"message\\\":\\\"下周一晚上\\\"}],\\\"note_md\\\":\\\"# 备忘录标题\\\\n刘家俊背景及与刘双广PPT审查预约\\\\n\\\\n## 优化版正文\\\\n\\\\士。\\\\n- **技术专长**：精通Java、Spring全家桶，专于微服务架构与高并发系统开发。\\\\n- **工作经验**：\\\\n  - **杭州刻睿科技**：负责核心项目如Gig系统、邮件模板系统，促进视频内容自动生成及邮件自动化的技术创新。广约定于**下周一晚上**审查PPT，具体时间需再次确认。\\\\n\\\\n### 待解决问题\\\\n- **优势/市场分析预测**：目前缺乏清晰思路，需进一步探讨。\\\\n\\\\n## 标签\\\\n刘家俊；华东交通大学；Java；Spring；微服务架构；Gig系统；同审查PPT的时间，确保项目进展顺利。同时，标记关于市场分析预测部分的讨论需求。\\\"}}\"\n" +
                "\t},\n" +
                "\t\"usage\": {},\n" +
                "\t\"request_id\": \"a92dcd6f-561e-900d-88ce-79e7016de05c\"\n" +
                "}";
        BailianResponse bailianResponse = new ObjectMapper().readValue(str, BailianResponse.class);
        System.out.println(bailianResponse.output().text());

        System.out.println("----------------------------------------------");


        BailianResponseData data = new ObjectMapper().readValue(bailianResponse.output().text(), BailianResponseData.class);
        System.out.println(data.wechatImgAIStream().note());
        /*String note = "【备忘录标题】\\\\n图片识别功能与后端测试问题汇总\\\\n\\\\n【优化版正文】\\\\n### 对话摘要\\\\n- **问题讨论**：近期测试中遇到两个主要问题：  \\\\n  1.成功，应用未按预期跳转至详情页。\\\\n  2. **图片识别功能缺失**：已启用图片识别功能，但实际操作中未见识别悬浮窗出现。\\\\n\\\\n### 待办事项\\\\n- **重新测试后端**\\\\n  - **任务描述**：重新执行后端测试流程，确保所有；功能调试；跳转问题；悬浮窗\\\\n\\\\n【备忘录分类】\\\\n工作解决方案\\\\n\\\\n【背景信息】\\\\n本次备忘内容基于技术交流与功能测试的对话记录，涉及移动应用开发中后端响应处理与新增功能（图片识别）的实施状况分析。\\\\n\\\\用户体验。";

        ImageNoteDigest imageNoteDigest = extractImageNoteDigest(note);
        System.out.println(imageNoteDigest.title());
        System.out.println(imageNoteDigest.content());*/

    }

    @Override
    public Mono<NoteAnalysis> createImageNote(String deviceId, FilePart filePart) {
        logger.info("Creating image note");
        HttpHeaders headers = filePart.headers();
        String contentType = headers.getFirst(HttpHeaders.CONTENT_TYPE);
        if (contentType == null || !contentType.startsWith("image/")) {
            return Mono.error(new IllegalArgumentException("Only image file can be uploaded."));
        }
        return fileUploadService.compressImage(filePart)
                .flatMap(uploadResult -> {
                    if (uploadResult.isFailure()) {
                        return Mono.error(new RuntimeException("File upload failed."));
                    }
                    String filePath = uploadResult.getData();
                    //String filePath = "http://aifunc.top/upload-files/216f7fc2-fcee-4f03-bb5a-865c63f4807d.jpg";
                    return BailianClient.getInstance()
                            .callAppFiltered(
                                    "2d02940ad45c49499ab732df94b249c2",
                                    Map.of("url", filePath))
                            .flatMap(response -> {
                                try {
                                    BailianResponseData data = objectMapper.readValue(response, BailianResponseData.class);
                                    BailianResponseData.WechatImgAIStream wechatImgAIStream = data.wechatImgAIStream();
                                    if (wechatImgAIStream == null ||
                                            (wechatImgAIStream.todoList().isEmpty() &&
                                                    wechatImgAIStream.noteList().isEmpty() &&
                                                    wechatImgAIStream.chatHistory().isEmpty() /*&&
                                                    StringUtils.isBlank(wechatImgAIStream.note())*/)) {
                                        logger.warn("Image-note provider response was empty");
                                        return Mono.empty();
                                    }
                                    NoteAnalysis noteAnalysis = new NoteAnalysis();
                                    noteAnalysis.setId(snowflakeIdGenerator.nextId(NoteAnalysis.class));
                                    noteAnalysis.setDeviceId(deviceId);
                                    noteAnalysis.setImageUrl(filePath);
                                    noteAnalysis.setTitle("");
                                    if (StringUtils.isNotBlank(wechatImgAIStream.note())) {
                                        ImageNoteDigest imageNoteDigest = extractImageNoteDigest(wechatImgAIStream.note());
                                        noteAnalysis.setTitle(imageNoteDigest.title());
                                        noteAnalysis.setRawNote(imageNoteDigest.content());
                                        noteAnalysis.setNoteAnalysisContent(imageNoteDigest.content());
                                        setNoteContentText(imageNoteDigest.title(), noteAnalysis.getRawNote(), noteAnalysis);
                                    } else {
                                        logger.warn("Image-note provider response contained no notes");
                                        noteAnalysis.setRawNote("");
                                        noteAnalysis.setNoteAnalysisContent("");
                                        noteAnalysis.setNoteContentText("");
                                    }
                                    // String content = data.output_ocr();
                                    noteAnalysis.setDimension(0);
                                    noteAnalysis.setNoteType(0);
                                    noteAnalysis.setCreatedAt(LocalDateTime.now());
                                    noteAnalysis.setUpdatedAt(LocalDateTime.now());
                                    return noteAnalysisRepository.insert(noteAnalysis);
                                } catch (JsonProcessingException e) {
                                    logger.error("Unable to parse image-note provider response");
                                    return Mono.error(new RuntimeException("Error occurred while parsing coze image note response."));
                                }
                            });
                });

    }

    @Override
    public Mono<NoteAnalysis> createNote(String deviceId, CreateNoteAnalysisRequest request) {
        logger.info("Creating note");
        if (StringUtils.isBlank(request.getRawNote()) && StringUtils.isBlank(request.getTitle())) {
            return Mono.error(new IllegalArgumentException("Note content and title cannot be empty at the same time."));
            //return Mono.empty();
        }
        //fixme 待删除 去除换行符和空格后判断是否为空，这是为了兼容旧版前端传入的数据。
        String content = request.getRawNote().replaceAll("\n", "").replaceAll(" ", "");
        if (content.equals("标题：正文：")) {
            return Mono.error(new IllegalArgumentException("Note content and title cannot be empty at the same time."));
            //return Mono.empty();
        }
        NoteAnalysis noteAnalysis = new NoteAnalysis();
        noteAnalysis.setId(snowflakeIdGenerator.nextId(NoteAnalysis.class));
        noteAnalysis.setDeviceId(deviceId);
        noteAnalysis.setTitle(StringUtils.isEmpty(request.getTitle()) ? "" : request.getTitle());
        noteAnalysis.setRawNote(request.getRawNote());
        noteAnalysis.setNoteAnalysisContent(request.getRawNote());
        noteAnalysis.setDimension(request.getDimension());
        noteAnalysis.setNoteType(request.getNoteType() == null ? 0 : request.getNoteType());
        setNoteContentText(request.getTitle(), request.getRawNote(), noteAnalysis);
        noteAnalysis.setCreatedAt(LocalDateTime.now());
        noteAnalysis.setUpdatedAt(LocalDateTime.now());
        return noteAnalysisRepository.insert(noteAnalysis);
        //.flatMap(inserted -> noteAnalysisRepository.findById(inserted.getId()));
    }

    @Override
    public Mono<NoteAnalysis> appendToOriginalText(long id, String toAppendText) {
        return noteAnalysisRepository.findById(id)
                .flatMap(exists -> {
                    if (StringUtils.isBlank(toAppendText)) {
                        return Mono.just(exists);
                    }
                    exists.setNoteAnalysisContent(exists.getNoteAnalysisContent() + "\n" + toAppendText);
                    List<Term> termList = HanLP.segment(toAppendText);
                    String str = termList.stream().map(term -> term.word).filter(StringUtils::isNotBlank).map(s -> s + "0").collect(Collectors.joining(" "));
                    exists.setNoteContentText(exists.getNoteContentText() + "\n" + str);
                    exists.setUpdatedAt(LocalDateTime.now());
                    return noteAnalysisRepository.save(exists);
                });
    }

    @Override
    public Mono<NoteAnalysis> prependToOriginalText(long id, String toAppendText) {
        return noteAnalysisRepository.findById(id)
                .flatMap(exists -> {
                    if (StringUtils.isBlank(toAppendText)) {
                        return Mono.just(exists);
                    }
                    exists.setNoteAnalysisContent(toAppendText + "\n---\n" + exists.getNoteAnalysisContent());
                    List<Term> termList = HanLP.segment(toAppendText);
                    String str = termList.stream().map(term -> term.word).filter(StringUtils::isNotBlank).map(s -> s + "0").collect(Collectors.joining(" "));
                    exists.setNoteContentText(str + "\n" + exists.getNoteContentText());
                    exists.setUpdatedAt(LocalDateTime.now());

                    return noteAnalysisRepository.save(exists);
                });
    }

    @Override
    public Mono<NoteAnalysis> getNoteById(String deviceId, long id) {
        return noteAnalysisRepository.findById(id)
                .filter(note -> note.getDeviceId().equals(deviceId))
                .zipWith(noteAnalysisHistoryService.allAnalysisHistories(id)
                                .filter(history -> !history.isEmpty())
                                .collectList(),
                        (note, historyList) -> {
                            note.setNoteAnalysisHistories(historyList);
                            return note;
                        });
    }

    @Override
    public Flux<NoteAnalysis> listNote(String deviceId, SearchNoteRequest request) {
        logger.info("Listing notes");
        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getSize(), request.isAscending() ? Sort.by(request.getSortBy()).ascending() : Sort.by(request.getSortBy()).descending());
        NoteAnalysisFilter noteAnalysisFilter = buildFilter(deviceId, request);
        return noteAnalysisRepository.findByDynamicCriteria(noteAnalysisFilter, pageable);
    }

    @Override
    public Mono<NoteAnalysis> updateNote(long id, UpdateNoteAnalysisRequest request) {
        return noteAnalysisRepository.findById(id)
                .flatMap(exists -> {
                    if (request.getTitle() == null
                            && request.getRawNote() == null
                            && request.getNoteType() == null) {
                        return Mono.just(exists);
                    }
                    if (request.getTitle() != null) {
                        exists.setTitle(request.getTitle());
                    }
                    if (request.getRawNote() != null) {
                        exists.setRawNote(request.getRawNote());
                        exists.setNoteAnalysisContent(request.getRawNote());
                    }
                    if (request.getNoteType() != null) {
                        exists.setNoteType(request.getNoteType());
                    }
                    if (request.getTalkSnapshot() != null) {
                        exists.setTalkSnapshot(request.getTalkSnapshot().toString());
                    }
                    setNoteContentText(request.getTitle(), request.getRawNote(), exists);
                    exists.setUpdatedAt(LocalDateTime.now());
                    return noteAnalysisRepository.save(exists);
                });
    }

    @Override
    public Mono<NoteAnalysis> deleteNote(long id) {
        return noteAnalysisRepository.findById(id)
                .flatMap(exists -> {
                    exists.setDeleted(true);
                    exists.setDeletedAt(LocalDateTime.now());
                    return noteAnalysisRepository.save(exists);
                });
    }

    @Override
    public Mono<Long> count(String deviceId, SearchNoteRequest request) {
        NoteAnalysisFilter noteAnalysisFilter = buildFilter(deviceId, request);
        return noteAnalysisRepository.countByDynamicCriteria(noteAnalysisFilter);
    }

    @Override
    public Flux<NoteAnalysis> getNotesByIds(List<Long> ids) {
        return noteAnalysisRepository.findAllById(ids);
    }


}
