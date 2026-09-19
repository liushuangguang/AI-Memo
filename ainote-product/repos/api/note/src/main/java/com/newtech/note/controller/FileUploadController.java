package com.newtech.note.controller;

import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/files")
@Tag(name = "文件上传下载处理器", description = "文件处理器，可用于上传和下载")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @PostMapping("/upload")
    @Operation(
            summary = "上传文件",
            description = "上传文件大小不得超过5MB")
    public Mono<NoteBaseResponse<String>> uploadFile(FilePart file) {
        return fileUploadService.uploadFile(file);
    }


    @PostMapping(value = "/uploadImage", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "压缩图片",
            description = "上传图片会自动压缩图片，按照一定比例压缩图片，图片的长宽控制在800px以内，并返回压缩后的图片地址")
    public Mono<NoteBaseResponse<String>> compressImage(FilePart filePart) {
        return fileUploadService.compressImage(filePart);
    }

    @PostMapping("/uploadTmp")
    @Operation(
            summary = "上传临时文件",
            description = "上传文件大小不得超过5MB")
    public Mono<NoteBaseResponse<String>> uploadTmpFile(FilePart file) {
        return fileUploadService.uploadTmpFile(file);
    }

    @Operation(
            summary = "下载文件",
            description = "下载文件")
    @GetMapping("/download/{filename}")
    public Mono<ResponseEntity<Resource>> downloadFile(@PathVariable String filename) {
        return fileUploadService.downloadFile(filename);
    }
}
