package com.newtech.note.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtech.note.client.CozeClientV2;
import com.newtech.note.client.PddClient;
import com.newtech.note.client.entity.PddGoodsPromotionUrlGenerateResponse;
import com.newtech.note.client.entity.PddGoodsSearchResponse;
import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.common.enumeration.NoteAnalysisType;
import com.newtech.note.common.enumeration.NoteType;
import com.newtech.note.common.enumeration.ProductPlatform;
import com.newtech.note.entity.dto.NoteAnalysis;
import com.newtech.note.entity.request.*;
import com.newtech.note.entity.vo.CategorizedNote;
import com.newtech.note.entity.vo.NoteProductRecommendation;
import com.newtech.note.entity.vo.NoteRelatedLink;
import com.newtech.note.entity.vo.RelationalInformationVo;
import com.newtech.note.repositories.NoteAnalysisRepository;
import com.newtech.note.service.NoteAnalysisHistoryService;
import com.newtech.note.service.NoteAnalysisService;
import com.newtech.note.service.NoteService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class NoteAnalysisServiceImpl implements NoteAnalysisService {
    private static final Logger logger = LogManager.getLogger(NoteAnalysisServiceImpl.class);

    private final NoteAnalysisRepository noteAnalysisRepository;
    private final NoteService noteService;
    private final NoteAnalysisHistoryService noteAnalysisHistoryService;
    private final DatabaseClient databaseClient;
    private final ObjectMapper objectMapper;
    private final CozeClientV2 cozeClientV2;

    // 提取括号内的数字
    public static int extractNumber(String input) {
        Pattern pattern = Pattern.compile("（(\\d+)）");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            String numberStr = matcher.group(1);
            return Integer.parseInt(numberStr);
        } else {
            return -1;
        }
    }

    private static UpdateNoteAnalysisHistoryRequest buildUpdateNoteAnalysisHistoryRequest(AnalyzeNoteRequest request) {
        UpdateNoteAnalysisHistoryRequest updateHistoryRequest = new UpdateNoteAnalysisHistoryRequest();
        updateHistoryRequest.setNoteAnalysisId(request.getId());
        updateHistoryRequest.setVersion((request.getVersion()));
        updateHistoryRequest.setAnalysisType(NoteAnalysisType.ORGANIZE_WITH_ONE_CLICK.getType());
        return updateHistoryRequest;
    }

    private static NoteProductRecommendation buildNoteProductRecommendation(String productType,
                                                                            PddGoodsSearchResponse.Goods goods,
                                                                            PddGoodsPromotionUrlGenerateResponse.GoodsPromotionUrl goodsPromotionUrl,
                                                                            Map<String, GoodsRecommendation> map) {
        NoteProductRecommendation noteProductRecommendation = new NoteProductRecommendation();
        noteProductRecommendation.setProductId(goods.getGoods_sign());
        noteProductRecommendation.setProductName(goods.getGoods_name());
        noteProductRecommendation.setProductDesc(goods.getGoods_desc());
        noteProductRecommendation.setProductSearchName(productType);
        noteProductRecommendation.setProductImageUrl(goods.getGoods_image_url());
        noteProductRecommendation.setProductSchemaUrl(goodsPromotionUrl.getSchema_url());
        noteProductRecommendation.setProductShortUrl(goodsPromotionUrl.getShort_url());
        // 定义格式
        DecimalFormat df = new DecimalFormat("#.00");
        noteProductRecommendation.setMinGroupPrice(df.format(goods.getMin_group_price() / 100.0));
        noteProductRecommendation.setMinNormalPrice(df.format(goods.getMin_normal_price() / 100.0));
        return noteProductRecommendation;
    }

    /**
     * 从给定的内容中提取信息
     * <p>
     * 该方法主要用于从一段文本中，根据开始标记和结束模式提取出位于两者之间的内容
     * 它使用正则表达式编译给定的开始标记和结束模式，然后在内容中搜索匹配项
     * 如果找到匹配项，则返回匹配项中指定部分的内容，否则返回空字符串
     *
     * @param content     待搜索的文本内容
     * @param startMarker 开始标记，标记提取内容的起始位置
     * @param endPattern  结束模式，标记提取内容的结束位置
     * @return 开始标记和结束模式之间的内容，如果不存在则返回空字符串
     */
    private static String extractInfo(String content, String startMarker, String endPattern) {
        Pattern pattern = Pattern.compile(Pattern.quote(startMarker) + "(.*)" + endPattern, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    @Override
    public Flux<String> organizedNoteText(AnalyzeNoteRequest request, ServerHttpRequest req) {
        final StringBuffer stringBuffer = new StringBuffer();
        BiFunction<AnalyzeNoteRequest, StringBuffer, Flux<String>> analyseNoteByAgent =
                (request1, stringBuffer1) -> noteAnalysisRepository.findById(request1.getId())
                        .flatMapMany(exists -> cozeClientV2.callStreamCozeBotApiFiltered(exists.getDeviceId(), exists.getTitle() + "\n" + exists.getRawNote(), "7379855594448240680", req)
                                .filter(str -> !str.equals("有意义且能理解"))
                                .map(str -> str.replaceAll("- ", "\n- ")))
                        .flatMap(text -> {
                            stringBuffer1.append(text);
                            return Flux.just(text);
                        })
                        .publishOn(Schedulers.boundedElastic())
                        .doOnComplete(() -> {
                            logger.info("Legacy note organization completed");
                            UpdateNoteAnalysisHistoryRequest updateHistoryRequest = new UpdateNoteAnalysisHistoryRequest();
                            updateHistoryRequest.setNoteAnalysisId(request1.getId());
                            updateHistoryRequest.setVersion((request1.getVersion()));
                            updateHistoryRequest.setOrganizedNoteText(stringBuffer1.toString());
                            updateHistoryRequest.setAnalysisType(NoteAnalysisType.ORGANIZE_WITH_ONE_CLICK.getType());
                            noteAnalysisHistoryService.update(updateHistoryRequest).subscribe();
                        });
        return noteAnalysisHistoryService
                .findByNoteAnalysisIdAndVersion(request.getId(), request.getVersion(),
                        NoteAnalysisType.ORGANIZE_WITH_ONE_CLICK.getType())
                .flatMapMany(noteAnalysisHistory -> {
                    // get data from database
                    if (StringUtils.isNotEmpty(noteAnalysisHistory.getOrganizedNoteText())) {
                        return Flux.just(noteAnalysisHistory.getOrganizedNoteText());
                    }
                    return analyseNoteByAgent.apply(request, stringBuffer);
                }).switchIfEmpty(Flux.defer(() -> analyseNoteByAgent.apply(request, stringBuffer)));

    }

    @Override
    public Mono<NoteBaseResponse<List<NoteAnalysis>>> relatedNotes(AnalyzeNoteRequest request, ServerHttpRequest req) {
        Function<AnalyzeNoteRequest, Mono<List<NoteAnalysis>>> analyseNoteRequestByAgent = request1 -> noteAnalysisRepository
                .findById(request1.getId())
                .flatMap(exists ->
                        // 7391514724015341578
                        cozeClientV2.callCozeBotApiFiltered(exists.getDeviceId(), exists.getTitle() + "\n" + exists.getRawNote(), "7394702292080967721", req)
                                .flatMap(str -> {
                                    String[] strArr = str.replaceAll(" ", "").split("\n");
                                    List<String> tagsText = Arrays.stream(strArr).map(String::trim)
                                            .filter(StringUtils::isNotBlank).map(str1 -> {
                                                int i = str1.indexOf("|");
                                                return i == -1 ? str1 : str1.substring(i + 1);
                                            }).collect(Collectors.toList());
                                    String chineseTags = String.join(" ", tagsText);
                                    String hashTagsStr = tagsText.stream().map(String::hashCode).map(Integer::toHexString)
                                            .collect(Collectors.joining(" "));
                                    // String hashTagsStr = PinyinUtil.chineseToPinyin(chineseTags);
                                    // it is better to search the node again because the node may be updated
                                    return noteAnalysisRepository.findById(request1.getId())
                                            .flatMap(existingEntity -> {
                                                existingEntity.setTags(chineseTags);
                                                existingEntity.setHashTags(hashTagsStr);
                                                existingEntity.setUpdatedAt(LocalDateTime.now());
                                                return noteAnalysisRepository.save(existingEntity).flatMapMany(
                                                        ignored -> databaseClient
                                                                // SELECT * FROM products WHERE MATCH (description) AGAINST ('+red
                                                                // +shoes -high' IN BOOLEAN MODE);
                                                                .sql("SELECT * FROM note_analysis WHERE is_deleted = 0 AND device_id = ? AND MATCH(hash_tags) AGAINST ( '"
                                                                        + hashTagsStr + "' IN BOOLEAN MODE)")
                                                                .bind(0, exists.getDeviceId())
                                                                .fetch()
                                                                .all()
                                                                .filter(map -> {
                                                                    String id = map.get("id").toString();
                                                                    return !id.equals(String.valueOf(request1.getId()));
                                                                }) // map == bean 属性=值)
                                                                .map(map ->  // map == bean 属性=值
                                                                        buildNoteAnalysis(exists, map, tagsText)
                                                                )).collectList();
                                            });

                                }))
                .flatMap(list -> {
                    try {
                        String str = objectMapper.writeValueAsString(list.stream().map(NoteAnalysis::getId));
                        UpdateNoteAnalysisHistoryRequest updateHistoryRequest = new UpdateNoteAnalysisHistoryRequest();
                        updateHistoryRequest.setNoteAnalysisId(request1.getId());
                        updateHistoryRequest.setVersion((request1.getVersion()));
                        updateHistoryRequest.setRelatedNotes(str);
                        updateHistoryRequest.setAnalysisType(NoteAnalysisType.ORGANIZE_WITH_ONE_CLICK.getType());
                        return noteAnalysisHistoryService.update(updateHistoryRequest).thenReturn(list);
                    } catch (Exception e) {
                        logger.error("Unable to parse related notes");
                    }
                    return Mono.just(list);
                });

        return noteAnalysisHistoryService
                .findByNoteAnalysisIdAndVersion(request.getId(), request.getVersion(),
                        NoteAnalysisType.ORGANIZE_WITH_ONE_CLICK.getType())
                .<NoteBaseResponse<List<NoteAnalysis>>>flatMap(noteAnalysisHistory -> {
                    // get data from database
                    if (StringUtils.isNotEmpty(noteAnalysisHistory.getRelatedNotes())) {
                        String jsonStr = noteAnalysisHistory.getRelatedNotes();
                        if (noteAnalysisHistory.getRelatedNotes().isBlank()) {
                            return Mono.just(NoteBaseResponse.success(List.of()));
                        }
                        try {
                            List<Long> ls = objectMapper.readValue(jsonStr, new TypeReference<>() {
                            });
                            return noteService.getNotesByIds(ls).collectList().map(NoteBaseResponse::success);
                        } catch (JsonProcessingException e) {
                            logger.error("Unable to parse related notes");
                            return Mono.just(NoteBaseResponse.failure("error occurred while parsing related notes"));
                        }
                    }
                    return analyseNoteRequestByAgent.apply(request).map(NoteBaseResponse::success);
                }).switchIfEmpty(Mono.defer(() -> analyseNoteRequestByAgent.apply(request).map(NoteBaseResponse::success)));

    }

    private NoteAnalysis buildNoteAnalysis(NoteAnalysis exists, Map<String, Object> map, List<String> tagsText) {
        String id = map.get("id").toString();
        String title = Optional.ofNullable(map.get("title"))
                .map(Object::toString).orElse("");
        String rawNote = Optional.ofNullable(map.get("raw_note"))
                .map(Object::toString).orElse("");
        String noteAnalysisContent = Optional
                .ofNullable(map.get("note_analysis_content"))
                .map(Object::toString).orElse("");
        String tagString = Optional.ofNullable(map.get("tags"))
                .map(Object::toString).orElse("");
        String hashTags = Optional.ofNullable(map.get("hash_tags"))
                .map(Object::toString).orElse("");
        Integer noteType = Optional.ofNullable(map.get("note_type"))
                .map(Object::toString).map(Integer::parseInt).orElse(0);
        String imageUrl = Optional.ofNullable(map.get("image_url"))
                .map(Object::toString).orElse("");
        List<String> tags = List.of();
        if (StringUtils.isNotBlank(tagString)) {
            tags = Arrays.stream(tagString.split("\\s+"))
                    .collect(Collectors.toList());
        }
        LocalDateTime createdAt = ZonedDateTime
                .parse(map.get("created_at").toString(),
                        DateTimeFormatter.ISO_DATE_TIME
                                .withZone(ZoneId.of("Asia/Shanghai")))
                .toLocalDateTime();
        LocalDateTime updatedAt = ZonedDateTime
                .parse(map.get("updated_at").toString(),
                        DateTimeFormatter.ISO_DATE_TIME
                                .withZone(ZoneId.of("Asia/Shanghai")))
                .toLocalDateTime();
        List<String> hitTags = (List<String>) CollectionUtils
                .intersection(tags, tagsText);
        NoteAnalysis noteAnalysis = new NoteAnalysis();
        noteAnalysis.setId(Long.parseLong(id));
        noteAnalysis.setDeviceId(exists.getDeviceId());
        noteAnalysis.setNoteType(noteType);
        noteAnalysis.setTitle(title);
        noteAnalysis.setRawNote(rawNote);
        noteAnalysis.setNoteAnalysisContent(noteAnalysisContent);
        noteAnalysis.setHashTags(hashTags);
        noteAnalysis.setCreatedAt(createdAt);
        noteAnalysis.setUpdatedAt(updatedAt);
        noteAnalysis.setHitTags(hitTags);
        noteAnalysis.setTags(tagString);
        noteAnalysis.setTagList(tags);
        noteAnalysis.setDeleted(false);
        noteAnalysis.setDeletedAt(null);
        noteAnalysis.setImageUrl(imageUrl);
        return noteAnalysis;
    }

    private NoteType getNoteType(String str) {
        String[] split = str.replaceAll("-", "").replaceAll(" ", "").split("\n");
        String firstRow = split[0];
        String substring = firstRow.substring(1);
        return NoteType.getType(extractNumber(substring));
    }

    @Override
    public Mono<NoteBaseResponse<List<CategorizedNote>>> categorizedNotes(AnalyzeNoteRequest request, ServerHttpRequest req) {
        Function<AnalyzeNoteRequest, Mono<NoteBaseResponse<List<CategorizedNote>>>> analyseNoteByAgent = request1 -> noteAnalysisRepository
                .findById(request1.getId())
                .flatMap(exists -> cozeClientV2.callCozeBotApiFiltered(exists.getDeviceId(),
                                exists.getTitle() + "\n" + exists.getRawNote(), "7391795335511212043", req)
                        .flatMap(str -> {
                            logger.info("Legacy categorization provider call completed");
                            if ("该类型的信息暂不支持归类".equals(str) || str.length() < 15) {
                                logger.info("categorized note is not supported for note {}", request.getId());
                                return Mono.just(NoteBaseResponse.success("该类型信息不支持归类", List.of()));
                            }
                            String[] categorizedNoteArr = str.split("\\n\\n");
                            return Flux.fromArray(categorizedNoteArr)
                                    .concatMap(categorizedNoteStr -> saveEachCategorizedNote(exists, categorizedNoteStr))
                                    .collectList()
                                    .flatMap(list -> {
                                        try {
                                            String jsonString = objectMapper.writeValueAsString(list);
                                            UpdateNoteAnalysisHistoryRequest updateHistoryRequest = buildUpdateNoteAnalysisHistoryRequest(
                                                    request);
                                            updateHistoryRequest.setCategorizedNotes(jsonString);
                                            updateHistoryRequest.setAnalysisType(NoteAnalysisType.ORGANIZE_WITH_ONE_CLICK.getType());
                                            return noteAnalysisHistoryService.update(updateHistoryRequest)
                                                    .map(ignored -> NoteBaseResponse.success(list));
                                        } catch (JsonProcessingException e) {
                                            logger.error("error occurred while parsing categorized note list, note_analysis id: {} ",
                                                    request.getId(), e);
                                            return Mono.just(NoteBaseResponse.<List<CategorizedNote>>failure("error occurred while parsing categorized note list"));
                                        }

                                    });

                        }));
        return noteAnalysisHistoryService
                .findByNoteAnalysisIdAndVersion(request.getId(), request.getVersion(),
                        NoteAnalysisType.ORGANIZE_WITH_ONE_CLICK.getType())
                .<NoteBaseResponse<List<CategorizedNote>>>flatMap(noteAnalysisHistory -> {
                    // get data from database
                    String jsonStr = noteAnalysisHistory.getCategorizedNotes();
                    if (StringUtils.isNotEmpty(jsonStr)) {
                        if (jsonStr.isBlank()) {
                            return Mono.just(NoteBaseResponse.success(List.of()));
                        }
                        try {
                            List<CategorizedNote> categorizedNotes = objectMapper.readValue(jsonStr, new TypeReference<>() {
                            });
                            return Mono.just(NoteBaseResponse.success(categorizedNotes));
                        } catch (JsonProcessingException e) {
                            logger.error("error occurred while parsing categorized note, noteAnalysisHistory id: {} ",
                                    noteAnalysisHistory.getId(), e);
                            return Mono.just(NoteBaseResponse.failure("error occurred while parsing categorized note"));
                        }
                    }
                    return analyseNoteByAgent.apply(request);
                }).switchIfEmpty(Mono.defer(() -> analyseNoteByAgent.apply(request)));
    }

    private Mono<CategorizedNote> saveEachCategorizedNote(NoteAnalysis exists, String str) {
        NoteType noteType = getNoteType(str);
        if (noteType == null) {
            return Mono.empty();
        }
        // build CategorizedNote
        final CategorizedNote categorizedNote = new CategorizedNote();
        categorizedNote.setNoteType(noteType.getNoteType());
        List<String> lineList = Arrays.stream(str.split("\\\\n"))
                .map(String::trim)
                .map(s -> s.replaceAll("- ", "\n- ")/*.replaceAll("●", "-")*/)
                .filter(s -> !s.endsWith("（空）")).toList();
        categorizedNote.setNoteText(String.join("\n", lineList));

        SearchNoteRequest searchNoteRequest = new SearchNoteRequest();
        searchNoteRequest.setNoteType(noteType.getNoteType());
        searchNoteRequest.setDimension(1);

        List<String> lines = new ArrayList<>(lineList);
        if (!CollectionUtils.isEmpty(lines)) {
            lines.remove(0);
        }
        String noteText = String.join("\n", lines);
        // 找到这个类型的系统维度note，append到该note后面， 如果找不到则新增一条系统维度的note
        return noteService.listNote(exists.getDeviceId(), searchNoteRequest)
                .next()
                .flatMap(note -> noteService.prependToOriginalText(note.getId(), noteText))
                .switchIfEmpty(Mono.defer(() -> {
                    CreateNoteAnalysisRequest createNoteAnalysisRequest = new CreateNoteAnalysisRequest();
                    createNoteAnalysisRequest.setRawNote(noteText);
                    createNoteAnalysisRequest.setTitle(noteType.getNoteTypeName());
                    createNoteAnalysisRequest.setDimension(1);
                    createNoteAnalysisRequest.setNoteType(noteType.getNoteType());
                    return noteService.createNote(exists.getDeviceId(), createNoteAnalysisRequest);
                })).map(noteAnalysis -> {
                    categorizedNote.setNoteAnalysisId(noteAnalysis.getId());
                    return categorizedNote;
                });
    }

    @Override
    public Mono<NoteBaseResponse<List<NoteProductRecommendation>>> productRecommendations(AnalyzeNoteRequest request, ServerHttpRequest req) {
        Function<AnalyzeNoteRequest, Mono<NoteBaseResponse<List<NoteProductRecommendation>>>> analyseNoteByAgent = request1 -> noteAnalysisRepository
                .findById(request1.getId())
                .flatMap(existsOne -> cozeClientV2.callCozeBotApiFiltered(existsOne.getDeviceId(),
                                existsOne.getTitle() + "\n" + existsOne.getRawNote(), "7392167059633471515", req)
                        .flatMap(str -> {
                            List<String> goodsNameList = List.of();
                            try {
                                goodsNameList = objectMapper.readValue(str, new TypeReference<>() {
                                });
                            } catch (Exception e) {
                                logger.error(
                                        "error occurred while parsing product recommendations, noteAnalysisHistory id: {} ",
                                        request1.getId(), e);
                            }
                            return getMockProductRecommendations(existsOne.getDeviceId(), request1, goodsNameList)
                                    .map(NoteBaseResponse::<List<NoteProductRecommendation>>success);
                        }));
        return noteAnalysisHistoryService
                .findByNoteAnalysisIdAndVersion(request.getId(), request.getVersion(),
                        NoteAnalysisType.ORGANIZE_WITH_ONE_CLICK.getType())
                .<NoteBaseResponse<List<NoteProductRecommendation>>>flatMap(noteAnalysisHistory -> {
                    // get data from database
                    String jsonStr = noteAnalysisHistory.getProductRecommendations();
                    if (StringUtils.isNotEmpty(jsonStr)) {
                        if (jsonStr.isBlank()) {
                            return Mono.just(NoteBaseResponse.success(List.of()));
                        }
                        try {
                            List<NoteProductRecommendation> ls = objectMapper.readValue(jsonStr, new TypeReference<>() {
                            });
                            return Mono.just(NoteBaseResponse.success(ls));
                        } catch (JsonProcessingException e) {
                            logger.error(
                                    "error occurred while parsing product recommendation list, note_analysis_history id: {} ",
                                    noteAnalysisHistory.getId(), e);
                            return Mono.just(NoteBaseResponse.failure("error occurred while parsing product recommendation list"));
                        }
                    }
                    return analyseNoteByAgent.apply(request);
                }).switchIfEmpty(Mono.defer(() -> analyseNoteByAgent.apply(request)));
    }

    private Mono<List<NoteProductRecommendation>> getMockProductRecommendations(String deviceId,
                                                                                AnalyzeNoteRequest request,
                                                                                List<String> goodsNameList) {
        return Mono.just(List.of(new NoteProductRecommendation(
                        ProductPlatform.DUO_DUO_KE.getPlatformId(),
                        "1",
                        "保健产品",
                        "四川麦冬无硫麦门冬干货批发非野生真品绵阳麦冬正宗中药材煲汤",
                        "四川麦冬无硫麦门冬干货批发非野生真品绵阳麦冬正宗中药材煲汤",
                        "https://img.pddpic.com/mms-material-img/2021-11-13/1f769f4a-85d0-4869-8ff7-59fcd4eb8115.jpeg.a.jpeg",
                        "",
                        "https://mobile.yangkeduo.com/goods2.html?goods_id=598924543589&share_token=XNbNy3bdwBqM8-gOTghjuUE5YEAUEHN_0MmezBCkbD3W7XMmlwxyJ6i26v-CDklXvvehbYQtSZN2VyeoUDrWOC",
                        "\uD83D\uDC96【养生必备】四川麦冬，品质之选！\uD83C\uDF89\n" +
                                "\uD83C\uDF3F无硫处理，天然健康，让您放心食用。\n" +
                                "\uD83D\uDC40非野生真品，绵阳麦冬，正宗地道。\n" +
                                "\uD83D\uDCAA用来煲汤，营养丰富，滋味鲜美。\n" +
                                "\uD83D\uDC4D是您调养身体，呵护家人健康的好帮手！\n" +
                                "#四川麦冬 #中药材 #养生汤料  赶紧入手吧！\uD83D\uDE1C",
                        "100.00",
                        "110.00"),
                new NoteProductRecommendation(
                        ProductPlatform.DUO_DUO_KE.getPlatformId(),
                        "2",
                        "养肾护肝",
                        "北京同仁堂人参玛咖五宝茶八宝茶熬夜枸杞黄精男人滋补养生茶150g",
                        "北京同仁堂人参玛咖五宝茶八宝茶熬夜枸杞黄精男人滋补养生茶150g",
                        "https://img.pddpic.com/mms-material-img/2024-03-21/6cae27e3-4430-4e6a-ae7c-446ff08dec10.jpeg",
                        "",
                        "https://mobile.yangkeduo.com/goods2.html?goods_id=269622856129&share_token=XNbNy3bdwBqM8-gOTghjuUE5YEAUEHN_0MmezBCkbDJRnRmkOV2E7tQP9UVn_Vmk0inAeqJ9jHuLWRB4moM0po",
                        "\uD83D\uDCAA\uD83C\uDFFB男士们看过来！北京同仁堂人参玛咖五宝茶，专为熬夜的你准备！\uD83D\uDE1C\n" +
                                "\uD83C\uDF3F精选人参、玛咖、枸杞、黄精等优质原料，精心调配成 150g 滋补养生茶。\n" +
                                "\uD83C\uDF75喝一杯，为疲惫的身体注入活力，恢复满满元气！\n" +
                                "\uD83D\uDC96百年同仁堂品牌，品质有保障，值得信赖。\n" +
                                "#养生茶 #男人滋补 #北京同仁堂 快来试试吧！\uD83C\uDF89",
                        "80.00",
                        "85.00")));

    }

    private Mono<List<NoteProductRecommendation>> getNoteProductRecommendations(String deviceId,
                                                                                AnalyzeNoteRequest request,
                                                                                List<String> goodsNameList,
                                                                                ServerHttpRequest req) {
        final List<NoteProductRecommendation> noteProductRecommendationList = Collections
                .synchronizedList(new ArrayList<>());
        PddClient pddClient = PddClient.getInstance();
        return Flux.fromIterable(goodsNameList).flatMap(productType -> pddClient.searchGoods(productType)
                        .flatMapMany(rsp -> {
                            if (!rsp.isRight()) {
                                logger.error("error occurred while searching goods for note analysis {}", request.getId());
                                return Flux.empty();
                            }
                            PddGoodsSearchResponse goodsSearchResponse = rsp.get();
                            String goodsDescList = goodsSearchResponse.getGoods_list().stream()
                                    .map(goods -> {
                                        String goodsSign = goods.getGoods_sign();
                                        String goodsName = goods.getGoods_name();
                                        String goodsDesc = goods.getGoods_desc();
                                        return goodsSign + "|" + goodsName + "|" + goodsDesc;
                                    }).collect(Collectors.joining("\n"));
                            return getRecommendationReason(deviceId, goodsDescList,req)
                                    .flatMapMany(recommendationReasons -> {
                                        Map<String, GoodsRecommendation> map = recommendationReasons.stream().collect(
                                                Collectors.toMap(GoodsRecommendation::getGoodsId, Function.identity()));
                                        return Flux.fromIterable(goodsSearchResponse.getGoods_list())
                                                .flatMap(goods -> pddClient.generateGoodsUrl(goods.getGoods_sign())
                                                        .filter(urlRsp -> urlRsp.isRight()
                                                                && !urlRsp.get().getGoods_promotion_url_list().isEmpty())
                                                        .map(urlRsp -> {
                                                            NoteProductRecommendation noteProductRecommendation = buildNoteProductRecommendation(
                                                                    productType, goods,
                                                                    urlRsp.get().getGoods_promotion_url_list().get(0), map);
                                                            noteProductRecommendationList.add(noteProductRecommendation);
                                                            return noteProductRecommendation;
                                                        }));

                                    });

                        })
                ).publishOn(Schedulers.boundedElastic())
                .doOnComplete(() -> {
                    try {
                        String jsonString = objectMapper.writeValueAsString(noteProductRecommendationList);
                        UpdateNoteAnalysisHistoryRequest updateHistoryRequest = buildUpdateNoteAnalysisHistoryRequest(
                                request);
                        updateHistoryRequest.setProductRecommendations(jsonString);
                        updateHistoryRequest.setAnalysisType(NoteAnalysisType.ORGANIZE_WITH_ONE_CLICK.getType());
                        noteAnalysisHistoryService.update(updateHistoryRequest).subscribe();
                    } catch (JsonProcessingException e) {
                        logger.error("error occurred while parsing product recommendation list, note_analysis id: {} ",
                                request.getId(), e);
                    }
                }).collectList();
    }

    @Override
    public Mono<NoteBaseResponse<List<NoteRelatedLink>>> relatedLinks(AnalyzeNoteRequest request, ServerHttpRequest req) {
        // 流式处理
        Function<AnalyzeNoteRequest, Mono<NoteBaseResponse<List<NoteRelatedLink>>>> analyseNoteByAgent = request1 -> noteAnalysisRepository
                .findById(request1.getId())
                .flatMap(existsOne -> cozeClientV2.callCozeBotApiFiltered(existsOne.getDeviceId(),
                                existsOne.getTitle() + "\n" + existsOne.getRawNote(), "7391734959410642984", req)
                        .flatMap(str -> {
                            try {
                                List<String> textAndLinks = objectMapper.readValue(str, new TypeReference<>() {
                                });
                                return Flux.fromIterable(textAndLinks).flatMap(textAndLinkStr -> {
                                            String[] textAndLink = textAndLinkStr.split("\\|");
                                            if (textAndLink.length < 2) {
                                                return Mono.empty();
                                            }
                                            String link = textAndLink[1].trim();
                                            if (StringUtils.isBlank(link) || (!link.startsWith("http") && !link.startsWith("https"))) {
                                                return Mono.empty();
                                            }
                                            NoteRelatedLink noteRelatedLink = new NoteRelatedLink();
                                            noteRelatedLink.setText(textAndLink[0].trim());
                                            noteRelatedLink.setLink(textAndLink[1].trim());
                                            return Mono.just(noteRelatedLink);
                                        })
                                        .collectList()
                                        .flatMap(noteRelatedLinks -> {
                                            try {
                                                String jsonString = objectMapper.writeValueAsString(noteRelatedLinks);
                                                UpdateNoteAnalysisHistoryRequest updateHistoryRequest = buildUpdateNoteAnalysisHistoryRequest(
                                                        request1);
                                                updateHistoryRequest.setRelatedLinks(jsonString);
                                                return noteAnalysisHistoryService.update(updateHistoryRequest)
                                                        .map(ignore -> NoteBaseResponse.success(noteRelatedLinks));
                                            } catch (Exception e) {
                                                logger.error(
                                                        "error occurred while parsing string from note related link, note_analysis id: {} ",
                                                        request1.getId(), e);
                                                return Mono.just(NoteBaseResponse.<List<NoteRelatedLink>>failure("error occurred while parsing note related link"));
                                            }

                                        });
                            } catch (Exception e) {
                                logger.error("error occurred while parsing text and links, note_analysis id: {} ",
                                        request1.getId(), e);
                                return Mono.just(NoteBaseResponse.<List<NoteRelatedLink>>failure("error occurred while parsing note related link"));
                            }
                        }));
        return noteAnalysisHistoryService
                .findByNoteAnalysisIdAndVersion(request.getId(), request.getVersion(),
                        NoteAnalysisType.ORGANIZE_WITH_ONE_CLICK.getType())
                .<NoteBaseResponse<List<NoteRelatedLink>>>flatMap(noteAnalysisHistory -> {
                    // get data from database
                    String jsonStr = noteAnalysisHistory.getRelatedLinks();
                    if (StringUtils.isNotEmpty(jsonStr)) {
                        if (jsonStr.isBlank()) {
                            return Mono.just(NoteBaseResponse.success(List.of()));
                        }
                        try {
                            List<NoteRelatedLink> ls = objectMapper.readValue(jsonStr, new TypeReference<>() {
                            });
                            return Mono.just(NoteBaseResponse.success(ls));
                        } catch (JsonProcessingException e) {
                            logger.error("error occurred while parsing note related link, note_analysis id: {} ",
                                    request.getId(), e);
                            return Mono.just(NoteBaseResponse.failure("error occurred while parsing note related link"));

                        }
                    }
                    return analyseNoteByAgent.apply(request);
                }).switchIfEmpty(Mono.defer(() -> analyseNoteByAgent.apply(request)));
    }

    @Override
    public Flux<String> assistedContent(AssistContentRequest request, ServerHttpRequest req) {
        AtomicReference<StringBuffer> stringBuffer = new AtomicReference<>(new StringBuffer());
        BiFunction<AssistContentRequest, AtomicReference<StringBuffer>, Flux<String>> analyseNoteByAgent = (request1,
                                                                                                            stringBuffer1) -> noteAnalysisRepository.findById(request1.getId())
                .flatMapMany(exists -> {
                    String title = request.getSelectedTitle();
                    // 如果未选中内容，那么内容取选中的title
                    String content = StringUtils.isBlank(request.getSelectedContent())
                            ? request.getSelectedTitle()
                            : request.getSelectedContent();
                    // 如果选中的title和content都为空，那么取原始note里的title和content
                    if (StringUtils.isBlank(content)) {
                        title = exists.getTitle();
                        content = exists.getRawNote();
                    }
                    String requestBody = "备忘录标题：" +
                            (StringUtils.isBlank(title) ? "（空）" : title) +
                            "备忘录分类：（空）" +
                            "\\|" +
                            "备忘录正文：" +
                            content +
                            "备忘录代办：（空）" +
                            "\\|" +
                            String.join(",", request1.getAssistDirections());
                    return cozeClientV2.callStreamCozeBotApiFiltered(exists.getDeviceId(),
                                    requestBody, "7405604420173529142", req)
                            .map(str -> str.replaceAll("- ", "\n- "));
                })
                .map(text -> {
                    stringBuffer1.get().append(text);
                    return text;
                })
                .publishOn(Schedulers.boundedElastic())
                .doOnComplete(() -> {
                    UpdateNoteAnalysisHistoryRequest updateHistoryRequest = new UpdateNoteAnalysisHistoryRequest();
                    updateHistoryRequest.setNoteAnalysisId(request1.getId());
                    updateHistoryRequest.setVersion((request1.getVersion()));
                    updateHistoryRequest.setAssistedContent(stringBuffer1.get().toString());
                    updateHistoryRequest.setAnalysisType(NoteAnalysisType.ASSIST_CONTENT.getType());
                    noteAnalysisHistoryService.update(updateHistoryRequest).subscribe();
                });
        return noteAnalysisHistoryService
                .findByNoteAnalysisIdAndVersion(request.getId(), request.getVersion(),
                        NoteAnalysisType.ASSIST_CONTENT.getType())
                .flatMapMany(noteAnalysisHistory -> {
                    // get data from database
                    if (StringUtils.isNotEmpty(noteAnalysisHistory.getAssistedContent())) {
                        return Flux.just(noteAnalysisHistory.getAssistedContent());
                    }
                    return analyseNoteByAgent.apply(request, stringBuffer);
                }).switchIfEmpty(Flux.defer(() -> analyseNoteByAgent.apply(request, stringBuffer)));

    }

    @Override
    public Mono<NoteBaseResponse<List<String>>> selectedAssistanceDirection(SelectAssistanceDirectionRequest request, ServerHttpRequest req) {
        return noteAnalysisRepository.findById(request.getId())
                .flatMap(exists -> cozeClientV2.callCozeBotApiFiltered(exists.getDeviceId(),
                                exists.getTitle() + "\n" + exists.getRawNote(), "7406275928462639119", req)
                        .map(str -> {
                            logger.info("selected assistance direction provider call completed for note {}", request.getId());
                            String[] strings = str.split("\n");
                            if (strings[0].startsWith("对不起，您记录的信息我无法理解，故不做任何改写")) {
                                return NoteBaseResponse.failure("对不起，您记录的信息我无法理解，故不做任何改写");
                            } else if (strings.length < 3) {
                                return NoteBaseResponse.failure("服务端数据异常，请联系管理员");
                            } else {
                                return NoteBaseResponse.success(strings[2],
                                        Arrays.stream(strings[1].split("\\|")).map(String::trim).toList());
                            }
                        }));
    }

    @Override
    public Mono<NoteBaseResponse<String>> imageLink(AnalyzeNoteRequest request, ServerHttpRequest req) {
        Function<AnalyzeNoteRequest, Mono<NoteBaseResponse<String>>> analyseNoteByAgent = request1 -> noteAnalysisRepository
                .findById(request1.getId())
                .flatMap(exists -> cozeClientV2.callCozeBotApiFiltered(exists.getDeviceId(),
                                exists.getTitle() + "\n" + exists.getRawNote(), "7390308315718418451", req)
                        .flatMap(str -> {
                            if (!str.startsWith("http") && !str.startsWith("https")) {
                                return Mono.just(NoteBaseResponse.failure(str));
                            }
                            UpdateNoteAnalysisHistoryRequest updateHistoryRequest = buildUpdateNoteAnalysisHistoryRequest(
                                    request);
                            updateHistoryRequest.setImageLink(str);
                            return noteAnalysisHistoryService.update(updateHistoryRequest).map(ignored -> NoteBaseResponse.success(str));
                        }));

        return noteAnalysisHistoryService
                .findByNoteAnalysisIdAndVersion(request.getId(), request.getVersion(),
                        NoteAnalysisType.ORGANIZE_WITH_ONE_CLICK.getType())
                .flatMap(noteAnalysisHistory -> {
                    String imageLink = noteAnalysisHistory.getImageLink();
                    if (StringUtils.isNotBlank(imageLink)) {
                        return Mono.just(NoteBaseResponse.success(imageLink));
                    }
                    return analyseNoteByAgent.apply(request);
                }).switchIfEmpty(Mono.defer(() -> analyseNoteByAgent.apply(request)));
    }

    /**
     * @param request 分析note请求
     * @return 相关信息
     */
    @Override
    public Mono<NoteBaseResponse<RelationalInformationVo>> relationalInformation(AnalyzeNoteRequest request, ServerHttpRequest req) {
        Function<AnalyzeNoteRequest, Mono<NoteBaseResponse<RelationalInformationVo>>> analyseNoteByAgent = (
                request1) -> noteAnalysisRepository.findById(request1.getId())
                .flatMap(exists -> cozeClientV2.callCozeBotApiFiltered(exists.getDeviceId(), exists.getTitle() + "\n" + exists.getRawNote(),
                                "7412629872146825270", req)
                        .flatMap(str -> {
                            JSONObject resObj = JSONUtil.parseObj(str);
                            // 提取“直接相关信息”
                            String relatedInfo = resObj.getStr("直接相关信息", "");
                            // 提取“相关领域信息”
                            String fieldInfo = resObj.getStr("相关领域信息", "");
                            RelationalInformationVo resultVo = new RelationalInformationVo();
                            if (StringUtils.isBlank(str)) {
                                return Mono.just(NoteBaseResponse.failure("AI返回数据为空"));
                            }
                            resultVo.setFieldInfo(fieldInfo);
                            resultVo.setRelatedInfo(relatedInfo);
                            UpdateNoteAnalysisHistoryRequest updateHistoryRequest = buildUpdateNoteAnalysisHistoryRequest(
                                    request);
                            updateHistoryRequest.setRelationalRelatedInfo(resultVo.getRelatedInfo());
                            updateHistoryRequest.setRelationalFieldInfo(resultVo.getFieldInfo());
                            return noteAnalysisHistoryService.update(updateHistoryRequest).map(ignored -> NoteBaseResponse.success(resultVo));
                        }));

        return noteAnalysisHistoryService
                .findByNoteAnalysisIdAndVersion(request.getId(), request.getVersion(),
                        NoteAnalysisType.ORGANIZE_WITH_ONE_CLICK.getType())
                .flatMap(noteAnalysisHistory -> {
                    String relationalRelatedInfo = noteAnalysisHistory.getRelationalRelatedInfo();
                    String relationalFieldInfo = noteAnalysisHistory.getRelationalFieldInfo();
                    if (StringUtils.isNotBlank(relationalRelatedInfo) && StringUtils.isNotBlank(relationalFieldInfo)) {
                        return Mono.just(NoteBaseResponse.success(new RelationalInformationVo(relationalRelatedInfo, relationalFieldInfo)));
                    }
                    return analyseNoteByAgent.apply(request);
                }).switchIfEmpty(Mono.defer(() -> analyseNoteByAgent.apply(request)));
    }

    private Mono<List<GoodsRecommendation>> getRecommendationReason(String deviceId, String goodsDesc, ServerHttpRequest req) {
        return cozeClientV2.callCozeBotApiFiltered(deviceId, goodsDesc, "7392167059633471515", req)
                .map(str -> {
                    List<GoodsRecommendation> list = List.of();
                    try {
                        list = objectMapper.readValue(str, new TypeReference<>() {
                        });
                    } catch (Exception e) {
                        logger.error("error occurred while parsing recommendation reason", e);
                    }
                    return list;
                });
    }


    @Setter
    @Getter
    @NoArgsConstructor
    static class GoodsRecommendation {
        String goodsId;
        String goodsRecommendation;
    }
}
