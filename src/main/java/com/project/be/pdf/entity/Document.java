package com.project.be.pdf.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long    id;

    private String  title;

    // 원본 파일 url
    private String  originalFileUrl;
    // 전처리 파일 url (초기엔 null)
    private String processedFileUrl;

    @OneToOne(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Summary summary;

    @Enumerated(EnumType.STRING)
    private Status status;

    private Long fileSize;

    private LocalDateTime createdAt;

    public enum Status {
        UPLOADED,       // 업로드 완료
        PROCESSING,     // 전처리 중
        COMPLETED,      // 완료
        FAILED          // 실패
    }
}
