package com.project.be.pdf.service;

import com.project.be.pdf.dto.AiResponseDto;
import com.project.be.pdf.entity.Document;
import com.project.be.pdf.repository.DocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
public class AiService {

    private final DocumentRepository documentRepository;
    private final WebClient webClient;
    private final CloudinaryUploaderService cloudinaryUploaderService;

    // WebClient.Builder를 주입받아 AI 서버 주소로 기본 설정된 WebClient를 생성합니다.
    public AiService(DocumentRepository documentRepository, WebClient.Builder webClientBuilder,
                     CloudinaryUploaderService cloudinaryUploaderService,
                     @Value("${ai.server.url}") String aiServerUrl) {
        this.documentRepository = documentRepository;
        this.cloudinaryUploaderService = cloudinaryUploaderService;
        this.webClient = webClientBuilder.baseUrl(aiServerUrl).build();
    }

    /**
     * AI 서버에 문서 처리를 비동기적으로 요청합니다.
     * @param id 문서 ID
     * @param fileUrl Cloudinary에 업로드된 PDF 파일의 URL
     */
    public void requestProcessing(Long id, String fileUrl) {
        log.info("Requesting AI processing for document id: {}, url: {}", id, fileUrl);

        // 1. 문서 상태를 'PROCESSING'으로 변경
        documentRepository.findById(id).ifPresent(doc -> {
            doc.setStatus(Document.Status.PROCESSING);
            documentRepository.save(doc);

            // 2. WebClient를 사용하여 AI 서버에 POST 요청을 보냅니다.
            webClient.post()
                    .uri("/document/extract") // FastAPI의 엔드포인트
                    .bodyValue(Map.of(
                            "document_id", id, // FastAPI에서 사용할 수 있도록 document_id와 url을 JSON 본문에 담아 전송
                            "url", fileUrl
                    ))
                    .retrieve() // 응답을 받기 시작
                    .bodyToMono(AiResponseDto.class) // 응답을 AiDTO가 받음
                    .subscribe( // ⭐️ 중요: subscribe()를 호출해야 실제 요청이 전송됩니다.
                            aiResponse -> {
                                try {
                                    log.info("Successfully received chunks for document id: {}", id);
                                    // 1. chunk들을 하나의 문자열로 합칩니다.
                                    String combinedText = String.join("\n\n--- (chunk end) ---\n\n", aiResponse.getChunks());
                                    String txtFilename = "chunked_" + id + ".txt";

                                    // 2. 합쳐진 텍스트를 Cloudinary에 업로드합니다.
                                    String chunkedFileUrl = cloudinaryUploaderService.upload(combinedText, txtFilename);
                                    log.info("Uploaded chunked text for document id: {}. URL: {}", id, chunkedFileUrl);

                                    // 3. DB에 최종 처리된 URL과 상태를 업데이트합니다.
                                    doc.setProcessedFileUrl(chunkedFileUrl);
                                    doc.setStatus(Document.Status.COMPLETED);
                                    documentRepository.save(doc);
                                } catch (IOException e) {
                                    log.error("Failed to upload chunked text for document id: {}", id, e);
                                    // 업로드 실패 시 상태를 FAILED로 변경
                                    doc.setStatus(Document.Status.FAILED);
                                    documentRepository.save(doc);
                                }
                            },
                            error -> {
                                log.error("AI server request failed for document id: {}. Error: {}", id, error.getMessage());
                                doc.setStatus(Document.Status.FAILED);
                                documentRepository.save(doc);
                            }
                    );
        });
    }
}