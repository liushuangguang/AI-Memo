package com.newtech.note;

import com.newtech.note.util.ImageUtil;

import java.io.IOException;
import java.nio.file.Paths;

public class ImageUtilTest {
    public static void main(String[] args) throws IOException {
        ImageUtil.compressAndSaveImage(
                Paths.get("upload-files").resolve("01bc36d4-f384-42ab-ae42-4b9f95f2e02e.jpg"),
                Paths.get("upload-files").resolve("compressed_01bc36d4-f384-42ab-ae42-4b9f95f2e02e.jpg"),
                0.6f
        );
    }
}
