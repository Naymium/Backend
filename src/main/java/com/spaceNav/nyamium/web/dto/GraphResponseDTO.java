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
        private String imageUrl;
        private Long dataNum;
        private LocalDateTime createdAt;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Builder
    public static class SaveResponseDTO{
        private Long id;
        private String message; //저장 관련 메시지
    }
}
