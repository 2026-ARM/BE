package com.project.be.pdf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.be.pdf.dto.AiExtractResponseDto;
import com.project.be.pdf.dto.AiProcessResponseDto;
import com.project.be.pdf.entity.Document;
import com.project.be.pdf.entity.Summary;
import com.project.be.pdf.repository.DocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
public class AiService {

    private final DocumentRepository documentRepository;
    private final CloudinaryUploaderService cloudinaryUploaderService;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public AiService(DocumentRepository documentRepository,
                     CloudinaryUploaderService cloudinaryUploaderService,
                     ObjectMapper objectMapper,
                     WebClient.Builder webClientBuilder,
                     @Value("${ai.server.url}") String aiServerUrl) {
        this.documentRepository = documentRepository;
        this.cloudinaryUploaderService = cloudinaryUploaderService;
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder.baseUrl(aiServerUrl).build();
    }

    @Transactional
    public void requestProcessing(Long id, String fileUrl) {
        log.info("Requesting AI processing for document id: {}, url: {}", id, fileUrl);

        documentRepository.findById(id).ifPresent(doc -> {
            doc.setStatus(Document.Status.PROCESSING);

            // 1. 텍스트 추출 API 호출
            Mono<AiExtractResponseDto> extractMono = webClient.post()
                    .uri("/document/extract")
                    .bodyValue(Map.of("document_id", id, "url", fileUrl))
                    .retrieve()
                    .bodyToMono(AiExtractResponseDto.class);

            // 2. 요약 API 호출
            Mono<AiProcessResponseDto> processMono = webClient.post()
                    .uri("/process")
                    .bodyValue(Map.of("document_id", id,"url", fileUrl))
                    .retrieve()
                    .bodyToMono(AiProcessResponseDto.class);

            // 3. Mono.zip으로 두 '독립적인' API를 병렬로 동시에 호출하고, 두 응답이 모두 올 때까지 기다립니다.
            Mono.zip(extractMono, processMono)
                    .subscribe(
                            // 4. 두 응답이 모두 성공적으로 도착하면 이 블록이 실행됩니다.
                            tuple -> {
                                AiExtractResponseDto extractResponse = tuple.getT1(); // 첫 번째 응답 (chunks)
                                AiProcessResponseDto processResponse = tuple.getT2(); // 두 번째 응답 (summary)
                                updateDocumentWithAiResults(id, extractResponse, processResponse);
                            },
                            // 두 API 중 하나라도 실패하면 이 블록이 실행됩니다.
                            error -> handleProcessingError(id, "AI processing failed", error)
                    );
        });
    }

    @Transactional
    public void updateDocumentWithAiResults(Long docId, AiExtractResponseDto extractResponse, AiProcessResponseDto processResponse) {
        documentRepository.findById(docId).ifPresent(doc -> {
            try {
                log.info("Updating document with AI results for id: {}", docId);

                // Chunks를 Cloudinary에 .txt 파일로 업로드
                if (extractResponse.getChunks() != null && !extractResponse.getChunks().isEmpty()) {
                    String combinedText = String.join("\n\n--- (chunk end) ---\n\n", extractResponse.getChunks());
                    String txtFilename = "chunked_" + docId + ".txt";
                    String chunkedFileUrl = cloudinaryUploaderService.upload(combinedText, txtFilename);
                    doc.setProcessedFileUrl(chunkedFileUrl);
                    log.info("Uploaded chunked text for document id: {}. URL: {}", docId, chunkedFileUrl);
                }

                // 요약 응답을 Summary 엔티티로 변환
                String summaryJson = objectMapper.writeValueAsString(processResponse);
                Summary summary = Summary.builder()
                        .document(doc)
                        .summaryJson(summaryJson)
                        .createdAt(LocalDateTime.now())
                        .build();

                // DB에 모든 최종 결과를 한 번에 저장
                doc.setSummary(summary);
                doc.setStatus(Document.Status.COMPLETED);

                documentRepository.save(doc);
                log.info("Successfully processed and saved all results for document id: {}", docId);

            } catch (IOException e) {
                log.error("Failed to process AI response for document id: {}. Error: {}", docId, e.getMessage());
                doc.setStatus(Document.Status.FAILED);
                documentRepository.save(doc);
            }
        });
    }

    @Transactional
    public void handleProcessingError(Long docId, String message, Throwable error) {
        log.error("{} for document id: {}. Error: {}", docId, message, error.getMessage());
        documentRepository.findById(docId).ifPresent(doc -> {
            doc.setStatus(Document.Status.FAILED);
            documentRepository.save(doc);
        });
    }
}