package com.project.be.pdf.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

@Service

public class CloudinaryUploaderService {

    private final Cloudinary cloudinary;

    public CloudinaryUploaderService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }


    /**
     * MultipartFile (PDF 등)을 업로드합니다.
     * @param file 사용자가 업로드한 원본 파일
     * @return 업로드된 파일의 URL
     */

    public String upload(MultipartFile file) throws IOException {

        File tempFile = Files.createTempFile(
                "upload_",
                "_" + file.getOriginalFilename()
        ).toFile();

        file.transferTo(tempFile);

        try {

            Map params = ObjectUtils.asMap(
                    "resource_type", "raw",
                    "use_filename", true,
                    "unique_filename", true,
                    "folder", "pdf"
            );

            Map uploadResult =
                    cloudinary.uploader().upload(tempFile, params);

            return uploadResult.get("secure_url").toString();

        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }


    // upload 메서드 두개 (메서드 오버로딩)
    /**
     * String 콘텐츠를 텍스트 파일(.txt)로 업로드합니다.
     * @param content 업로드할 텍스트 내용
     * @param filename Cloudinary에 저장될 파일 이름 (확장자 포함)
     * @return 업로드된 파일의 URL
     */
    public String upload(String content, String filename) throws IOException {
        Map params = ObjectUtils.asMap(
                "resource_type", "raw",
                "public_id", "chunks/" + filename, // 'chunks' 폴더에 저장
                "access_mode", "public"
        );

        // String을 byte 배열로 변환하여 업로드
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        Map uploadResult = cloudinary.uploader().upload(bytes, params);

        return uploadResult.get("secure_url").toString();

    }

}