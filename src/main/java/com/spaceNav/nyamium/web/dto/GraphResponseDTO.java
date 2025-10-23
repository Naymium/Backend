package com.spaceNav.nyamium.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class GraphResponseDTO {

    /* 그래프 데이터 */
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Builder
    public static class GraphResultResponseDTO {
        private Long id;

        private String normalDataUrl;
        private String abnormalDataUrl;

        private String normalDataKeyName;
        private String abnormalDataKeyName;

        private Long normalDataNum;
        private Long abnormalDataNum;
    }
}
