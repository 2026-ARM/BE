package com.project.be.pdf.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
public class UpdateDto {
    private String processedFileUrl;

    public UpdateDto(String processedFileUrl) {
        this.processedFileUrl = processedFileUrl;
    }
}
