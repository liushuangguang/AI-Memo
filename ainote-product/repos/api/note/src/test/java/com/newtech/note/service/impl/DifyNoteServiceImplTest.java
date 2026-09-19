package com.newtech.note.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.newtech.note.client.DeepSeekClient;
import com.newtech.note.client.DeepSeekCompletion;
import com.newtech.note.client.DifyClient;
import com.newtech.note.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.function.Supplier;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

class DifyNoteServiceImplTest {
    private DifyClient difyClient;
    private DeepSeekClient deepSeekClient;
    private DifyNoteServiceImpl service;

    @BeforeEach
    void setUp() {
        difyClient = mock(DifyClient.class);
        deepSeekClient = mock(DeepSeekClient.class);
        service = new DifyNoteServiceImpl(
                mock(SearchService.class), new ObjectMapper().registerModule(new JavaTimeModule()),
                difyClient, deepSeekClient);
        ReflectionTestUtils.setField(service, "apiKeyForOrganizeNote", "");
        ReflectionTestUtils.setField(service, "apiKeyForIsMakeSense", "");
        ReflectionTestUtils.setField(service, "apiKeyForAiSuggestion", "");
        ReflectionTestUtils.setField(service, "apiKeyForRelatedTitle", "");
        ReflectionTestUtils.setField(service, "apiKeyForRewriteContent", "");
        ReflectionTestUtils.setField(service, "apiKeyForCompleteInfo", "");
    }

