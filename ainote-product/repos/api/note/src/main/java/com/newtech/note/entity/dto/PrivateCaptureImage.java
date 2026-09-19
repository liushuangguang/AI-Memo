package com.newtech.note.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "private_capture_images")
public class PrivateCaptureImage {
    @Id
    private String id;
    private String ownerId;
    private String fileName;
    private String mimeType;
    private LocalDateTime createdAt;
}
