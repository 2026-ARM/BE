package com.project.be.pdf.controller;

import com.project.be.pdf.dto.UpdateDto;
import com.project.be.pdf.entity.Document;
import com.project.be.pdf.service.AiService;
import com.project.be.pdf.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/pdf")
public class DocumentController {

    private final DocumentService documentService;
    private final AiService aiService;

    public DocumentController(DocumentService documentService, AiService aiService) {
        this.documentService = documentService;
        this.aiService = aiService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) throws Exception {

        Document doc = documentService.save(file);

        aiService.requestProcessing(doc.getId(), doc.getOriginalFileUrl());

        return ResponseEntity.ok(Map.of(
                "documentId", doc.getId(),
                "status", doc.getStatus().toString()
        ));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody UpdateDto dto) {
        documentService.update(id, dto.getProcessedFileUrl());
        return ResponseEntity.ok().build();
    }
}
