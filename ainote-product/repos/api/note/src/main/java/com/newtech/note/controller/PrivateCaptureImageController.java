package com.newtech.note.controller;

import com.newtech.note.config.Filter.MyWebFilter;
import com.newtech.note.service.PrivateCaptureImageService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v2/capture/images")
public class PrivateCaptureImageController {
    private final PrivateCaptureImageService images;

    public PrivateCaptureImageController(PrivateCaptureImageService images) {
        this.images = images;
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Resource>> getImage(
            @RequestHeader(MyWebFilter.AUTHENTICATED_OWNER_HEADER) String ownerId,
            @PathVariable("id") String imageId) {
        return images.getOwned(ownerId, imageId);
    }
}
