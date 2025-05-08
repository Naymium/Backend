package com.spaceNav.nyamium.web.dto;

import com.spaceNav.nyamium.domain.enums.Prediction;
import com.spaceNav.nyamium.domain.enums.Save;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class PredictResponseDTO {

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Builder
    public static class ResultResponseDTO{
        private Long id;
        private Prediction prediction;
        private Float probability;
        private Float rangingError;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Builder
    public static class ResultResponseFileDTO{
        private List<ResultResponseDTO> results;
        private String fileName;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Builder
    public static class SaveResponseDTO{
        private List<Long> id;
        private String message; //저장 관련 메시지
    }
}
