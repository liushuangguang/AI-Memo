package com.newtech.note.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.newtech.note.entity.dto.noteModules.items.KeyValueItem;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/*
 {
      "GoodArticleExcerpts": [

      ]
    },
    {
      "AccountAndPassword": [

      ]
    },
    {
      "HealthRelated": [

      ]
    },
    {
      "FinancialManagement": [

      ]
    },
    {
      "ImportantDates": [

      ]
    },
    {
      "PersonalPlanning": [

      ]
    },
    {
      "MovieRecords": [

      ]
    }
   }
 */
@Data
public class InfoClassification {
    @JsonProperty("GoodArticleExcerpts")
    private List<GoodArticleExcerpt> goodArticleExcerpts;
    @JsonProperty("AccountAndPassword")
    private List<AccountAndPassword> accountAndPasswords;
    @JsonProperty("HealthRelated")
    private List<HealthRelated> healthRelateds;
    @JsonProperty("FinancialManagement")
    private List<FinancialManagement> financialManagements;
    @JsonProperty("ImportantDates")
    private List<ImportantDate> importantDates;
    @JsonProperty("PersonalPlanning")
    private List<PersonalPlanning> personalPlannings;
    @JsonProperty("MovieRecords")
    private List<MovieRecord> movieRecords;

    @Setter
    @Getter
    @NoArgsConstructor
    public abstract static class BaseClassification {
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime recordTime;
        private String reason;
        private String other;
        private String originalContent;

        public abstract List<KeyValueItem> getKVPairs();

    }

    /*
    {
              "content": "（好文词句的具体内容）",
              "source": "（查询该词句的来源，可能来自古诗、某作家某作品等）",
              "link": "（用户记录的该文章词句的网址）",
              "appreciation": "（对词句进行简单的鉴赏分析）",
              "recordTime": "{{{{#1731380312856.text#}}}}",
              "reason": "（归类理由）",
              "other": "（其他的信息）",
              "originalContent": "（源备忘录被提取部分的内容）"
            }
     */
    @Setter
    @Getter
    @NoArgsConstructor
    public static class GoodArticleExcerpt extends BaseClassification {
        private String content;
        private String source;
        private String link;
        private String appreciation;

        /**
         * 转换为KeyValueItem
         *
         * @return key-value对
         */
        public List<KeyValueItem> getKVPairs() {
            return List.of(new KeyValueItem("好文词句", getContent()),
                    new KeyValueItem("来源出处", getSource()),
                    new KeyValueItem("来源网址", getLink()),
                    new KeyValueItem("鉴赏分析", getAppreciation()));
        }
    }

    /*
    {
              "platform": "（服务或应用的名称）",
              "link": "（服务或应用的网址）",
              "account": "（注册的用户名或邮箱）",
              "password": "（账户的密码）",
              "recordTime": "{{{{#1731380312856.text#}}}}",
              "reason": "（归类理由）",
              "other": "（其他的信息）",
              "originalContent": "（源备忘录被提取部分的内容）"
            }
     */
    @Setter
    @Getter
    @NoArgsConstructor
    public static class AccountAndPassword extends BaseClassification {
        private String platform;
        private String link;
        private String account;
        private String password;

        /**
         * 转换为KeyValueItem
         *
         * @return key-value对
         */
        @Override
        public List<KeyValueItem> getKVPairs() {
            return List.of(
                    new KeyValueItem("服务名称", getPlatform()),
                    new KeyValueItem("网址", getLink()),
                    new KeyValueItem("用户名", getAccount()),
                    new KeyValueItem("账户的密码", getPassword())
            );
        }
    }


    /*
    {
              "dailySymptomsRecord": "（日常的正在记录）",
              "medicinesToBuy": "（待购药物的药品信息、名称、品牌等，如果药品名称不完整，可以猜测可能的2个药品全称）",
              "medicineUsage": "正在使用的药物名称、剂量和服用时间",
              "hospital": "（就医的医院名称，如果医院名称不完整，可以猜测可能的2个医院全称）",
              "followUpTreatmentPlan": "（后续治疗规划）",
              "recordTime": "{{{{#1731380312856.text#}}}}",
              "reason": "（归类理由）",
              "other": "（其他的信息）",
              "originalContent": "（源备忘录被提取部分的内容）"
            }
     */


    @Setter
    @Getter
    @NoArgsConstructor
    public static class HealthRelated extends BaseClassification {
        private String dailySymptomsRecord;
        private String medicinesToBuy;
        private String medicineUsage;
        private String hospital;
        private String followUpTreatmentPlan;