    @Test
    void providerResponsePreservesOrganizedDtoContract() {
        when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                anyString(), any(), eq("text"), any(), any()))
                .thenReturn(Mono.just("""
                        {"title":"买菜","user_scenario":"日常购物","body_text":"买西红柿",
                        "Memo_classification":"日常购物","todo":[],"tag":["采购"]}
                        """));

        StepVerifier.create(service.organizeNote("买西红柿", null))
                .assertNext(note -> {
                    assertThat(note.getTitle()).isEqualTo("买菜");
                    assertThat(note.getMemoClassification()).isEqualTo("日常购物");
                    assertThat(note.getTodoList()).isEmpty();
                })
                .verifyComplete();
        verify(difyClient).callDifyBlockingWorkflowApiWithFallback(
                anyString(), argThat(parameters -> parameters.containsKey("note")
                        && !parameters.containsKey("inp")), eq("text"),
                nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any());
    }

    @Test
    void normalizesMinutePrecisionOrganizedTodoTime() {
        when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                anyString(), any(), eq("text"), any(), any()))
                .thenReturn(Mono.just("""
                        {"title":"安排","user_scenario":"工作","body_text":"正文",
                        "Memo_classification":"其他","todo":[{"description":"会议","time":"09-04 21:00"}],"tag":[]}
                        """));

        StepVerifier.create(service.organizeNote("会议", null))
                .assertNext(note -> assertThat(note.getTodoList().get(0).getScheduledAt())
                        .hasYear(LocalDate.now(ZoneId.of("Asia/Shanghai")).getYear())
                        .hasMonthValue(9)
                        .hasDayOfMonth(4)
                        .hasHour(21)
                        .hasMinute(0)
                        .hasSecond(0))
                .verifyComplete();
    }

    @Test
    void rejectsInvalidOrganizedTodoTimesWhenEveryOtherFieldIsValid() {
        for (String invalidTime : new String[]{"周六上午", "2026-02-30 10:00"}) {
            when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                    anyString(), any(), eq("text"), any(), any()))
                    .thenReturn(Mono.just("""
                            {"title":"安排","user_scenario":"工作","body_text":"正文",
                             "Memo_classification":"其他","todo":[{"description":"会议","time":"%s"}],
                             "tag":["会议"]}
                            """.formatted(invalidTime)));

            StepVerifier.create(service.organizeNote("会议", null))
                    .expectErrorSatisfies(error -> assertThat(error)
                            .hasMessageContaining("invalid output for note organization"))
                    .verify();
        }
    }

    @Test
    void successfulButMalformedDifyOutputDoesNotLeakToFallbackProvider() {
        when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                anyString(), any(), anyString(), any(), any()))
                .thenReturn(Mono.just("not-json"));

        StepVerifier.create(service.isNoteMakeSense("正常笔记", null))
                .expectErrorSatisfies(error ->
                        assertThat(error).hasMessageContaining("invalid output"))
                .verify();

        org.mockito.Mockito.verifyNoInteractions(deepSeekClient);
    }

    @Test
    void normalizesFencedSuggestionJsonAndLightAliases() throws Exception {
        when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                anyString(), any(), eq("suggestion"), nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any()))
                .thenReturn(Mono.just("""
                        ```json
                        {"overallSuggestions":[{"suggestion":"A","emoji":"☀️"}],"overall_todos":[],
                         "specific_suggestions":[{"Suggestion":"B"}],
                         "specificTodoItems":[{"task":"C","description":"N","time":null}]}
                        ```
                        """));

        String normalized = service.aiSuggestion("笔记", null).single().block();
        var json = new ObjectMapper().readTree(normalized);
        assertThat(json.fieldNames()).toIterable().containsExactly(
                "overall_Suggestions", "overall_ToDo_Items",
                "specific_Suggestions", "specific_ToDo_Items");
        assertThat(json.path("overall_Suggestions").isArray()).isTrue();
        assertThat(json.path("specific_ToDo_Items").isArray()).isTrue();
    }

    @Test
    void rejectsPlainTextSuggestionAsInvalidProviderOutput() {
        when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                anyString(), any(), eq("suggestion"), nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any()))
                .thenReturn(Mono.just("这是一段普通建议"));

        StepVerifier.create(service.aiSuggestion("笔记", null))
                .expectErrorSatisfies(error -> assertThat(error)
                        .hasMessageContaining("invalid output for AI suggestion"))
                .verify();
    }

    @Test
    void rejectsArbitraryStringArrayItems() {
        when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                anyString(), any(), eq("suggestion"), nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any()))
                .thenReturn(Mono.just("""
                        {"overall_Suggestions":["not-an-object"],"overall_ToDo_Items":[],
                         "specific_Suggestions":[],"specific_ToDo_Items":[]}
                        """));

        StepVerifier.create(service.aiSuggestion("笔记", null))
                .expectErrorSatisfies(error -> assertThat(error).hasMessageContaining("invalid output"))
                .verify();
    }

    @Test
    @SuppressWarnings("unchecked")
    void deepSeekFallbackUsesJsonContractAndIsNormalized() {
        when(deepSeekClient.completeJsonWithUsage(anyString(), anyString()))
                .thenReturn(Mono.just(new DeepSeekCompletion("""
                        {"overall_Suggestions":[],
                         "overall_ToDo_Items":[{"ToDo_Content":"T","Todo_notes":"","Time":null}],
                         "specific_Suggestions":[],"specific_ToDo_Items":[]}
                        """, 12)));
        when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                anyString(), any(), eq("suggestion"), nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any()))
                .thenAnswer(invocation -> {
                    Supplier<Mono<DeepSeekCompletion>> fallback = invocation.getArgument(4);
                    return fallback.get().map(DeepSeekCompletion::content);
                });

        StepVerifier.create(service.aiSuggestion("笔记", null))
                .assertNext(json -> assertThat(json).contains("\"Todo_Content\":\"T\""))
                .verifyComplete();

        verify(deepSeekClient).completeJsonWithUsage(anyString(), argThat(prompt ->
                prompt.contains("Suggestion 和 Todo_Content 必须为非空文本")
                        && prompt.contains("不要输出该条目")));
        verify(deepSeekClient, never()).completeTextWithUsage(anyString(), anyString());
    }

    @Test
    void skipsSuggestionItemsMissingRequiredTextWhenAnotherItemIsValid() throws Exception {
        when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                anyString(), any(), eq("suggestion"), nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any()))
                .thenReturn(Mono.just("""
                        {"overall_Suggestions":[{}, {"Suggestion":"保留"}],
                         "overall_ToDo_Items":[{"ToDo_Content":"   "}, {"ToDo_Content":"待办","Time":null}],
                         "specific_Suggestions":[],"specific_ToDo_Items":[]}
                        """));

        JsonNode json = new ObjectMapper().readTree(service.aiSuggestion("笔记", null).single().block());
        assertThat(json.path("overall_Suggestions")).hasSize(1);
        assertThat(json.path("overall_Suggestions").get(0).path("Suggestion").asText()).isEqualTo("保留");
        assertThat(json.path("overall_ToDo_Items")).hasSize(1);
        assertThat(json.path("overall_ToDo_Items").get(0).path("Todo_Content").asText()).isEqualTo("待办");
    }

    @Test
    void rejectsNonEmptySuggestionArrayWhenEveryRequiredTextIsMissing() {
        when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                anyString(), any(), eq("suggestion"), nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any()))
                .thenReturn(Mono.just("""
                        {"overall_Suggestions":[{}, {"Suggestion":"  "}],
                         "overall_ToDo_Items":[],"specific_Suggestions":[],"specific_ToDo_Items":[]}
                        """));

        StepVerifier.create(service.aiSuggestion("笔记", null))
                .expectErrorSatisfies(error -> assertThat(error)
                        .hasMessageContaining("invalid output for AI suggestion"))
                .verify();
    }

    @Test
    void rejectsSuggestionOutputWhenAnyLogicalArrayIsMissingOrNullOrNotAnArray() {
        for (String arrays : new String[]{
                "\"overall_ToDo_Items\":[],\"specific_Suggestions\":[],\"specific_ToDo_Items\":[]",
                "\"overall_Suggestions\":null,\"overall_ToDo_Items\":[],\"specific_Suggestions\":[],\"specific_ToDo_Items\":[]",
                "\"overall_Suggestions\":{},\"overall_ToDo_Items\":[],\"specific_Suggestions\":[],\"specific_ToDo_Items\":[]"}) {
            when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                    anyString(), any(), eq("suggestion"), nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any()))
                    .thenReturn(Mono.just("{" + arrays + "}"));

            StepVerifier.create(service.aiSuggestion("笔记", null))
                    .expectErrorSatisfies(error -> assertThat(error)
                            .hasMessageContaining("invalid output for AI suggestion"))
                    .verify();
        }
    }

    @Test
    void acceptsMixedAliasesAndSkipsOnlyMissingRequiredItems() throws Exception {
        when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                anyString(), any(), eq("suggestion"), nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any()))
                .thenReturn(Mono.just("""
                        {"overallSuggestions":[{"suggestion":"建议"},{}],
                         "overallTodos":[{"task":"待办"}],
                         "specificSuggestion":[{"Suggestion":"具体"}],
                         "specificTodoItem":[{"content":"具体待办"}]}
                        """));

        JsonNode json = new ObjectMapper().readTree(service.aiSuggestion("笔记", null).single().block());
        assertThat(json.path("overall_Suggestions")).hasSize(1);
        assertThat(json.path("overall_ToDo_Items")).hasSize(1);
        assertThat(json.path("specific_Suggestions")).hasSize(1);
        assertThat(json.path("specific_ToDo_Items")).hasSize(1);
    }

    @Test
    void legacySuggestionSkipsMixedMissingTodoButRejectsAllMissingAndInvalidRequiredType() {
        for (String todo : new String[]{
                "[{\"description\":null},{\"description\":\"保留\",\"time\":null}]",
                "[{\"description\":\"   \"}]",
                "[{\"description\":123}]"}) {
            when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                    anyString(), any(), eq("suggestion"), nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any()))
                    .thenReturn(Mono.just("{\"information\":\"建议\",\"todo\":" + todo + "}"));

            if (todo.contains("保留")) {
                StepVerifier.create(service.aiSuggestion("笔记", null))
                        .assertNext(json -> assertThat(json).contains("保留"))
                        .verifyComplete();
            } else {
                StepVerifier.create(service.aiSuggestion("笔记", null))
                        .expectErrorSatisfies(error -> assertThat(error)
                                .hasMessageContaining("invalid output for AI suggestion"))
                        .verify();
            }
        }
    }

    @Test
    void adaptsRealLegacySuggestionFixtureToStrictMobileSchema() throws Exception {
        when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                anyString(), any(), eq("suggestion"), nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any()))
                .thenReturn(Mono.just("""
                        ```json
                        {"information":"🌟 先明确目标\\n\\n- 拆分为可执行步骤",
                         "todo":[{"description":"完成第一步","time":null}]}
                        ```
                        """));

        JsonNode json = new ObjectMapper().readTree(service.aiSuggestion("笔记", null).single().block());
        assertThat(json.path("overall_Suggestions")).hasSize(2);
        assertThat(json.path("overall_Suggestions").get(0).path("Suggestion").asText())
                .isEqualTo("先明确目标");
        assertThat(json.path("overall_ToDo_Items").get(0).path("Todo_Content").asText())
                .isEqualTo("完成第一步");
        assertThat(json.path("specific_Suggestions")).isEmpty();
        assertThat(json.path("specific_ToDo_Items")).isEmpty();
        verify(difyClient).callDifyBlockingWorkflowApiWithFallback(
                anyString(), argThat(parameters -> parameters.containsKey("note")
                        && !parameters.containsKey("inp")), eq("suggestion"),
                nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any());
    }

    @Test
    void rejectsOrganizedOutputWithMissingRequiredFieldOrUnknownClassification() {
        when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                anyString(), any(), eq("text"), any(), any()))
                .thenReturn(Mono.just("""
                        {"title":"标题","user_scenario":"场景","body_text":"正文",
                        "Memo_classification":"未知分类","todo":[],"tag":[]}
                        """));

        StepVerifier.create(service.organizeNote("笔记", null))
                .expectErrorSatisfies(error -> assertThat(error)
                        .hasMessageContaining("invalid output for note organization"))
                .verify();
    }

    @Test
    void normalizesSuggestionTodoTimeAndCanonicalizesSpecificTodoContent() throws Exception {
        when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                anyString(), any(), eq("suggestion"), nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any()))
                .thenReturn(Mono.just("""
                        {"overall_Suggestions":[],"overall_ToDo_Items":[{"ToDo_Content":"总待办","Time":"09-04 21:00"}],
                        "specific_Suggestions":[],"specific_ToDo_Items":[{"Todo_Content":"具体待办","Time":"2026-09-05 08:30"}]}
                        """));

        JsonNode json = new ObjectMapper().readTree(service.aiSuggestion("笔记", null).single().block());
        assertThat(json.path("overall_ToDo_Items").get(0).path("Time").asText())
                .isEqualTo(LocalDate.now(ZoneId.of("Asia/Shanghai")).getYear() + "-09-04 21:00:00");
        assertThat(json.path("specific_ToDo_Items").get(0).path("Todo_Content").asText())
                .isEqualTo("具体待办");
        assertThat(json.path("specific_ToDo_Items").get(0).has("ToDo_Content")).isFalse();
    }

    @Test
    void normalizesRewriteTodoMinuteTime() {
        when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                anyString(), any(), eq("text"), nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any()))
                .thenReturn(Mono.just("""
                        {"rewrittenContent":"正文","todos":[{"description":"待办","time":"2026-09-05 08:30"}]}
                        """));

        StepVerifier.create(service.rewriteContent("笔记", "正文", "改写", null))
                .assertNext(result -> assertThat(result.getTodos().get(0).getScheduledAt().toString())
                        .isEqualTo("2026-09-05T08:30"))
                .verifyComplete();
    }

    @Test
    void rejectsMalformedEmptyBlankAndNullRewriteOutput() {
        for (String raw : new String[]{
                "not-json",
                "{}",
                "\"   \"",
                "null",
                "{\"rewrittenContent\":\"   \",\"todos\":[]}"}) {
            when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                    anyString(), any(), eq("text"),
                    nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any()))
                    .thenReturn(Mono.just(raw));

            StepVerifier.create(service.rewriteContent("笔记", "正文", "改写", null))
                    .expectErrorSatisfies(error -> assertThat(error)
                            .hasMessageContaining("invalid output for content rewrite"))
                    .verify();
        }
    }

    @Test
    void rejectsStructurallyInvalidRewriteTodoItems() {
        for (String raw : new String[]{
                "{\"rewrittenContent\":\"正文\",\"todos\":[null]}",
                "{\"rewrittenContent\":\"正文\",\"todos\":[{}]}",
                "{\"rewrittenContent\":\"正文\",\"todos\":[{\"description\":\"   \",\"time\":null}]}",
                "{\"rewrittenContent\":\"正文\",\"todos\":[{\"description\":\"待办\",\"time\":\"下周一\"}]}"}) {
            when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                    anyString(), any(), eq("text"),
                    nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any()))
                    .thenReturn(Mono.just(raw));

            StepVerifier.create(service.rewriteContent("笔记", "正文", "改写", null))
                    .expectErrorSatisfies(error -> assertThat(error)
                            .hasMessageContaining("invalid output for content rewrite"))
                    .verify();
        }
    }

    @Test
    void preservesMissingRewriteTodosAsAnEmptyCompatibilityList() {
        when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                anyString(), any(), eq("text"),
                nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any()))
                .thenReturn(Mono.just("{\"rewrittenContent\":\"正文\"}"));

        StepVerifier.create(service.rewriteContent("笔记", "正文", "改写", null))
                .assertNext(result -> assertThat(result.getTodos()).isEmpty())
                .verifyComplete();
    }

    @Test
    void acceptsWrappedAndBareRelatedTitleListsWithinCardinalityBounds() {
        String items = """
                [{"title":"标题一","emoji":"1"},{"title":"标题二","emoji":"2"},
                 {"title":"标题三","emoji":"3"}]
                """;
        when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                anyString(), any(), eq("res"),
                nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any()))
                .thenReturn(Mono.just("{\"items\":" + items + "}"), Mono.just(items));

        StepVerifier.create(service.relatedTitle("笔记", null))
                .assertNext(titles -> assertThat(titles).extracting("title")
                        .containsExactly("标题一", "标题二", "标题三"))
                .verifyComplete();
        StepVerifier.create(service.relatedTitle("笔记", null))
                .assertNext(titles -> assertThat(titles).hasSize(3))
                .verifyComplete();
    }

    @Test
    void rejectsMalformedEmptyBlankAndNullRelatedTitleOutput() {
        for (String raw : new String[]{"not-json", "{}", "\"   \"", "null"}) {
            when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                    anyString(), any(), eq("res"),
                    nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any()))
                    .thenReturn(Mono.just(raw));

            StepVerifier.create(service.relatedTitle("笔记", null))
                    .expectErrorSatisfies(error -> assertThat(error)
                            .hasMessageContaining("invalid output for related titles"))
                    .verify();
        }
    }

    @Test
    void rejectsRelatedTitleListsOutsideThreeToFiveItems() {
        for (String raw : new String[]{
                "{\"items\":[]}",
                "{\"items\":[{\"title\":\"1\"},{\"title\":\"2\"}]}",
                "{\"items\":[{\"title\":\"1\"},{\"title\":\"2\"},{\"title\":\"3\"},"
                        + "{\"title\":\"4\"},{\"title\":\"5\"},{\"title\":\"6\"}]}"}) {
            when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                    anyString(), any(), eq("res"),
                    nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any()))
                    .thenReturn(Mono.just(raw));

            StepVerifier.create(service.relatedTitle("笔记", null))
                    .expectErrorSatisfies(error -> assertThat(error)
                            .hasMessageContaining("invalid output for related titles"))
                    .verify();
        }
    }

    @Test
    void rejectsNullAndBlankRelatedTitleElements() {
        for (String raw : new String[]{
                "{\"items\":[{\"title\":\"1\"},null,{\"title\":\"3\"}]}",
                "{\"items\":[{\"title\":\"1\"},{\"title\":\"   \"},{\"title\":\"3\"}]}"}) {
            when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                    anyString(), any(), eq("res"),
                    nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any()))
                    .thenReturn(Mono.just(raw));

            StepVerifier.create(service.relatedTitle("笔记", null))
                    .expectErrorSatisfies(error -> assertThat(error)
                            .hasMessageContaining("invalid output for related titles"))
                    .verify();
        }
    }

    @Test
    void normalizesChineseMonthDaySuggestionTodoTimes() throws Exception {
        when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                anyString(), any(), eq("suggestion"),
                nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any()))
                .thenReturn(Mono.just("""
                        {"overall_Suggestions":[],"overall_ToDo_Items":[
                         {"ToDo_Content":"无时分","Time":"9月20日"},
                         {"ToDo_Content":"有时分","Time":"9月20日 14:35"}],
                         "specific_Suggestions":[],"specific_ToDo_Items":[]}
                        """));

        JsonNode json = new ObjectMapper().readTree(service.aiSuggestion("笔记", null).single().block());
        int year = LocalDate.now(ZoneId.of("Asia/Shanghai")).getYear();
        assertThat(json.path("overall_ToDo_Items").get(0).path("Time").asText())
                .isEqualTo(year + "-09-20 00:00:00");
        assertThat(json.path("overall_ToDo_Items").get(1).path("Time").asText())
                .isEqualTo(year + "-09-20 14:35:00");
    }

    @Test
    void degradesArbitraryNaturalLanguageSuggestionTodoTimesToNull() throws Exception {
        when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                anyString(), any(), eq("suggestion"),
                nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any()))
                .thenReturn(Mono.just("""
                        {"overall_Suggestions":[],
                         "overall_ToDo_Items":[{"ToDo_Content":"上午待办","Todo_notes":"保留上午备注","Time":"周六上午"}],
                         "specific_Suggestions":[],
                         "specific_ToDo_Items":[{"original_todo":"原始事项","Todo_Content":"下午待办",
                         "Todo_notes":"保留下午备注","Time":"周六下午"}]}
                        """));

        JsonNode json = new ObjectMapper().readTree(service.aiSuggestion("笔记", null).single().block());
        JsonNode overallTodo = json.path("overall_ToDo_Items").get(0);
        assertThat(overallTodo.path("Todo_Content").asText()).isEqualTo("上午待办");
        assertThat(overallTodo.path("Todo_notes").asText()).isEqualTo("保留上午备注");
        assertThat(overallTodo.path("Time").isNull()).isTrue();
        JsonNode specificTodo = json.path("specific_ToDo_Items").get(0);
        assertThat(specificTodo.path("original_todo").asText()).isEqualTo("原始事项");
        assertThat(specificTodo.path("Todo_Content").asText()).isEqualTo("下午待办");
        assertThat(specificTodo.path("Todo_notes").asText()).isEqualTo("保留下午备注");
        assertThat(specificTodo.path("Time").isNull()).isTrue();
    }

    @Test
    void canonicalSuggestionWritesJsonNullForNullEmptyAndWhitespaceTimes() throws Exception {
        when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                anyString(), any(), eq("suggestion"),
                nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any()))
                .thenReturn(Mono.just("""
                        {"overall_Suggestions":[],"overall_ToDo_Items":[
                         {"ToDo_Content":"null 时间","Time":null},
                         {"ToDo_Content":"空字符串时间","Time":""},
                         {"ToDo_Content":"纯空白时间","Time":"   "}],
                         "specific_Suggestions":[],"specific_ToDo_Items":[]}
                        """));

        JsonNode todos = new ObjectMapper()
                .readTree(service.aiSuggestion("笔记", null).single().block())
                .path("overall_ToDo_Items");
        assertThat(todos).hasSize(3);
        assertThat(todos.get(0).path("Todo_Content").asText()).isEqualTo("null 时间");
        assertThat(todos.get(1).path("Todo_Content").asText()).isEqualTo("空字符串时间");
        assertThat(todos.get(2).path("Todo_Content").asText()).isEqualTo("纯空白时间");
        for (JsonNode todo : todos) {
            assertThat(todo.has("Time")).isTrue();
            assertThat(todo.path("Time").isNull()).isTrue();
        }
    }

    @Test
    void canonicalSuggestionStillRejectsNonTextualTimes() {
        for (String invalidTime : new String[]{"123", "{}", "[]", "true"}) {
            when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                    anyString(), any(), eq("suggestion"),
                    nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any()))
                    .thenReturn(Mono.just("""
                            {"overall_Suggestions":[],
                             "overall_ToDo_Items":[{"ToDo_Content":"待办","Time":%s}],
                             "specific_Suggestions":[],"specific_ToDo_Items":[]}
                            """.formatted(invalidTime)));

            StepVerifier.create(service.aiSuggestion("笔记", null))
                    .expectErrorSatisfies(error -> assertThat(error)
                            .hasMessageContaining("invalid output for AI suggestion"))
                    .verify();
        }
    }

    @Test
    void legacySuggestionDegradesBlankAndNaturalLanguageTimesToNull() throws Exception {
        when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                anyString(), any(), eq("suggestion"),
                nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any()))
                .thenReturn(Mono.just("""
                        {"information":"明确下一步",
                         "todo":[{"description":"空白时间待办","time":"   "},
                                 {"description":"周末待办","time":"周六下午"}]}
                        """));

        JsonNode todos = new ObjectMapper()
                .readTree(service.aiSuggestion("笔记", null).single().block())
                .path("overall_ToDo_Items");
        assertThat(todos).hasSize(2);
        assertThat(todos.get(0).path("Todo_Content").asText()).isEqualTo("空白时间待办");
        assertThat(todos.get(0).path("Time").isNull()).isTrue();
        assertThat(todos.get(1).path("Todo_Content").asText()).isEqualTo("周末待办");
        assertThat(todos.get(1).path("Time").isNull()).isTrue();
    }

    @Test
    void legacySuggestionStillRejectsNonTextualTime() {
        when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                anyString(), any(), eq("suggestion"),
                nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any()))
                .thenReturn(Mono.just("""
                        {"information":"明确下一步",
                         "todo":[{"description":"待办","time":{"relative":"周六"}}]}
                        """));

        StepVerifier.create(service.aiSuggestion("笔记", null))
                .expectErrorSatisfies(error -> assertThat(error)
                        .hasMessageContaining("invalid output for AI suggestion"))
                .verify();
    }

    @Test
    void acceptsValidAndEmptyInformationCompletionLists() {
        String valid = """
                {"items":[{"vague_phrase":"周末","inquiry_process":"是周六还是周日？",
                 "options":[{"option":"周六"},{"option":"周日"}]}]}
                """;
        when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                anyString(), any(), eq("text"),
                nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any()))
                .thenReturn(Mono.just(valid), Mono.just("{\"items\":[]}"));

        StepVerifier.create(service.completeInfo("周末搬家", null))
                .assertNext(items -> {
                    assertThat(items).hasSize(1);
                    assertThat(items.getFirst().getVaguePhrase()).isEqualTo("周末");
                    assertThat(items.getFirst().getInquiryProcess()).isEqualTo("是周六还是周日？");
                    assertThat(items.getFirst().getOptions()).hasSize(2);
                })
                .verifyComplete();
        StepVerifier.create(service.completeInfo("没有歧义", null))
                .assertNext(items -> assertThat(items).isEmpty())
                .verifyComplete();
    }

    @Test
    void normalizesSupportedInformationCompletionShapesAndCapsOptionsWithoutRetry() {
        when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                anyString(), any(), eq("text"),
                nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any()))
                .thenReturn(Mono.just("""
                        ```json
                        {"questions":[{"vaguePhrase":"那里","question":"具体是哪里？",
                         "choices":["客户公司","手动输入",{"label":"项目现场"},{"value":"会议室"},
                                    {"text":"客户公司"},"咖啡馆","酒店"]}]}
                        ```
                        """));

        StepVerifier.create(service.completeInfo("下周找小王谈一下项目，之后再去那里见客户。", null))
                .assertNext(items -> {
                    assertThat(items).hasSize(1);
                    assertThat(items.getFirst().getVaguePhrase()).isEqualTo("那里");
                    assertThat(items.getFirst().getInquiryProcess()).isEqualTo("具体是哪里？");
                    JsonNode normalized = new ObjectMapper().valueToTree(items.getFirst());
                    assertThat(normalized.path("options")).hasSize(4);
                    assertThat(normalized.path("options").get(0).path("option").asText())
                            .isEqualTo("客户公司");
                    assertThat(normalized.path("options").get(3).path("option").asText())
                            .isEqualTo("咖啡馆");
                    assertThat(normalized.toString()).doesNotContain("手动输入");
                })
                .verifyComplete();

        verify(difyClient).callDifyBlockingWorkflowApiWithFallback(
                anyString(), any(), eq("text"),
                nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any());
        verify(deepSeekClient, never()).completeJsonWithUsage(anyString(), anyString());
    }

    @Test
    void acceptsHistoricalDirectArrayAndSingleQuestionObject() {
        when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                anyString(), any(), eq("text"),
                nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any()))
                .thenReturn(Mono.just("""
                                [{"vague_phrase":"周末","inquiry_process":"是周六还是周日？",
                                  "options":[{"option":"周六"},{"option":"周日"}]}]
                                """),
                        Mono.just("""
                                {"ambiguousPhrase":"换窝","prompt":"具体要换什么？",
                                 "candidates":["换房子","换卧室"]}
                                """));

        StepVerifier.create(service.completeInfo("周末搬家", null))
                .assertNext(items -> assertThat(items.getFirst().getOptions()).hasSize(2))
                .verifyComplete();
        StepVerifier.create(service.completeInfo("周末搬家，换窝。", null))
                .assertNext(items -> assertThat(items.getFirst().getVaguePhrase()).isEqualTo("换窝"))
                .verifyComplete();
    }

    @Test
    void acceptsSingleLayerJsonStringCompletionOutput() {
        when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                anyString(), any(), eq("text"),
                nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any()))
                .thenReturn(Mono.just("\"[{\\\"vague_phrase\\\":\\\"周末\\\","
                        + "\\\"inquiry_process\\\":\\\"是哪一天？\\\","
                        + "\\\"options\\\":[\\\"周六\\\",\\\"周日\\\"]}]\""));

        StepVerifier.create(service.completeInfo("周末搬家。", null))
                .assertNext(items -> assertThat(items.getFirst().getOptions()).hasSize(2))
                .verifyComplete();
    }

    @Test
    void rejectsQuestionWhoseVaguePhraseIsNotInOriginalNote() {
        when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                anyString(), any(), eq("text"),
                nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any()))
                .thenReturn(Mono.just("""
                        {"items":[{"vague_phrase":"下周三","inquiry_process":"几点见？",
                         "options":["上午","下午"]}]}
                        """));

        StepVerifier.create(service.completeInfo("下周找小王谈一下项目。", null))
                .expectErrorSatisfies(error -> assertThat(error)
                        .hasMessageContaining("invalid output for information completion"))
                .verify();
    }

    @Test
    void rejectsCompletionWithOnlyReservedManualInputOption() {
        when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                anyString(), any(), eq("text"),
                nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any()))
                .thenReturn(Mono.just("""
                        {"items":[{"vague_phrase":"那里","inquiry_process":"具体是哪里？",
                         "options":["手动输入"]}]}
                        """));

        StepVerifier.create(service.completeInfo("之后再去那里见客户。", null))
                .expectErrorSatisfies(error -> assertThat(error)
                        .hasMessageContaining("invalid output for information completion"))
                .verify();
    }

    @Test
    void rejectsMalformedAndStructurallyInvalidInformationCompletionOutput() {
        for (String raw : new String[]{
                "",
                "```json\n```",
                "not-json",
                "[] trailing",
                "[] {}",
                "```json\n[]\n``` trailing",
                "{}",
                "null",
                "{\"items\":[null]}",
                "{\"items\":[{\"vague_phrase\":\"   \",\"inquiry_process\":\"追问\","
                        + "\"options\":[{\"option\":\"选项\"}]}]}",
                "{\"items\":[{\"vague_phrase\":\"笔记\",\"inquiry_process\":\"   \","
                        + "\"options\":[{\"option\":\"选项\"}]}]}",
                "{\"items\":[{\"vague_phrase\":\"笔记\",\"inquiry_process\":\"追问\",\"options\":null}]}",
                "{\"items\":[{\"vague_phrase\":\"笔记\",\"inquiry_process\":\"追问\",\"options\":[]}]}",
                "{\"items\":[{\"vague_phrase\":\"笔记\",\"inquiry_process\":\"追问\","
                        + "\"options\":[null]}]}",
                "{\"items\":[{\"vague_phrase\":\"笔记\",\"inquiry_process\":\"追问\","
                        + "\"options\":[{\"option\":\"   \"}]}]}",
                "{\"items\":[{\"vague_phrase\":\"笔记\",\"inquiry_process\":\"追问\","
                        + "\"options\":[{\"option\":123}]}]}",
                "{\"items\":[{\"vague_phrase\":\"笔记\",\"inquiry_process\":\"追问\","
                        + "\"options\":[\"1\",\"2\",\"3\",\"4\",123]}]}",
                "{\"items\":[{\"vague_phrase\":\"笔记\",\"vaguePhrase\":\"其他\","
                        + "\"inquiry_process\":\"追问\",\"options\":[\"选项\"]}]}",
                "{\"items\":[{\"v.a.g.u.e_p-h-r-a-s-e\":\"笔记\",\"inquiry_process\":\"追问\","
                        + "\"options\":[\"选项\"]}]}"}) {
            when(difyClient.callDifyBlockingWorkflowApiWithFallback(
                    anyString(), any(), eq("text"),
                    nullable(org.springframework.http.server.reactive.ServerHttpRequest.class), any()))
                    .thenReturn(Mono.just(raw));

            StepVerifier.create(service.completeInfo("笔记", null))
                    .expectErrorSatisfies(error -> assertThat(error)
                            .hasMessageContaining("invalid output for information completion"))
                    .verify();
        }
    }
}
