package com.project.be.pdf.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.util.List;

@Setter
@Getter
@NoArgsConstructor
public class AiResponseDto {
    private List<String> chunks;
}
