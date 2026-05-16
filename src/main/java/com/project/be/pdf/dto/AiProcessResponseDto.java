package com.project.be.pdf.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AiProcessResponseDto {
    private Long documentId;
    private String type;
    private String generatedLanguage;
    private JsonNode summary;
}