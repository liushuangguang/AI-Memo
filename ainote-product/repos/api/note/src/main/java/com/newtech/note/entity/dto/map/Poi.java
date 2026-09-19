package com.newtech.note.entity.dto.map;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Poi {
    private String address;
    @JsonProperty("city_name")
    private String cityName;
    @JsonProperty("district_name")
    private String district_name;
    private String location;
    private String name;
    private String[] photos;
    @JsonProperty("province_name")
    private String province_name;
    private String type;
}
