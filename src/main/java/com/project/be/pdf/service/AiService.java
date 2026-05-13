package com.project.be.pdf.service;

import com.project.be.pdf.entity.Document;
import com.project.be.pdf.repository.DocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Service
public class AiService {

    private final DocumentRepository documentRepository;
    private final WebClient webClient;

    // WebClient.Builder를 주입받아 AI 서버 주소로 기본 설정된 WebClient를 생성합니다.
    public AiService(DocumentRepository documentRepository, WebClient.Builder webClientBuilder, @Value("${ai.server.url}") String aiServerUrl) {
        this.documentRepository = documentRepository;
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
                    .bodyToMono(Void.class) // 응답 본문은 필요 없으므로 Void로 처리
                    .subscribe( // ⭐️ 중요: subscribe()를 호출해야 실제 요청이 전송됩니다.
                            null, // 성공 시 별도 작업 없음
                            error -> log.error("AI server request failed for document id: {}. Error: {}", id, error.getMessage()) // 실패 시 에러 로그
                    );
        });
    }
}