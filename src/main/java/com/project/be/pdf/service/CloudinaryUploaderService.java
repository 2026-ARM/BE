package com.project.be.pdf.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

@Service

public class CloudinaryUploaderService {

    private final Cloudinary cloudinary;

    public CloudinaryUploaderService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public String upload(MultipartFile file) throws IOException {

        File tempFile = Files.createTempFile(
                "upload_",
                "_" + file.getOriginalFilename()
        ).toFile();

        file.transferTo(tempFile);

        try {

            Map params = ObjectUtils.asMap(
                    "resource_type", "image",
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
}