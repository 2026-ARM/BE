package com.project.be.pdf.service;

import com.project.be.pdf.entity.Document;
import com.project.be.pdf.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class AiService {

    private final DocumentRepository documentRepository;
    private final WebClient webClient;

    public AiService(DocumentRepository documentRepository, WebClient.Builder webClientBuilder) {
        this.documentRepository = documentRepository;
        this.webClient = webClientBuilder.baseUrl("http://ai-server").build();
    }

    public void requestProcessing(Long id, String fileUrl) {
        documentRepository.findById(id).ifPresent(doc -> {
            doc.setStatus(Document.Status.PROCESSING);
            documentRepository.save(doc);

            webClient.post()
                    .uri("/process")
                    .bodyValue(Map.of(
                            "documentId", id,
                            "fileUrl", fileUrl
                    ))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .subscribe();
        });
    }
}
