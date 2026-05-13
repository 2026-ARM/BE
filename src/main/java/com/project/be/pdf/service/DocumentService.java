package com.project.be.pdf.service;

import com.project.be.pdf.entity.Document;
import com.project.be.pdf.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
public class DocumentService {
    private final DocumentRepository repository;
    private final AiService aiService;
    private final CloudinaryUploaderService cloudinaryUploaderService;

    public DocumentService(DocumentRepository repository, AiService aiService, CloudinaryUploaderService cloudinaryUploaderService) {
        this.repository = repository;
        this.aiService = aiService;
        this.cloudinaryUploaderService = cloudinaryUploaderService;
    }

    public Document save(MultipartFile file) throws IOException {

        String fileUrl = cloudinaryUploaderService.upload(file);

        Document doc = Document.builder()
                .originalFileUrl(fileUrl)
                .status(Document.Status.UPLOADED)
                .createdAt(LocalDateTime.now())
                .fileSize(file.getSize())
                .build();

        return repository.save(doc);
    }

    public void update(Long id, String processedFileUrl) {
        repository.findById(id).ifPresent(doc -> {
            doc.setProcessedFileUrl(processedFileUrl);
            doc.setStatus(Document.Status.COMPLETED);
            repository.save(doc);
        });
    }
}
