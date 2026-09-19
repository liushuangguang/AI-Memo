package com.newtech.note.service.impl;

import com.newtech.note.common.NoteBaseResponse;
import com.newtech.note.service.FileUploadService;
import com.newtech.note.util.ImageUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestPart;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.UUID;

@Service
public class FileUploadServiceImpl implements FileUploadService {
    private static final Logger logger = LogManager.getLogger(FileUploadServiceImpl.class);
    private final Path rootLocation = Paths.get("upload-files");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    private static final float compressQuality = 0.1f; //压缩质量


    @Value("${note.hostName}")
    private String hostName;

    public FileUploadServiceImpl() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory!");
        }
    }


    public Mono<NoteBaseResponse<String>> uploadFile(@RequestPart("file") FilePart file) {
        return checkFile(file)
                .flatMap(fileMeta -> {
                    Path destinationFile = rootLocation.resolve(fileMeta.fileName());
                    return file.transferTo(destinationFile)
                            .then(Mono.defer(() -> {
                                if (destinationFile.toFile().length() > MAX_FILE_SIZE) {
                                    return Mono.fromCallable(() -> destinationFile.toFile().delete())
                                            .flatMap(done -> {
                                                logger.error("Uploaded file exceeded the size limit");
                                                return Mono.just(NoteBaseResponse.failure("File size exceeds the limit of 5MB"));
                                            });
                                }
                                return Mono.just(NoteBaseResponse.success(hostName + destinationFile));
                            }));
                });
    }

    /**
     * Mono.fromCallable()：用于将 File.createTempFile() 和其他阻塞操作包装成 Mono，并将它们交给合适的线程池（Schedulers.boundedElastic()）执行。
     * Schedulers.boundedElastic()：这是一个专门为阻塞 I/O 操作设计的线程池，适合用于文件操作、数据库操作等可能会阻塞的任务。
     *
     * @param file
     * @return
     */
    public Mono<NoteBaseResponse<String>> uploadTmpFile(@RequestPart("file") FilePart file) {
        //通过将阻塞操作移到专门的线程池（boundedElastic）中，我们可以确保反应式线程池不会被阻塞，从而保持高并发处理能力
        return Mono.fromCallable(() ->
                        File.createTempFile("tmp-", null, rootLocation.toAbsolutePath().toFile()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(tempFile ->
                        file.transferTo(tempFile.toPath())
                                .doFinally((signal) -> {
                                    boolean delete = tempFile.delete();
                                    System.out.println("delete temp file: " + delete);
                                    tempFile.deleteOnExit();
                                })
                                .then(Mono.fromCallable(() -> NoteBaseResponse.success(tempFile.toString()))));
    }

    public Mono<ResponseEntity<Resource>> downloadFile(@PathVariable String filename) {
        Path file = rootLocation.resolve(filename);
        Resource resource;
        try {
            resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return Mono.just(ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource));
            } else {
                return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
            }
        } catch (Exception e) {
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
        }
    }


    @Override
    public Mono<NoteBaseResponse<String>> compressImage(FilePart filePart) {
        HttpHeaders headers = filePart.headers();
        String contentType = headers.getFirst(HttpHeaders.CONTENT_TYPE);
        if (contentType == null || !contentType.startsWith("image/")) {
            return Mono.error(new IllegalArgumentException("Only image file can be uploaded."));
        }
        return checkFile(filePart)
                .flatMap(fileMeta -> {
                    Path originalFile = rootLocation.resolve(fileMeta.fileName());
                    return filePart.transferTo(originalFile)
                            .then(Mono.fromCallable(() -> {
                                Path targetLocation = rootLocation.resolve("compressed_" + fileMeta.fileName());
                                ImageUtil.compressAndSaveImage(
                                        rootLocation.resolve(fileMeta.fileName()),
                                        targetLocation,
                                        compressQuality
                                );
                                return NoteBaseResponse.success(hostName + targetLocation);
                            }).subscribeOn(Schedulers.boundedElastic()))
                            .onErrorResume(error -> Mono.just(NoteBaseResponse.failure(error.getMessage())));
                });
    }

    public static void main(String[] args) throws IOException {
        Path destinationFile = Paths.get("upload-files").resolve("d4ef09d1-3df9-4e70-b854-5107ac43e3fe.jpg");
        // 打印原始图像信息
        printImageInfo(destinationFile);
        BufferedImage originalImage = ImageIO.read(destinationFile.toFile());
        // 调整图像大小
        BufferedImage resizedImage = resizeImage(originalImage, 1024, 1024);// 获取图像写入器

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        ImageWriter writer = writers.next();

        // 设置压缩参数
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.5f); // 设置压缩质量
        Path compressedFilePath = Paths.get("upload-files").resolve("compressed_d4ef09d1-3df9-4e70-b854-5107ac43e3fe.jpg");


        // 写入压缩后的图像
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(new File(compressedFilePath.toString()))) {
            writer.setOutput(ios);
            writer.write(null, new javax.imageio.IIOImage(resizedImage, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    public static void printImageInfo(Path filePath) throws IOException {
        System.out.println("----------------------------------------------------------------------");
        BufferedImage image = ImageIO.read(filePath.toFile());

        int width = image.getWidth();
        int height = image.getHeight();
        System.out.println("图像尺寸: " + width + " x " + height);
        System.out.println("图片大小: " + filePath.toFile().length() / 1024 + "KB");
    }

    public static BufferedImage resizeImage(BufferedImage originalImage, int width, int height) {
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, width, height, null);
        g.dispose();
        return resizedImage;
    }


    public record FileMeta(String fileName, String fileId, String ext) {

    }

    private static Mono<FileMeta> checkFile(FilePart filePart) {
        String originalFilename = filePart.filename();
        String[] fileNameParts = originalFilename.split("\\.");
        if (fileNameParts.length < 2) {
            logger.error("Invalid uploaded file name");
            return Mono.error(new IllegalArgumentException("Invalid file name: " + originalFilename));
        }
        String ext = fileNameParts[fileNameParts.length - 1];
        String uuid = UUID.randomUUID().toString().replace("_", "");
        String fileName = uuid + "." + ext;
        return Mono.just(new FileMeta(fileName, uuid, ext));
    }

    public static void compressAndSaveImage(BufferedImage image, Path outputPath, float quality) throws IOException {
        // 获取图像写入器
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        ImageWriter writer = writers.next();

        // 设置压缩参数
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality); // 设置压缩质量

        // 写入压缩后的图像
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(new File(outputPath.toString()))) {
            writer.setOutput(ios);
            writer.write(null, new javax.imageio.IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }


}
