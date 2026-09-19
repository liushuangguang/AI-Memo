package com.newtech.note.entity.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "pointsConsumptionLocks")
public class PointsConsumptionLease {
    @Id
    private Long uid;
    private String owner;
    private Date acquiredAt;
}