        /**
         * 转换为KeyValueItem
         * map.put("dailySymptomsRecord", "日常的正在记录");
         * map.put("medicinesToBuy", "待购药物的药品信息、名称、品牌等，如果药品名称不完整，可以猜测可能的2个药品全称");
         * map.put("medicineUsage", "正在使用的药物名称、剂量和服用时间");
         * map.put("hospital", "就医的医院名称，如果医院名称不完整，可以猜测可能的2个医院全称");
         * map.put("followUpTreatmentPlan", "后续治疗规划");
         *
         * @return key-value对
         */
        @Override
        public List<KeyValueItem> getKVPairs() {
            return List.of(
                    new KeyValueItem("日常的正在记录", getDailySymptomsRecord()),
                    new KeyValueItem("待购药物", getMedicinesToBuy()),
                    new KeyValueItem("正在使用的药物", getMedicineUsage()),
                    new KeyValueItem("就医的医院", getHospital()),
                    new KeyValueItem("后续治疗规划", getFollowUpTreatmentPlan())
            );
        }

    }

    /*
    {
          "FinancialManagement": [
            {
              "type": "（收入/支出/发票/其他）",
              "amount": "（具体金额，附带单位）",
              "transactionDescription": "（交易的具体内容或目的）",
              "recordTime": "{{{{#1731380312856.text#}}}}",
              "reason": "（归类理由）",
              "other": "（其他的信息）",
              "originalContent": "（源备忘录被提取部分的内容）"
            }
     */
    @Setter
    @Getter
    @NoArgsConstructor
    public static class FinancialManagement extends BaseClassification {
        private String type;
        private String amount;
        private String transactionDescription;

        /**
         * 转换为KeyValueItem
         *
         * @return key-value对
         */
        @Override
        public List<KeyValueItem> getKVPairs() {
            return List.of(new KeyValueItem("收入/支出/发票/其他", getType()),
                    new KeyValueItem("具体金额，附带单位", getAmount()),
                    new KeyValueItem("简述", getTransactionDescription()));
        }


    }

    /*
    {
              "eventName": "（纪念日、节日或特殊事件的名称）",
              "date": "（具体的日期或日期范围）",
              "relatedPeople": "（与日期相关的人物或组织）",
              "eventDescription": "（事件的简短描述或重要性说明）",
              "recordTime": "{{{{#1731380312856.text#}}}}",
              "reason": "（归类理由）",
              "other": "（其他的信息）",
              "originalContent": "（源备忘录被提取部分的内容）"
            }
     */
    @Setter
    @Getter
    @NoArgsConstructor
    public static class ImportantDate extends BaseClassification {
        private String eventName;
        private String date;
        private String relatedPeople;
        private String eventDescription;

        /**
         * 转换为KeyValueItem
         *
         * @return key-value对
         */
        @Override
        public List<KeyValueItem> getKVPairs() {
            return List.of(new KeyValueItem("纪念日、节日或特殊事件的名称", getEventName()),
                    new KeyValueItem("日期或日期范围", getDate()),
                    new KeyValueItem("相关人物或组织", getRelatedPeople()),
                    new KeyValueItem("简述", getEventDescription()));
        }
    }

    /*
    {
              "goalDescription": "（目标的详细描述和期望结果）",
              "goalType": "（个人、职业、健康等）",
              "deadline": "（目标计划完成的时间）",
              "actionPlan": "（实现目标的具体步骤和计划）",
              "recordTime": "{{{{#1731380312856.text#}}}}",
              "reason": "（归类理由）",
              "other": "（其他的信息）",
              "originalContent": "（源备忘录被提取部分的内容）"
            }
     */
    @Setter
    @Getter
    @NoArgsConstructor
    public static class PersonalPlanning extends BaseClassification {
        private String goalDescription;
        private String goalType;
        private String deadline;
        private String actionPlan;

        /**
         * 转换为KeyValueItem
         * map.put("goalDescription", "目标的详细描述和期望结果");
         * map.put("goalType", "个人、职业、健康等");
         * map.put("deadline", "目标计划完成的时间");
         * map.put("actionPlan", "实现目标的具体步骤和计划");
         *
         * @return key-value对
         */
        public List<KeyValueItem> getKVPairs() {
            return List.of(new KeyValueItem("目标描述", getGoalDescription()),
                    new KeyValueItem("目标类型", getGoalType()),
                    new KeyValueItem("目标完成时间", getDeadline()),
                    new KeyValueItem("具体步骤和计划", getActionPlan()));
        }
    }

    /*
    {
              "movieName": "（影片名称）",
              "movieType": "（电影、连续剧、动漫等）",
              "movieLink": "（baidu.com/s?ie=UTF-8&wd=电影%20{{movieName}}%20免费在线观看）",
              "recordTime": "{{{{#1731380312856.text#}}}}",
              "reason": "（归类理由）",
              "other": "（其他的信息）",
              "originalContent": "（源备忘录被提取部分的内容）"
            }
     */
    @Setter
    @Getter
    @NoArgsConstructor
    public static class MovieRecord extends BaseClassification {
        private String movieName;
        private String movieType;
        private String movieLink;

        /**
         * 转换为KeyValueItem
         *
         * @return key-value对
         */
        @Override
        public List<KeyValueItem> getKVPairs() {
            return List.of(new KeyValueItem("影片名称", getMovieName()),
                    new KeyValueItem("电影类型", getMovieType()),
                    new KeyValueItem("网址", getMovieLink()));

        }

    }
}


