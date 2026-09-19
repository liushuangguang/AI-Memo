package com.newtech.note.entity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class CompleteInfo {
    @JsonProperty("vague_phrase")
    private String vaguePhrase;
    @JsonProperty("inquiry_process")
    private String inquiryProcess;
    @JsonProperty("options")
    private List<Option> options;
}

@Data
class Option {
    private String option;
}
