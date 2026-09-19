package com.newtech.note.service;

import com.newtech.note.common.NoteBaseResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

public interface FileUploadService {

    Mono<NoteBaseResponse<String>> uploadFile(FilePart file);

    Mono<NoteBaseResponse<String>> uploadTmpFile(FilePart file);

    Mono<ResponseEntity<Resource>> downloadFile(String filename);

    Mono<NoteBaseResponse<String>> compressImage(FilePart filePart);
}
