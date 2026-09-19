package com.newtech.note.util;

import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

public class ImageUtil {

    /**
     * 控制压缩后的图像大小，长宽最大为1008px，也就分辨率为1008x1008px, 消耗token最大为36 * 36 = 1296个token
     */
    private static final long imageMaxLength = 1008; //1008px

    /**
     * {reference: <a href="https://help.aliyun.com/zh/model-studio/user-guide/vision?spm=a2c4g.11186623.0.0.67181d1cCE4N55">...</a>}
     * 通义千问VL模型按输入和输出的总Token数进行计费。
     * 图像转换为Token的规则：512x512像素的图像约等于334个Token，其他分辨率图像按比例换算；最小单位是28x28像素，即每28x28像素对应一个Token，如果图像的长或宽不是28的整数倍，则向上取整至28的整数倍；一张图最少4个Token。
     */
    private static final double imageMaxLengthRatioNumerator = 1008.00; //1008px


    private static Pair<Integer, Integer> calculateNewDimensions(int originalWidth, int originalHeight) {
        int newWidth = originalWidth;
        int newHeight = originalHeight;
        // 如果任一维度小于imageMaxLength，则不进行调整
        if (newWidth < imageMaxLength || newHeight < imageMaxLength) {
            return Pair.of(newWidth, newHeight);
        }
        // 长宽都大于imageMaxLength时，判断是否需要调整
        if (newWidth > imageMaxLength || newHeight > imageMaxLength) {
            double scale = Math.min(imageMaxLengthRatioNumerator / newWidth, imageMaxLengthRatioNumerator / newHeight);
            newWidth = (int) (newWidth * scale);
            newHeight = (int) (newHeight * scale);
        }
        return Pair.of(newWidth, newHeight);
    }


    public static void compressAndSaveImage(Path inputFile, Path outputFile, float quality) throws IOException {
        BufferedImage originalImage = ImageIO.read(inputFile.toFile());
        Pair<Integer, Integer> dimensions = calculateNewDimensions(originalImage.getWidth(), originalImage.getHeight());

        // 调整图像大小
        BufferedImage resizedImage = resizeImage(originalImage, dimensions.getLeft(), dimensions.getRight());

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        ImageWriter writer = writers.next();

        // 设置压缩参数
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality); // 设置压缩质量

        // 写入压缩后的图像
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(new File(outputFile.toString()))) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(resizedImage, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    public static BufferedImage resizeImage(BufferedImage originalImage, int width, int height) {
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, width, height, null);
        g.dispose();
        return resizedImage;
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
